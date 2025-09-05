
# FLUJO FILTER
Cliente (Postman / Frontend)
         |
         | 1. Request HTTP
         |    (con o sin Header Authorization)
         v
+----------------------+
| JwtAuthenticationFilter (WebFilter) |
+----------------------+
         |
         | 2. Lee Header "Authorization"
         |
         +--> ❌ NO hay header o no empieza con "Bearer "
         |        → deja pasar al endpoint sin autenticación
         |
         +--> ✅ Hay header con "Bearer ..."
                   |
                   v
            3. Extrae token JWT
                   |
                   v
            4. Valida token (JwtUtil)
                 ├─ Firma válida? 
                 ├─ No expirado?
                 └─ Estructura correcta?
                   |
        ┌──────────┴──────────┐
        |                     |
   ❌ Token inválido      ✅ Token válido
   (firma, expirado,       |
   corrupto, etc.)         v
        |            5. Extrae Claims (payload)
        |               - username
        |               - role
        |               - otros datos
        |
        |            6. Crea Authentication
        |               UsernamePasswordAuthenticationToken
        |               con autoridad "ROLE_X"
        |
        v
  401 UNAUTHORIZED        |
  (Response corta aquí)   v
                       7. Guarda Authentication
                          en ReactiveSecurityContextHolder
                          |
                          v
                  8. Continúa la cadena de filtros
                     y llega al endpoint protegido
