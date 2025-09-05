🔐 Flujo de Autenticación con JWT

Este proyecto usa un filtro WebFlux (WebFilter) que procesa y valida los tokens JWT en cada request.

📝 Cómo funciona
1️⃣ Llega la request

El cliente envía una petición HTTP al backend.

Si incluye el header Authorization: Bearer <token>, el filtro lo captura.

2️⃣ Verificación inicial

Si no hay token o no empieza con "Bearer ", la petición sigue sin autenticación.

3️⃣ Extracción del token

Se obtiene el valor después de "Bearer ".

4️⃣ Validación con JwtUtil

Se verifica:

Firma correcta

No expirado

Estructura válida

5️⃣ Manejo de errores

Token inválido o expirado → Respuesta 401 Unauthorized.

6️⃣ Token válido

Se extraen los claims: username, role, etc.

Se crea un objeto Authentication con estos datos.

El contexto de seguridad (ReactiveSecurityContextHolder) se actualiza con esa autenticación.

7️⃣ Continuación del flujo

La petición sigue su curso hacia los endpoints protegidos.

El backend ya reconoce al usuario y sus roles.