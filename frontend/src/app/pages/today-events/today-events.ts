import { DatePipe, isPlatformBrowser } from '@angular/common';
import { Component, PLATFORM_ID, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideArrowLeft, lucideCalendarDays, lucideMapPin } from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  EventsApiService,
  eventPhotoUrl,
  type EventRecord,
} from '../../core/events/events-api.service';

@Component({
  selector: 'app-today-events',
  imports: [DatePipe, RouterLink, NgIcon, HlmButton],
  viewProviders: [provideIcons({ lucideArrowLeft, lucideCalendarDays, lucideMapPin })],
  templateUrl: './today-events.html',
  styles: `
    @keyframes today-rise {
      from {
        opacity: 0;
        transform: translateY(12px);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }
    .today-card {
      animation: today-rise 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
    }
    @media (prefers-reduced-motion: reduce) {
      .today-card {
        animation: none;
      }
    }
  `,
})
export class TodayEvents {
  private readonly api = inject(EventsApiService);
  private readonly platformId = inject(PLATFORM_ID);

  protected readonly events = signal<EventRecord[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly todayLabel = new Intl.DateTimeFormat('en-PH', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    timeZone: 'Asia/Manila',
  }).format(new Date());
  protected readonly photoUrl = eventPhotoUrl;

  constructor() {
    if (!isPlatformBrowser(this.platformId)) {
      this.loading.set(false);
      return;
    }
    this.api.listToday().subscribe({
      next: (events) => {
        this.events.set(events);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load today’s events');
      },
    });
  }
}
