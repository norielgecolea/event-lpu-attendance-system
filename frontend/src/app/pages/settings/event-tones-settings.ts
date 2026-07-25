import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideMusic2,
  lucidePlay,
  lucideTrash2,
  lucideUpload,
} from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  EventTonesApiService,
  eventToneUrl,
  type EventTone,
  type EventToneEvent,
} from '../../core/settings/event-tones-api.service';

interface EventOption {
  key: EventToneEvent;
  label: string;
  description: string;
}

@Component({
  selector: 'app-event-tones-settings',
  imports: [DatePipe, DecimalPipe, FormsModule, NgIcon, HlmButton],
  viewProviders: [
    provideIcons({ lucideMusic2, lucidePlay, lucideTrash2, lucideUpload }),
  ],
  templateUrl: './event-tones-settings.html',
  host: { class: 'flex h-full flex-col' },
})
export class EventTonesSettings {
  private static readonly MAX_DURATION_SEC = 10;

  private readonly api = inject(EventTonesApiService);

  protected readonly tones = signal<EventTone[]>([]);
  protected readonly assignments = signal<Record<EventToneEvent, string | null>>({
    TIME_IN: null,
    TIME_OUT: null,
    ERROR: null,
    BIRTHDAY: null,
  });
  protected readonly loading = signal(true);
  protected readonly uploading = signal(false);
  protected readonly saving = signal(false);
  protected readonly deletingId = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly message = signal<string | null>(null);

  protected readonly eventOptions: EventOption[] = [
    {
      key: 'TIME_IN',
      label: 'Time in',
      description: 'Played when a successful time-in is recorded.',
    },
    {
      key: 'TIME_OUT',
      label: 'Time out',
      description: 'Played when a successful time-out is recorded.',
    },
    {
      key: 'ERROR',
      label: 'Error / not found',
      description: 'Played for unrecognized RFID or failed taps.',
    },
    {
      key: 'BIRTHDAY',
      label: 'Birthday',
      description: 'Played when someone taps on their birthday.',
    },
  ];

  constructor() {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getSettings().subscribe({
      next: (settings) => {
        this.tones.set(settings.tones);
        this.assignments.set({
          TIME_IN: settings.assignments.TIME_IN ?? null,
          TIME_OUT: settings.assignments.TIME_OUT ?? null,
          ERROR: settings.assignments.ERROR ?? null,
          BIRTHDAY: settings.assignments.BIRTHDAY ?? null,
        });
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load event tones');
      },
    });
  }

  protected async onFilesSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    input.value = '';
    if (files.length === 0 || this.uploading()) {
      return;
    }

    this.uploading.set(true);
    this.error.set(null);
    this.message.set(null);

    try {
      const accepted: File[] = [];
      const rejected: string[] = [];
      for (const file of files) {
        const duration = await audioDurationSeconds(file);
        if (duration > EventTonesSettings.MAX_DURATION_SEC) {
          rejected.push(
            `${file.name} (${duration.toFixed(1)}s — max ${EventTonesSettings.MAX_DURATION_SEC}s)`,
          );
          continue;
        }
        accepted.push(file);
      }

      if (rejected.length > 0) {
        this.error.set(
          `Tone must be ${EventTonesSettings.MAX_DURATION_SEC} seconds or shorter. Skipped: ${rejected.join('; ')}`,
        );
      }
      if (accepted.length === 0) {
        this.uploading.set(false);
        return;
      }

      this.api.upload(accepted).subscribe({
        next: (tones) => {
          this.tones.set(tones);
          this.uploading.set(false);
          this.message.set(`Uploaded ${accepted.length} tone(s).`);
        },
        error: (err: { error?: { message?: string } }) => {
          this.uploading.set(false);
          this.error.set(err?.error?.message ?? 'Failed to upload tones');
        },
      });
    } catch (err) {
      this.uploading.set(false);
      this.error.set(err instanceof Error ? err.message : 'Could not read audio duration.');
    }
  }

  protected setAssignment(eventType: EventToneEvent, toneId: string): void {
    const normalized = toneId != null && String(toneId).trim() !== '' ? String(toneId).trim() : null;
    this.assignments.update((current) => ({
      ...current,
      [eventType]: normalized,
    }));
  }

  protected saveAssignments(): void {
    if (this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.message.set(null);
    const current = this.assignments();
    const payload: Record<EventToneEvent, string> = {
      TIME_IN: current.TIME_IN ?? '',
      TIME_OUT: current.TIME_OUT ?? '',
      ERROR: current.ERROR ?? '',
      BIRTHDAY: current.BIRTHDAY ?? '',
    };
    this.api.setAssignments(payload).subscribe({
      next: (settings) => {
        this.tones.set(settings.tones);
        this.assignments.set({
          TIME_IN: settings.assignments.TIME_IN ?? null,
          TIME_OUT: settings.assignments.TIME_OUT ?? null,
          ERROR: settings.assignments.ERROR ?? null,
          BIRTHDAY: settings.assignments.BIRTHDAY ?? null,
        });
        this.saving.set(false);
        this.message.set('Tone assignments saved.');
      },
      error: (err: { error?: { message?: string } }) => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'Failed to save assignments');
      },
    });
  }

  protected deleteTone(tone: EventTone): void {
    if (!confirm(`Delete tone "${tone.originalName}"?`)) {
      return;
    }
    this.deletingId.set(tone.id);
    this.error.set(null);
    this.api.delete(tone.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.reload();
        this.message.set('Tone deleted.');
      },
      error: (err: { error?: { message?: string } }) => {
        this.deletingId.set(null);
        this.error.set(err?.error?.message ?? 'Failed to delete tone');
      },
    });
  }

  protected preview(tone: EventTone): void {
    const audio = new Audio(eventToneUrl(tone.url));
    void audio.play().catch(() => undefined);
  }

  protected sizeKb(bytes: number): number {
    return bytes / 1024;
  }
}

function audioDurationSeconds(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const audio = new Audio();
    audio.preload = 'metadata';
    audio.onloadedmetadata = () => {
      const duration = audio.duration;
      URL.revokeObjectURL(url);
      if (!Number.isFinite(duration) || duration <= 0) {
        reject(new Error(`Could not read duration for ${file.name}.`));
        return;
      }
      resolve(duration);
    };
    audio.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error(`Could not read audio file ${file.name}.`));
    };
    audio.src = url;
  });
}
