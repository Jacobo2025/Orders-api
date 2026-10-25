## Tabla resumen de `orders-api-nonproxy`

| Recurso | Método | Lambda | Plantilla de request (`application/json`) | Status HTTP | Regex de integración |
|---|---|---|---|---:|---|
| `/orders` | `GET` | `orders-find-by-customer` | `{"customerId": "$util.escapeJavaScript($input.params('customerId'))"}` | 200 | `default` (sin regex) |
| `/orders` | `GET` | `orders-find-by-customer` | Igual a la anterior | 400 | `(?s)VALIDATION_ERROR:.*` |
| `/orders` | `GET` | `orders-find-by-customer` | Igual a la anterior | 500 | `catch-all` |
| `/orders` | `POST` | `orders-create` | `$input.json('$')` | 201 | `default` (sin regex) |
| `/orders` | `POST` | `orders-create` | Igual a la anterior | 400 | `(?s)VALIDATION_ERROR:.*` |
| `/orders` | `POST` | `orders-create` | Igual a la anterior | 500 | `(?s)(?!VALIDATION_ERROR:|ORDER_NOT_FOUND:).+` |
| `/orders/{id}` | `GET` | `orders-get` | `{"id": "$util.escapeJavaScript($input.params('id'))"}` | 200 | `default` (sin regex) |
| `/orders/{id}` | `GET` | `orders-get` | Igual a la anterior | 404 | `(?s)ORDER_NOT_FOUND:.*` |
| `/orders/{id}` | `GET` | `orders-get` | Igual a la anterior | 500 | `(?s)(?!VALIDATION_ERROR:|ORDER_NOT_FOUND:).+` |

### Observaciones importantes

- **`default`**: respuesta de integración para ejecuciones exitosas que no coinciden con una regex. En `POST /orders`, devuelve `201` y construye el header `Location` con el ID de la orden.
- **`(?s)`**: permite que `.` coincida también con saltos de línea.
- **`catch-all`**: en `GET /orders`, aparece como selector literal, no como una regex universal. Verifica que la configuración real de API Gateway permita capturar los errores inesperados.
- **Plantillas de respuesta**: son distintas de las plantillas de request. Las de `400` y `404` extraen el tipo y mensaje del error; las de `500` devuelven `INTERNAL_ERROR`; y las respuestas exitosas transforman el resultado de Lambda.