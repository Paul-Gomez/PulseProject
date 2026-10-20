"use client";

import { Logo } from "@/components/Logo";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useAuth } from "@/lib/auth";
import { errorMessage } from "@/lib/errors";
import styles from "../auth.module.css";

const USERNAME = /^[a-zA-Z0-9_.-]{3,50}$/;

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const [form, setForm] = useState({ displayName: "", username: "", email: "", password: "" });
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Same rules as the backend's RegisterRequest; the backend validates again anyway.
  const problems = {
    displayName: !form.displayName.trim() && "El nombre es obligatorio.",
    username: !USERNAME.test(form.username) && "Entre 3 y 50 caracteres: letras, números, punto o guion.",
    email: !/^\S+@\S+\.\S+$/.test(form.email) && "Introduce un email válido.",
    password: form.password.length < 8 && "Mínimo 8 caracteres.",
  };
  const isValid = !Object.values(problems).some(Boolean);

  const update = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setTouched(true);
    if (!isValid) return;
    setLoading(true);
    setError(null);
    try {
      await register(form);
      router.replace("/");
    } catch (err) {
      setError(errorMessage(err));
      setLoading(false);
    }
  }

  const field = (name: keyof typeof form, label: string, type = "text") => (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      <input id={name} className="input" type={type} value={form[name]} onChange={update(name)} />
      {touched && problems[name] && <span className={styles.error}>{problems[name]}</span>}
    </div>
  );

  return (
    <main className={styles.page}>
      <form className={`card ${styles.card}`} onSubmit={handleSubmit} noValidate>
        <div className={styles.brand}><Logo size={34} />Pulse</div>
        <p className={styles.subtitle}>Crea tu cuenta</p>
        {error && <div className="alert">{error}</div>}
        {field("displayName", "Nombre")}
        {field("username", "Usuario")}
        {field("email", "Email", "email")}
        {field("password", "Contraseña", "password")}
        <button className="btn" type="submit" disabled={loading} style={{ width: "100%" }}>
          {loading ? "Creando cuenta..." : "Crear cuenta"}
        </button>
        <p className={styles.links}>
          ¿Ya tienes cuenta? <Link href="/login">Inicia sesión</Link>
        </p>
      </form>
    </main>
  );
}
