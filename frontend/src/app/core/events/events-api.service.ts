import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EventRecord {
  id: string;
  title: string;
  description: string | null;
  location: string | null;
  photo: string | null;
  startsAt: string;
  endsAt: string | null;
  active: boolean;
  createdByUserId?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface EventPayload {
  title: string;
  description: string | null;
  location: string | null;
  startsAt: string;
  endsAt: string | null;
  active: boolean;
  photoFile?: File | null;
}

export interface EventAttendanceLog {
  id: string;
  eventId: string;
  eventTitle?: string | null;
  eventLocation?: string | null;
  personType: 'STUDENT' | 'EMPLOYEE' | string;
  studentId: string | null;
  employeeId: string | null;
  personName: string | null;
  personNo: string | null;
  rfid: string | null;
  personPhoto?: string | null;
  timeIn: string | null;
  timeOut: string | null;
  lastAction: 'TIME_IN' | 'TIME_OUT' | string;
  tappedByUserId?: string | null;
  tapCount?: number;
  birthday?: boolean;
  /** True when a rapid re-tap returned the previous transaction unchanged. */
  duplicate?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface EventAttendancePage {
  items: EventAttendanceLog[];
  total: number;
}

export interface EventAttendanceHourlyBucket {
  hour: number;
  count: number;
  label: string;
}

export interface EventAttendanceStats {
  totalAttendees: number;
  studentAttendees: number;
  employeeAttendees: number;
  currentlyCheckedIn: number;
  completedVisits: number;
  averageStayMinutes: number | null;
  firstCheckIn: string | null;
  lastActivity: string | null;
  checkInsByHour: EventAttendanceHourlyBucket[];
}

/** Builds a browser URL for an event photo path from the API. */
export function eventPhotoUrl(photo: string | null | undefined): string | null {
  if (!photo) {
    return null;
  }
  if (photo.startsWith('http://') || photo.startsWith('https://') || photo.startsWith('data:')) {
    return photo;
  }
  const path = photo.startsWith('/') ? photo : `/${photo}`;
  return `${environment.contextPath}${path}`;
}

@Injectable({ providedIn: 'root' })
export class EventsApiService {
  private readonly http = inject(HttpClient);
  private readonly eventsUrl = `${environment.apiBaseUrl}/events`;
  private readonly attendanceUrl = `${environment.apiBaseUrl}/event-attendance`;

  list(options: { year?: number; month?: number; activeOnly?: boolean } = {}): Observable<EventRecord[]> {
    let params = new HttpParams();
    if (options.year != null) {
      params = params.set('year', options.year);
    }
    if (options.month != null) {
      params = params.set('month', options.month);
    }
    if (options.activeOnly) {
      params = params.set('activeOnly', 'true');
    }
    return this.http.get<EventRecord[]>(this.eventsUrl, { params });
  }

  /** Public endpoint — active events spanning today (Asia/Manila). */
  listToday(): Observable<EventRecord[]> {
    return this.http.get<EventRecord[]>(`${this.eventsUrl}/today`);
  }

  /** Public active event details for the check-in kiosk. */
  getPublic(id: string): Observable<EventRecord> {
    return this.http.get<EventRecord>(`${this.eventsUrl}/${id}/public`);
  }

  getById(id: string): Observable<EventRecord> {
    return this.http.get<EventRecord>(`${this.eventsUrl}/${id}`);
  }

  create(payload: EventPayload): Observable<EventRecord> {
    return this.http.post<EventRecord>(this.eventsUrl, this.toFormData(payload));
  }

  update(id: string, payload: EventPayload): Observable<EventRecord> {
    return this.http.put<EventRecord>(`${this.eventsUrl}/${id}`, this.toFormData(payload));
  }

  setActive(id: string, active: boolean): Observable<EventRecord> {
    return this.http.patch<EventRecord>(`${this.eventsUrl}/${id}/active`, { active });
  }

  delete(id: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.eventsUrl}/${id}`);
  }

  /** Public kiosk tap (no auth). */
  publicTap(eventId: string, identifier: string): Observable<EventAttendanceLog> {
    return this.http.post<EventAttendanceLog>(`${this.attendanceUrl}/public-tap`, {
      eventId: Number(eventId),
      identifier,
    });
  }

  listAttendance(
    eventId: string,
    options: { search?: string; offset?: number; limit?: number } = {},
  ): Observable<EventAttendancePage> {
    let params = new HttpParams().set('eventId', eventId);
    if (options.search) {
      params = params.set('search', options.search);
    }
    if (options.offset != null) {
      params = params.set('offset', options.offset);
    }
    if (options.limit != null) {
      params = params.set('limit', options.limit);
    }
    return this.http.get<EventAttendancePage>(this.attendanceUrl, { params });
  }

  attendanceStats(eventId: string): Observable<EventAttendanceStats> {
    const params = new HttpParams().set('eventId', eventId);
    return this.http.get<EventAttendanceStats>(`${this.attendanceUrl}/stats`, { params });
  }

  recentAttendance(limit = 5): Observable<EventAttendanceLog[]> {
    const params = new HttpParams().set('limit', String(limit));
    return this.http.get<EventAttendanceLog[]>(`${this.attendanceUrl}/recent`, { params });
  }

  exportAttendanceCsv(eventId: string): Observable<Blob> {
    const params = new HttpParams().set('eventId', eventId);
    return this.http.get(`${this.attendanceUrl}/export`, {
      params,
      responseType: 'blob',
    });
  }

  private toFormData(payload: EventPayload): FormData {
    const { photoFile, ...event } = payload;
    const form = new FormData();
    form.append(
      'event',
      new Blob([JSON.stringify(event)], { type: 'application/json' }),
    );
    if (photoFile) {
      form.append('photo', photoFile, photoFile.name);
    }
    return form;
  }
}
