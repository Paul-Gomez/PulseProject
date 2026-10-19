"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useWorkspace } from "@/lib/workspace-context";

/** Entering a workspace opens its first channel, or explains how to create one. */
export function WorkspaceRedirect() {
  const { current, channels, loading } = useWorkspace();
  const router = useRouter();

  useEffect(() => {
    if (!loading && current && channels[0]) {
      router.replace(`/w/${current.id}/c/${channels[0].id}`);
    }
  }, [loading, current, channels, router]);

  if (loading || !current) {
    return <div className="skeleton" style={{ height: 28, width: "40%", margin: "1.5rem" }} />;
  }
  return (
    <div className="empty">
      <h2>{current.name}</h2>
      <p>Este workspace todavía no tiene canales. Crea uno desde la barra lateral.</p>
    </div>
  );
}
