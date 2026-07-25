export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  username: string;
  role: string;
  location?: string | null;
  expiresInMs: number;
}

export interface AuthUser {
  username: string;
  role: string;
  location?: string | null;
}

export interface AuthEventMessage {
  type: string;
  username?: string;
  message?: string;
  timestamp?: string;
  action?: string;
  payload?: unknown;
  /** Present on EVENT_KIOSK_PRESENCE (and optionally other) events. */
  eventIds?: string[];
  kioskCounts?: Record<string, number>;
  /** Legacy gate field — unused here. */
  locations?: string[];
}
