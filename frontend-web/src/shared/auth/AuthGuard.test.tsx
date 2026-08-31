import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AuthGuard } from "@/shared/auth/AuthGuard";

vi.mock("@/shared/auth/AuthContext", () => ({
  useAuth: vi.fn(),
}));

import { useAuth } from "@/shared/auth/AuthContext";

const mockedUseAuth = vi.mocked(useAuth);

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>tela-login</div>} />
        <Route path="/primeiro-acesso" element={<div>tela-primeiro-acesso</div>} />
        <Route element={<AuthGuard />}>
          <Route path="/inicio" element={<div>tela-inicio</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe("AuthGuard", () => {
  beforeEach(() => {
    mockedUseAuth.mockReset();
  });

  it("redireciona para /login sem sessão", () => {
    mockedUseAuth.mockReturnValue({
      status: "anon",
      session: false,
      mustChangePassword: false,
      mustAcceptLgpd: false,
      needsFirstAccess: false,
      me: null,
      login: vi.fn(),
      completeFirstAccess: vi.fn(),
      logout: vi.fn(),
    });
    renderAt("/inicio");
    expect(screen.getByText("tela-login")).toBeInTheDocument();
    expect(screen.queryByText("tela-inicio")).not.toBeInTheDocument();
  });

  it("redireciona para /primeiro-acesso quando a senha ainda não foi trocada", () => {
    mockedUseAuth.mockReturnValue({
      status: "authed",
      session: true,
      mustChangePassword: true,
      mustAcceptLgpd: true,
      needsFirstAccess: true,
      me: null,
      login: vi.fn(),
      completeFirstAccess: vi.fn(),
      logout: vi.fn(),
    });
    renderAt("/inicio");
    expect(screen.getByText("tela-primeiro-acesso")).toBeInTheDocument();
    expect(screen.queryByText("tela-inicio")).not.toBeInTheDocument();
  });
});
