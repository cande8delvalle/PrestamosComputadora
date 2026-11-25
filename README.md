# Préstamos de Computadoras

App Android (Kotlin + Jetpack Compose) para registrar préstamos y devoluciones de computadoras.
Los datos, el login y las actualizaciones en tiempo real usan [Supabase](https://supabase.com).

## Configuración

### 1. Credenciales de Supabase

1. Copiar `local.properties.example` como `local.properties` (en la raíz del proyecto).
2. Completar, con los datos de Supabase > Project Settings > API:
   - `SUPABASE_URL`: la Project URL (ej: `https://xxxxxxxx.supabase.co`).
   - `SUPABASE_KEY`: la clave **publishable** (la que empieza con `sb_publishable`).

`local.properties` no se sube a Git. La clave **secret** (o `service_role`) nunca va en la app
ni en el repositorio: da acceso total a la base y saltea las reglas de seguridad (RLS).

### 2. Base de datos

El esquema está en `supabase/migrations`. Correr cada archivo en el SQL Editor de Supabase,
en este orden:

1. `20261012000000_schema_inicial.sql`
2. `20261012000001_devolucion_trigger.sql`
3. `20261103000000_aulas.sql` (aulas: tabla `aula`, `prestamo.id_aula` y la vista del historial con el aula)

### 3. Recuperación de contraseña ("¿Olvidaste tu contraseña?")

Flujo de 3 pantallas: correo → código de 6 dígitos que llega por mail → contraseña nueva
(mínimo 8 caracteres). Al terminar vuelve al login con "Contraseña actualizada". Usa
`resetPasswordForEmail`, `verifyEmailOtp` (tipo `RECOVERY`) y `updateUser` de Supabase Auth;
no hay cambios en la base ni migraciones.

Requiere configurar Supabase (Dashboard > Authentication):

1. **SMTP propio** (Authentication > Emails > SMTP Settings). El mail integrado de Supabase
   permite muy pocos envíos por hora y solo a miembros del equipo del proyecto.
2. **Plantilla "Reset password"** (Authentication > Emails > Templates): tiene que mostrar el
   código y no el enlace, por ejemplo: `<p>Tu código para recuperar la contraseña es: <b>{{ .Token }}</b></p>`
3. **Código de 6 dígitos**: en Authentication > Sign In / Providers > Email, "Email OTP Length"
   en 6 (y "Email OTP Expiration" a gusto; por defecto 1 hora).

Notas: la app pide esperar 60 s entre envíos (Supabase también limita los envíos por correo).
El mensaje al pedir el código es el mismo exista o no el correo. Si se abandona el flujo después
de verificar el código, la sesión temporal que crea Supabase se cierra.

### 4. Compilar

Abrir el proyecto en Android Studio, o desde la terminal:

```
.\gradlew assembleDebug testDebugUnitTest
```

## Versión anterior (Room)

La primera versión guardaba los datos localmente en el teléfono con Room. Quedó guardada en la
rama `backup-room-local` y en el tag `v1.0-room-local`.
