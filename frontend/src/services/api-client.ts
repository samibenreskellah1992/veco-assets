import axios from 'axios'

/**
 * Single axios instance for all backend calls. Base URL is empty on
 * purpose: in dev, Vite proxies "/api" to the backend (see vite.config.ts);
 * in production the frontend and API are expected to sit behind the same
 * origin/reverse proxy. Never hardcode a backend host here.
 */
export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 15_000,
})

// TODO (Phase 3 - Authentification): attach the JWT bearer token to every
// request here via an axios request interceptor, and handle 401 responses
// with a redirect to /login via a response interceptor.
