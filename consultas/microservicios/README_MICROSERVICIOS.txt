MICROSERVICIOS OPCIONALES - CONSULTAS MEDILINK

El proyecto principal se mantiene como aplicacion web y escritorio sobre bd_citas_medicas.
Se incluye un microservicio opcional de correo para separar la responsabilidad de envio de notificaciones.

1. Proyecto principal
   Ruta: ./
   Puerto: 8087
   Base de datos: bd_citas_medicas
   APIs:
   - GET /api/comprobantes/pagos/{idPago}
   - GET /api/comprobantes/pagos/{idPago}/pdf
   - GET /api/correos/estado
   - POST /api/correos/citas/{idCita}/confirmacion
   - POST /api/correos/pagos/{idPago}/comprobante
   - POST /api/correos/prueba?ML_correo=correo@dominio.com

2. Microservicio ML_correo_service
   Ruta: microservicios/ML_correo_service
   Puerto: 8091
   API:
   - GET /api/correo/estado
   - POST /api/correo/enviar

3. Activar uso del microservicio desde el proyecto principal
   ML_MAIL_ENABLED=true
   ML_MICROSERVICIO_CORREO_ENABLED=true
   ML_MICROSERVICIO_CORREO_URL=http://localhost:8091
   ML_MAIL_HOST=smtp.tu-proveedor.com
   ML_MAIL_PORT=587
   ML_MAIL_USER=usuario
   ML_MAIL_PASSWORD=clave
   ML_MAIL_FROM=notificaciones@tudominio.com

Si el correo o el microservicio no estan configurados, las citas, pagos, reportes y comprobantes siguen funcionando.
