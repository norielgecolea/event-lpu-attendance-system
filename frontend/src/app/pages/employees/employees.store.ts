import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { EmployeesApiService, type Employee } from '../../core/employees/employees-api.service';

export type { Employee };

const PAGE_SIZE = 50;

@Injectable({ providedIn: 'root' })
export class EmployeesStore {
  private readonly api = inject(EmployeesApiService);

  readonly employees = signal<Employee[]>([]);
  readonly total = signal(0);
  readonly hasMore = signal(false);
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly error = signal<string | null>(null);

  private searchTerm = '';

  load(search: string = ''): Observable<Employee[]> {
    this.searchTerm = search;
    this.loading.set(true);
    this.error.set(null);
    return this.api.page(search, 0, PAGE_SIZE).pipe(
      map((page) => {
        this.employees.set(page.items);
        this.total.set(page.total);
        this.hasMore.set(page.items.length < page.total);
        this.loading.set(false);
        return page.items;
      }),
      catchError((err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Failed to load employees');
        return throwError(() => err);
      }),
    );
  }

  loadMore(): void {
    if (this.loading() || this.loadingMore() || !this.hasMore()) {
      return;
    }
    this.loadingMore.set(true);
    this.api.page(this.searchTerm, this.employees().length, PAGE_SIZE).subscribe({
      next: (page) => {
        this.employees.update((list) => [...list, ...page.items]);
        this.total.set(page.total);
        this.hasMore.set(this.employees().length < page.total);
        this.loadingMore.set(false);
      },
      error: () => this.loadingMore.set(false),
    });
  }
}
