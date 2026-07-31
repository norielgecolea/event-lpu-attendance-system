import { USER_ROLES } from '../users/users-api.service';

/** Roles the signed-in user may assign when creating or editing accounts. */
export function assignableRolesFor(actorRole: string | null | undefined): readonly string[] {
  switch (actorRole) {
    case 'SUPERADMIN':
      return USER_ROLES;
    default:
      return [];
  }
}

export function canManageUserRole(
  actorRole: string | null | undefined,
  targetRole: string,
): boolean {
  return assignableRolesFor(actorRole).includes(targetRole);
}

/** Whether a route is reachable for the current portal role. */
export function canAccessAdminRoute(role: string | null | undefined, route: string): boolean {
  if (!role) {
    return false;
  }

  // Shared dashboard for all portal roles
  if (route === '/dashboard') {
    return role === 'SUPERADMIN' || role === 'OSAS' || role === 'EVENT_MAKER';
  }

  if (role === 'EVENT_MAKER') {
    return route === '/events' || route.startsWith('/events/');
  }

  if (role === 'OSAS') {
    return (
      route.startsWith('/events') ||
      route.startsWith('/students') ||
      route === '/tap-errors'
    );
  }

  if (role === 'SUPERADMIN') {
    return (
      route.startsWith('/events') ||
      route.startsWith('/students') ||
      route.startsWith('/employees') ||
      route === '/users' ||
      route === '/settings/event-tones' ||
      route === '/tap-errors'
    );
  }

  return false;
}

export function canEditEvents(role: string | null | undefined): boolean {
  return role === 'SUPERADMIN' || role === 'OSAS';
}

export function canToggleEventActive(role: string | null | undefined): boolean {
  return role === 'SUPERADMIN' || role === 'OSAS';
}

export function canDeleteEvents(role: string | null | undefined): boolean {
  return role === 'SUPERADMIN';
}

export function hidesAttendanceIdentifiers(role: string | null | undefined): boolean {
  return role === 'EVENT_MAKER';
}
