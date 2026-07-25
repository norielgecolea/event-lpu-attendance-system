import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthEventMessage } from '../auth/auth.models';

export interface EventKioskStatus {
  active: boolean;
  kioskCount: number;
  activeEventIds: string[];
  kioskCounts: Record<string, number>;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);
  private socket: WebSocket | null = null;
  private kioskSocket: WebSocket | null = null;
  private token: string | null = null;
  private kioskEventId: string | null = null;
  private reconnectTimer?: ReturnType<typeof setTimeout>;
  private kioskReconnectTimer?: ReturnType<typeof setTimeout>;
  private reconnectAttempt = 0;
  private kioskReconnectAttempt = 0;
  private intentionalClose = false;
  private intentionalKioskClose = false;
  private readonly eventsSubject = new Subject<AuthEventMessage>();
  private presenceFromSocket = false;

  readonly events$ = this.eventsSubject.asObservable();
  readonly latestEvent = signal<AuthEventMessage | null>(null);
  readonly connected = signal(false);
  /** Event IDs that currently have at least one open attendance kiosk. */
  readonly onlineEventKioskIds = signal<string[]>([]);
  readonly eventKioskCounts = signal<Record<string, number>>({});

  connect(token: string): void {
    if (!isPlatformBrowser(this.platformId) || !token) {
      return;
    }
    this.token = token;
    this.intentionalClose = false;
    this.clearReconnect();
    this.openSocket();
  }

  disconnect(): void {
    this.intentionalClose = true;
    this.clearReconnect();
    this.token = null;
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
    this.connected.set(false);
    this.presenceFromSocket = false;
    this.onlineEventKioskIds.set([]);
    this.eventKioskCounts.set({});
  }

  /** Public check-in page: announce this browser as an active kiosk for the event. */
  connectEventKiosk(eventId: string): void {
    if (!isPlatformBrowser(this.platformId) || !eventId) {
      return;
    }
    this.kioskEventId = String(eventId);
    this.intentionalKioskClose = false;
    this.clearKioskReconnect();
    this.openKioskSocket();
  }

  disconnectEventKiosk(): void {
    this.intentionalKioskClose = true;
    this.clearKioskReconnect();
    this.kioskEventId = null;
    if (this.kioskSocket) {
      this.kioskSocket.close();
      this.kioskSocket = null;
    }
  }

  kioskCountFor(eventId: string): number {
    return this.eventKioskCounts()[String(eventId)] ?? 0;
  }

  hasKioskFor(eventId: string): boolean {
    return this.kioskCountFor(eventId) > 0;
  }

  dismissLatest(): void {
    this.latestEvent.set(null);
  }

  private openSocket(): void {
    if (!this.token) {
      return;
    }
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.onmessage = null;
      this.socket.onopen = null;
      this.socket.close();
      this.socket = null;
    }

    this.presenceFromSocket = false;
    const url = `${environment.wsUrl}?token=${encodeURIComponent(this.token)}`;
    this.socket = new WebSocket(url);

    this.socket.onopen = () => {
      this.reconnectAttempt = 0;
      this.connected.set(true);
      this.refreshKioskStatus();
    };

    this.socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data as string) as AuthEventMessage;
        this.latestEvent.set(payload);
        this.eventsSubject.next(payload);
        if (payload.type === 'EVENT_KIOSK_PRESENCE') {
          this.applyKioskPresence(payload);
        }
      } catch {
        // ignore malformed payloads
      }
    };

    this.socket.onclose = () => {
      this.connected.set(false);
      this.socket = null;
      this.scheduleReconnect();
    };

    this.socket.onerror = () => {
      this.connected.set(false);
    };
  }

  private openKioskSocket(): void {
    if (!this.kioskEventId) {
      return;
    }
    if (this.kioskSocket) {
      this.kioskSocket.onclose = null;
      this.kioskSocket.onerror = null;
      this.kioskSocket.onmessage = null;
      this.kioskSocket.onopen = null;
      this.kioskSocket.close();
      this.kioskSocket = null;
    }

    const base = environment.wsUrl.includes('/ws/notifications')
      ? environment.wsUrl.replace('/ws/notifications', '/ws/event-kiosk')
      : `${environment.contextPath}/ws/event-kiosk`;
    const url = `${base}?eventId=${encodeURIComponent(this.kioskEventId)}`;
    this.kioskSocket = new WebSocket(url);

    this.kioskSocket.onopen = () => {
      this.kioskReconnectAttempt = 0;
    };

    this.kioskSocket.onclose = () => {
      this.kioskSocket = null;
      this.scheduleKioskReconnect();
    };

    this.kioskSocket.onerror = () => {
      // reconnect via onclose
    };
  }

  private refreshKioskStatus(): void {
    this.http
      .get<EventKioskStatus>(`${environment.apiBaseUrl}/event-attendance/kiosk-status`)
      .subscribe({
        next: (status) => {
          if (!this.presenceFromSocket) {
            this.onlineEventKioskIds.set((status.activeEventIds ?? []).map(String));
            this.eventKioskCounts.set(status.kioskCounts ?? {});
          }
        },
        error: () => undefined,
      });
  }

  private applyKioskPresence(payload: AuthEventMessage): void {
    this.presenceFromSocket = true;
    const ids = (payload.eventIds ?? []).map((id) => String(id).trim()).filter(Boolean);
    this.onlineEventKioskIds.set(ids);
    const counts: Record<string, number> = {};
    if (payload.kioskCounts && typeof payload.kioskCounts === 'object') {
      for (const [key, value] of Object.entries(payload.kioskCounts)) {
        counts[String(key)] = Number(value) || 0;
      }
    } else {
      for (const id of ids) {
        counts[id] = 1;
      }
    }
    this.eventKioskCounts.set(counts);
  }

  private scheduleReconnect(): void {
    if (this.intentionalClose || !this.token) {
      return;
    }
    this.clearReconnect();
    const delay = Math.min(1000 * 2 ** this.reconnectAttempt, 15000);
    this.reconnectAttempt += 1;
    this.reconnectTimer = setTimeout(() => this.openSocket(), delay);
  }

  private scheduleKioskReconnect(): void {
    if (this.intentionalKioskClose || !this.kioskEventId) {
      return;
    }
    this.clearKioskReconnect();
    const delay = Math.min(1000 * 2 ** this.kioskReconnectAttempt, 15000);
    this.kioskReconnectAttempt += 1;
    this.kioskReconnectTimer = setTimeout(() => this.openKioskSocket(), delay);
  }

  private clearReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = undefined;
    }
  }

  private clearKioskReconnect(): void {
    if (this.kioskReconnectTimer) {
      clearTimeout(this.kioskReconnectTimer);
      this.kioskReconnectTimer = undefined;
    }
  }
}
