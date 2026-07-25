const contextPath = '/event-attendance-system';

export const environment = {
  production: true,
  contextPath,
  apiBaseUrl: `${contextPath}/api`,
  /** Empty = same-origin proxy at /gate-attendance */
  gateAttendanceUrl: '',
  gatePicturesPath: '/gate-attendance/attendance-system/pictures',
  wsUrl: `${typeof window !== 'undefined' ? window.location.origin.replace(/^http/, 'ws') : ''}${contextPath}/ws/notifications`,
};
