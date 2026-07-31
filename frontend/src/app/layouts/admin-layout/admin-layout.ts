import { DatePipe } from '@angular/common';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterOutlet,
} from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { NgIcon, provideIcons } from '@ng-icons/core';
import {
  lucideBriefcase,
  lucideCalendarDays,
  lucideGraduationCap,
  lucideLayoutDashboard,
  lucideLogOut,
  lucideMenu,
  lucidePanelLeft,
  lucideShieldCheck,
  lucideMusic2,
  lucideTriangleAlert,
  lucideUserRound,
  lucideX,
} from '@ng-icons/lucide';
import { HlmButton } from '@spartan-ng/helm/button';
import {
  HlmNavigationMenu,
  HlmNavigationMenuItem,
  HlmNavigationMenuLink,
  HlmNavigationMenuList,
} from '@spartan-ng/helm/navigation-menu';
import { AlertSoundService } from '../../core/alert-sound.service';
import { AuthService } from '../../core/auth/auth.service';
import { canAccessAdminRoute } from '../../core/auth/role-access';
import { NotificationService } from '../../core/notifications/notification.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavSection {
  label: string | null;
  items: NavItem[];
}

interface TapErrorPayload {
  identifier?: string | null;
  eventTitle?: string | null;
  location?: string | null;
  tappedAt?: string | null;
}

interface TapErrorAlert {
  id: number;
  identifier: string;
  eventTitle: string;
  location: string;
  time: Date;
}

@Component({
  selector: 'app-admin-layout',
  imports: [
    DatePipe,
    RouterOutlet,
    RouterLink,
    NgIcon,
    HlmButton,
    HlmNavigationMenu,
    HlmNavigationMenuList,
    HlmNavigationMenuItem,
    HlmNavigationMenuLink,
  ],
  viewProviders: [
    provideIcons({
      lucideLayoutDashboard,
      lucideCalendarDays,
      lucideGraduationCap,
      lucideBriefcase,
      lucideShieldCheck,
      lucideMusic2,
      lucideTriangleAlert,
      lucidePanelLeft,
      lucideMenu,
      lucideUserRound,
      lucideLogOut,
      lucideX,
    }),
  ],
  templateUrl: './admin-layout.html',
  host: { class: 'block h-full' },
  styles: `
    @keyframes alert-in {
      from {
        opacity: 0;
        transform: translateX(24px) scale(0.96);
      }
      to {
        opacity: 1;
        transform: none;
      }
    }

    .alert-card {
      animation: alert-in 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
    }

    @media (prefers-reduced-motion: reduce) {
      .alert-card {
        animation: none;
      }
    }

    .admin-main {
      -webkit-overflow-scrolling: touch;
      scrollbar-gutter: stable;
    }

    .sidebar-nav {
      scrollbar-width: thin;
      scrollbar-color: color-mix(in oklch, var(--sidebar-border) 80%, transparent) transparent;
    }

    .sidebar-nav::-webkit-scrollbar {
      width: 4px;
    }

    .sidebar-nav::-webkit-scrollbar-thumb {
      border-radius: 9999px;
      background: color-mix(in oklch, var(--sidebar-border) 80%, transparent);
    }
  `,
})
export class AdminLayout implements OnDestroy {
  protected readonly sidebarOpen = signal(true);
  protected readonly mobileNavOpen = signal(false);
  protected readonly loggingOut = signal(false);
  protected readonly tapErrors = signal<TapErrorAlert[]>([]);
  private nextAlertId = 1;
  private readonly alertTimers = new Set<ReturnType<typeof setTimeout>>();
  private readonly tapErrorSub: Subscription;

