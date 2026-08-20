import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ServerTime {
  now: string;
  zone: string;
}

@Injectable({ providedIn: 'root' })
export class ServerTimeApiService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/time`;

  current(): Observable<ServerTime> {
    return this.http.get<ServerTime>(this.url);
  }
}
