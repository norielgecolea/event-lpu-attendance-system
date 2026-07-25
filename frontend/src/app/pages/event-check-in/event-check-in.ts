import { DatePipe, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  PLATFORM_ID,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideArrowLeft,
  lucideCalendarDays,
  lucideCircleAlert,
  lucideClock,
  lucideMapPin,
  lucideScanBarcode,
  lucideUserRound,
} from '@ng-icons/lucide';
import { filter } from 'rxjs';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  EventsApiService,
  eventPhotoUrl,
  type EventAttendanceLog,
  type EventRecord,
} from '../../core/events/events-api.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { studentPhotoUrl } from '../../core/students/student-photo.util';
import { EventKioskSounds } from './event-kiosk-sounds';
import { EventTonesApiService } from '../../core/settings/event-tones-api.service';

type WindowStatus = 'loading' | 'not_started' | 'open' | 'ended' | 'missing';
type Flash = 'idle' | 'in' | 'out' | 'error' | 'birthday';

interface ConfettiPiece {
  left: string;
  color: string;
  delay: string;
  duration: string;
  size: string;
  drift: string;
  spin: string;
}

interface BalloonPiece {
  left: string;
  color: string;
  delay: string;
  duration: string;
  scale: number;
}

interface FireworkBurst {
  left: string;
  bottom: string;
  delay: string;
  color: string;
  sparks: { angle: number; distance: string; size: string }[];
}

@Component({
  selector: 'app-event-check-in',
  imports: [DatePipe, FormsModule, RouterLink, NgIcon, HlmButton],
  viewProviders: [
    provideIcons({
      lucideArrowLeft,
      lucideCalendarDays,
      lucideCircleAlert,
      lucideClock,
      lucideMapPin,
      lucideScanBarcode,
      lucideUserRound,
    }),
  ],
  templateUrl: './event-check-in.html',
  styleUrl: './event-check-in.css',
  host: { class: 'event-kiosk-host' },
})
export class EventCheckIn implements AfterViewInit, OnDestroy {
  @ViewChild('idInput') private readonly idInput?: ElementRef<HTMLInputElement>;

