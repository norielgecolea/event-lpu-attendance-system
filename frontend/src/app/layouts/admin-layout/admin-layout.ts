import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  Router,
  RouterLink,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs';
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
import { AuthService } from '../../core/auth/auth.service';
import { canAccessAdminRoute } from '../../core/auth/role-access';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavSection {
  label: string | null;
  items: NavItem[];
}

@Component({
  selector: 'app-admin-layout',
  imports: [
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
export class AdminLayout {
  protected readonly sidebarOpen = signal(true);
  protected readonly mobileNavOpen = signal(false);
  protected readonly loggingOut = signal(false);

  protected readonly navSections: NavSection[] = [
    {
      label: null,
      items: [
        { label: 'Dashboard', icon: 'lucideLayoutDashboard', route: '/dashboard' },
        { label: 'Events', icon: 'lucideCalendarDays', route: '/events' },
        { label: 'Student Records', icon: 'lucideGraduationCap', route: '/students' },
        { label: 'Employee Records', icon: 'lucideBriefcase', route: '/employees' },
        { label: 'User Management', icon: 'lucideShieldCheck', route: '/users' },
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
}
