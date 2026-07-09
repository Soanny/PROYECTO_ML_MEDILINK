package com.medicas.correo.api;

import jakarta.mail.internet.InternetAddress;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/correo")
public class ML_CorreoController {

    private final JavaMailSender ML_mailSender;

    @Value("${ml.mail.from:notificaciones@medilink.local}")
    private String ML_remitente;

    @Value("${ml.mail.from-name:MediLink CONSULTAS}")
    private String ML_nombreRemitente;

    @Value("${spring.mail.host:}")
    private String ML_hostCorreo;

    public ML_CorreoController(JavaMailSender ML_mailSender) {
        this.ML_mailSender = ML_mailSender;
    }

    @GetMapping("/estado")
    public ResponseEntity<Map<String, Object>> ML_estado() {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("ok", true);
        ML_respuesta.put("servicio", "ML_correo_service");
        ML_respuesta.put("smtpHost", ML_hostCorreo == null || ML_hostCorreo.isBlank() ? "No configurado" : ML_hostCorreo);
        ML_respuesta.put("fecha", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        return ResponseEntity.ok(ML_respuesta);
    }

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, Object>> ML_enviar(@Valid @RequestBody ML_CorreoRequest ML_request) {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        try {
            var ML_mensaje = ML_mailSender.createMimeMessage();
            MimeMessageHelper ML_helper = new MimeMessageHelper(ML_mensaje, true, "UTF-8");
            ML_helper.setFrom(new InternetAddress(ML_remitente, ML_nombreRemitente));
            ML_helper.setTo(ML_request.getDestinatario());
            ML_helper.setSubject(ML_request.getAsunto());
            ML_helper.setText(ML_request.getContenidoHtml(), true);
            ML_mailSender.send(ML_mensaje);
            ML_respuesta.put("ok", true);
            ML_respuesta.put("mensaje", "Correo enviado correctamente.");
            return ResponseEntity.ok(ML_respuesta);
        } catch (Exception e) {
            ML_respuesta.put("ok", false);
            ML_respuesta.put("mensaje", "No se pudo enviar el correo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(ML_respuesta);
        }
    }
}
