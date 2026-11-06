# Biblioteca de Don Quijote de la Mancha

[![CI](https://github.com/JOCHIMANU/biblioteca-don-quijote/actions/workflows/ci.yml/badge.svg)](https://github.com/JOCHIMANU/biblioteca-don-quijote/actions/workflows/ci.yml)

Aplicación web para asentar los préstamos de la biblioteca de Don Quijote (más de 300 libros de caballerías,
un solo ejemplar de cada uno). Su escudero Sancho Panza reclama los libros vencidos, avisado por una
«paloma mensajera» dentro de la propia app (no se envía ningún mensaje real).

**Stack:** Java 17+, Spring Boot 3, Maven, Spring Data JPA, Thymeleaf, Bean Validation, Spring Security.
Base de datos: H2 en archivo en tu equipo, PostgreSQL en la nube. HTML/CSS/JS propios y SVG hechos a mano,
sin frameworks CSS.

## Contenido

1. [Cómo correrla en tu equipo](#cómo-correrla-en-tu-equipo)
2. [Entrar: usuario y contraseña](#entrar-usuario-y-contraseña)
3. [Ponerla en la nube](#ponerla-en-la-nube-gratis-y-siempre-disponible)
4. [Pantallas y funciones](#pantallas-y-funciones)
5. [Reglas de la casa](#reglas-de-la-casa)
6. [Pruebas](#pruebas)
7. [Estructura](#estructura)

## Cómo correrla en tu equipo

Solo necesitas un JDK 17 o superior (`java -version`). Maven se descarga solo gracias al wrapper.

```bash
./mvnw spring-boot:run
```

En Windows (PowerShell o cmd): `.\mvnw.cmd spring-boot:run`. Después abre <http://localhost:8080>.

La base de datos se guarda en `./data/biblioteca.mv.db` y persiste entre arranques. Para empezar de cero,
para la app y borra la carpeta `data/`. Con la base vacía, la app carga **15 libros de caballerías, 5 lectores
y 5 préstamos de ejemplo** (dos vencidos); para empezar en blanco usa `DATOS_EJEMPLO=false`.

## Entrar: usuario y contraseña

Toda la app, salvo la pantalla de entrada, exige usuario y contraseña.

| Variable de entorno | Para qué | Por defecto |
|---|---|---|
| `BIBLIOTECA_USUARIO` | Nombre del usuario | `admin` |
| `BIBLIOTECA_CLAVE` | Contraseña | **si falta, se genera una al azar en cada arranque y se escribe una vez en el registro de la app** |

La clave **nunca va en el código ni en el repositorio**. En tu equipo, la forma más cómoda es crear una sola vez
`config/application.properties` (carpeta ignorada por git):

```
biblioteca.seguridad.clave=tu-clave-larga
decolecta.token=tu-token-de-decolecta
```

o, para una sola sesión de PowerShell: `$env:BIBLIOTECA_CLAVE = "tu-clave-larga"`.

Medidas de seguridad incluidas:

- **CSRF** en todos los formularios, y **política de seguridad de contenido (CSP)** estricta (sin scripts en línea).
- **Freno a la fuerza bruta:** tras 5 fallos seguidos desde una misma dirección, se bloquea la entrada 15 minutos.
- La cookie de sesión es `HttpOnly`, `SameSite=Lax` y, en la nube, `Secure`. La sesión caduca a las 8 horas.
- Las claves se guardan con BCrypt. Las consultas internas (`/api/**`) sin sesión reciben un 401 limpio.
- La [auditoría](#pantallas-y-funciones) anota **quién** hizo cada acción.

## Ponerla en la nube (gratis y siempre disponible)

GitHub Pages no sirve para esto (solo aloja páginas estáticas), y el disco de los planes gratuitos se borra al
reiniciar, así que la receta es: **la app en Render** + **la base de datos PostgreSQL en Neon** + **un vigilante
gratuito que evita que Render la duerma**. Los tres tienen plan gratuito y se entra con tu cuenta de GitHub.

### 1. La base de datos (Neon)

1. Crea una cuenta en <https://neon.tech> (con GitHub) y un proyecto nuevo.
2. En el panel, en **Connection Details**, apunta: *host*, *database*, *user* y *password*.
3. Tu URL de conexión será: `jdbc:postgresql://HOST/DATABASE?sslmode=require`

### 2. La app (Render)

1. Pulsa  
   [![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/JOCHIMANU/biblioteca-don-quijote)  
   o, en <https://render.com>: **New → Blueprint** y elige este repositorio.
2. Render te pedirá los valores secretos (los de `render.yaml` marcados con `sync: false`):

   | Variable | Qué poner |
   |---|---|
   | `BIBLIOTECA_CLAVE` | La contraseña con la que entrará quien use la app (la que le darás a tu docente) |
   | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST/DATABASE?sslmode=require` (de Neon) |
   | `SPRING_DATASOURCE_USERNAME` | El *user* de Neon |
   | `SPRING_DATASOURCE_PASSWORD` | El *password* de Neon |
   | `DECOLECTA_TOKEN` | Tu token de Decolecta (opcional: sin él, el nombre se escribe a mano) |

3. Espera a que termine el despliegue (unos 5–8 minutos la primera vez). Tu enlace será
   `https://biblioteca-don-quijote.onrender.com` (o el nombre que Render te asigne).

La primera vez, con la base vacía, se cargan los datos de ejemplo. Cada vez que hagas `git push` a `main`,
Render vuelve a desplegar solo (`autoDeploy`).

### 3. Que nunca se duerma (UptimeRobot)

Los servicios gratuitos de Render se duermen tras 15 minutos sin visitas y tardan casi un minuto en despertar.
Para evitarlo: crea una cuenta gratuita en <https://uptimerobot.com> → **Add New Monitor** → tipo **HTTP(s)**,
URL `https://TU-APP.onrender.com/actuator/health`, intervalo de **5 minutos**. El mismo vigilante te avisa por
correo si la app se cae.

### Variables de entorno (resumen)

| Variable | Para qué | Por defecto |
|---|---|---|
| `BIBLIOTECA_USUARIO` / `BIBLIOTECA_CLAVE` | Entrada a la app | `admin` / al azar |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Base de datos (PostgreSQL en la nube) | H2 en `./data` |
| `DECOLECTA_TOKEN` | Autocompletar el nombre por DNI | vacío (a mano) |
| `COOKIE_SEGURA` | `true` detrás de https | `false` |
| `DATOS_EJEMPLO` | Cargar datos de ejemplo con la base vacía | `true` |
| `PORT` | Puerto (lo fija el proveedor) | `8080` |
| `DB_POOL` | Conexiones máximas a la base | `5` |

### Otros proveedores

Funciona igual en cualquier sitio que ejecute un contenedor Docker (hay un `Dockerfile`) o un JAR de Java 17:
Railway, Fly.io, Koyeb, un VPS… Solo cambian las variables de entorno de arriba.

> **Respaldo en la nube:** con PostgreSQL, el botón «Descargar respaldo» no existe; las copias las hace el
> proveedor de la base (en Neon, restauración a un instante anterior y *branches*). Los reportes CSV de
> «VII. Configuración» sirven como copia legible de las listas.

## Pantallas y funciones

**I. Anaquel** (libros) · **II. Lectores** · **III. Préstamos** («Libros en tierras ajenas», asentar y devolver con
confirmación de multa) · **IV. Morosos** · **V. Palomas** · **VI. Auditoría** · **VII. Configuración**
(reglas, reportes y respaldo). Cada libro y cada lector tiene su historial de préstamos.

- **Préstamo de varios libros** a un mismo lector: cada uno queda como préstamo independiente (su plazo, su
  multa, su devolución) y se asientan todos o ninguno. Libro y lector se eligen con buscadores que filtran en el
  servidor, así que aguantan miles de registros.
- **Doble préstamo bloqueado:** un libro con préstamo abierto no puede prestarse hasta que se devuelva.
- **Nombre por DNI:** al teclear los 8 dígitos, el servidor consulta a [Decolecta](https://decolecta.gitbook.io/docs/servicios/integrations-2.md)
  (RENIEC) y rellena el nombre. El token se queda en el servidor.
- **Corregir errores:** editar libros y lectores; borrarlos solo si nunca tuvieron préstamos; **anular** un
  préstamo asentado por error; **deshacer una devolución**. Las acciones sin vuelta atrás piden confirmación.
- **Avisos (palomas):** uno por préstamo vencido, nunca duplicado; se generan al arrancar, al abrir la portada o el
  panel, y con una tarea `@Scheduled` diaria. Sancho los marca como «reclamado» con «Despachar paloma».
- **VI. Auditoría:** cada acción importante deja un asiento con fecha y hora, detalle, **quién** entró y desde qué
  equipo; se filtra por tipo, viene paginado y no se puede editar desde la app.
- **VII. Reportes en CSV** (`;`, UTF-8 con BOM: abren con doble clic en Excel): libros, lectores, registro de
  préstamos, morosos y multas cobradas entre dos fechas (con total). Las celdas que empiezan como fórmula
  (`=`, `+`, `-`, `@`) se neutralizan.
- **Respaldo** (con H2, en tu equipo): botón que baja un `.zip` con la base de datos, consistente aunque la app
  esté en uso. Para **restaurar**: apagar la app, guardar aparte `data/`, descomprimir el zip dentro de `data/`
  (reemplaza `biblioteca.mv.db`) y volver a arrancar.

## Reglas de la casa

Se editan en **VII. Configuración** (se guardan en la base de datos y rigen al instante). Los valores de
`application.properties` son solo los iniciales, mientras no se guarde nada en esa pantalla.

| Regla | Valor inicial | Propiedad inicial |
|---|---|---|
| Plazo del préstamo | 7 días | `biblioteca.plazo-dias` |
| Multa por día de retraso | 1 sol | `biblioteca.multa-por-dia` |
| Máximo de libros por lector (0 = sin límite) | 0 | `biblioteca.max-libros-por-lector` |
| No prestar a lectores con libros vencidos | no | `biblioteca.bloquear-morosos` |
| Ronda diaria de la paloma (8:00) | solo `application.properties` | `biblioteca.cron-palomas` |

- **Multa** = `max(0, díasEntre(préstamo, devolución) − plazo) × multaPorDia`, siempre en soles.
- Cada préstamo **guarda el plazo y la multa con que se asentó**: cambiar la configuración solo afecta a los
  préstamos nuevos, nunca a los que ya están en curso.

## Pruebas

```bash
./mvnw test
```

Unas 100 pruebas automáticas que cubren: reglas de negocio (doble préstamo, multa de 0, 1 y varios días, códigos y
DNI únicos, avisos sin duplicados, tope de libros, morosos), todas las pantallas, la seguridad (sin sesión no se ve
nada, CSRF, freno a la fuerza bruta, cabeceras), los reportes y el respaldo, y la integración con Decolecta
(contra un servidor simulado: no gasta tu cupo).

Hay dos pruebas que miran más allá de lo habitual:

- **`PostgresTest`** ejecuta la lógica contra un **PostgreSQL real** (embebido, sin Docker), que es la base que se
  usa en la nube y es más estricta que H2.
- **`FlujoRealTest`** hace un recorrido por HTTP con **servidor y cookies de verdad** (entrar, navegar, formularios con
  y sin token, salir). MockMvc no confirma la respuesta como lo hace Tomcat, y esa prueba cazó un error real de la
  pantalla de entrada.

La integración continua (`.github/workflows/ci.yml`) corre todo esto en cada `push`, y además construye la imagen
de Docker, la arranca y comprueba que responde.

## Estructura

```
src/main/java/com/quijote/biblioteca
├── modelo/        entidades (Libro, Lector, Prestamo, Aviso, Ajuste, RegistroAuditoria)
├── repositorio/   Spring Data JPA
├── servicio/      reglas de negocio (aquí y solo aquí)
├── controlador/   MVC + formularios con Bean Validation
├── seguridad/     entrada, CSRF, CSP y freno de intentos
├── excepcion/     excepciones propias con mensajes cervantinos
└── config/        reglas configurables y reloj
src/main/resources/{templates,static}   Thymeleaf, CSS, JS y SVG
Dockerfile · render.yaml · .github/workflows/ci.yml
```
