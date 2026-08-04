import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideClipboardList,
  lucidePlus,
  lucidePower,
  lucideSearch,
  lucideSquarePen,
  lucideTrash2,
} from '@ng-icons/lucide';
import { HlmBadge } from '@spartan-ng/helm/badge';
import { HlmButton } from '@spartan-ng/helm/button';
import { HlmDialogService } from '@spartan-ng/helm/dialog';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmTableImports } from '@spartan-ng/helm/table';
import { filter, take } from 'rxjs';
import { EventsApiService, eventPhotoUrl, type EventPayload, type EventRecord } from '../../core/events/events-api.service';
import { AuthService } from '../../core/auth/auth.service';
import { canDeleteEvents, canEditEvents, canToggleEventActive } from '../../core/auth/role-access';
import { MonthSelector } from '../../shared/ui/month-selector/month-selector';
import { EventFormDialog } from './event-form-dialog';

@Component({
  selector: 'app-events',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    NgIcon,
    HlmButton,
    HlmInput,
    HlmBadge,
    HlmTableImports,
    MonthSelector,
  ],
  viewProviders: [
    provideIcons({
      lucidePlus,
      lucideSearch,
      lucideSquarePen,
      lucideClipboardList,
      lucideTrash2,
      lucidePower,
    }),
  ],
  templateUrl: './events.html',
  host: { class: 'flex h-full flex-col' },
})
export class Events {
  private readonly api = inject(EventsApiService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(HlmDialogService);

  protected readonly events = signal<EventRecord[]>([]);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly busyId = signal<string | null>(null);
  protected readonly search = signal('');
  /** YYYY-MM in Asia/Manila calendar month. */
  protected readonly month = signal(currentMonthValue());
  protected readonly photoUrl = eventPhotoUrl;
  protected readonly canDelete = computed(() => canDeleteEvents(this.auth.user()?.role));
  protected readonly canEdit = computed(() => canEditEvents(this.auth.user()?.role));
  protected readonly canToggleActive = computed(() => canToggleEventActive(this.auth.user()?.role));
  protected readonly canCreate = computed(
    () =>
      this.auth.isSuperAdmin() || this.auth.isOsas() || this.auth.isEventMaker(),
  );

  protected readonly monthLabel = computed(() => formatMonthLabel(this.month()));

  protected readonly filtered = computed(() => {
    const term = this.search().trim().toLowerCase();
    if (!term) {
      return this.events();
    }
    return this.events().filter((e) =>
      [e.title, e.description ?? '', e.location ?? ''].join(' ').toLowerCase().includes(term),
    );
  });

  constructor() {
    this.reload();
  }

  protected onMonthChange(value: string): void {
    if (!/^\d{4}-\d{2}$/.test(value) || value === this.month()) {
      return;
    }
    this.month.set(value);
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.error.set(null);
    const { year, month } = parseMonthValue(this.month());
    this.api.list({ year, month }).subscribe({
      next: (events) => {
        this.events.set(events);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load events');
      },
    });
  }

  protected openCreate(): void {
    this.openForm('create');
  }

  protected openEdit(event: EventRecord): void {
    if (!this.canEdit()) {
      return;
    }
    this.openForm('edit', event);
  }

  protected toggleActive(event: EventRecord): void {
    if (!this.canToggleActive()) {
      return;
    }
    const next = !event.active;
    const label = next ? 'activate' : 'deactivate';
    if (!confirm(`${next ? 'Activate' : 'Deactivate'} event "${event.title}"?`)) {
      return;
    }
    this.error.set(null);
    this.busyId.set(event.id);
    this.api.setActive(event.id, next).subscribe({
      next: (saved) => {
        this.events.update((list) => list.map((e) => (e.id === saved.id ? saved : e)));
        this.busyId.set(null);
      },
      error: (err: { error?: { message?: string } }) => {
        this.busyId.set(null);
        this.error.set(err?.error?.message ?? `Failed to ${label} event`);
      },
    });
  }

  protected delete(event: EventRecord): void {
    if (!this.canDelete()) {
      return;
    }
    if (
      !confirm(
        `Permanently delete event "${event.title}"?\n\nThis removes the event and all of its attendance logs. This cannot be undone.`,
      )
    ) {
      return;
    }
    this.error.set(null);
    this.busyId.set(event.id);
    this.api.delete(event.id).subscribe({
      next: () => {
        this.events.update((list) => list.filter((e) => e.id !== event.id));
        this.busyId.set(null);
      },
      error: (err: { error?: { message?: string } }) => {
        this.busyId.set(null);
        this.error.set(err?.error?.message ?? 'Failed to delete event');
      },
    });
  }

  private openForm(mode: 'create' | 'edit', event?: EventRecord): void {
    const ref = this.dialog.open(EventFormDialog, {
      context: {
        mode,
        event,
        canSetActive: this.canToggleActive(),
      },
      contentClass:
        'max-w-[calc(100%-1rem)] w-[calc(100%-1rem)] sm:max-w-[min(96vw,90rem)] sm:w-[min(96vw,90rem)] p-4 sm:p-8 max-h-[min(96vh,70rem)]',
    });

    ref.closed$
      .pipe(
        take(1),
        filter((result): result is EventPayload => !!result),
      )
      .subscribe((result) => {
        this.error.set(null);
        const request =
          mode === 'create' ? this.api.create(result) : this.api.update(event!.id, result);
        request.subscribe({
          next: (saved) => {
            this.applySavedEvent(mode, saved);
          },
          error: (err: { error?: { message?: string } }) =>
            this.error.set(err?.error?.message ?? 'Failed to save event'),
        });
      });
  }

  private applySavedEvent(mode: 'create' | 'edit', saved: EventRecord): void {
    const inSelectedMonth = startsInMonth(saved.startsAt, this.month());
    this.events.update((list) => {
      const without = list.filter((e) => e.id !== saved.id);
      if (!inSelectedMonth) {
        return without;
      }
      if (mode === 'create') {
        return [saved, ...without];
      }
      return [saved, ...without].sort(
        (a, b) => new Date(b.startsAt).getTime() - new Date(a.startsAt).getTime(),
      );
    });
  }
}

function manilaParts(date: Date): { year: number; month: number } {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Manila',
    year: 'numeric',
    month: '2-digit',
  }).formatToParts(date);
  const year = Number(parts.find((p) => p.type === 'year')?.value);
  const month = Number(parts.find((p) => p.type === 'month')?.value);
  return { year, month };
}

function currentMonthValue(): string {
  const { year, month } = manilaParts(new Date());
  return `${year}-${month.toString().padStart(2, '0')}`;
}

function parseMonthValue(value: string): { year: number; month: number } {
  const [yearText, monthText] = value.split('-');
  return { year: Number(yearText), month: Number(monthText) };
}

function formatMonthLabel(value: string): string {
  const { year, month } = parseMonthValue(value);
  if (!year || !month) {
    return value;
  }
  return new Date(Date.UTC(year, month - 1, 1)).toLocaleDateString('en-PH', {
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  });
}

function startsInMonth(startsAt: string, monthValue: string): boolean {
  const d = new Date(startsAt);
  if (Number.isNaN(d.getTime())) {
    return false;
  }
  const { year, month } = manilaParts(d);
  return `${year}-${month.toString().padStart(2, '0')}` === monthValue;
}
