# TechStore

Plataforma web con autenticación segura: backend en Spring Boot y frontend en React.

## Estructura

```
techstore-repo/
├── backend/techstore/        API REST (Spring Boot, Java)
└── frontend/techstore-web/   Web (React + Vite)
```

## Tecnologías

- **Backend:** Spring Boot, Spring Security, JWT, OAuth2 (Google y GitHub), MFA, PostgreSQL
- **Frontend:** React, Vite

## Funcionalidades

- Registro e inicio de sesión con JWT
- Verificación en dos pasos (MFA)
- Login social con Google y GitHub
- Control de acceso por roles (usuario y administrador)
- Panel de administración de usuarios

## Endpoints principales

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/register` | Registrar usuario |
| POST | `/api/auth/login` | Iniciar sesión |
| POST | `/api/auth/mfa/verify` | Verificar código MFA |
| GET | `/api/me` | Datos del usuario autenticado |
| GET | `/api/reportes` | Reportes |
| GET | `/api/admin/users` | Listar usuarios (admin) |
| PUT | `/api/admin/users/{id}` | Editar usuario (admin) |
| POST | `/api/admin/users/{id}/unlock` | Desbloquear usuario (admin) |
| DELETE | `/api/admin/users/{id}` | Eliminar usuario (admin) |

## Cómo ejecutarlo

### Requisitos

- Java 17 o superior
- PostgreSQL con una base de datos llamada `techstore`
- Node.js (LTS)

### Backend

Abre `backend/techstore` en IntelliJ y ejecuta `TechstoreApplication`.

Define estas variables de entorno (en IntelliJ: Run > Edit Configurations > Environment variables):

```
DB_PASSWORD=tu_clave_de_postgres
JWT_SECRET=un_secret_largo_y_seguro
```

Opcionales, para el login social:

```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
```

El backend queda en http://localhost:8080

### Frontend

```
cd frontend/techstore-web
npm install
npm run dev
```

La web queda en http://localhost:5173

## Conclusiones

1. Construí un backend con Spring Boot que gestiona registro, login con JWT y verificación MFA, y que protege las rutas según el rol del usuario.
2. Integré login social con Google y GitHub mediante OAuth2, de modo que ofrezco varias formas de entrar sin duplicar la lógica de usuarios.
3. Desarrollé un frontend en React con Vite que consume mi API, y separé frontend y backend en carpetas independientes configurando la URL del servidor por variable de entorno.

## Autora

Sthefanie — [@saraisoto-crypto](https://github.com/saraisoto-crypto)
