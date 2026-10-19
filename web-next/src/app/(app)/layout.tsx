import { Suspense } from "react";
import { AppShell } from "@/components/AppShell";

// AppShell reads the URL params on the client; with cacheComponents enabled that has to
// happen inside a Suspense boundary so the static shell can be prerendered.
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <Suspense fallback={<div style={{ padding: "2rem", color: "var(--muted)" }}>Cargando...</div>}>
      <AppShell>{children}</AppShell>
    </Suspense>
  );
}
