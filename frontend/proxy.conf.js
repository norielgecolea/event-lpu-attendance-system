const backendUrl = process.env.BACKEND_URL || 'http://localhost:8080';
const contextPath = process.env.BACKEND_CONTEXT_PATH || '/event-attendance-system';
const gateAttendanceUrl = process.env.GATE_ATTENDANCE_URL || 'http://localhost';

module.exports = {
  [`${contextPath}/api`]: {
    target: backendUrl,
    secure: false,
    changeOrigin: true,
  },
  [`${contextPath}/ws`]: {
    target: backendUrl,
    secure: false,
    changeOrigin: true,
    ws: true,
  },
  [`${contextPath}/pictures`]: {
    target: backendUrl,
    secure: false,
    changeOrigin: true,
  },
  [`${contextPath}/tones`]: {
    target: backendUrl,
    secure: false,
    changeOrigin: true,
  },
  '/gate-attendance': {
    target: gateAttendanceUrl,
    secure: false,
    changeOrigin: true,
    pathRewrite: { '^/gate-attendance': '' },
  },
};
