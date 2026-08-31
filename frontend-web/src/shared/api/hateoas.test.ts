import { describe, expect, it } from "vitest";
import { renderHook } from "@testing-library/react";
import { useActions } from "./hateoas";

describe("useActions", () => {
  it("returns false for missing link", () => {
    const { result } = renderHook(() => useActions({ _links: {} }));
    expect(result.current.can("deliberar")).toBe(false);
    expect(result.current.href("deliberar")).toBeUndefined();
  });

  it("reads HAL href objects", () => {
    const { result } = renderHook(() =>
      useActions({
        _links: { novaSolicitacao: { href: "/solicitacoes/nova" } },
      }),
    );
    expect(result.current.can("novaSolicitacao")).toBe(true);
    expect(result.current.href("novaSolicitacao")).toBe("/solicitacoes/nova");
  });

  it("reads string hrefs from OpenAPI oneOf", () => {
    const { result } = renderHook(() =>
      useActions({ _links: { self: "/bff/dashboard/aluno" } }),
    );
    expect(result.current.href("self")).toBe("/bff/dashboard/aluno");
  });
});
