import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBriefcase,
  lucideCalendarCheck,
  lucideCalendarClock,
  lucideCalendarDays,
  lucideGraduationCap,
  lucideRadio,
  lucideUsers,
  lucideUserCheck,
} from '@ng-icons/lucide';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import { Subject, debounceTime, filter } from 'rxjs';
import {
  DashboardApiService,
  type DashboardSummary,
} from '../../core/dashboard/dashboard-api.service';
import {
  EventsApiService,
  type EventAttendanceLog,
} from '../../core/events/events-api.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { studentPhotoUrl } from '../../core/students/student-photo.util';

interface StatCard {
  label: string;
  value: number;
  icon: string;
  tone: 'primary' | 'emerald' | 'amber' | 'sky' | 'violet' | 'rose';
  caption: string | null;
}

@Component({
  selector: 'app-dashboard',
  imports: [
    DatePipe,
    DecimalPipe,
    NgClass,
    RouterLink,
    NgIcon,
    HlmAvatarImports,
    HlmButton,
    HlmCardImports,
  ],
  viewProviders: [
    provideIcons({
      lucideActivity,
      lucideBriefcase,
      lucideCalendarCheck,
      lucideCalendarClock,
      lucideCalendarDays,
      lucideGraduationCap,
      lucideRadio,
      lucideUsers,
      lucideUserCheck,
    }),
  ],
  templateUrl: './dashboard.html',
  styles: `
    @keyframes tap-card-in {
      from {
        opacity: 0;
        transform: translateY(10px) scale(0.97);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }

    .tap-card {
      animation: tap-card-in 0.5s cubic-bezier(0.22, 1, 0.36, 1) both;
    }

    .tap-card .tap-badge {
      animation: tap-card-in 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
    }

    @media (prefers-reduced-motion: reduce) {
      .tap-card,
      .tap-card .tap-badge {
        animation: none;
      }
    }
  `,
})
export class Dashboard {
  private static readonly RECENT_LIMIT = 5;

  private readonly api = inject(DashboardApiService);
  private readonly attendanceApi = inject(EventsApiService);
  private readonly notifications = inject(NotificationService);
  private readonly refresh$ = new Subject<void>();

  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly recentTaps = signal<EventAttendanceLog[]>([]);
  protected readonly tapsLoading = signal(true);
  protected readonly liveConnected = this.notifications.connected;

  protected readonly todayLabel = new Date().toLocaleDateString('en-PH', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
    timeZone: 'Asia/Manila',
  });

  protected readonly stats = computed<StatCard[]>(() => {
    const s = this.summary();
    return [
      {
        label: 'Total Events',
        value: s?.totalEvents ?? 0,
        icon: 'lucideCalendarDays',
        tone: 'primary',
        caption: 'All events in the system',
      },
      {
        label: 'Active Events',
        value: s?.activeEvents ?? 0,
        icon: 'lucideCalendarCheck',
        tone: 'emerald',
        caption: 'Open for attendance',
      },
      {
        label: 'Total Students',
        value: s?.totalStudents ?? 0,
        icon: 'lucideGraduationCap',
        tone: 'amber',
        caption: 'From gate attendance records',
      },
      {
        label: 'Total Employees',
        value: s?.totalEmployees ?? 0,
        icon: 'lucideBriefcase',
        tone: 'sky',
        caption: 'From gate attendance records',
      },
      {
        label: 'Total Check-ins',
        value: s?.totalCheckIns ?? 0,
        icon: 'lucideActivity',
        tone: 'violet',
        caption: 'Unique attendees across events',
      },
      {
        label: 'Currently Checked In',
        value: s?.currentlyCheckedIn ?? 0,
        icon: 'lucideUserCheck',
        tone: 'rose',
        caption: 'Last action is Time In',
      },
    ];
  });

  protected readonly studentShare = computed(() => {
    const s = this.summary();
    const total = (s?.studentCheckIns ?? 0) + (s?.employeeCheckIns ?? 0);
    if (total <= 0) {
      return 0;
    }
    return Math.round(((s?.studentCheckIns ?? 0) / total) * 100);
  });

  protected readonly employeeShare = computed(() => {
    const share = this.studentShare();
    const s = this.summary();
    const total = (s?.studentCheckIns ?? 0) + (s?.employeeCheckIns ?? 0);
    return total <= 0 ? 0 : 100 - share;
  });

  protected readonly maxTopAttendance = computed(() => {
    const top = this.summary()?.topEventsByAttendance ?? [];
    return Math.max(1, ...top.map((e) => e.attendees));
  });

  constructor() {
    this.loadSummary(true);
    this.loadRecentTaps();

    this.refresh$
      .pipe(debounceTime(400), takeUntilDestroyed())
      .subscribe(() => this.loadSummary(false));

    this.notifications.events$
      .pipe(
        filter(
          (e) =>
            e.type === 'EVENT_ATTENDANCE_TAP' || e.type === 'EVENT_UPDATED',
        ),
        takeUntilDestroyed(),
      )
      .subscribe((event) => {
        if (event.type === 'EVENT_ATTENDANCE_TAP') {
          const tap = event.payload as EventAttendanceLog | undefined;
          if (tap?.id) {
            this.recentTaps.update((list) =>
              [tap, ...list.filter((t) => t.id !== tap.id)].slice(
                0,
                Dashboard.RECENT_LIMIT,
              ),
            );
          }
        }
        this.refresh$.next();
      });
  }

  protected barWidth(attendees: number): string {
    return `${Math.max(4, Math.round((attendees / this.maxTopAttendance()) * 100))}%`;
  }

  protected isStudent(tap: EventAttendanceLog): boolean {
    return tap.personType === 'STUDENT';
  }

  protected tapTime(tap: EventAttendanceLog): string {
    return tap.lastAction === 'TIME_OUT' && tap.timeOut
      ? tap.timeOut
      : (tap.timeIn ?? tap.updatedAt ?? '');
  }

  protected initials(name: string | null | undefined): string {
    const parts = (name ?? '').replace(',', '').trim().split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase();
  }

  protected photoSrc(photo: string | null | undefined): string | null {
    return studentPhotoUrl(photo);
  }

  private loadRecentTaps(): void {
    this.attendanceApi.recentAttendance(Dashboard.RECENT_LIMIT).subscribe({
      next: (taps) => {
        this.recentTaps.set(taps);
        this.tapsLoading.set(false);
      },
      error: () => this.tapsLoading.set(false),
    });
  }

  /** Initial load shows placeholders; live WS refreshes update numbers in place. */
  private loadSummary(showLoading: boolean): void {
    if (showLoading) {
      this.loading.set(true);
      this.error.set(null);
    }
    this.api.summary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
        this.error.set(null);
      },
      error: (err: { error?: { message?: string } }) => {
        if (showLoading) {
          this.loading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to load dashboard summary');
        }
      },
    });
  }
}
