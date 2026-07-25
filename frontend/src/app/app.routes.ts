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
import { EventTonesSettings } from './pages/settings/event-tones-settings';
import { adminPortalGuard, allowRoles, guestGuard } from './core/auth/auth.guards';

const PORTAL_ROLES = ['SUPERADMIN', 'OSAS', 'EVENT_MAKER'] as const;
const EVENT_ROLES = ['SUPERADMIN', 'OSAS', 'EVENT_MAKER'] as const;
const STUDENT_ROLES = ['SUPERADMIN', 'OSAS'] as const;
const SUPERADMIN_ONLY = ['SUPERADMIN'] as const;

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
        canActivate: [allowRoles(...STUDENT_ROLES)],
      },
      {
        path: 'employees',
        component: Employees,
        canActivate: [allowRoles(...SUPERADMIN_ONLY)],
      },
      {
        path: 'users',
        component: Users,
        canActivate: [allowRoles(...SUPERADMIN_ONLY)],
      },
      {
        path: 'settings/event-tones',
        component: EventTonesSettings,
        canActivate: [allowRoles(...SUPERADMIN_ONLY)],
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', redirectTo: '' },
];
