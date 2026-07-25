import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BrnDialogRef, injectBrnDialogContext } from '@spartan-ng/brain/dialog';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  HlmDialogDescription,
  HlmDialogFooter,
  HlmDialogHeader,
  HlmDialogTitle,
} from '@spartan-ng/helm/dialog';
import { HlmFieldImports } from '@spartan-ng/helm/field';
import { HlmInput } from '@spartan-ng/helm/input';
import {
  eventPhotoUrl,
  type EventPayload,
  type EventRecord,
} from '../../core/events/events-api.service';
import { compressImageForUpload } from '../../core/images/compress-image';

export interface EventFormContext {
  mode: 'create' | 'edit';
  event?: EventRecord;
  /** SUPERADMIN / OSAS may set active; EVENT_MAKER creates as active. */
  canSetActive?: boolean;
}

@Component({
  selector: 'app-event-form-dialog',
  imports: [
    FormsModule,
    HlmButton,
    HlmDialogHeader,
    HlmDialogTitle,
    HlmDialogDescription,
    HlmDialogFooter,
    HlmFieldImports,
    HlmInput,
  ],
  templateUrl: './event-form-dialog.html',
})
export class EventFormDialog {
  private readonly dialogRef = inject<BrnDialogRef<EventPayload | null>>(BrnDialogRef);
  private readonly context = injectBrnDialogContext<EventFormContext>();

  protected readonly mode = this.context.mode;
  protected readonly canSetActive = this.context.canSetActive ?? false;
  protected readonly error = signal<string | null>(null);
  protected readonly compressing = signal(false);
  protected readonly previewUrl = signal<string | null>(
    eventPhotoUrl(this.context.event?.photo),
  );

  protected title = this.context.event?.title ?? '';
  protected description = this.context.event?.description ?? '';
  protected location = this.context.event?.location ?? '';
  protected startDate = '';
  protected startTime = '';
  protected endDate = '';
  protected endTime = '';
  protected active = this.context.event?.active ?? true;
  protected photoFile: File | null = null;

  constructor() {
    const start = splitDateTime(this.context.event?.startsAt) ?? defaultStartParts();
    this.startDate = start.date;
    this.startTime = start.time;
    const end = splitDateTime(this.context.event?.endsAt);
    if (end) {
      this.endDate = end.date;
      this.endTime = end.time;
    }
  }

  protected cancel(): void {
    this.dialogRef.close(null);
  }

  protected async onPhotoSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    if (!file) {
      return;
    }
    if (!file.type.startsWith('image/')) {
      this.error.set('Please choose an image file (JPEG, PNG, WebP, or GIF).');
      input.value = '';
      return;
    }

    this.error.set(null);
    this.compressing.set(true);
    try {
      const compressed = await compressImageForUpload(file);
      this.photoFile = compressed;
      const reader = new FileReader();
      reader.onload = () => this.previewUrl.set(String(reader.result));
      reader.readAsDataURL(compressed);
    } catch {
      this.error.set('Could not process the selected image.');
      input.value = '';
      this.photoFile = null;
    } finally {
      this.compressing.set(false);
    }
  }

  protected clearPhoto(): void {
    this.photoFile = null;
    this.previewUrl.set(eventPhotoUrl(this.context.event?.photo));
  }

  protected submit(): void {
    this.error.set(null);
    if (this.compressing()) {
      this.error.set('Please wait for the picture to finish compressing.');
      return;
    }
    if (!this.title.trim()) {
      this.error.set('Title is required.');
      return;
    }
    if (!this.startDate || !this.startTime) {
      this.error.set('Start date and start time are required.');
      return;
    }

    const startsAtIso = combineDateTime(this.startDate, this.startTime);
    if (!startsAtIso) {
      this.error.set('Start date/time is invalid.');
      return;
    }

    const hasEndDate = !!this.endDate.trim();
    const hasEndTime = !!this.endTime.trim();
    if (hasEndDate !== hasEndTime) {
      this.error.set('Provide both end date and end time, or leave both empty.');
      return;
    }

    let endsAtIso: string | null = null;
    if (hasEndDate && hasEndTime) {
      endsAtIso = combineDateTime(this.endDate, this.endTime);
      if (!endsAtIso) {
        this.error.set('End date/time is invalid.');
        return;
      }
      if (endsAtIso < startsAtIso) {
        this.error.set('End must be after the start time.');
        return;
      }
    }

    this.dialogRef.close({
      title: this.title.trim(),
      description: this.description.trim() || null,
      location: this.location.trim() || null,
      startsAt: startsAtIso,
      endsAt: endsAtIso,
      active: this.canSetActive ? this.active : true,
      photoFile: this.photoFile,
    });
  }
}

function splitDateTime(iso: string | null | undefined): { date: string; time: string } | null {
  if (!iso) {
    return null;
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return null;
  }
  const pad = (n: number) => n.toString().padStart(2, '0');
  return {
    date: `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`,
    time: `${pad(d.getHours())}:${pad(d.getMinutes())}`,
  };
}

function combineDateTime(date: string, time: string): string | null {
  const d = new Date(`${date}T${time}`);
  if (Number.isNaN(d.getTime())) {
    return null;
  }
  return d.toISOString();
}

function defaultStartParts(): { date: string; time: string } {
  const d = new Date();
  d.setMinutes(0, 0, 0);
  d.setHours(d.getHours() + 1);
  return splitDateTime(d.toISOString())!;
}
