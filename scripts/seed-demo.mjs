// Crea datos de demo (usuarios de prueba, un workspace, canales, mensajes y reacciones).
// Uso: node scripts/seed-demo.mjs   (con el backend arrancado)
// Si tu backend no está en el 8080: PULSE_API=http://localhost:8081/api/v1 node scripts/seed-demo.mjs
// Ejecútalo una sola vez por base de datos: si lo repites, vuelve a enviar los mensajes.
const API = process.env.PULSE_API ?? "http://localhost:8080/api/v1";
const PASSWORD = "Pulse12345";

async function call(method, path, body, token) {
  const res = await fetch(API + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status} ${text}`);
  return data;
}

const people = [
  { email: "lucia.demo@pulse.test", username: "lucia", displayName: "Lucía Martín" },
  { email: "mario.demo@pulse.test", username: "mario", displayName: "Mario Ruiz" },
  { email: "sofia.demo@pulse.test", username: "sofia", displayName: "Sofía López" },
  { email: "diego.demo@pulse.test", username: "diego", displayName: "Diego Navarro" },
];

const sessions = {};
for (const p of people) {
  try {
    await call("POST", "/auth/register", { ...p, password: PASSWORD });
  } catch (e) {
    console.log("register (ya existe o aviso):", e.message.slice(0, 120));
  }
  const pair = await call("POST", "/auth/login", { email: p.email, password: PASSWORD });
  const me = await call("GET", "/users/me", null, pair.accessToken);
  sessions[p.username] = { token: pair.accessToken, id: me.id, displayName: p.displayName, email: p.email };
  console.log("login", p.username, me.id);
}

const [lucia, mario, sofia, diego] = ["lucia", "mario", "sofia", "diego"].map((u) => sessions[u]);

// Workspace de la demo
// Reutiliza el workspace si ya existe (el script puede relanzarse sin duplicarlo)
const mine = await call("GET", "/workspaces", null, lucia.token);
const list = mine.content ?? mine;
let ws = list.find((w) => w.name === "Pulse Studio");
if (!ws) {
  ws = await call("POST", "/workspaces", { name: "Pulse Studio", description: "Equipo de diseño y desarrollo: proyectos, dudas y avisos." }, lucia.token);
  for (const other of [mario, sofia, diego]) {
    await call("POST", `/workspaces/${ws.id}/members`, { email: other.email }, lucia.token);
  }
}
console.log("workspace", ws.id);

const existing = await call("GET", `/workspaces/${ws.id}/channels`, null, lucia.token);
const channelsList = existing.content ?? existing;
const ensureChannel = async (name, type) =>
  channelsList.find((c) => c.name === name) ??
  (await call("POST", `/workspaces/${ws.id}/channels`, { name, type, isPrivate: false }, lucia.token));

const general = await ensureChannel("general", "TEXT");
const created = {
  "diseño": await ensureChannel("diseño", "TEXT"),
  backend: await ensureChannel("backend", "TEXT"),
  anuncios: await ensureChannel("anuncios", "ANNOUNCEMENT"),
};
console.log("canales", ["general", ...Object.keys(created)].join(", "));

const chatter = [
  [lucia, general, "¡Bienvenidos a Pulse Studio! Aquí hablamos de todo lo del equipo."],
  [mario, general, "Gracias Lucía. ¿Alguien ha revisado la maqueta del login?"],
  [sofia, general, "Sí, la he subido a diseño. Tiene unos cambios en la paleta."],
  [diego, general, "Buena, yo mañana miro el endpoint de sesiones."],
  [lucia, general, "Perfecto. Reunión de seguimiento el viernes a las 10:00."],
];
const diseno = [
  [sofia, created["diseño"], "Subo la nueva paleta teal para el modo oscuro."],
  [lucia, created["diseño"], "Me gusta. Mantén el contraste en los botones secundarios."],
  [mario, created["diseño"], "Ya lo tengo en cuenta en el componente de tarjetas."],
];
const backend = [
  [diego, created.backend, "He terminado el refresh token con rotación y detección de reutilización."],
  [mario, created.backend, "Genial. Falta añadir tests de integración para la búsqueda."],
  [diego, created.backend, "Lo dejo para esta semana."],
];
const anuncios = [
  [lucia, created.anuncios, "Nueva versión de la app móvil disponible en el repositorio."],
];

const posted = [];
for (const [who, channel, content] of [...chatter, ...diseno, ...backend, ...anuncios]) {
  const msg = await call("POST", `/channels/${channel.id}/messages`, { content }, who.token);
  posted.push({ msg, who });
}
console.log("mensajes de canal", posted.length);

// Reacciones sobre algunos mensajes
const emojis = ["👍", "🔥", "🎉", "❤️"];
for (let i = 0; i < posted.length; i += 2) {
  const reactors = [lucia, mario, sofia, diego].filter((u) => u !== posted[i].who);
  await call("POST", `/messages/${posted[i].msg.id}/reactions`, { emoji: emojis[i % emojis.length] }, reactors[0].token);
  await call("POST", `/messages/${posted[i].msg.id}/reactions`, { emoji: emojis[(i + 1) % emojis.length] }, reactors[1].token);
}
console.log("reacciones añadidas");

// Conversación privada entre Lucía y Mario
const conv = await call("POST", "/conversations", { participantIds: [mario.id] }, lucia.token);
await call("POST", `/conversations/${conv.id}/messages`, { content: "Mario, ¿tienes listo el borrador de la landing?" }, lucia.token);
await call("POST", `/conversations/${conv.id}/messages`, { content: "Casi. Te lo paso mañana por la mañana." }, mario.token);
await call("POST", `/conversations/${conv.id}/messages`, { content: "Perfecto, gracias." }, lucia.token);
console.log("conversación privada", conv.id);

console.log("listo");
