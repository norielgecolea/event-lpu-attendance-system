import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  model,
  signal,
} from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideCalendarDays,
  lucideChevronDown,
  lucideChevronLeft,
  lucideChevronRight,
  lucideChevronUp,
} from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';

interface MonthOption {
  label: string;
  shortLabel: string;
  value: number;
}

type PickerView = 'month' | 'year';

const YEAR_PAGE_SIZE = 12;

const MONTHS: MonthOption[] = [
  { label: 'January', shortLabel: 'Jan', value: 0 },
  { label: 'February', shortLabel: 'Feb', value: 1 },
  { label: 'March', shortLabel: 'Mar', value: 2 },
  { label: 'April', shortLabel: 'Apr', value: 3 },
  { label: 'May', shortLabel: 'May', value: 4 },
  { label: 'June', shortLabel: 'Jun', value: 5 },
  { label: 'July', shortLabel: 'Jul', value: 6 },
  { label: 'August', shortLabel: 'Aug', value: 7 },
  { label: 'September', shortLabel: 'Sep', value: 8 },
  { label: 'October', shortLabel: 'Oct', value: 9 },
  { label: 'November', shortLabel: 'Nov', value: 10 },
  { label: 'December', shortLabel: 'Dec', value: 11 },
];

function parseYearMonth(value: string): { year: number; month: number } | null {
  const match = /^(\d{4})-(\d{2})$/.exec(value);
  if (!match) {
    return null;
  }
  const year = Number(match[1]);
  const month = Number(match[2]) - 1;
  if (!Number.isInteger(year) || month < 0 || month > 11) {
    return null;
  }
  return { year, month };
}

