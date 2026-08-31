import { zodResolver } from "@hookform/resolvers/zod";
import { Shield } from "lucide-react";
import { useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { problemFrom } from "@/shared/api/client";
import { useAuth } from "@/shared/auth/AuthContext";
import { AlertBanner } from "@/shared/ui/alert-banner";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Checkbox } from "@/shared/ui/checkbox";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { firstAccessSchema, type FirstAccessFormValues } from "@/features/auth/schemas";

export function PrimeiroAcessoPage() {
  const { completeFirstAccess } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const form = useForm<FirstAccessFormValues>({
    resolver: zodResolver(firstAccessSchema),
    defaultValues: {
      novaSenha: "",
      confirmarSenha: "",
      aceiteLgpd: false,
    },
  });

  const aceite = form.watch("aceiteLgpd");

  async function onSubmit(values: FirstAccessFormValues) {
    setFormError(null);
    try {
      await completeFirstAccess({ novaSenha: values.novaSenha, aceiteLgpd: values.aceiteLgpd });
    } catch (error) {
      const problem = problemFrom(error);
      setFormError(problem.detail ?? "Não foi possível concluir o primeiro acesso");
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-default p-space-md">
      <Card className="w-full max-w-lg p-space-xl shadow-shadow-md">
        <div className="mb-space-lg flex flex-col gap-space-sm">
          <Shield className="h-8 w-8 text-brand-primary" aria-hidden />
          <h1 className="text-h2 text-text-primary">Primeiro acesso</h1>
          <p className="text-body text-text-secondary">
            Defina uma senha pessoal e aceite a política de privacidade para desbloquear o sistema.
          </p>
        </div>
        {formError ? <AlertBanner className="mb-space-md" tone="danger" title={formError} /> : null}
        <form className="flex flex-col gap-space-md" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <div className="flex flex-col gap-space-xs">
            <Label htmlFor="novaSenha">Nova senha</Label>
            <Input
              id="novaSenha"
              type="password"
              autoComplete="new-password"
              {...form.register("novaSenha")}
            />
            <p className="text-caption text-text-muted">
              12+ caracteres, com maiúscula, minúscula, dígito e especial (@$!%*?&).
            </p>
            {form.formState.errors.novaSenha ? (
              <p className="text-caption text-status-danger">{form.formState.errors.novaSenha.message}</p>
            ) : null}
          </div>
          <div className="flex flex-col gap-space-xs">
            <Label htmlFor="confirmarSenha">Confirmar senha</Label>
            <Input
              id="confirmarSenha"
              type="password"
              autoComplete="new-password"
              {...form.register("confirmarSenha")}
            />
            {form.formState.errors.confirmarSenha ? (
              <p className="text-caption text-status-danger">
                {form.formState.errors.confirmarSenha.message}
              </p>
            ) : null}
          </div>
          <Controller
            control={form.control}
            name="aceiteLgpd"
            render={({ field }) => (
              <div className="flex min-h-touch items-start gap-space-sm">
                <Checkbox
                  id="aceiteLgpd"
                  checked={Boolean(field.value)}
                  onCheckedChange={(value) => field.onChange(value === true)}
                />
                <Label htmlFor="aceiteLgpd" className="text-body text-text-primary">
                  Li e aceito a{" "}
                  <a
                    className="text-brand-primary underline"
                    href="https://www.ufpr.br/portalufpr/lgpd/"
                    target="_blank"
                    rel="noreferrer"
                  >
                    política de privacidade (LGPD)
                  </a>
                  .
                </Label>
              </div>
            )}
          />
          {form.formState.errors.aceiteLgpd ? (
            <p className="text-caption text-status-danger">{form.formState.errors.aceiteLgpd.message}</p>
          ) : null}
          <Button type="submit" size="full" disabled={!aceite || form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Salvando..." : "Continuar"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
