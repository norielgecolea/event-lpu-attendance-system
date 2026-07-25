import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideClipboardList,
  lucidePlus,
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
import { EventFormDialog } from './event-form-dialog';

@Component({
  selector: 'app-events',
  imports: [DatePipe, FormsModule, RouterLink, NgIcon, HlmButton, HlmInput, HlmBadge, HlmTableImports],
  viewProviders: [
    provideIcons({ lucidePlus, lucideSearch, lucideSquarePen, lucideClipboardList, lucideTrash2 }),
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
  protected readonly filter = signal('');
  protected readonly photoUrl = eventPhotoUrl;
  protected readonly canDelete = this.auth.isSuperAdmin;

  protected readonly filtered = computed(() => {
    const term = this.filter().trim().toLowerCase();
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

  protected reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().subscribe({
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
    this.openForm('edit', event);
  }

  protected deactivate(event: EventRecord): void {
    if (!this.canDelete()) {
      return;
    }
    if (!confirm(`Deactivate event "${event.title}"?`)) {
      return;
    }
    this.error.set(null);
    this.busyId.set(event.id);
    this.api.deactivate(event.id).subscribe({
      next: () => {
        this.events.update((list) =>
          list.map((e) => (e.id === event.id ? { ...e, active: false } : e)),
        );
        this.busyId.set(null);
      },
      error: (err: { error?: { message?: string } }) => {
        this.busyId.set(null);
        this.error.set(err?.error?.message ?? 'Failed to deactivate event');
      },
    });
  }

  private openForm(mode: 'create' | 'edit', event?: EventRecord): void {
    const ref = this.dialog.open(EventFormDialog, {
      context: { mode, event },
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
            this.events.update((list) => {
              if (mode === 'create') {
                return [saved, ...list];
              }
              return list.map((e) => (e.id === saved.id ? saved : e));
            });
          },
          error: (err: { error?: { message?: string } }) =>
            this.error.set(err?.error?.message ?? 'Failed to save event'),
        });
      });
  }
}
