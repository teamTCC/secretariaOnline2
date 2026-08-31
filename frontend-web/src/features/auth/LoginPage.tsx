import { zodResolver } from "@hookform/resolvers/zod";
import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useAuth } from "@/shared/auth/AuthContext";
import { problemFrom } from "@/shared/api/client";
import { AlertBanner } from "@/shared/ui/alert-banner";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { loginSchema, type LoginFormValues } from "@/features/auth/schemas";

export function LoginPage() {
  const { login } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identificador: "", senha: "" },
  });

  async function onSubmit(values: LoginFormValues) {
    setFormError(null);
    try {
      await login(values);
    } catch (error) {
      const problem = problemFrom(error);
      if (problem.status === 429) {
        setFormError("Muitas tentativas. Aguarde.");
      } else if ((problem.status ?? 0) >= 500) {
        setFormError("Não foi possível entrar. Tente novamente.");
      } else {
        setFormError("Credenciais inválidas");
      }
      form.setValue("senha", "");
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-auth p-space-md">
      <Card className="w-full max-w-md p-space-xl shadow-shadow-md">
        <div className="mb-space-lg flex flex-col items-center gap-space-sm text-center">
          <p className="text-caption font-medium uppercase tracking-wide text-brand-primary">
            UFPR SEPT
          </p>
          <h1 className="text-h2 text-text-primary">Entrar</h1>
          <p className="text-caption text-text-secondary">SecretariaOnline2</p>
        </div>
        {formError ? (
          <AlertBanner className="mb-space-md" tone="danger" title={formError} />
        ) : null}
        <form className="flex flex-col gap-space-md" onSubmit={form.handleSubmit(onSubmit)} noValidate>
          <div className="flex flex-col gap-space-xs">
            <Label htmlFor="identificador">Email ou GRR</Label>
            <Input
              id="identificador"
              autoComplete="username"
              placeholder="ana.silva@ufpr.br"
              {...form.register("identificador")}
            />
            {form.formState.errors.identificador ? (
              <p className="text-caption text-status-danger">
                {form.formState.errors.identificador.message}
              </p>
            ) : null}
          </div>
          <div className="flex flex-col gap-space-xs">
            <Label htmlFor="senha">Senha</Label>
            <div className="relative">
              <Input
                id="senha"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                className="pr-space-2xl"
                {...form.register("senha")}
              />
              <button
                type="button"
                className="absolute inset-y-0 right-0 px-space-md text-text-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-primary"
                aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                onClick={() => setShowPassword((v) => !v)}
              >
                {showPassword ? <EyeOff aria-hidden className="h-4 w-4" /> : <Eye aria-hidden className="h-4 w-4" />}
              </button>
            </div>
            {form.formState.errors.senha ? (
              <p className="text-caption text-status-danger">{form.formState.errors.senha.message}</p>
            ) : null}
          </div>
          <Button type="submit" size="full" disabled={form.formState.isSubmitting}>
            {form.formState.isSubmitting ? "Entrando..." : "Entrar"}
          </Button>
        </form>
        <p className="mt-space-lg text-center text-caption text-text-muted">
          Universidade Federal do Paraná — SEPT
        </p>
      </Card>
    </div>
  );
}
