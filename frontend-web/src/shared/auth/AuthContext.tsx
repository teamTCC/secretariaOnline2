import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiClient, ensureCsrfToken, invalidateCsrfToken, problemFrom } from "@/shared/api/client";
import type { FirstAccessRequest, LoginRequest, LoginResponse, MeResponse } from "@/shared/api/types";

export type AuthStatus = "boot" | "anon" | "authed";

type AuthContextValue = {
  status: AuthStatus;
  session: boolean;
  mustChangePassword: boolean;
  mustAcceptLgpd: boolean;
  needsFirstAccess: boolean;
  me: MeResponse | null;
  login: (payload: LoginRequest) => Promise<LoginResponse>;
  completeFirstAccess: (payload: FirstAccessRequest) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

async function fetchMe(): Promise<MeResponse> {
  const res = await apiClient.get<MeResponse>("/me");
  return res.data;
}

function flagsFromProfile(profile: MeResponse) {
  return {
    mustChangePassword: Boolean(profile.mustChangePassword),
    mustAcceptLgpd: Boolean(profile.mustAcceptLgpd),
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("boot");
  const [session, setSession] = useState(false);
  const [mustChangePassword, setMustChangePassword] = useState(false);
  const [mustAcceptLgpd, setMustAcceptLgpd] = useState(false);
  const [me, setMe] = useState<MeResponse | null>(null);

  const applyProfile = useCallback((profile: MeResponse, loginFlags?: LoginResponse) => {
    setMe(profile);
    setSession(true);
    setMustChangePassword(loginFlags?.mustChangePassword ?? flagsFromProfile(profile).mustChangePassword);
    setMustAcceptLgpd(loginFlags?.mustAcceptLgpd ?? flagsFromProfile(profile).mustAcceptLgpd);
    setStatus("authed");
  }, []);

  const clearSession = useCallback(() => {
    invalidateCsrfToken();
    setSession(false);
    setMe(null);
    setMustChangePassword(false);
    setMustAcceptLgpd(false);
    setStatus("anon");
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        await ensureCsrfToken();
        const profile = await fetchMe();
        if (cancelled) return;
        applyProfile(profile);
      } catch {
        if (cancelled) return;
        clearSession();
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applyProfile, clearSession]);

  const login = useCallback(
    async (payload: LoginRequest) => {
      const res = await apiClient.post<LoginResponse>("/auth/login", payload);
      invalidateCsrfToken();
      const profile = await fetchMe();
      applyProfile(profile, res.data);
      return res.data;
    },
    [applyProfile],
  );

  const completeFirstAccess = useCallback(
    async (payload: FirstAccessRequest) => {
      await apiClient.post("/auth/first-access", payload);
      invalidateCsrfToken();
      const profile = await fetchMe();
      applyProfile(profile, { mustChangePassword: false, mustAcceptLgpd: false });
    },
    [applyProfile],
  );

  const logout = useCallback(async () => {
    try {
      await apiClient.post("/auth/logout");
    } catch {
      // still drop the local session if the cookie is already gone
    }
    clearSession();
  }, [clearSession]);

  const needsFirstAccess = mustChangePassword || mustAcceptLgpd;

  const value = useMemo(
    () => ({
      status,
      session,
      mustChangePassword,
      mustAcceptLgpd,
      needsFirstAccess,
      me,
      login,
      completeFirstAccess,
      logout,
    }),
    [
      status,
      session,
      mustChangePassword,
      mustAcceptLgpd,
      needsFirstAccess,
      me,
      login,
      completeFirstAccess,
      logout,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth deve ser usado dentro de AuthProvider");
  }
  return ctx;
}

export { problemFrom };
