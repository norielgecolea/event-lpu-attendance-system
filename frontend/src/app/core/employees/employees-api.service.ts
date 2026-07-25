import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Employee {
  id: string;
  name: string;
  employeeNo: string;
  photo?: string | null;
  rfid: string | null;
  birthdate?: string | null;
  department: string | null;
  position: string | null;
}

export interface EmployeePage {
  items: Employee[];
  total: number;
}

/** Read-only employee API (gate attendance DB). */
@Injectable({ providedIn: 'root' })
export class EmployeesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/employees`;

  page(search: string, offset: number, limit: number): Observable<EmployeePage> {
    const params = new HttpParams()
      .set('search', search)
      .set('offset', offset)
      .set('limit', limit);
    return this.http.get<EmployeePage>(`${this.baseUrl}/page`, { params });
  }
}
