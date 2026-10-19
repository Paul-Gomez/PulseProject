"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { api, json } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import { realtime } from "@/lib/realtime";
import type { Channel } from "@/lib/types";
import { useWorkspace, WorkspaceProvider } from "@/lib/workspace-context";
import { Avatar } from "./Avatar";
import styles from "./AppShell.module.css";

/** Protected area: redirects to /login without a session and keeps the realtime connection open. */
export function AppShell({ children }: { children: ReactNode }) {
  const { user, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && !user) router.replace("/login");
  }, [ready, user, router]);

  useEffect(() => {
    if (!user) return;
    realtime.connect();
    return () => realtime.disconnect();
  }, [user]);

  if (!ready || !user) {
    return <div className={styles.loading}>Cargando...</div>;
  }
  return (
    <WorkspaceProvider>
      <Layout>{children}</Layout>
    </WorkspaceProvider>
  );
}

function Layout({ children }: { children: ReactNode }) {
  const { user, logout } = useAuth();
  const ws = useWorkspace();
  const { channelId } = useParams<{ channelId?: string }>();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [newChannel, setNewChannel] = useState("");
  const [channelError, setChannelError] = useState<string | null>(null);

  function toggleTheme() {
    const dark = document.documentElement.classList.toggle("dark");
    localStorage.setItem("pulse.theme", dark ? "dark" : "light");
  }

  async function createChannel(event: FormEvent) {
    event.preventDefault();
    const name = newChannel.trim().toLowerCase().replace(/\s+/g, "-");
    if (!ws.current || !name) return;
    try {
      const channel = await api<Channel>(`/workspaces/${ws.current.id}/channels`, {
        method: "POST",
        body: json({ name, type: "TEXT", isPrivate: false }),
      });
      setNewChannel("");
      setChannelError(null);
      await ws.reloadChannels();
      router.push(`/w/${ws.current.id}/c/${channel.id}`);
    } catch (err) {
      setChannelError(errorMessage(err));
    }
  }

  return (
    <div className={`${styles.layout} ${menuOpen ? styles.open : ""}`}>
      <nav className={styles.rail} aria-label="Workspaces">
        <Link href="/" className={`${styles.railItem} ${!ws.current ? styles.active : ""}`} title="Inicio">P</Link>
        {ws.workspaces.map((w) => (
          <Link key={w.id} href={`/w/${w.id}`} title={w.name} onClick={() => setMenuOpen(false)}
                className={`${styles.railItem} ${ws.current?.id === w.id ? styles.active : ""}`}>
            {w.name.charAt(0).toUpperCase()}
          </Link>
        ))}
      </nav>

      <aside className={styles.sidebar}>
        <div className={styles.sidebarHeader}>{ws.current?.name ?? "Pulse"}</div>
        {ws.current ? (
          <>
            <div className={styles.sectionTitle}>Canales</div>
            {ws.loading
              ? [1, 2, 3].map((i) => <div key={i} className={`skeleton ${styles.channelSkeleton}`} />)
              : ws.channels.map((c) => (
                  <Link key={c.id} href={`/w/${ws.current!.id}/c/${c.id}`} onClick={() => setMenuOpen(false)}
                        className={`${styles.channel} ${c.id === channelId ? styles.channelActive : ""}`}>
                    {c.isPrivate ? "🔒" : "#"} {c.name}
                  </Link>
                ))}
            <form onSubmit={createChannel} className={styles.newChannel}>
              <input className="input" placeholder="+ nuevo canal" value={newChannel} maxLength={80}
                     onChange={(e) => setNewChannel(e.target.value)} />
              {channelError && <span className={styles.error}>{channelError}</span>}
            </form>

            <div className={styles.sectionTitle}>Miembros · {ws.members.size}</div>
            {[...ws.members.values()].map((m) => (
              <div key={m.id} className={styles.member}>
                <Avatar name={m.user.displayName} size={24} online={ws.onlineUserIds.has(m.userId)} />
                <span>{m.user.displayName}</span>
              </div>
            ))}
          </>
        ) : (
          <p className={styles.hint}>Elige un workspace o crea uno nuevo.</p>
        )}
      </aside>

      <div className={styles.main}>
        <header className={styles.topbar}>
          <button className={`btn btn-ghost ${styles.menu}`} onClick={() => setMenuOpen(!menuOpen)} aria-label="Menú">☰</button>
          <span style={{ flex: 1 }} />
          <span className={styles.bell} title="Notificaciones sin leer">
            🔔{ws.unreadNotifications > 0 && <span className={styles.badge}>{ws.unreadNotifications}</span>}
          </span>
          <button className="btn btn-ghost" onClick={toggleTheme} title="Cambiar tema">◐</button>
          {user && <Avatar name={user.displayName} size={30} />}
          <button className="btn btn-ghost" onClick={logout} title="Cerrar sesión">⏻</button>
        </header>
        <main className={styles.content}>{children}</main>
      </div>
      <div className={styles.backdrop} onClick={() => setMenuOpen(false)} />
    </div>
  );
}
