"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api, json } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import type { Workspace } from "@/lib/types";
import { useWorkspace } from "@/lib/workspace-context";

export function HomeView() {
  const { user } = useAuth();
  const { workspaces, reloadWorkspaces } = useWorkspace();
  const router = useRouter();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  async function create(event: FormEvent) {
    event.preventDefault();
    setCreating(true);
    try {
      const workspace = await api<Workspace>("/workspaces", { method: "POST", body: json({ name: name.trim() }) });
      await reloadWorkspaces();
      router.push(`/w/${workspace.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setCreating(false);
    }
  }

  return (
    <div style={{ padding: "1.5rem", maxWidth: 900, width: "100%", margin: "0 auto", overflowY: "auto" }}>
      <h1>Hola, {user?.displayName} 👋</h1>
      <h2>Tus workspaces</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "1rem", marginBottom: "2rem" }}>
        {workspaces.map((w) => (
          <Link key={w.id} href={`/w/${w.id}`} className="card" style={{ color: "inherit", padding: "1rem" }}>
            <strong>{w.name}</strong>
            <p className="muted" style={{ margin: "0.25rem 0 0", fontSize: "0.85rem" }}>{w.description || "Sin descripción"}</p>
          </Link>
        ))}
        {workspaces.length === 0 && (
          <p className="empty" style={{ gridColumn: "1 / -1" }}>Todavía no perteneces a ningún workspace. Crea el primero.</p>
        )}
      </div>

      <h2>Crear workspace</h2>
      <form className="card" onSubmit={create}>
        {error && <div className="alert">{error}</div>}
        <div className="field">
          <label htmlFor="name">Nombre</label>
          <input id="name" className="input" value={name} maxLength={100} placeholder="Gaming Club"
                 onChange={(e) => setName(e.target.value)} />
        </div>
        <button className="btn" type="submit" disabled={!name.trim() || creating}>Crear</button>
      </form>
    </div>
  );
}
