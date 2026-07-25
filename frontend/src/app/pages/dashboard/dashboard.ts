import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideActivity,
  lucideBriefcase,
  lucideCalendarCheck,
  lucideCalendarClock,
  lucideCalendarDays,
  lucideGraduationCap,
  lucideUsers,
  lucideUserCheck,
} from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmCardImports } from '@spartan-ng/helm/card';
import {
  DashboardApiService,
  type DashboardSummary,
} from '../../core/dashboard/dashboard-api.service';

interface StatCard {
  label: string;
  value: number;
  icon: string;
  tone: 'primary' | 'emerald' | 'amber' | 'sky' | 'violet' | 'rose';
  caption: string | null;
}

@Component({
  selector: 'app-dashboard',
  imports: [DatePipe, DecimalPipe, RouterLink, NgIcon, HlmButton, HlmCardImports],
  viewProviders: [
    provideIcons({
      lucideActivity,
      lucideBriefcase,
      lucideCalendarCheck,
      lucideCalendarClock,
      lucideCalendarDays,
      lucideGraduationCap,
      lucideUsers,
      lucideUserCheck,
    }),
  ],
  templateUrl: './dashboard.html',
})
export class Dashboard {
  private readonly api = inject(DashboardApiService);

  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

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
    this.api.summary().subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load dashboard summary');
      },
    });
  }

  protected barWidth(attendees: number): string {
    return `${Math.max(4, Math.round((attendees / this.maxTopAttendance()) * 100))}%`;
  }
}
