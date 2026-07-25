import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { StudentsApiService } from '../../core/students/students-api.service';

export interface Student {
  id: string;
  name: string;
  studentNo: string;
  photo?: string | null;
  rfid: string | null;
  birthdate?: string | null;
  department: string;
  course: string;
  school: string;
  financeTagged: boolean;
}

const PAGE_SIZE = 50;

@Injectable({ providedIn: 'root' })
export class StudentsStore {
  private readonly api = inject(StudentsApiService);

  readonly students = signal<Student[]>([]);
  readonly total = signal(0);
  readonly hasMore = signal(false);
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly error = signal<string | null>(null);

  private searchTerm = '';

  /** Loads the first page for the given search term (server-side paging). */
  load(search: string = ''): Observable<Student[]> {
    this.searchTerm = search;
    this.loading.set(true);
    this.error.set(null);
    return this.api.page(search, 0, PAGE_SIZE).pipe(
      map((page) => {
        this.students.set(page.items);
        this.total.set(page.total);
        this.hasMore.set(page.items.length < page.total);
        this.loading.set(false);
        return page.items;
      }),
      catchError((err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load students');
        return throwError(() => err);
      }),
    );
  }

  /** Appends the next page when scrolled near the bottom. */
  loadMore(): void {
    if (this.loading() || this.loadingMore() || !this.hasMore()) {
      return;
    }
    this.loadingMore.set(true);
    this.api.page(this.searchTerm, this.students().length, PAGE_SIZE).subscribe({
      next: (page) => {
        this.students.update((list) => [...list, ...page.items]);
        this.total.set(page.total);
        this.hasMore.set(this.students().length < page.total);
        this.loadingMore.set(false);
      },
      error: () => this.loadingMore.set(false),
    });
  }

  getById(id: string): Student | undefined {
    return this.students().find((s) => s.id === id);
  }
}
