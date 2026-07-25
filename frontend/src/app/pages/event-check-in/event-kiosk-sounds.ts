import { WebAudioPlayer } from '../../core/web-audio-player';
import {
  eventToneUrl,
  type EventToneEvent,
  type EventToneSettings,
} from '../../core/settings/event-tones-api.service';

/** Event kiosk sounds — custom uploaded tones when assigned, otherwise built-in. */
export class EventKioskSounds {
  private readonly player = new WebAudioPlayer(0.98);
  private urls: Partial<Record<EventToneEvent, string>> = {};
  private current: HTMLAudioElement | null = null;
  private playGeneration = 0;

  applySettings(settings: EventToneSettings): void {
    const byId = new Map(settings.tones.map((tone) => [String(tone.id), tone]));
    const next: Partial<Record<EventToneEvent, string>> = {};
    for (const event of ['TIME_IN', 'TIME_OUT', 'ERROR', 'BIRTHDAY'] as EventToneEvent[]) {
      const rawId = settings.assignments[event];
      const toneId =
        rawId != null && String(rawId).trim() !== '' ? String(rawId).trim() : null;
      const tone = toneId ? byId.get(toneId) : undefined;
      if (tone) {
        next[event] = eventToneUrl(tone.url);
      }
    }
    this.urls = next;
  }

  playTimeIn(): void {
    this.playCustomOr('TIME_IN', () => this.player.playTimeIn());
  }

  playTimeOut(): void {
    this.playCustomOr('TIME_OUT', () => this.player.playTimeOut());
  }

  playBirthday(): void {
    this.playCustomOr('BIRTHDAY', () => this.player.playBirthday());
  }

  playNotFound(): void {
    this.playCustomOr('ERROR', () => this.player.playNotFound());
  }

  playError(): void {
    this.playCustomOr('ERROR', () => this.player.playTapError());
  }

  private playCustomOr(event: EventToneEvent, fallback: () => void): void {
    const url = this.urls[event];
    if (!url) {
      fallback();
      return;
    }
    const generation = ++this.playGeneration;
    try {
      if (this.current) {
        this.current.pause();
        this.current.removeAttribute('src');
        this.current.load();
        this.current = null;
      }
      const audio = new Audio(url);
      audio.volume = 1;
      this.current = audio;
      void audio.play().catch((err: unknown) => {
        if (generation !== this.playGeneration) {
          return;
        }
        const name =
          err && typeof err === 'object' && 'name' in err
            ? String((err as { name?: string }).name)
            : '';
        if (name === 'AbortError') {
          return;
        }
        fallback();
      });
    } catch {
      if (generation === this.playGeneration) {
        fallback();
      }
    }
  }
}
