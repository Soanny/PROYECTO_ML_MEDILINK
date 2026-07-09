# Despliegue en Railway - CONSULTAS MediLink V4.2.5

## Requisitos

- Java 17
- MySQL 8.0+
- Cuenta en [Railway](https://railway.app)

## Variables de entorno

### Sistema principal

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `PORT` | Puerto asignado por Railway | `8087` |
| `ML_SERVER_PORT` | Puerto del servidor | `8087` |
| `ML_DB_URL` | URL de conexión MySQL | `jdbc:mysql://localhost:3306/bd_citas_medicas` |
| `ML_DB_USER` | Usuario de base de datos | `root` |
| `ML_DB_PASSWORD` | Contraseña de base de datos | *(vacío)* |
| `ML_MAIL_ENABLED` | Habilitar envío de correo SMTP | `false` |
| `ML_MAIL_HOST` | Servidor SMTP | *(vacío)* |
| `ML_MAIL_PORT` | Puerto SMTP | `587` |
| `ML_MAIL_USER` | Usuario SMTP | *(vacío)* |
| `ML_MAIL_PASSWORD` | Contraseña SMTP | *(vacío)* |
| `ML_MAIL_FROM` | Correo remitente | `notificaciones@medilink.local` |
| `ML_MICROSERVICIO_CORREO_ENABLED` | Usar microservicio de correo | `false` |
| `ML_MICROSERVICIO_CORREO_URL` | URL del microservicio de correo | `http://localhost:8091` |

### Microservicio de correo (`microservicios/ML_correo_service`)

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `PORT` | Puerto asignado por Railway | `8091` |
| `ML_CORREO_SERVER_PORT` | Puerto del servidor | `8091` |
| `ML_MAIL_ENABLED` | Habilitar envío de correo | `true` |
| `ML_MAIL_HOST` | Servidor SMTP | *(requerido)* |
| `ML_MAIL_PORT` | Puerto SMTP | `587` |
| `ML_MAIL_USER` | Usuario SMTP | *(requerido)* |
| `ML_MAIL_PASSWORD` | Contraseña SMTP | *(requerido)* |
| `ML_MAIL_FROM` | Correo remitente | *(requerido)* |

## Pasos para desplegar el sistema principal

### 1. Crear servicio MySQL en Railway

- Agrega un servicio **MySQL** desde el dashboard de Railway.
- Copia la conexión generada (`MYSQL_URL` o datos: host, puerto, usuario, contraseña, database).
- Asegúrate de que la base de datos se llame `bd_citas_medicas`.
- Ejecuta el script `database/bd_citas_medicas.sql` contra la base de datos de Railway (puedes usar la pestaña **Query** de Railway o un cliente externo).

### 2. Crear servicio web para la aplicación principal

- Conecta tu repositorio o sube el código.
- Railway detectará automáticamente `pom.xml` y usará Java 17.
- Configura las variables de entorno en Railway:

```
PORT=8087
ML_DB_URL=jdbc:mysql://<host>:<puerto>/bd_citas_medicas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima
ML_DB_USER=<usuario>
ML_DB_PASSWORD=<contraseña>
ML_MAIL_ENABLED=false
```

- Railway usará `Procfile` o `system.properties`. El proyecto ya incluye `system.properties` con `java.runtime.version=17`.
- Railway generará una URL pública similar a `https://consultas.up.railway.app`.

### 3. Verificar despliegue

- Accede a `https://<tu-app>.railway.app/api/salud`
- Respuesta esperada:
```json
{"sistema":"CONSULTAS - MediLink","estado":"activo","version":"4.2.5"}
```

## Pasos para desplegar el microservicio de correo (opcional)

1. Crea otro servicio web Railway desde la carpeta `microservicios/ML_correo_service`.
2. Configura las variables de entorno SMTP:

```
PORT=8091
ML_MAIL_ENABLED=true
ML_MAIL_HOST=smtp.gmail.com
ML_MAIL_PORT=587
ML_MAIL_USER=tu-correo@gmail.com
ML_MAIL_PASSWORD=tu-contraseña-de-aplicacion
ML_MAIL_FROM=tu-correo@gmail.com
```

3. Railway generará una URL para el microservicio.
4. En el sistema principal, configura:

```
ML_MICROSERVICIO_CORREO_ENABLED=true
ML_MICROSERVICIO_CORREO_URL=https://<micro-url>.railway.app
```

## APIs disponibles

### Públicas (sin autenticación)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/salud` | Estado del sistema |
| GET | `/api/catalogo` | Catálogo del sistema (roles, estados, métodos de pago) |
| GET | `/api/correos/estado` | Estado del servicio de correo |

### Requieren autenticación (ADMIN, SECRETARIA)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/correos/prueba?ML_correo=...` | Enviar correo de prueba (solo ADMIN) |
| POST | `/api/correos/citas/{idCita}/confirmacion` | Enviar confirmación de cita |
| POST | `/api/correos/pagos/{idPago}/comprobante` | Enviar comprobante de pago |

### Requieren autenticación (ADMIN, SECRETARIA, PACIENTE)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/comprobantes/pagos/{idPago}` | Comprobante de pago en JSON |
| GET | `/api/comprobantes/pagos/{idPago}/pdf` | Comprobante de pago en PDF |

## Pruebas con curl

```bash
# Salud del sistema
curl -s https://<tu-app>.railway.app/api/salud | jq

# Catálogo del sistema
curl -s https://<tu-app>.railway.app/api/catalogo | jq

# Estado del correo
curl -s https://<tu-app>.railway.app/api/correos/estado | jq

# Comprobante de pago (requiere autenticación)
curl -s -u admin@gmail.com:123456 https://<tu-app>.railway.app/api/comprobantes/pagos/1 | jq

# Comprobante de pago en PDF
curl -o comprobante.pdf -u admin@gmail.com:123456 https://<tu-app>.railway.app/api/comprobantes/pagos/1/pdf
```

## Notas importantes

- El sistema funciona **sin correo electrónico**. Si SMTP no está configurado, las citas, pagos y reportes siguen operando normalmente.
- El microservicio de correo es **opcional**. El sistema principal puede enviar correos directamente por SMTP sin necesidad del microservicio.
- No uses contraseñas literales en `application.properties`. Siempre usa variables de entorno.
- Si usas Gmail, genera una **contraseña de aplicación** en lugar de usar tu contraseña personal.
