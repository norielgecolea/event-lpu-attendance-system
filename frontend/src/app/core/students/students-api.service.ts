import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { Student } from '../../pages/students/students.store';

export interface StudentPage {
  items: Student[];
  total: number;
}

/** Read-only student API for the event attendance portal. */
@Injectable({ providedIn: 'root' })
export class StudentsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/students`;

  list(): Observable<Student[]> {
    return this.http.get<Student[]>(this.baseUrl);
  }

  /** Server-side paged + searched listing so large tables are never fetched whole. */
  page(search: string, offset: number, limit: number): Observable<StudentPage> {
    const params = new HttpParams()
      .set('search', search)
      .set('offset', offset)
      .set('limit', limit);
    return this.http.get<StudentPage>(`${this.baseUrl}/page`, { params });
  }

  getById(id: string): Observable<Student> {
    return this.http.get<Student>(`${this.baseUrl}/${id}`);
  }

  exportCsv(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, { responseType: 'blob' });
  }
}
