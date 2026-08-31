import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          primary: "var(--color-brand-primary)",
          accent: "var(--color-brand-accent)",
        },
        surface: {
          default: "var(--color-surface-default)",
          elevated: "var(--color-surface-elevated)",
          overlay: "var(--color-surface-overlay)",
          auth: "var(--color-surface-auth)",
          subtle: "var(--color-surface-subtle)",
        },
        text: {
          primary: "var(--color-text-primary)",
          secondary: "var(--color-text-secondary)",
          muted: "var(--color-text-muted)",
          disabled: "var(--color-text-disabled)",
          inverse: "var(--color-text-inverse)",
        },
        border: {
          default: "var(--color-border-default)",
          strong: "var(--color-border-strong)",
          focus: "var(--color-border-focus)",
          error: "var(--color-border-error)",
        },
        status: {
          success: "var(--color-status-success)",
          "success-bg": "var(--color-status-success-bg)",
          warning: "var(--color-status-warning)",
          "warning-bg": "var(--color-status-warning-bg)",
          danger: "var(--color-status-danger)",
          "danger-bg": "var(--color-status-danger-bg)",
          info: "var(--color-status-info)",
          "info-bg": "var(--color-status-info-bg)",
        },
      },
      spacing: {
        "space-xs": "var(--space-xs)",
        "space-sm": "var(--space-sm)",
        "space-md": "var(--space-md)",
        "space-lg": "var(--space-lg)",
        "space-xl": "var(--space-xl)",
        "space-2xl": "var(--space-2xl)",
      },
      borderRadius: {
        "radius-sm": "var(--radius-sm)",
        "radius-md": "var(--radius-md)",
        "radius-lg": "var(--radius-lg)",
        "radius-full": "var(--radius-full)",
      },
      boxShadow: {
        "shadow-sm": "var(--shadow-sm)",
        "shadow-md": "var(--shadow-md)",
        "shadow-lg": "var(--shadow-lg)",
      },
      fontSize: {
        h1: ["var(--font-size-h1)", { lineHeight: "1.2", fontWeight: "700" }],
        h2: ["var(--font-size-h2)", { lineHeight: "1.3", fontWeight: "600" }],
        h3: ["var(--font-size-h3)", { lineHeight: "1.4", fontWeight: "600" }],
        body: ["var(--font-size-body)", { lineHeight: "1.5", fontWeight: "400" }],
        caption: ["var(--font-size-caption)", { lineHeight: "1.4", fontWeight: "400" }],
      },
      minHeight: {
        touch: "var(--size-touch)",
        kpi: "var(--size-kpi)",
      },
    },
  },
  plugins: [],
} satisfies Config;
