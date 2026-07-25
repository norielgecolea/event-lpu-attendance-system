import { USER_ROLES } from '../users/users-api.service';

/** Roles the signed-in user may assign when creating or editing accounts. */
export function assignableRolesFor(actorRole: string | null | undefined): readonly string[] {
  switch (actorRole) {
    case 'SUPERADMIN':
      return USER_ROLES;
    case 'ADMIN':
      return ['ADMIN', 'SCANNER'];
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

/** Whether a route is reachable for the current admin-portal role. */
export function canAccessAdminRoute(role: string | null | undefined, route: string): boolean {
  if (!role) {
    return false;
  }
  if (role === 'SCANNER') {
    return route === '/dashboard';
  }
  if (role === 'OSAS') {
    return route === '/dashboard' || route.startsWith('/events');
  }
  if (role !== 'SUPERADMIN' && role !== 'ADMIN') {
    return false;
  }
  if (
    route === '/dashboard' ||
    route.startsWith('/events') ||
    route.startsWith('/students') ||
    route.startsWith('/employees')
  ) {
    return true;
  }
  if (route === '/users') {
    return role === 'SUPERADMIN' || role === 'ADMIN';
  }
  return false;
}
