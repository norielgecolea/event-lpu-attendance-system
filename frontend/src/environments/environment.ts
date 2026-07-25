const contextPath = '/event-attendance-system';

export const environment = {
  production: false,
  contextPath,
  apiBaseUrl: `${contextPath}/api`,
  /** Empty = same-origin proxy at /gate-attendance */
  gateAttendanceUrl: '',
  gatePicturesPath: '/gate-attendance/attendance-system/pictures',
  wsUrl: `${typeof window !== 'undefined' ? window.location.origin.replace(/^http/, 'ws') : 'ws://localhost:4200'}${contextPath}/ws/notifications`,
};
