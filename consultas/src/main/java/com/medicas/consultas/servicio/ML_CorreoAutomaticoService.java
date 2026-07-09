package com.medicas.consultas.servicio;

import jakarta.mail.internet.InternetAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

@Service
public class ML_CorreoAutomaticoService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final ObjectProvider<JavaMailSender> ML_mailSenderProvider;
    private final RestTemplate ML_restTemplate = new RestTemplate();

    @Value("${ml.mail.enabled:false}")
    private boolean ML_correoHabilitado;

    @Value("${ml.mail.from:notificaciones@medilink.local}")
    private String ML_correoRemitente;

    @Value("${ml.mail.from-name:MediLink CONSULTAS}")
    private String ML_nombreRemitente;

    @Value("${spring.mail.host:}")
    private String ML_hostCorreo;

    @Value("${spring.mail.password:}")
    private String ML_mailPassword;

    @Value("${ml.microservicio.correo.enabled:false}")
    private boolean ML_microservicioCorreoHabilitado;

    @Value("${ml.microservicio.correo.url:http://localhost:8091}")
    private String ML_microservicioCorreoUrl;

    public ML_CorreoAutomaticoService(JdbcTemplate ML_jdbcTemplate,
                                      ObjectProvider<JavaMailSender> ML_mailSenderProvider) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_mailSenderProvider = ML_mailSenderProvider;
    }

    public Map<String, Object> ML_estadoServicioCorreo() {
        Map<String, Object> ML_estado = new LinkedHashMap<>();
        ML_estado.put("correoAutomatico", ML_correoHabilitado ? "Habilitado" : "Deshabilitado");
        ML_estado.put("smtpHost", ML_hostCorreo == null || ML_hostCorreo.isBlank() ? "No configurado" : ML_hostCorreo);
        ML_estado.put("remitente", ML_correoRemitente);
        ML_estado.put("microservicioCorreo", ML_microservicioCorreoHabilitado ? "Habilitado" : "Deshabilitado");
        ML_estado.put("microservicioUrl", ML_microservicioCorreoUrl);
        ML_estado.put("fechaConsulta", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return ML_estado;
    }

    public void ML_programarConfirmacionCita(Integer ML_idCita) {
        ML_ejecutarDespuesDeCommit(() -> ML_enviarConfirmacionCitaAsync(ML_idCita));
    }

    public void ML_programarComprobantePago(Integer ML_idPago) {
        ML_ejecutarDespuesDeCommit(() -> ML_enviarComprobantePagoAsync(ML_idPago));
    }

    private void ML_ejecutarDespuesDeCommit(Runnable ML_accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ML_accion.run();
                }
            });
        } else {
            ML_accion.run();
        }
    }

    @Async
    public void ML_enviarConfirmacionCitaAsync(Integer ML_idCita) {
        ML_enviarConfirmacionCita(ML_idCita);
    }

    @Async
    public void ML_enviarComprobantePagoAsync(Integer ML_idPago) {
        ML_enviarComprobantePago(ML_idPago);
    }

    public Map<String, Object> ML_enviarConfirmacionCita(Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerDatosCita(ML_idCita);
        if (ML_cita.isEmpty()) {
            return ML_respuesta(false, "No se encontro la cita indicada.");
        }
        String ML_destino = ML_texto(ML_cita, "correo_paciente");
        String ML_asunto = "Confirmacion de cita Medilink #" + ML_idCita;
        String ML_html = ML_htmlBase(
                "Confirmacion de cita medica",
                "Tu cita fue registrada correctamente en Medilink.",
                "Paciente", ML_texto(ML_cita, "paciente"),
                "Doctor", ML_texto(ML_cita, "doctor"),
                "Especialidad", ML_texto(ML_cita, "especialidad"),
                "Consultorio", ML_texto(ML_cita, "consultorio"),
                "Fecha y hora", ML_texto(ML_cita, "fecha_cita_texto") + " " + ML_texto(ML_cita, "hora_inicio_texto"),
                "Estado", ML_texto(ML_cita, "estado")
        );
        return ML_enviarCorreo(ML_destino, ML_asunto, ML_html);
    }

    public Map<String, Object> ML_enviarComprobantePago(Integer ML_idPago) {
        Map<String, Object> ML_pago = ML_obtenerDatosPago(ML_idPago);
        if (ML_pago.isEmpty()) {
            return ML_respuesta(false, "No se encontro el pago indicado.");
        }
        String ML_destino = ML_texto(ML_pago, "correo_paciente");
        String ML_asunto = "Comprobante de pago Medilink PAGO-" + String.format("%08d", ML_idPago);
        String ML_monto = ML_formatearMonto(ML_pago.get("monto"));
        String ML_html = ML_htmlBase(
                "Comprobante de pago",
                "Se registro el pago de tu consulta medica. El PDF puede descargarse desde el sistema.",
                "Paciente", ML_texto(ML_pago, "paciente"),
                "Doctor", ML_texto(ML_pago, "doctor"),
                "Especialidad", ML_texto(ML_pago, "especialidad"),
                "Metodo de pago", ML_texto(ML_pago, "metodo_pago"),
                "Codigo de operacion", ML_texto(ML_pago, "codigo_operacion"),
                "Monto", "S/ " + ML_monto,
                "Estado", ML_texto(ML_pago, "estado_pago")
        );
        return ML_enviarCorreo(ML_destino, ML_asunto, ML_html);
    }

    public Map<String, Object> ML_enviarCorreoPrueba(String ML_destinatario) {
        String ML_html = ML_htmlBase(
                "Prueba de correo automatico",
                "La configuracion de correo automatico de Medilink esta operativa.",
                "Sistema", "CONSULTAS - MediLink",
                "Fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        );
        return ML_enviarCorreo(ML_destinatario, "Prueba de correo automatico Medilink", ML_html);
    }

    private Map<String, Object> ML_enviarCorreo(String ML_destinatario, String ML_asunto, String ML_html) {
        if (ML_destinatario == null || ML_destinatario.isBlank()) {
            return ML_respuesta(false, "No existe correo del destinatario.");
        }
        if (!ML_correoHabilitado) {
            return ML_respuesta(true, "Notificaciones deshabilitadas - el sistema funciona sin enviar correos.");
        }

        if (ML_microservicioCorreoHabilitado) {
            return ML_enviarMedianteMicroservicio(ML_destinatario, ML_asunto, ML_html);
        }

        if (ML_hostCorreo != null && ML_hostCorreo.contains("sendgrid.net")) {
            return ML_enviarPorSendGridAPI(ML_destinatario, ML_asunto, ML_html);
        }

        JavaMailSender ML_mailSender = ML_mailSenderProvider.getIfAvailable();
        if (ML_mailSender == null || ML_hostCorreo == null || ML_hostCorreo.isBlank()) {
            return ML_respuesta(false, "No se pudo enviar la notificacion. Revisa los datos SMTP configurados.");
        }

        try {
            var ML_mensaje = ML_mailSender.createMimeMessage();
            MimeMessageHelper ML_helper = new MimeMessageHelper(ML_mensaje, true, "UTF-8");
            ML_helper.setFrom(new InternetAddress(ML_correoRemitente, ML_nombreRemitente));
            ML_helper.setTo(ML_destinatario.trim());
            ML_helper.setSubject(ML_asunto);
            ML_helper.setText(ML_html, true);
            ML_mailSender.send(ML_mensaje);
            return ML_respuesta(true, "Notificacion enviada correctamente.");
        } catch (Exception e) {
            return ML_respuesta(false, "No se pudo enviar la notificacion: " + e.getMessage());
        }
    }

    private Map<String, Object> ML_enviarPorSendGridAPI(String ML_destinatario, String ML_asunto, String ML_html) {
        try {
            String ML_apiKey = ML_mailPassword;
            if (ML_apiKey == null || ML_apiKey.isBlank()) {
                return ML_respuesta(false, "No se pudo enviar la notificacion. API Key de SendGrid no configurada.");
            }

            HttpHeaders ML_headers = new HttpHeaders();
            ML_headers.setContentType(MediaType.APPLICATION_JSON);
            ML_headers.setBearerAuth(ML_apiKey);

            Map<String, Object> ML_from = new LinkedHashMap<>();
            ML_from.put("email", ML_correoRemitente);
            ML_from.put("name", ML_nombreRemitente);

            Map<String, Object> ML_to = new LinkedHashMap<>();
            ML_to.put("email", ML_destinatario.trim());

            Map<String, Object> ML_contentItem = new LinkedHashMap<>();
            ML_contentItem.put("type", "text/html");
            ML_contentItem.put("value", ML_html);

            Map<String, Object> ML_personalization = new LinkedHashMap<>();
            ML_personalization.put("to", List.of(ML_to));
            ML_personalization.put("subject", ML_asunto);

            Map<String, Object> ML_payload = new LinkedHashMap<>();
            ML_payload.put("personalizations", List.of(ML_personalization));
            ML_payload.put("from", ML_from);
            ML_payload.put("content", List.of(ML_contentItem));

            HttpEntity<Map<String, Object>> ML_request = new HttpEntity<>(ML_payload, ML_headers);
            ResponseEntity<String> ML_response = ML_restTemplate.postForEntity(
                    "https://api.sendgrid.com/v3/mail/send", ML_request, String.class);

            if (ML_response.getStatusCode() == HttpStatus.OK || ML_response.getStatusCode() == HttpStatus.ACCEPTED) {
                return ML_respuesta(true, "Notificacion enviada correctamente.");
            } else {
                return ML_respuesta(false, "No se pudo enviar la notificacion. SendGrid respondio: " + ML_response.getStatusCode());
            }
        } catch (Exception e) {
            return ML_respuesta(false, "No se pudo enviar la notificacion: " + e.getMessage());
        }
    }

    private Map<String, Object> ML_enviarMedianteMicroservicio(String ML_destinatario, String ML_asunto, String ML_html) {
        try {
            Map<String, Object> ML_payload = new LinkedHashMap<>();
            ML_payload.put("destinatario", ML_destinatario);
            ML_payload.put("asunto", ML_asunto);
            ML_payload.put("contenidoHtml", ML_html);
            HttpHeaders ML_headers = new HttpHeaders();
            ML_headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> ML_request = new HttpEntity<>(ML_payload, ML_headers);
            String ML_url = ML_microservicioCorreoUrl.replaceAll("/$", "") + "/api/correo/enviar";
            ML_restTemplate.postForObject(ML_url, ML_request, Map.class);
            return ML_respuesta(true, "Notificacion enviada correctamente.");
        } catch (Exception e) {
            return ML_respuesta(false, "No se pudo enviar la notificacion mediante el servicio de correo: " + e.getMessage());
        }
    }

    private Map<String, Object> ML_obtenerDatosCita(Integer ML_idCita) {
        if (ML_idCita == null) {
            return new HashMap<>();
        }
        String ML_sql = """
            SELECT c.id_cita,
                   c.estado,
                   up.correo AS correo_paciente,
                   CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                   CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                   e.nombre_especialidad AS especialidad,
                   COALESCE(co.nombre_consultorio, 'Sin consultorio') AS consultorio,
                   DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                   TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE c.id_cita = ?
            LIMIT 1
        """;
        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idCita);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private Map<String, Object> ML_obtenerDatosPago(Integer ML_idPago) {
        if (ML_idPago == null) {
            return new HashMap<>();
        }
        String ML_sql = """
            SELECT pg.id_pago,
                   pg.monto,
                   pg.metodo_pago,
                   pg.estado_pago,
                   COALESCE(pg.codigo_operacion, '-') AS codigo_operacion,
                   up.correo AS correo_paciente,
                   CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                   CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                   e.nombre_especialidad AS especialidad
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE pg.id_pago = ?
            LIMIT 1
        """;
        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPago);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String ML_htmlBase(String ML_titulo, String ML_intro, String... ML_pares) {
        StringBuilder ML_html = new StringBuilder();
        ML_html.append("<div style='font-family:Arial,sans-serif;background:#f4f7fb;padding:24px;'>");
        ML_html.append("<div style='max-width:620px;margin:auto;background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e5e7eb;'>");
        ML_html.append("<div style='background:linear-gradient(135deg,#0b63e5,#12b5cb);padding:24px;color:#fff;'>");
        ML_html.append("<h2 style='margin:0;font-size:24px;'>MediLink</h2><p style='margin:5px 0 0;'>Sistema de Gestion de Citas Medicas</p></div>");
        ML_html.append("<div style='padding:24px;'>");
        ML_html.append("<h3 style='margin-top:0;color:#0f172a;'>").append(ML_escapar(ML_titulo)).append("</h3>");
        ML_html.append("<p style='color:#475569;font-size:15px;'>").append(ML_escapar(ML_intro)).append("</p>");
        ML_html.append("<table style='width:100%;border-collapse:collapse;margin-top:16px;'>");
        for (int i = 0; i + 1 < ML_pares.length; i += 2) {
            ML_html.append("<tr>");
            ML_html.append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;color:#64748b;font-weight:bold;width:35%;'>").append(ML_escapar(ML_pares[i])).append("</td>");
            ML_html.append("<td style='padding:10px;border-bottom:1px solid #e5e7eb;color:#0f172a;'>").append(ML_escapar(ML_pares[i + 1])).append("</td>");
            ML_html.append("</tr>");
        }
        ML_html.append("</table>");
        ML_html.append("<p style='margin-top:22px;color:#94a3b8;font-size:12px;'>Correo automatico generado por CONSULTAS - MediLink.</p>");
        ML_html.append("</div></div></div>");
        return ML_html.toString();
    }

    private String ML_formatearMonto(Object ML_valor) {
        if (ML_valor == null) {
            return "0.00";
        }
        try {
            BigDecimal ML_monto = ML_valor instanceof BigDecimal ? (BigDecimal) ML_valor : new BigDecimal(String.valueOf(ML_valor));
            return String.format("%.2f", ML_monto);
        } catch (Exception e) {
            return String.valueOf(ML_valor);
        }
    }

    private String ML_texto(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        return ML_valor == null ? "-" : String.valueOf(ML_valor);
    }

    private String ML_escapar(String ML_valor) {
        if (ML_valor == null) {
            return "-";
        }
        return ML_valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private Map<String, Object> ML_respuesta(boolean ML_ok, String ML_mensaje) {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("ok", ML_ok);
        ML_respuesta.put("mensaje", ML_mensaje);
        return ML_respuesta;
    }
}
