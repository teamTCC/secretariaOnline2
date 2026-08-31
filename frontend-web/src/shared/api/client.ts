import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { CsrfResponse, ProblemDetail } from "@/shared/api/types";

const baseURL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

const CSRF_EXEMPT = [
  "/auth/login",
  "/auth/refresh",
  "/auth/ott",
  "/auth/forgot-password",
  "/auth/reset-password",
  "/auth/csrf",
];

function isCsrfExempt(url: string | undefined): boolean {
  if (!url) return false;
  return CSRF_EXEMPT.some((path) => url.includes(path));
}

function shouldSkipAuthRetry(url: string | undefined): boolean {
  if (!url) return false;
  return url.includes("/auth/login") || url.includes("/auth/refresh") || url.includes("/auth/csrf");
}

const rawClient = axios.create({
  baseURL,
  withCredentials: true,
});

export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
});

let csrfToken: string | null = null;
let csrfInFlight: Promise<string> | null = null;

export function invalidateCsrfToken(): void {
  csrfToken = null;
}

export async function ensureCsrfToken(): Promise<string> {
  if (csrfToken) return csrfToken;
  if (!csrfInFlight) {
    csrfInFlight = rawClient
      .get<CsrfResponse>("/auth/csrf")
      .then((res) => {
        csrfToken = res.data.token;
        return csrfToken;
      })
      .finally(() => {
        csrfInFlight = null;
      });
  }
  return csrfInFlight;
}

apiClient.interceptors.request.use(async (config) => {
  const method = (config.method ?? "get").toUpperCase();
  if (["POST", "PUT", "PATCH", "DELETE"].includes(method) && !isCsrfExempt(config.url)) {
    const token = await ensureCsrfToken();
    config.headers["X-XSRF-TOKEN"] = token;
  }
  return config;
});

let refreshInFlight: Promise<void> | null = null;

export async function refreshSession(): Promise<void> {
  if (!refreshInFlight) {
    refreshInFlight = rawClient
      .post("/auth/refresh")
      .then(() => {
        invalidateCsrfToken();
      })
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError<ProblemDetail>) => {
    const config = error.config as RetryConfig | undefined;
    const status = error.response?.status;
    if (status === 401 && config && !config._retry && !shouldSkipAuthRetry(config.url)) {
      config._retry = true;
      try {
        await refreshSession();
        return apiClient(config);
      } catch {
        invalidateCsrfToken();
      }
    }
    return Promise.reject(error);
  },
);

export function problemFrom(error: unknown): ProblemDetail {
  if (axios.isAxiosError<ProblemDetail>(error) && error.response?.data) {
    const data = error.response.data;
    return {
      title: data.title,
      detail: data.detail,
      status: data.status ?? error.response.status,
      errors: data.errors,
      type: data.type,
    };
  }
  return { title: "Erro", detail: "Não foi possível concluir a operação", status: 0 };
}