  protected readonly navSections: NavSection[] = [
    {
      label: 'Overview',
      items: [{ label: 'Dashboard', icon: 'lucideLayoutDashboard', route: '/dashboard' }],
    },
    {
      label: 'Events',
      items: [{ label: 'Events', icon: 'lucideCalendarDays', route: '/events' }],
    },
    {
      label: 'Records',
      items: [
        { label: 'Student Records', icon: 'lucideGraduationCap', route: '/students' },
        { label: 'Employee Records', icon: 'lucideBriefcase', route: '/employees' },
      ],
    },
    {
      label: 'Administration',
      items: [
        { label: 'User Management', icon: 'lucideShieldCheck', route: '/users' },
        { label: 'Event Tones', icon: 'lucideMusic2', route: '/settings/event-tones' },
        {
          label: 'RFID Error Logs',
          icon: 'lucideTriangleAlert',
          route: '/tap-errors',
        },
      ],
    },
  ];

  protected readonly visibleNavSections = computed(() => {
    const role = this.auth.user()?.role;
    return this.navSections
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => canAccessAdminRoute(role, item.route)),
      }))
      .filter((section) => section.items.length > 0);
  });

  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly alertSound = inject(AlertSoundService);
  protected readonly notifications = inject(NotificationService);

  private readonly currentUrl = signal(this.router.url);
  protected readonly pageTitle = signal(this.resolveTitle());
  protected readonly currentUser = this.auth.user;

  constructor() {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => {
        this.currentUrl.set(this.router.url);
        this.pageTitle.set(this.resolveTitle());
        this.mobileNavOpen.set(false);
      });

    this.tapErrorSub = this.notifications.events$
      .pipe(filter((e) => e.type === 'EVENT_ATTENDANCE_TAP_ERROR'))
      .subscribe((event) => {
        const payload = (event.payload ?? {}) as TapErrorPayload;
        this.pushTapError(payload);
      });
  }

  ngOnDestroy(): void {
    this.tapErrorSub.unsubscribe();
    this.alertTimers.forEach((t) => clearTimeout(t));
    this.alertTimers.clear();
  }

  /** Longest-prefix match so nested routes highlight the correct item. */
  protected isActive(route: string): boolean {
    return this.bestMatch() === route;
  }

  private bestMatch(): string | null {
    const url = this.currentUrl().split('?')[0];
    const items = this.visibleNavSections().flatMap((section) => section.items);
    let best: string | null = null;
    for (const item of items) {
      const matches = url === item.route || url.startsWith(`${item.route}/`);
      if (matches && (best === null || item.route.length > best.length)) {
        best = item.route;
      }
    }
    return best;
  }

  private resolveTitle(): string {
    const url = this.currentUrl().split('?')[0];
    if (/^\/events\/[^/]+\/attendance/.test(url)) {
      return 'Event Attendance';
    }
    const best = this.bestMatch();
    const items = this.visibleNavSections().flatMap((section) => section.items);
    return items.find((i) => i.route === best)?.label ?? 'Dashboard';
  }

  protected toggleSidebar(): void {
    if (typeof window !== 'undefined' && window.matchMedia('(max-width: 767px)').matches) {
      this.mobileNavOpen.update((open) => !open);
      return;
    }
    this.sidebarOpen.update((open) => !open);
  }

  protected closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  protected showSidebarLabels(): boolean {
    return this.sidebarOpen() || this.mobileNavOpen();
  }

  protected logout(): void {
    this.loggingOut.set(true);
    this.auth.logout().subscribe({
      next: () => this.loggingOut.set(false),
      error: () => this.loggingOut.set(false),
    });
  }

  protected dismissTapError(id: number): void {
    this.tapErrors.update((list) => list.filter((a) => a.id !== id));
  }

  private pushTapError(payload: TapErrorPayload): void {
    const alert: TapErrorAlert = {
      id: this.nextAlertId++,
      identifier: payload.identifier?.trim() || 'Unknown ID',
      eventTitle: payload.eventTitle?.trim() || 'Unknown event',
      location: payload.location?.trim() || 'No venue',
      time: payload.tappedAt ? new Date(payload.tappedAt) : new Date(),
    };
    this.tapErrors.update((list) => [alert, ...list].slice(0, 4));
    this.alertSound.playError();
    const timer = setTimeout(() => {
      this.dismissTapError(alert.id);
      this.alertTimers.delete(timer);
    }, 12_000);
    this.alertTimers.add(timer);
  }
}
