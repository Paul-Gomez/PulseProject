# Manual para enseñar Pulse en otro ordenador

Sigue estos pasos en orden. Si lo haces una vez en casa antes de la entrevista, en el otro ordenador solo tendrás que repetir los pasos 3 a 6.

## 1. Qué necesita el ordenador

| Programa | Para qué | Cómo comprobarlo |
|---|---|---|
| **Git** | Traer el proyecto | `git --version` |
| **Docker Desktop** (abierto y en marcha) | Base de datos, Redis, backend y web de Angular | `docker --version` |
| **Node.js 24** | Web de Next.js y script de datos de demo | `node --version` |

No hace falta instalar Java ni Maven: el backend corre dentro de Docker.

**Conexión a internet:** la primera vez Docker descarga las imágenes (tarda varios minutos) y `npm install` descarga las dependencias. Hazlo con buena conexión y antes de la entrevista, no durante.

## 2. Traer el proyecto

```bash
git clone https://github.com/Paul-Gomez/PulseProject.git
cd PulseProject
```

## 3. Configuración

```bash
cp .env.example .env
```

Abre `.env` y cambia el valor de `JWT_SECRET` por un texto largo cualquiera (mínimo 32 caracteres). Sin esto el backend no arranca. Los demás valores sirven tal cual.

> En Windows con PowerShell, usa `copy .env.example .env`.

## 4. Arrancar backend, base de datos y web de Angular

```bash
docker compose up -d --build
```

Espera a que termine y comprueba que todo está sano:

```bash
docker compose ps
```

Tienen que salir cinco servicios (`postgres`, `redis`, `storage`, `backend` y `frontend`). El backend tarda unos 30 segundos en pasar a *healthy*. Para comprobarlo, abre http://localhost:8080/actuator/health y debe responder `{"status":"UP"}`.

## 5. Cargar datos de demo

Sin datos la aplicación se ve vacía. Con el backend arrancado:

```bash
node scripts/seed-demo.mjs
```

Esto crea cuatro usuarios de prueba, el workspace **Pulse Studio** con los canales `general`, `diseño`, `backend` y `anuncios`, mensajes, reacciones y una conversación privada.

> Ejecútalo **una sola vez** por base de datos. Si lo repites, vuelve a enviar los mensajes.

Usuarios de prueba (todos con contraseña `Pulse12345`):

| Usuario | Email |
|---|---|
| Lucía Martín (propietaria) | lucia.demo@pulse.test |
| Mario Ruiz | mario.demo@pulse.test |
| Sofía López | sofia.demo@pulse.test |
| Diego Navarro | diego.demo@pulse.test |

## 6. Arrancar la web de Next.js

```bash
cd web-next
npm install
npm run dev
```

Abre http://localhost:3000 e inicia sesión con `lucia.demo@pulse.test`.

## 7. Direcciones útiles

| Qué | URL |
|---|---|
| Web en Next.js (la principal para la entrevista) | http://localhost:3000 |
| Web en Angular | http://localhost:4200 |
| Documentación de la API (Swagger) | http://localhost:8080/swagger-ui.html |
| Estado del backend | http://localhost:8080/actuator/health |

## 8. Guion de demo (5 minutos)

Abre dos ventanas: una normal con **Lucía** y otra de incógnito con **Mario**.

1. Inicia sesión como Lucía en http://localhost:3000. Enseña la barra lateral: workspaces, canales y miembros conectados.
2. Entra en `#general` y lee los mensajes. Señala las reacciones y las menciones resaltadas.
3. En la otra ventana, entra como Mario en el mismo canal y escribe un mensaje. Debe aparecer **al instante** en la ventana de Lucía.
4. Reacciona a un mensaje con un emoji desde una ventana y mira cómo cambia en la otra.
5. Prueba el **buscador rápido** con `Ctrl+K`, el **interruptor de modo oscuro** de arriba a la derecha y el **estado** (Disponible, Ausente, No molestar) de la tarjeta de abajo a la izquierda.
6. Reduce la ventana del navegador para enseñar que se adapta al móvil (aparece el menú ☰).
7. Abre Swagger y enseña la API que usa la web.
8. Si hay tiempo, abre la web de Angular (4200) y enseña que usa la misma API.

Si algo falla en directo: "esto lo tengo cubierto con tests", y abre un test del backend (`backend/src/test`).

## 9. Si algo no funciona

| Síntoma | Qué hacer |
|---|---|
| `docker compose` da error de conexión | Docker Desktop no está abierto. Ábrelo y espera a que diga que está en marcha. |
| "port is already allocated" | Otro programa usa ese puerto. Cámbialo en `.env` (`DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`) y repite el paso 4. |
| El backend se reinicia solo | Revisa `docker compose logs backend`. Casi siempre falta `JWT_SECRET` en `.env`. |
| `node scripts/seed-demo.mjs` dice "fetch failed" | El backend aún no está listo. Espera un minuto y repite. |
| La web de Next.js carga pero el login falla | Si tu backend no está en el 8080 (por ejemplo, si cambiaste `BACKEND_PORT`), crea `web-next/.env.local` con `BACKEND_URL=http://localhost:PUERTO` y `NEXT_PUBLIC_WS_URL=ws://localhost:PUERTO/ws`, y reinicia `npm run dev`. |
| Los mensajes no llegan en tiempo real | Revisa `NEXT_PUBLIC_WS_URL`: tiene que apuntar al mismo puerto que el backend. |
| Quiero empezar de cero | `docker compose down -v` borra todos los datos. Después repite desde el paso 4. |

## 10. Antes de salir de casa

- [ ] Hice todo el proceso una vez en este ordenador o en otro limpio.
- [ ] Sé qué usuarios usar y su contraseña (`Pulse12345`).
- [ ] Tengo abiertos: `README.md`, `web-next/src/lib/api.ts`, `web-next/src/lib/realtime.ts` y `backend/src/main/java/com/pulse/identity/service/AuthService.java`.
- [ ] Llevo la defensa en PDF (`Defensa-Pulse.pdf`), que no está en el repositorio. Cópiala en un USB o súbela a tu correo.
- [ ] Tengo a mano el enlace al repositorio: https://github.com/Paul-Gomez/PulseProject
