import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type EventToneEvent = 'TIME_IN' | 'TIME_OUT' | 'ERROR' | 'BIRTHDAY';

export interface EventTone {
  id: string;
  url: string;
  originalName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface EventToneSettings {
  tones: EventTone[];
  assignments: Partial<Record<EventToneEvent, string | null>>;
}

/** Resolves a stored tone path (/tones/...) to a browser URL under the WAR context. */
export function eventToneUrl(url: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  if (url.startsWith(environment.contextPath)) {
    return url;
  }
  return `${environment.contextPath}${url.startsWith('/') ? url : `/${url}`}`;
}

@Injectable({ providedIn: 'root' })
export class EventTonesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/event-tones`;

  getSettings(): Observable<EventToneSettings> {
    return this.http.get<EventToneSettings>(this.baseUrl);
  }

  upload(files: File[]): Observable<EventTone[]> {
    const form = new FormData();
    for (const file of files) {
      form.append('files', file, file.name);
    }
    return this.http.post<EventTone[]>(this.baseUrl, form);
  }

  setAssignments(
    assignments: Partial<Record<EventToneEvent, string | null>>,
  ): Observable<EventToneSettings> {
    const body: Record<string, string> = {};
    for (const [key, value] of Object.entries(assignments)) {
      body[key] = value == null ? '' : String(value);
    }
    return this.http.put<EventToneSettings>(`${this.baseUrl}/assignments`, body);
  }

  delete(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${id}`);
  }
}
