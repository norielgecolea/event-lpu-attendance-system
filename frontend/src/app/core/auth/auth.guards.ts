import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

function redirectAuthenticated(auth: AuthService, router: Router) {
  if (auth.isAdminPortal()) {
    return router.createUrlTree(['/dashboard']);
  }
  return router.createUrlTree(['/']);
}

/** Portal for SUPERADMIN, OSAS, and EVENT_MAKER. */
export const adminPortalGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated() && auth.isAdminPortal()) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return redirectAuthenticated(auth, router);
  }
  return router.createUrlTree(['/']);
};

export const superAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated() && auth.isSuperAdmin()) {
    return true;
  }
  if (auth.isAuthenticated()) {
    return redirectAuthenticated(auth, router);
  }
  return router.createUrlTree(['/']);
};

export function allowRoles(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.user()?.role;

    if (auth.isAuthenticated() && role && roles.includes(role)) {
      return true;
    }
    if (auth.isAuthenticated()) {
      return redirectAuthenticated(auth, router);
    }
    return router.createUrlTree(['/']);
  };
}

/** @deprecated gate-only */
export const guardRoleGuard: CanActivateFn = () => {
  const router = inject(Router);
  return router.createUrlTree(['/']);
};

/** @deprecated gate-only */
export const monitoringGuard: CanActivateFn = () => {
  const router = inject(Router);
  return router.createUrlTree(['/']);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return redirectAuthenticated(auth, router);
  }
  return true;
};

/** @deprecated use adminPortalGuard or superAdminGuard */
export const authGuard = superAdminGuard;
