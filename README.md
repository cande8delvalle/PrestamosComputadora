# Préstamos de Computadoras

App Android (Kotlin + Jetpack Compose) para registrar préstamos y devoluciones de computadoras.
Los datos, el login y las actualizaciones en tiempo real usan [Supabase](https://supabase.com).

## Configuración

### 1. Credenciales de Supabase

1. Copiar `local.properties.example` como `local.properties` (en la raíz del proyecto).
2. Completar, con los datos de Supabase > Project Settings > API:
   - `SUPABASE_URL`: la Project URL (ej: `https://xxxxxxxx.supabase.co`).
   - `SUPABASE_KEY`: la clave **publishable** (`sb_publishable_...`).

`local.properties` no se sube a Git. La clave **secret** (o `service_role`) nunca va en la app
ni en el repositorio: da acceso total a la base y saltea las reglas de seguridad (RLS).

### 2. Base de datos

El esquema está en `supabase/migrations`. Correr cada archivo en el SQL Editor de Supabase,
en este orden:

1. `20261012_schema_inicial.sql`
2. `20261012_devolucion_trigger.sql`

### 3. Compilar

Abrir el proyecto en Android Studio, o desde la terminal:

```
.\gradlew assembleDebug testDebugUnitTest
```

## Versión anterior (Room)

La primera versión guardaba los datos localmente en el teléfono con Room. Quedó guardada en la
rama `backup-room-local` y en el tag `v1.0-room-local`.
