import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideArrowLeft,
  lucideBriefcase,
  lucideClock,
  lucideDownload,
  lucideGraduationCap,
  lucideMonitorSmartphone,
  lucideRadio,
  lucideSearch,
  lucideUserCheck,
  lucideUsers,
} from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmTableImports } from '@spartan-ng/helm/table';
import { Subject, debounceTime, distinctUntilChanged, filter, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  EventsApiService,
  eventPhotoUrl,
  type EventAttendanceLog,
  type EventAttendanceStats,
  type EventRecord,
} from '../../core/events/events-api.service';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-event-attendance',
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    RouterLink,
    NgIcon,
    HlmButton,
    HlmCardImports,
    HlmInput,
    HlmTableImports,
  ],
  viewProviders: [
    provideIcons({
      lucideArrowLeft,
      lucideBriefcase,
      lucideClock,
      lucideDownload,
      lucideGraduationCap,
      lucideMonitorSmartphone,
      lucideRadio,
      lucideSearch,
      lucideUserCheck,
      lucideUsers,
    }),
  ],
  templateUrl: './event-attendance.html',
  host: { class: 'block w-full' },
})
export class EventAttendance {
  private readonly api = inject(EventsApiService);
  private readonly notifications = inject(NotificationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly eventId = signal('');
  protected readonly event = signal<EventRecord | null>(null);
  protected readonly logs = signal<EventAttendanceLog[]>([]);
  protected readonly total = signal(0);
  protected readonly stats = signal<EventAttendanceStats | null>(null);
  protected readonly loading = signal(false);
  protected readonly statsLoading = signal(false);
  protected readonly exporting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly search = signal('');
  protected readonly liveFlash = signal(false);
  protected readonly photoUrl = eventPhotoUrl;
  protected readonly liveConnected = this.notifications.connected;
  private readonly searchChanges = new Subject<string>();
  private liveFlashTimer?: ReturnType<typeof setTimeout>;

  protected readonly title = computed(
    () => this.event()?.title ?? 'Event attendance',
  );

  protected readonly kioskActive = computed(() => {
    const id = this.eventId();
    return !!id && this.notifications.hasKioskFor(id);
  });

  protected readonly kioskCount = computed(() => {
    const id = this.eventId();
    return id ? this.notifications.kioskCountFor(id) : 0;
  });

  protected readonly filteredLogs = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.logs();
    }
    return this.logs().filter((log) => {
      const hay = [log.personName, log.personNo, log.rfid, log.personType]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return hay.includes(term);
    });
  });

  protected readonly studentShare = computed(() => {
    const s = this.stats();
    const total = (s?.studentAttendees ?? 0) + (s?.employeeAttendees ?? 0);
    if (total <= 0) {
      return 0;
    }
    return Math.round(((s?.studentAttendees ?? 0) / total) * 100);
  });

  protected readonly employeeShare = computed(() => {
    const s = this.stats();
    const total = (s?.studentAttendees ?? 0) + (s?.employeeAttendees ?? 0);
    return total <= 0 ? 0 : 100 - this.studentShare();
  });

  protected readonly maxHourly = computed(() => {
    const hours = this.stats()?.checkInsByHour ?? [];
    return Math.max(1, ...hours.map((h) => h.count));
  });

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const id = params.get('id') ?? '';
      this.eventId.set(id);
      if (id) {
        this.loadEvent(id);
        this.loadLogs(id);
        this.loadStats(id);
      }
    });

    this.searchChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe(() => {
        /* Client-side filter via filteredLogs. */
      });

    this.notifications.events$
      .pipe(
        filter((e) => e.type === 'EVENT_ATTENDANCE_TAP'),
        takeUntilDestroyed(),
      )
      .subscribe((event) => {
        const payload = event.payload as EventAttendanceLog | null;
        if (!payload || String(payload.eventId) !== String(this.eventId())) {
          return;
        }
        this.applyLiveTap(payload);
      });
  }

  protected onSearchChange(term: string): void {
    this.search.set(term);
    this.searchChanges.next(term.trim());
  }

  protected hourBarHeight(count: number): string {
    return `${Math.max(8, Math.round((count / this.maxHourly()) * 100))}%`;
  }

  protected actionBadgeClass(action: string): string {
    return action === 'TIME_IN'
      ? 'bg-emerald-500/10 text-emerald-600'
      : 'bg-amber-500/10 text-amber-600';
  }

  protected exportCsv(): void {
    const id = this.eventId();
    if (!id || this.exporting()) {
      return;
    }
    this.exporting.set(true);
    this.api.exportAttendanceCsv(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        const safeTitle = (this.event()?.title ?? 'event')
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-|-$/g, '');
        anchor.href = url;
        anchor.download = `${safeTitle || 'event'}-attendance.csv`;
        anchor.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.exporting.set(false);
        this.error.set(err?.error?.message ?? 'Failed to export attendance CSV');
      },
    });
  }

  private applyLiveTap(tap: EventAttendanceLog): void {
    let added = false;
    this.logs.update((list) => {
      const idx = list.findIndex((row) => row.id === tap.id);
      if (idx >= 0) {
        const next = [...list];
        next[idx] = { ...next[idx], ...tap };
        return next;
      }
      added = true;
      return [tap, ...list];
    });
    if (added) {
      this.total.update((n) => n + 1);
    }
    this.loadStats(this.eventId());
    this.liveFlash.set(true);
    clearTimeout(this.liveFlashTimer);
    this.liveFlashTimer = setTimeout(() => this.liveFlash.set(false), 1200);
  }

  private loadEvent(id: string): void {
    this.api.getById(id).subscribe({
      next: (event) => this.event.set(event),
      error: () => this.event.set(null),
    });
  }

  private loadLogs(eventId: string): void {
    if (!eventId) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.api.listAttendance(eventId, { offset: 0, limit: 200 }).subscribe({
      next: (page) => {
        this.logs.set(page.items);
        this.total.set(page.total);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load attendance logs');
      },
    });
  }

  private loadStats(eventId: string): void {
    this.statsLoading.set(true);
    this.api
      .attendanceStats(eventId)
      .pipe(catchError(() => of(null)))
      .subscribe((stats) => {
        this.stats.set(stats);
        this.statsLoading.set(false);
      });
  }
}
