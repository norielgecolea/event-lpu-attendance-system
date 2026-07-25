import { environment } from '../../../environments/environment';

/**
 * Resolves a student photo path to a browser URL.
 * Photos live on the gate attendance system (proxied via /gate-attendance).
 */
export function studentPhotoUrl(photo: string | null | undefined): string | null {
  if (!photo) {
    return null;
  }
  if (photo.startsWith('http://') || photo.startsWith('https://') || photo.startsWith('data:')) {
    return photo;
  }

  const filename = photo.includes('/') ? photo.split('/').pop()! : photo;
  const path = `${environment.gatePicturesPath}/${filename}`;

  if (environment.gateAttendanceUrl) {
    const base = environment.gateAttendanceUrl.replace(/\/$/, '');
    // Absolute gate URL: strip the /gate-attendance proxy prefix if present
    if (path.startsWith('/gate-attendance')) {
      return `${base}${path.slice('/gate-attendance'.length)}`;
    }
    return `${base}${path.startsWith('/') ? path : `/${path}`}`;
  }

  return path;
}
