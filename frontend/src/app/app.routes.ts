import { Routes } from '@angular/router';
import { Login } from './pages/login/login';
import { TodayEvents } from './pages/today-events/today-events';
import { EventCheckIn } from './pages/event-check-in/event-check-in';
import { AdminLayout } from './layouts/admin-layout/admin-layout';
import { Dashboard } from './pages/dashboard/dashboard';
import { Events } from './pages/events/events';
import { EventAttendance } from './pages/events/event-attendance';
import { Students } from './pages/students/students';
import { Employees } from './pages/employees/employees';
import { Users } from './pages/users/users';
import { adminPortalGuard, allowRoles, guestGuard } from './core/auth/auth.guards';

const PORTAL_ROLES = ['SUPERADMIN', 'ADMIN', 'OSAS', 'SCANNER'] as const;
const EVENT_ROLES = ['SUPERADMIN', 'ADMIN', 'OSAS'] as const;
const ADMIN_ROLES = ['SUPERADMIN', 'ADMIN'] as const;
const USER_MGMT_ROLES = ['SUPERADMIN', 'ADMIN'] as const;

export const routes: Routes = [
  { path: '', component: Login, canActivate: [guestGuard], pathMatch: 'full' },
  { path: 'login', redirectTo: '' },
  { path: 'today', component: TodayEvents },
  { path: 'attend/:id', component: EventCheckIn },
  {
    path: '',
    component: AdminLayout,
    canActivate: [adminPortalGuard],
    children: [
      {
        path: 'dashboard',
        component: Dashboard,
        canActivate: [allowRoles(...PORTAL_ROLES)],
      },
      {
        path: 'events',
        component: Events,
        canActivate: [allowRoles(...EVENT_ROLES)],
      },
      {
        path: 'events/:id/attendance',
        component: EventAttendance,
        canActivate: [allowRoles(...PORTAL_ROLES)],
      },
      {
        path: 'students',
        component: Students,
        canActivate: [allowRoles(...ADMIN_ROLES)],
      },
      {
        path: 'employees',
        component: Employees,
        canActivate: [allowRoles(...ADMIN_ROLES)],
      },
      {
        path: 'users',
        component: Users,
        canActivate: [allowRoles(...USER_MGMT_ROLES)],
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