  private readonly api = inject(EventsApiService);
  private readonly notifications = inject(NotificationService);
  private readonly tonesApi = inject(EventTonesApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly sounds = new EventKioskSounds();

  protected readonly event = signal<EventRecord | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly identifier = signal('');
  protected readonly tapError = signal<string | null>(null);
  protected readonly errorNotFound = signal(false);
  protected readonly lastTap = signal<EventAttendanceLog | null>(null);
  protected readonly flash = signal<Flash>('idle');
  protected readonly animKey = signal(0);
  protected readonly clock = signal(new Date());
  protected readonly nowTick = signal(Date.now());
  protected readonly confetti = signal<ConfettiPiece[]>([]);
  protected readonly balloons = signal<BalloonPiece[]>([]);
  protected readonly fireworks = signal<FireworkBurst[]>([]);
  protected readonly eventPhoto = eventPhotoUrl;
  protected readonly personPhoto = studentPhotoUrl;

  private clockTimer?: ReturnType<typeof setInterval>;
  private tickTimer?: ReturnType<typeof setInterval>;
  private focusTimer?: ReturnType<typeof setInterval>;
  private clearTimer?: ReturnType<typeof setTimeout>;
  private lastScanId: string | null = null;
  private lastScanAt = 0;
  private inFlight = 0;

  protected readonly windowStatus = computed<WindowStatus>(() => {
    const event = this.event();
    if (this.loading()) {
      return 'loading';
    }
    if (!event) {
      return 'missing';
    }
    if (!event.active) {
      return 'ended';
    }
    const now = this.nowTick();
    const start = new Date(event.startsAt).getTime();
    const end = event.endsAt ? new Date(event.endsAt).getTime() : null;
    if (now < start) {
      return 'not_started';
    }
    if (end != null && now > end) {
      return 'ended';
    }
    return 'open';
  });

  /** Input stays enabled while open — taps are not blocked by in-flight requests. */
  protected readonly canTap = computed(() => this.windowStatus() === 'open');

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.clockTimer = setInterval(() => this.clock.set(new Date()), 1000);
      this.tickTimer = setInterval(() => this.nowTick.set(Date.now()), 250);
      this.loadToneSettings();
    }

    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id');
      if (!id || !isPlatformBrowser(this.platformId)) {
        this.loading.set(false);
        return;
      }
      this.loadEvent(id);
    });

    this.notifications.events$
      .pipe(
        filter((e) => e.type === 'EVENT_UPDATED' || e.type === 'EVENT_TONES_CHANGED'),
        takeUntilDestroyed(),
      )
      .subscribe((message) => {
        if (message.type === 'EVENT_TONES_CHANGED') {
          this.loadToneSettings();
          return;
        }
        const payload = message.payload as EventRecord | null;
        if (!payload?.id) {
          return;
        }
        const current = this.event();
        if (!current || String(payload.id) !== String(current.id)) {
          return;
        }
        this.event.set({
          ...current,
          ...payload,
          id: String(payload.id),
        });
        this.nowTick.set(Date.now());
        queueMicrotask(() => this.focusInput());
      });
  }

  ngAfterViewInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.focusInput();
      this.focusTimer = setInterval(() => this.focusInput(), 400);
    }
  }

  ngOnDestroy(): void {
    clearInterval(this.clockTimer);
    clearInterval(this.tickTimer);
    clearInterval(this.focusTimer);
    clearTimeout(this.clearTimer);
    this.clearParty();
    this.notifications.disconnectEventKiosk();
  }

  protected onSubmit(): void {
    const event = this.event();
    const identifier = this.identifier().trim();

    // Clear immediately so the next RFID scan can land without waiting on the network.
    this.identifier.set('');
    queueMicrotask(() => this.focusInput());

    if (!event || !this.canTap() || !identifier) {
      return;
    }

    const now = Date.now();
    if (identifier === this.lastScanId && now - this.lastScanAt < 750) {
      return;
    }
    this.lastScanId = identifier;
    this.lastScanAt = now;
    this.tapError.set(null);
    this.errorNotFound.set(false);
    this.inFlight += 1;

    this.api.publicTap(event.id, identifier).subscribe({
      next: (log) => {
        this.inFlight = Math.max(0, this.inFlight - 1);
        this.lastTap.set(log);
        this.errorNotFound.set(false);
        this.flash.set(
          log.birthday ? 'birthday' : log.lastAction === 'TIME_OUT' ? 'out' : 'in',
        );
        this.animKey.update((k) => k + 1);
        if (log.birthday) {
          this.launchParty();
        } else {
          this.clearParty();
        }
        // Double-tap / cooldown: show last transaction without replaying tones.
        if (!log.duplicate) {
          this.playTapSound(log);
        }
        this.scheduleClear();
        this.focusInput();
      },
      error: (err: { error?: { message?: string }; status?: number }) => {
        this.inFlight = Math.max(0, this.inFlight - 1);
        this.lastTap.set(null);
        this.clearParty();
        this.flash.set('error');
        this.animKey.update((k) => k + 1);
        const notFound = err?.status === 404;
        this.errorNotFound.set(notFound);
        this.tapError.set(err?.error?.message ?? 'Tap failed. Please try again.');
        if (notFound) {
          this.sounds.playNotFound();
        } else {
          this.sounds.playError();
        }
        this.scheduleClear();
        this.focusInput();
      },
    });
  }

  private launchParty(): void {
    const colors = ['#ef4444', '#f59e0b', '#22c55e', '#3b82f6', '#a855f7', '#ec4899', '#eab308', '#14b8a6'];
    const pick = <T>(arr: T[]): T => arr[Math.floor(Math.random() * arr.length)]!;

    this.confetti.set(
      Array.from({ length: 96 }, () => ({
        left: `${Math.random() * 100}%`,
        color: pick(colors),
        delay: `${Math.random() * 2.2}s`,
        duration: `${2.6 + Math.random() * 2.4}s`,
        size: `${6 + Math.random() * 8}px`,
        drift: `${(Math.random() - 0.5) * 200}px`,
        spin: `${540 + Math.random() * 720}deg`,
      })),
    );

    this.balloons.set(
      Array.from({ length: 12 }, (_, i) => ({
        left: `${3 + i * 8 + Math.random() * 4}%`,
        color: pick(colors),
        delay: `${Math.random() * 1.8}s`,
        duration: `${4.2 + Math.random() * 3.2}s`,
        scale: 0.72 + Math.random() * 0.55,
      })),
    );

    this.fireworks.set(
      Array.from({ length: 5 }, (_, i) => ({
        left: `${12 + i * 18 + Math.random() * 6}%`,
        bottom: `${18 + Math.random() * 28}%`,
        delay: `${i * 0.35 + Math.random() * 0.4}s`,
        color: pick(colors),
        sparks: Array.from({ length: 14 }, (__, s) => ({
          angle: (360 / 14) * s + Math.random() * 12,
          distance: `${42 + Math.random() * 38}px`,
          size: `${3 + Math.random() * 4}px`,
        })),
      })),
    );
  }

  private clearParty(): void {
    this.confetti.set([]);
    this.balloons.set([]);
    this.fireworks.set([]);
  }

  private playTapSound(log: EventAttendanceLog): void {
    if (log.birthday) {
      this.sounds.playBirthday();
    } else if (log.lastAction === 'TIME_OUT') {
      this.sounds.playTimeOut();
    } else {
      this.sounds.playTimeIn();
    }
  }

  private loadToneSettings(): void {
    this.tonesApi.getSettings().subscribe({
      next: (settings) => this.sounds.applySettings(settings),
      error: () => undefined,
    });
  }

  private scheduleClear(): void {
    clearTimeout(this.clearTimer);
    this.clearTimer = setTimeout(() => {
      if (this.inFlight > 0) {
        return;
      }
      this.lastTap.set(null);
      this.tapError.set(null);
      this.errorNotFound.set(false);
      this.flash.set('idle');
      this.clearParty();
    }, 4200);
  }

  private focusInput(): void {
    const el = this.idInput?.nativeElement;
    if (!el || el.disabled) {
      return;
    }
    if (document.activeElement !== el) {
      el.focus({ preventScroll: true });
    }
  }

  private loadEvent(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getPublic(id).subscribe({
      next: (event) => {
        this.event.set(event);
        this.loading.set(false);
        this.notifications.connectEventKiosk(event.id);
        queueMicrotask(() => this.focusInput());
      },
      error: (err: { error?: { message?: string } }) => {
        this.event.set(null);
        this.loading.set(false);
        this.notifications.disconnectEventKiosk();
        this.error.set(err?.error?.message ?? 'Event not found');
      },
    });
  }
}
