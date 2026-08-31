import { z } from "zod";

export const loginSchema = z.object({
  identificador: z.string().min(1, "Informe email ou GRR"),
  senha: z.string().min(1, "Informe a senha"),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

const PASSWORD_SPECIAL = /[@$!%*?&]/;

export const firstAccessSchema = z
  .object({
    novaSenha: z
      .string()
      .min(12, "Mínimo de 12 caracteres")
      .regex(/[a-z]/, "Inclua uma letra minúscula")
      .regex(/[A-Z]/, "Inclua uma letra maiúscula")
      .regex(/\d/, "Inclua um dígito")
      .regex(PASSWORD_SPECIAL, "Inclua um caractere especial (@$!%*?&)"),
    confirmarSenha: z.string().min(1, "Confirme a senha"),
    aceiteLgpd: z
      .boolean()
      .refine((value) => value, { message: "É necessário aceitar os termos LGPD" }),
  })
  .refine((data) => data.novaSenha === data.confirmarSenha, {
    message: "As senhas não coincidem",
    path: ["confirmarSenha"],
  });

export type FirstAccessFormValues = z.infer<typeof firstAccessSchema>;
