"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { api, json } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import type { Workspace } from "@/lib/types";
import { useWorkspace } from "@/lib/workspace-context";
import styles from "./HomeView.module.css";

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
    <div className={styles.page}>
      <header className={styles.header}>
        <h1>Hola, {user?.displayName}</h1>
        <p className="muted">Elige un workspace para entrar en sus canales o crea uno nuevo.</p>
      </header>

      <section aria-labelledby="workspaces-title">
        <h2 id="workspaces-title" className={styles.sectionTitle}>Tus workspaces</h2>
        {workspaces.length > 0 ? (
          <div className={styles.grid}>
            {workspaces.map((w) => (
              <Link key={w.id} href={`/w/${w.id}`} className={styles.workspace}>
                <span className={styles.initial} aria-hidden>{w.name.charAt(0).toUpperCase()}</span>
                <span className={styles.workspaceText}>
                  <strong>{w.name}</strong>
                  <span className="muted">{w.description || "Sin descripción"}</span>
                </span>
              </Link>
            ))}
          </div>
        ) : (
          <p className={styles.empty}>Todavía no perteneces a ningún workspace. Crea el primero aquí abajo.</p>
        )}
      </section>

      <section aria-labelledby="create-title">
        <h2 id="create-title" className={styles.sectionTitle}>Crear workspace</h2>
        <form className={styles.form} onSubmit={create} noValidate>
          <div className="field">
            <label htmlFor="name">Nombre</label>
            <input id="name" className="input" value={name} maxLength={100} placeholder="Por ejemplo, Equipo de diseño"
                   required aria-invalid={Boolean(error)} aria-describedby={error ? "name-error" : undefined}
                   onChange={(e) => setName(e.target.value)} />
            {error && <span id="name-error" className={styles.fieldError}>{error}</span>}
          </div>
          <button className="btn" type="submit" disabled={!name.trim() || creating}>
            {creating ? "Creando..." : "Crear workspace"}
          </button>
        </form>
      </section>
    </div>
  );
}
