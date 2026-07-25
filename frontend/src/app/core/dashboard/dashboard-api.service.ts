import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface EventAttendanceStat {
  eventId: string;
  title: string;
  location: string | null;
  active: boolean;
  startsAt: string;
  attendees: number;
  studentAttendees: number;
  employeeAttendees: number;
}

export interface UpcomingEventStat {
  eventId: string;
  title: string;
  location: string | null;
  startsAt: string;
  endsAt: string | null;
  active: boolean;
  attendees: number;
}

export interface DashboardSummary {
  totalEvents: number;
  activeEvents: number;
  eventsToday: number;
  upcomingEvents: number;
  totalStudents: number;
  totalEmployees: number;
  totalPortalUsers: number;
  activePortalUsers: number;
  totalCheckIns: number;
  studentCheckIns: number;
  employeeCheckIns: number;
  currentlyCheckedIn: number;
  topEventsByAttendance: EventAttendanceStat[];
  upcomingEventList: UpcomingEventStat[];
}

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/dashboard`;

  summary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/summary`);
  }
}