function formatYearMonth(year: number, month: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}`;
}

function getYearRangeStart(year: number): number {
  return Math.floor(year / 10) * 10;
}

/** Month/year picker matching the reservation dashboard date selector. */
@Component({
  selector: 'app-month-selector',
  imports: [NgIcon, HlmButton],
  changeDetection: ChangeDetectionStrategy.OnPush,
  viewProviders: [
    provideIcons({
      lucideCalendarDays,
      lucideChevronDown,
      lucideChevronLeft,
      lucideChevronRight,
      lucideChevronUp,
    }),
  ],
  host: {
    class: 'relative inline-block',
  },
  template: `
    <button
      hlmBtn
      type="button"
      variant="outline"
      class="bg-card h-10 w-[13.5rem] justify-between gap-2 px-3 font-semibold shadow-sm"
      [attr.aria-expanded]="open()"
      aria-haspopup="dialog"
      (click)="toggle()"
    >
      <ng-icon name="lucideCalendarDays" class="text-primary shrink-0 text-base" />
      <span class="min-w-0 flex-1 truncate text-center">{{ displayValue() }}</span>
      <ng-icon
        [name]="open() ? 'lucideChevronUp' : 'lucideChevronDown'"
        class="text-muted-foreground shrink-0 text-base"
      />
    </button>

    @if (open()) {
      <div
        class="bg-popover text-popover-foreground border-border absolute top-[calc(100%+0.375rem)] left-0 z-50 w-[13.5rem] rounded-xl border p-3 shadow-lg"
        role="dialog"
        aria-label="Choose month"
      >
        <div class="flex items-center gap-1">
          <button
            hlmBtn
            type="button"
            size="icon"
            variant="ghost"
            class="size-8 shrink-0"
            [attr.aria-label]="pickerView() === 'year' ? 'Previous years' : 'Previous year'"
            (click)="shiftHeader(-1)"
          >
            <ng-icon name="lucideChevronLeft" class="text-base" />
          </button>

          <button
            hlmBtn
            type="button"
            variant="ghost"
            class="h-8 min-w-0 flex-1 gap-1 px-2 text-sm font-extrabold"
            [attr.aria-label]="pickerView() === 'year' ? 'Show months' : 'Choose year'"
            (click)="toggleYearPicker()"
          >
            <span class="truncate">{{ headerLabel() }}</span>
            <ng-icon
              [name]="pickerView() === 'year' ? 'lucideChevronUp' : 'lucideChevronDown'"
              class="shrink-0 text-sm"
            />
          </button>

          <button
            hlmBtn
            type="button"
            size="icon"
            variant="ghost"
            class="size-8 shrink-0"
            [attr.aria-label]="pickerView() === 'year' ? 'Next years' : 'Next year'"
            (click)="shiftHeader(1)"
          >
            <ng-icon name="lucideChevronRight" class="text-base" />
          </button>
        </div>

        @if (pickerView() === 'year') {
          <div class="mt-3 grid grid-cols-3 gap-1.5">
            @for (year of yearOptions(); track year) {
              <button
                type="button"
                class="aspect-square rounded-lg text-sm font-semibold transition-colors"
                [class]="
                  isSelectedYear(year)
                    ? 'bg-primary text-primary-foreground hover:bg-primary'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                "
                (click)="selectYear(year)"
              >
                {{ year }}
              </button>
            }
          </div>
        } @else {
          <div class="mt-3 grid grid-cols-3 gap-1.5">
            @for (month of months; track month.value) {
              <button
                type="button"
                class="aspect-square rounded-lg text-sm font-semibold transition-colors"
                [class]="
                  isSelectedMonth(month.value)
                    ? 'bg-primary text-primary-foreground hover:bg-primary'
                    : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                "
                (click)="selectMonth(month.value)"
              >
                {{ month.shortLabel }}
              </button>
            }
          </div>
        }
      </div>
    }
  `,
})
export class MonthSelector {
  private readonly host = inject(ElementRef<HTMLElement>);

  /** YYYY-MM value. */
  readonly value = model<string>('');

  protected readonly months = MONTHS;
  protected readonly open = signal(false);
  protected readonly pickerView = signal<PickerView>('month');
  protected readonly pickerYear = signal(new Date().getFullYear());
  protected readonly yearRangeStart = signal(getYearRangeStart(new Date().getFullYear()));

  protected readonly selectedDate = computed(() => parseYearMonth(this.value()));

  protected readonly yearOptions = computed(() =>
    Array.from({ length: YEAR_PAGE_SIZE }, (_, index) => this.yearRangeStart() + index),
  );

  protected readonly headerLabel = computed(() => {
    if (this.pickerView() === 'month') {
      return String(this.pickerYear());
    }
    const years = this.yearOptions();
    return `${years[0]} – ${years[years.length - 1]}`;
  });

  protected readonly displayValue = computed(() => {
    const selected = this.selectedDate();
    if (!selected) {
      return 'Select month';
    }
    return `${MONTHS[selected.month].label} ${selected.year}`;
  });

  @HostListener('document:pointerdown', ['$event'])
  protected onDocumentPointerDown(event: PointerEvent): void {
    if (!this.open()) {
      return;
    }
    const target = event.target as Node | null;
    if (target && !this.host.nativeElement.contains(target)) {
      this.open.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.open.set(false);
  }

  protected toggle(): void {
    if (this.open()) {
      this.open.set(false);
      return;
    }
    this.syncPickerYear();
    this.open.set(true);
  }

  protected syncPickerYear(): void {
    const year = this.selectedDate()?.year ?? new Date().getFullYear();
    this.pickerYear.set(year);
    this.yearRangeStart.set(getYearRangeStart(year));
    this.pickerView.set('month');
  }

  protected shiftHeader(delta: number): void {
    if (this.pickerView() === 'year') {
      this.yearRangeStart.update((year) => year + delta * YEAR_PAGE_SIZE);
      return;
    }
    this.pickerYear.update((year) => year + delta);
  }

  protected toggleYearPicker(): void {
    if (this.pickerView() === 'year') {
      this.pickerView.set('month');
      return;
    }
    this.yearRangeStart.set(getYearRangeStart(this.pickerYear()));
    this.pickerView.set('year');
  }

  protected isSelectedYear(year: number): boolean {
    return this.selectedDate()?.year === year;
  }

  protected selectYear(year: number): void {
    const selected = this.selectedDate();
    this.pickerYear.set(year);
    if (selected) {
      this.value.set(formatYearMonth(year, selected.month));
    }
    this.pickerView.set('month');
  }

  protected isSelectedMonth(month: number): boolean {
    const selected = this.selectedDate();
    return selected?.year === this.pickerYear() && selected.month === month;
  }

  protected selectMonth(month: number): void {
    this.value.set(formatYearMonth(this.pickerYear(), month));
    this.open.set(false);
  }
}
