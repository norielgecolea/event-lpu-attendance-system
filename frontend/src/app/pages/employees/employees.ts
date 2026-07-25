import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideChevronDown,
  lucideChevronsUpDown,
  lucideChevronUp,
  lucideSearch,
} from '@ng-icons/lucide';
import { HlmAvatarImports } from '@spartan-ng/helm/avatar';
import { HlmBadge } from '@spartan-ng/helm/badge';
import { HlmInput } from '@spartan-ng/helm/input';
import { HlmTableImports } from '@spartan-ng/helm/table';
import {
  type ColumnDef,
  type SortingState,
  createAngularTable,
  getCoreRowModel,
  getSortedRowModel,
} from '@tanstack/angular-table';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { studentPhotoUrl } from '../../core/students/student-photo.util';
import { infiniteScroll } from '../../shared/infinite-scroll';
import { type Employee, EmployeesStore } from './employees.store';

@Component({
  selector: 'app-employees',
  imports: [FormsModule, NgIcon, HlmInput, HlmBadge, HlmTableImports, HlmAvatarImports],
  viewProviders: [
    provideIcons({
      lucideSearch,
      lucideChevronsUpDown,
      lucideChevronUp,
      lucideChevronDown,
    }),
  ],
  templateUrl: './employees.html',
  host: { class: 'flex h-full flex-col' },
})
export class Employees {
  private readonly store = inject(EmployeesStore);

  protected readonly data = this.store.employees;
  protected readonly total = this.store.total;
  protected readonly hasMore = this.store.hasMore;
  protected readonly loading = this.store.loading;
  protected readonly loadingMore = this.store.loadingMore;
  protected readonly error = this.store.error;

  protected readonly sorting = signal<SortingState>([]);
  protected readonly search = signal('');
  protected readonly scroll = infiniteScroll();
  private readonly searchChanges = new Subject<string>();

  private readonly columns: ColumnDef<Employee>[] = [
    { accessorKey: 'name', header: 'Name' },
    { accessorKey: 'rfid', header: 'RFID #' },
    { accessorKey: 'department', header: 'Department' },
    { accessorKey: 'position', header: 'Position' },
  ];

  protected readonly table = createAngularTable<Employee>(() => ({
    data: this.data(),
    columns: this.columns,
    state: {
      sorting: this.sorting(),
    },
    getRowId: (row) => row.id,
    onSortingChange: (updater) =>
      this.sorting.set(typeof updater === 'function' ? updater(this.sorting()) : updater),
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  }));

  constructor() {
    this.reload();
    this.searchChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((term) => {
        this.scroll.reset();
        this.store.load(term).subscribe();
      });
  }

  protected onSearchChange(term: string): void {
    this.search.set(term);
    this.searchChanges.next(term.trim());
  }

  protected onTableScroll(event: Event): void {
    const loadedRows = this.table.getRowModel().rows.length;
    this.scroll.onScroll(event, loadedRows);
    const el = event.target as HTMLElement;
    if (el.scrollTop + el.clientHeight >= el.scrollHeight - 200 && this.scroll.visible() >= loadedRows) {
      this.store.loadMore();
    }
  }

  protected reload(): void {
    this.store.load(this.search().trim()).subscribe({ error: () => undefined });
  }

  protected sortIcon(state: false | 'asc' | 'desc'): string {
    if (state === 'asc') return 'lucideChevronUp';
    if (state === 'desc') return 'lucideChevronDown';
    return 'lucideChevronsUpDown';
  }

  protected initials(name: string): string {
    const parts = name.replace(',', '').trim().split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase();
  }

  protected photoSrc(photo: string | null | undefined): string | null {
    return studentPhotoUrl(photo);
  }
}
