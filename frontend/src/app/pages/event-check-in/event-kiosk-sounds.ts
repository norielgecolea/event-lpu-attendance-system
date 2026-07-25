import { WebAudioPlayer } from '../../core/web-audio-player';

/** Built-in kiosk tones (same palette as gate attendance). */
export class EventKioskSounds {
  private readonly player = new WebAudioPlayer(0.98);

  playTimeIn(): void {
    this.player.playTimeIn();
  }

  playTimeOut(): void {
    this.player.playTimeOut();
  }

  playBirthday(): void {
    this.player.playBirthday();
  }

  playNotFound(): void {
    this.player.playNotFound();
  }

  playError(): void {
    this.player.playTapError();
  }
}
