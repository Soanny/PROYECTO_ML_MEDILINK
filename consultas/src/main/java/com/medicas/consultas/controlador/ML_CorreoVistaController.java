package com.medicas.consultas.controlador;

import com.medicas.consultas.servicio.ML_CorreoAutomaticoService;
import com.medicas.consultas.servicio.ML_SecretariaPanelService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ML_CorreoVistaController {

    private final ML_CorreoAutomaticoService ML_correoAutomaticoService;
    private final ML_SecretariaPanelService ML_secretariaPanelService;
    private final JdbcTemplate ML_jdbcTemplate;

    public ML_CorreoVistaController(ML_CorreoAutomaticoService ML_correoAutomaticoService,
                                    ML_SecretariaPanelService ML_secretariaPanelService,
                                    JdbcTemplate ML_jdbcTemplate) {
        this.ML_correoAutomaticoService = ML_correoAutomaticoService;
        this.ML_secretariaPanelService = ML_secretariaPanelService;
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    @GetMapping("/admin/correos")
    public String ML_mostrarPanelCorreosAdmin(Model model) {
        ML_cargarDatosCorreo(model, "Administrador");
        return "admin/ML_ACorreos";
    }

    @PostMapping("/admin/correos/prueba")
    public String ML_enviarCorreoPruebaAdmin(@RequestParam("ML_correo") String ML_correo,
                                             RedirectAttributes redirectAttributes) {
        ML_agregarRespuestaCorreo(redirectAttributes, ML_correoAutomaticoService.ML_enviarCorreoPrueba(ML_correo));
        return "redirect:/admin/correos";
    }

    @PostMapping("/admin/correos/cita")
    public String ML_enviarCorreoCitaAdmin(@RequestParam("ML_idCita") Integer ML_idCita,
                                           RedirectAttributes redirectAttributes) {
        ML_agregarRespuestaCorreo(redirectAttributes, ML_correoAutomaticoService.ML_enviarConfirmacionCita(ML_idCita));
        return "redirect:/admin/correos";
    }

    @PostMapping("/admin/correos/pago")
    public String ML_enviarCorreoPagoAdmin(@RequestParam("ML_idPago") Integer ML_idPago,
                                           RedirectAttributes redirectAttributes) {
        ML_agregarRespuestaCorreo(redirectAttributes, ML_correoAutomaticoService.ML_enviarComprobantePago(ML_idPago));
        return "redirect:/admin/correos";
    }

    @GetMapping("/secretaria/correos")
    public String ML_mostrarPanelCorreosSecretaria(Authentication authentication, Model model) {
        ML_cargarDatosCorreo(model, "Secretaria");
        String ML_correo = authentication != null ? authentication.getName() : "";
        model.addAttribute("ML_perfilSecretaria", ML_secretariaPanelService.ML_obtenerPerfilSecretaria(ML_correo));
        return "secretaria/ML_correosSecretaria";
    }

    @PostMapping("/secretaria/correos/cita")
    public String ML_enviarCorreoCitaSecretaria(@RequestParam("ML_idCita") Integer ML_idCita,
                                                RedirectAttributes redirectAttributes) {
        ML_agregarRespuestaCorreo(redirectAttributes, ML_correoAutomaticoService.ML_enviarConfirmacionCita(ML_idCita));
        return "redirect:/secretaria/correos";
    }

    @PostMapping("/secretaria/correos/pago")
    public String ML_enviarCorreoPagoSecretaria(@RequestParam("ML_idPago") Integer ML_idPago,
                                                RedirectAttributes redirectAttributes) {
        ML_agregarRespuestaCorreo(redirectAttributes, ML_correoAutomaticoService.ML_enviarComprobantePago(ML_idPago));
        return "redirect:/secretaria/correos";
    }

    private void ML_cargarDatosCorreo(Model model, String ML_rol) {
        model.addAttribute("ML_tituloPagina", "Notificaciones");
        model.addAttribute("ML_rolVista", ML_rol);
        model.addAttribute("ML_estadoCorreo", ML_correoAutomaticoService.ML_estadoServicioCorreo());
        model.addAttribute("ML_ultimasCitas", ML_listarUltimasCitas());
        model.addAttribute("ML_ultimosPagos", ML_listarUltimosPagos());
    }

    private void ML_agregarRespuestaCorreo(RedirectAttributes redirectAttributes, Map<String, Object> ML_respuesta) {
        boolean ML_ok = Boolean.TRUE.equals(ML_respuesta.get("ok"));
        String ML_mensaje = String.valueOf(ML_respuesta.getOrDefault("mensaje", "Solicitud procesada."));
        if (ML_ok) {
            redirectAttributes.addFlashAttribute("ML_mensajeExito", ML_mensaje);
        } else {
            redirectAttributes.addFlashAttribute("ML_mensajeError", ML_mensaje);
        }
    }

    private List<Map<String, Object>> ML_listarUltimasCitas() {
        String ML_sql = """
            SELECT
                c.id_cita,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.correo AS correo_paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%H:%i') AS hora_inicio_texto,
                c.estado
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC, c.id_cita DESC
            LIMIT 20
        """;
        return ML_consultarLista(ML_sql);
    }

    private List<Map<String, Object>> ML_listarUltimosPagos() {
        String ML_sql = """
            SELECT
                pg.id_pago,
                pg.id_cita,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.correo AS correo_paciente,
                pg.metodo_pago,
                pg.estado_pago,
                pg.monto,
                DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %H:%i') AS fecha_pago_texto
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            ORDER BY pg.fecha_pago DESC, pg.id_pago DESC
            LIMIT 20
        """;
        return ML_consultarLista(ML_sql);
    }

    private List<Map<String, Object>> ML_consultarLista(String ML_sql) {
        try {
            return ML_jdbcTemplate.queryForList(ML_sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
