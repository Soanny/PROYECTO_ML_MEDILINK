package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_CalendarioAdminDTO;
import com.medicas.consultas.dto.ML_OpcionDTO;
import com.medicas.consultas.dto.ML_SecretariaCitaFormDTO;
import com.medicas.consultas.dto.ML_SecretariaPagoDTO;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_SecretariaPanelService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final PasswordEncoder ML_passwordEncoder;
    private final ML_CorreoAutomaticoService ML_correoAutomaticoService;

    public ML_SecretariaPanelService(JdbcTemplate ML_jdbcTemplate,
                                     PasswordEncoder ML_passwordEncoder,
                                     ML_CorreoAutomaticoService ML_correoAutomaticoService) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_passwordEncoder = ML_passwordEncoder;
        this.ML_correoAutomaticoService = ML_correoAutomaticoService;
    }

    public Map<String, Object> ML_obtenerPerfilSecretaria(String ML_correo) {
        String ML_sql = """
            SELECT
                s.id_secretaria AS id_secretaria,
                u.id_usuario AS id_usuario,
                CONCAT(u.nombres, ' ', u.apellidos) AS secretaria,
                u.nombres AS nombres,
                u.apellidos AS apellidos,
                u.correo AS correo,
                u.dni AS dni,
                u.celular AS celular,
                u.estado AS estado,
                DATE_FORMAT(s.fecha_ingreso, '%d/%m/%Y') AS fecha_ingreso_texto,
                s.turno AS turno
            FROM secretarias s
            INNER JOIN usuarios u ON s.id_usuario = u.id_usuario
            WHERE u.correo = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_correo);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public int ML_contarTotalCitas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public int ML_contarCitasHoy() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE fecha_cita = CURDATE()");
    }

    public int ML_contarCitasPendientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Pendiente'");
    }

    public int ML_contarCitasConfirmadas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Confirmada'");
    }

    public int ML_contarCitasAtendidas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Atendida'");
    }

    public int ML_contarCitasCanceladas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Cancelada'");
    }

    public String ML_obtenerIngresosPagadosFormateado() {
        BigDecimal ML_total = ML_obtenerDecimal(ML_sqlIngresosPagadosUnicos());
        return "S/ " + ML_total.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public List<Map<String, Object>> ML_listarProximasCitas() {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.fecha_cita >= CURDATE()
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
            LIMIT 6
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_listarCitas() {
        String ML_sql = ML_sqlCitasBase() + """
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC, c.id_cita DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    public Map<String, Object> ML_obtenerCitaDetalle(Integer ML_idCita) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_cita = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idCita);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public ML_SecretariaCitaFormDTO ML_prepararNuevaCita() {
        ML_SecretariaCitaFormDTO ML_form = new ML_SecretariaCitaFormDTO();
        ML_form.setML_estado("Confirmada");
        return ML_form;
    }

    public ML_SecretariaCitaFormDTO ML_obtenerFormularioCita(Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerCitaDetalle(ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }

        ML_SecretariaCitaFormDTO ML_form = new ML_SecretariaCitaFormDTO();
        ML_form.setML_idPaciente(ML_obtenerEnteroDesdeMapa(ML_cita, "id_paciente"));
        ML_form.setML_idDoctor(ML_obtenerEnteroDesdeMapa(ML_cita, "id_doctor"));
        ML_form.setML_fechaCita(ML_obtenerTextoDesdeMapa(ML_cita, "fecha_cita_input"));
        ML_form.setML_horaInicio(ML_obtenerTextoDesdeMapa(ML_cita, "hora_inicio_input"));
        ML_form.setML_horaFin(ML_obtenerTextoDesdeMapa(ML_cita, "hora_fin_input"));
        ML_form.setML_motivo(ML_obtenerTextoDesdeMapa(ML_cita, "motivo"));
        ML_form.setML_estado(ML_obtenerTextoDesdeMapa(ML_cita, "estado"));
        return ML_form;
    }

    public List<ML_OpcionDTO> ML_listarPacientesActivos() {
        String ML_sql = """
            SELECT
                p.id_paciente,
                CONCAT(u.nombres, ' ', u.apellidos, ' - DNI: ', COALESCE(u.dni, 'Sin DNI')) AS paciente
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE u.estado = 'Activo'
            ORDER BY u.apellidos ASC, u.nombres ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> new ML_OpcionDTO(
                    rs.getInt("id_paciente"),
                    rs.getString("paciente")
            ));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> ML_listarPacientesActivosTabla() {
        String ML_sql = """
            SELECT
                p.id_paciente AS id_paciente,
                CONCAT(u.nombres, ' ', u.apellidos) AS paciente,
                u.dni AS dni,
                u.celular AS celular,
                u.correo AS correo,
                DATE_FORMAT(p.fecha_nacimiento, '%d/%m/%Y') AS fecha_nacimiento_texto,
                p.genero AS genero,
                COALESCE(p.grupo_sanguineo, '-') AS grupo_sanguineo,
                COALESCE(p.alergias, 'No registradas') AS alergias
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE u.estado = 'Activo'
            ORDER BY u.apellidos ASC, u.nombres ASC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<ML_OpcionDTO> ML_listarDoctoresActivos() {
        String ML_sql = """
            SELECT
                d.id_doctor,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos, ' - ', e.nombre_especialidad, ' - ', COALESCE(co.nombre_consultorio, 'Sin consultorio asignado')) AS doctor
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON d.id_consultorio = co.id_consultorio
            WHERE d.estado = 'Activo'
              AND u.estado = 'Activo'
            ORDER BY u.apellidos ASC, u.nombres ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> new ML_OpcionDTO(
                    rs.getInt("id_doctor"),
                    rs.getString("doctor")
            ));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void ML_registrarCitaPresencial(ML_SecretariaCitaFormDTO ML_form) {
        String ML_tipoRegistro = ML_normalizarTipoRegistroCita(ML_form.getML_tipoRegistro());
        Integer ML_idPaciente = ML_form.getML_idPaciente();
        Integer ML_idDoctor = ML_form.getML_idDoctor();
        LocalDate ML_fecha = ML_parsearFecha(ML_form.getML_fechaCita());
        LocalTime ML_horaInicio = ML_parsearHora(ML_form.getML_horaInicio(), "Debe seleccionar la hora de inicio de la cita.");
        LocalTime ML_horaFin = ML_parsearHoraFin(ML_form.getML_horaFin(), ML_horaInicio);

        if ("Invitado".equals(ML_tipoRegistro)) {
            ML_idPaciente = ML_registrarPacienteInvitado(ML_form);
        } else {
            if (ML_idPaciente == null) {
                throw new IllegalArgumentException("Debe seleccionar un paciente miembro del sistema.");
            }

            if (!ML_existePacienteActivo(ML_idPaciente)) {
                throw new IllegalArgumentException("El paciente seleccionado no existe o no está activo.");
            }
        }

        if (ML_idDoctor == null) {
            throw new IllegalArgumentException("Debe seleccionar el doctor de la cita.");
        }

        Integer ML_idConsultorio = ML_obtenerConsultorioDoctor(ML_idDoctor);

        ML_validarDisponibilidadDoctor(ML_idDoctor, ML_fecha, ML_horaInicio, ML_horaFin);
        ML_validarCruceHorario(null, ML_idDoctor, ML_idConsultorio, ML_fecha, ML_horaInicio, ML_horaFin);

        String ML_estado = ML_normalizarEstadoCita(ML_form.getML_estado());
        if ("Atendida".equalsIgnoreCase(ML_estado)) {
            throw new IllegalArgumentException("La cita solo puede marcarse como Atendida cuando el Doctor registra el historial clínico.");
        }
        String ML_motivo = ML_limpiarTexto(ML_form.getML_motivo());

        String ML_sql = """
            INSERT INTO citas
            (id_paciente, id_doctor, id_consultorio, fecha_cita, hora_inicio, hora_fin, motivo, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(
                ML_sql,
                ML_idPaciente,
                ML_idDoctor,
                ML_idConsultorio,
                Date.valueOf(ML_fecha),
                Time.valueOf(ML_horaInicio),
                Time.valueOf(ML_horaFin),
                ML_motivo,
                ML_estado
        );

        Integer ML_idCitaRegistrada = ML_obtenerUltimoIdInsertado();
        ML_correoAutomaticoService.ML_programarConfirmacionCita(ML_idCitaRegistrada);
    }

    @Transactional
    public void ML_actualizarDatosCita(Integer ML_idCita, ML_SecretariaCitaFormDTO ML_form) {
        Map<String, Object> ML_citaActual = ML_obtenerCitaDetalle(ML_idCita);

        if (ML_citaActual.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }

        String ML_estadoActual = ML_obtenerTextoDesdeMapa(ML_citaActual, "estado");
        if ("Atendida".equalsIgnoreCase(ML_estadoActual)) {
            throw new IllegalArgumentException("La cita ya está atendida y no puede modificarse.");
        }

        Integer ML_idPacienteActual = ML_obtenerEnteroDesdeMapa(ML_citaActual, "id_paciente");
        Integer ML_idDoctor = ML_form.getML_idDoctor();
        LocalDate ML_fecha = ML_parsearFecha(ML_form.getML_fechaCita());
        LocalTime ML_horaInicio = ML_parsearHora(ML_form.getML_horaInicio(), "Debe seleccionar la hora de inicio de la cita.");
        LocalTime ML_horaFin = ML_parsearHoraFin(ML_form.getML_horaFin(), ML_horaInicio);

        if (ML_idDoctor == null) {
            throw new IllegalArgumentException("Debe seleccionar el doctor de la cita.");
        }

        Integer ML_idConsultorio = ML_obtenerConsultorioDoctor(ML_idDoctor);

        ML_validarDisponibilidadDoctor(ML_idDoctor, ML_fecha, ML_horaInicio, ML_horaFin);
        ML_validarCruceHorario(ML_idCita, ML_idDoctor, ML_idConsultorio, ML_fecha, ML_horaInicio, ML_horaFin);

        String ML_estado = ML_normalizarEstadoCita(ML_form.getML_estado());
        if ("Atendida".equalsIgnoreCase(ML_estado)) {
            throw new IllegalArgumentException("La cita solo puede marcarse como Atendida cuando el Doctor registra el historial clínico.");
        }
        if ("Cancelada".equalsIgnoreCase(ML_estado) && ML_existePagoPagadoCita(ML_idCita)) {
            throw new IllegalArgumentException("No se puede cancelar una cita con pago confirmado. Primero debe anularse el pago desde el módulo Pagos.");
        }
        String ML_motivo = ML_limpiarTexto(ML_form.getML_motivo());

        String ML_sql = """
            UPDATE citas
            SET id_paciente = ?,
                id_doctor = ?,
                id_consultorio = ?,
                fecha_cita = ?,
                hora_inicio = ?,
                hora_fin = ?,
                motivo = ?,
                estado = ?
            WHERE id_cita = ?
        """;

        int ML_filas = ML_jdbcTemplate.update(
                ML_sql,
                ML_idPacienteActual,
                ML_idDoctor,
                ML_idConsultorio,
                Date.valueOf(ML_fecha),
                Time.valueOf(ML_horaInicio),
                Time.valueOf(ML_horaFin),
                ML_motivo,
                ML_estado,
                ML_idCita
        );

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar la cita.");
        }
    }

    @Transactional
    public void ML_actualizarEstadoCita(Integer ML_idCita, String ML_estado) {
        String ML_estadoActual = ML_obtenerEstadoCita(ML_idCita);
        if (ML_estadoActual == null) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }
        if ("Atendida".equalsIgnoreCase(ML_estadoActual)) {
            throw new IllegalArgumentException("La cita ya está atendida y no puede modificarse.");
        }

        String ML_estadoNormalizado = ML_normalizarEstadoCita(ML_estado);
        if ("Atendida".equalsIgnoreCase(ML_estadoNormalizado)) {
            throw new IllegalArgumentException("La cita solo puede marcarse como Atendida cuando el Doctor registra el historial clínico.");
        }
        if ("Cancelada".equalsIgnoreCase(ML_estadoNormalizado) && ML_existePagoPagadoCita(ML_idCita)) {
            throw new IllegalArgumentException("No se puede cancelar una cita con pago confirmado. Primero debe anularse el pago desde el módulo Pagos.");
        }
        int ML_filas = ML_jdbcTemplate.update(
                "UPDATE citas SET estado = ? WHERE id_cita = ? AND estado <> 'Atendida'",
                ML_estadoNormalizado,
                ML_idCita
        );

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el estado de la cita.");
        }
    }

    public ML_SecretariaPagoDTO ML_prepararPagoCita(Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerCitaDetalle(ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }

        if ("Cancelada".equalsIgnoreCase(ML_obtenerTextoDesdeMapa(ML_cita, "estado"))) {
            throw new IllegalArgumentException("No se puede registrar pago de una cita cancelada.");
        }

        if (ML_existePagoPagadoCita(ML_idCita)) {
            throw new IllegalArgumentException("La cita ya tiene un pago en estado Pagado. No puede registrarse ni modificarse otro pago.");
        }

        ML_SecretariaPagoDTO ML_pago = new ML_SecretariaPagoDTO();
        ML_pago.setML_idCita(ML_idCita);
        Object ML_precio = ML_cita.get("precio_consulta");

        if (ML_precio instanceof BigDecimal ML_bigDecimal) {
            ML_pago.setML_monto(ML_bigDecimal);
        } else if (ML_precio instanceof Number ML_number) {
            ML_pago.setML_monto(BigDecimal.valueOf(ML_number.doubleValue()));
        }

        return ML_pago;
    }

    @Transactional
    public void ML_registrarPagoCita(Integer ML_idCita, ML_SecretariaPagoDTO ML_pago) {
        if (ML_pago == null) {
            throw new IllegalArgumentException("Debe completar los datos del pago.");
        }

        Map<String, Object> ML_cita = ML_obtenerCitaDetalle(ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }

        String ML_estadoCita = ML_obtenerTextoDesdeMapa(ML_cita, "estado");
        if ("Cancelada".equalsIgnoreCase(ML_estadoCita)) {
            throw new IllegalArgumentException("No se puede registrar pago de una cita cancelada.");
        }

        if (ML_existePagoPagadoCita(ML_idCita)) {
            throw new IllegalArgumentException("Esta cita ya tiene un pago en estado Pagado. Si hubo un error, el Administrador debe anular el pago primero.");
        }

        BigDecimal ML_monto = ML_pago.getML_monto();
        if (ML_monto == null || ML_monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debe ingresar un monto válido para el pago.");
        }

        BigDecimal ML_precioConsulta = ML_obtenerBigDecimalDesdeMapa(ML_cita, "precio_consulta");
        if (ML_precioConsulta != null && ML_precioConsulta.compareTo(BigDecimal.ZERO) > 0
                && ML_monto.compareTo(ML_precioConsulta) > 0) {
            throw new IllegalArgumentException("El monto ingresado no puede superar el precio de la consulta: S/ " + ML_precioConsulta + ".");
        }

        String ML_metodoPago = ML_normalizarMetodoPago(ML_pago.getML_metodoPago());
        String ML_estadoPago = ML_normalizarEstadoPago(ML_pago.getML_estadoPago());
        if ("Pagado".equals(ML_estadoPago)
                && ML_precioConsulta != null
                && ML_precioConsulta.compareTo(BigDecimal.ZERO) > 0
                && ML_monto.compareTo(ML_precioConsulta) != 0) {
            throw new IllegalArgumentException("Para confirmar el pago como Pagado, el monto debe coincidir con el precio de la consulta: S/ " + ML_precioConsulta + ".");
        }
        String ML_codigoOperacion = ML_limpiarTexto(ML_pago.getML_codigoOperacion());
        String ML_observacion = ML_limpiarTexto(ML_pago.getML_observacion());
        Timestamp ML_fechaPago = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Lima")));
        Integer ML_idPagoPendiente = ML_obtenerIdPagoPendienteCita(ML_idCita);

        if ("Efectivo".equals(ML_metodoPago)) {
            ML_codigoOperacion = null;
            if (ML_observacion == null) {
                ML_observacion = "Pago realizado en recepción";
            }
        } else if ("Yape".equals(ML_metodoPago) || "Plin".equals(ML_metodoPago)) {
            if (ML_codigoOperacion == null) {
                throw new IllegalArgumentException("Para pagos por " + ML_metodoPago + " debe registrar el número de operación del comprobante.");
            }
            if (ML_codigoOperacion.length() < 4 || ML_codigoOperacion.length() > 30) {
                throw new IllegalArgumentException("El número de operación debe tener entre 4 y 30 caracteres.");
            }
            if (ML_existeCodigoOperacionActivo(ML_metodoPago, ML_codigoOperacion, ML_idPagoPendiente)) {
                throw new IllegalArgumentException("El número de operación de " + ML_metodoPago + " ya fue registrado en otro pago activo.");
            }

            if (ML_observacion == null) {
                ML_observacion = "Yape".equals(ML_metodoPago)
                        ? "Comprobante verificado por secretaria"
                        : "Pago confirmado manualmente";
            }
        }

        int ML_filas;
        Integer ML_idPagoRegistrado = ML_idPagoPendiente;
        if (ML_idPagoPendiente != null) {
            ML_filas = ML_jdbcTemplate.update("""
                UPDATE pagos
                SET monto = ?,
                    metodo_pago = ?,
                    estado_pago = ?,
                    codigo_operacion = ?,
                    observacion = ?,
                    fecha_pago = ?
                WHERE id_pago = ?
                  AND id_cita = ?
                  AND estado_pago = 'Pendiente'
            """,
                    ML_monto,
                    ML_metodoPago,
                    ML_estadoPago,
                    ML_codigoOperacion,
                    ML_observacion,
                    ML_fechaPago,
                    ML_idPagoPendiente,
                    ML_idCita
            );
        } else {
            String ML_sql = """
                INSERT INTO pagos
                (id_cita, monto, metodo_pago, estado_pago, codigo_operacion, observacion, fecha_pago)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

            ML_filas = ML_jdbcTemplate.update(
                    ML_sql,
                    ML_idCita,
                    ML_monto,
                    ML_metodoPago,
                    ML_estadoPago,
                    ML_codigoOperacion,
                    ML_observacion,
                    ML_fechaPago
            );
            ML_idPagoRegistrado = ML_obtenerUltimoIdInsertado();
        }

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo registrar o actualizar el pago.");
        }

        if ("Pagado".equals(ML_estadoPago)) {
            ML_jdbcTemplate.update("""
                UPDATE citas
                SET estado = 'Confirmada'
                WHERE id_cita = ?
                  AND estado NOT IN ('Atendida', 'Cancelada')
            """, ML_idCita);
            ML_correoAutomaticoService.ML_programarComprobantePago(ML_idPagoRegistrado);
        }
    }

    public List<String> ML_listarMetodosPagoPermitidos() {
        return Arrays.asList("Efectivo", "Yape", "Plin");
    }

    public List<String> ML_listarEstadosPagoPermitidos() {
        return Arrays.asList("Pagado", "Pendiente");
    }

    public List<String> ML_listarTiposComprobantePermitidos() {
        return Arrays.asList("Recibo");
    }

    public List<Map<String, Object>> ML_listarPagos() {
        String ML_sql = """
            SELECT
                pg.id_pago AS id_pago,
                pg.id_cita AS id_cita,
                pg.monto AS monto,
                pg.metodo_pago AS metodo_pago,
                pg.estado_pago AS estado_pago,
                COALESCE(pg.codigo_operacion, '-') AS codigo_operacion,
                COALESCE(pg.observacion, '-') AS observacion,
                'Recibo' AS tipo_comprobante,
                'PAGO' AS serie_comprobante,
                pg.id_pago AS numero_comprobante,
                'Generado por sistema' AS estado_comprobante,
                DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %h:%i %p') AS fecha_pago_texto,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            WHERE NOT (
                pg.estado_pago = 'Pendiente'
                AND EXISTS (
                    SELECT 1
                    FROM pagos pgp
                    WHERE pgp.id_cita = pg.id_cita
                      AND pgp.estado_pago = 'Pagado'
                )
            )
            ORDER BY pg.fecha_pago DESC, pg.id_pago DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    @Transactional
    public void ML_anularPagoAdmin(Integer ML_idPago, String ML_observacionAdmin) {
        Map<String, Object> ML_pago = ML_obtenerPagoBasico(ML_idPago);

        if (ML_pago.isEmpty()) {
            throw new IllegalArgumentException("El pago seleccionado no existe.");
        }

        String ML_estadoActual = ML_obtenerTextoDesdeMapa(ML_pago, "estado_pago");
        if ("Anulado".equalsIgnoreCase(ML_estadoActual)) {
            throw new IllegalArgumentException("El pago ya se encuentra anulado.");
        }

        Integer ML_idCita = ML_obtenerEnteroDesdeMapa(ML_pago, "id_cita");
        String ML_observacion = ML_limpiarTexto(ML_observacionAdmin);
        if (ML_observacion == null) {
            ML_observacion = "Pago anulado por Administrador por error de registro";
        }

        int ML_filas = ML_jdbcTemplate.update("""
            UPDATE pagos
            SET estado_pago = 'Anulado',
                observacion = ?
            WHERE id_pago = ?
        """, ML_observacion, ML_idPago);

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo anular el pago seleccionado.");
        }

        if (ML_idCita != null && !ML_existePagoPagadoCita(ML_idCita)) {
            ML_jdbcTemplate.update("""
                UPDATE citas
                SET estado = 'Pendiente'
                WHERE id_cita = ?
                  AND estado NOT IN ('Atendida', 'Cancelada')
            """, ML_idCita);
        }
    }

    public List<Map<String, Object>> ML_obtenerReportePagosDiarios() {
        String ML_sql = """
            SELECT
                DATE_FORMAT(fecha_pago, '%d/%m/%Y') AS fecha_pago,
                COUNT(*) AS total_operaciones,
                SUM(CASE WHEN estado_pago = 'Pagado' THEN 1 ELSE 0 END) AS pagos_confirmados,
                SUM(CASE WHEN estado_pago = 'Anulado' THEN 1 ELSE 0 END) AS pagos_anulados,
                COALESCE(SUM(CASE WHEN estado_pago = 'Pagado' THEN monto ELSE 0 END), 0) AS monto_total
            FROM pagos pg
            WHERE NOT (
                pg.estado_pago = 'Pendiente'
                AND EXISTS (
                    SELECT 1
                    FROM pagos pgp
                    WHERE pgp.id_cita = pg.id_cita
                      AND pgp.estado_pago = 'Pagado'
                )
            )
            GROUP BY DATE(fecha_pago), DATE_FORMAT(fecha_pago, '%d/%m/%Y')
            ORDER BY DATE(fecha_pago) DESC
            LIMIT 10
        """;
        return ML_consultarLista(ML_sql);
    }

    public String ML_obtenerMontoPagosHoyFormateado() {
        BigDecimal ML_total = ML_obtenerDecimal("""
            SELECT COALESCE(SUM(monto), 0)
            FROM pagos
            WHERE estado_pago = 'Pagado'
              AND DATE(fecha_pago) = CURDATE()
        """);
        return "S/ " + ML_total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public int ML_contarPagosHoy() {
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM pagos
            WHERE estado_pago = 'Pagado'
              AND DATE(fecha_pago) = CURDATE()
        """);
    }

    public int ML_contarPagosAnulados() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pagos WHERE estado_pago = 'Anulado'");
    }

    public List<Map<String, Object>> ML_listarCitasParaPago() {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.estado NOT IN ('Cancelada')
              AND NOT EXISTS (
                    SELECT 1
                    FROM pagos pg2
                    WHERE pg2.id_cita = c.id_cita
                      AND pg2.estado_pago = 'Pagado'
              )
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<ML_CalendarioAdminDTO> ML_listarCitasCalendario() {
        String ML_sql = """
            SELECT
                c.id_cita,
                c.fecha_cita,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                HOUR(c.hora_inicio) AS hora_numero,
                MINUTE(c.hora_inicio) AS minuto_numero,
                DAYOFWEEK(c.fecha_cita) AS dia_semana,
                c.estado,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS nombre_consultorio
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> {
                Date ML_fecha = rs.getDate("fecha_cita");
                String ML_fechaTexto = ML_fecha != null ? ML_fecha.toString() : "";
                String ML_estado = rs.getString("estado");
                String ML_especialidad = rs.getString("nombre_especialidad");
                int ML_diaIndex = ML_convertirDiaSemana(rs.getInt("dia_semana"));
                int ML_topPx = ML_calcularTopCalendario(rs.getInt("hora_numero"), rs.getInt("minuto_numero"));

                return new ML_CalendarioAdminDTO(
                        rs.getInt("id_cita"),
                        ML_fechaTexto,
                        rs.getString("hora_inicio_texto"),
                        ML_estado,
                        ML_obtenerClaseEstado(ML_estado),
                        rs.getString("paciente"),
                        rs.getString("doctor"),
                        ML_especialidad,
                        rs.getString("nombre_consultorio"),
                        ML_diaIndex,
                        ML_topPx,
                        ML_obtenerClaseEspecialidad(ML_especialidad)
                );
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> ML_obtenerCitasPorEstado() {
        String ML_sql = """
            SELECT estado, COUNT(*) AS total
            FROM citas
            GROUP BY estado
            ORDER BY total DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerCitasPorDoctor() {
        String ML_sql = """
            SELECT
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COUNT(c.id_cita) AS total_citas,
                SUM(CASE WHEN c.estado = 'Pendiente' THEN 1 ELSE 0 END) AS pendientes,
                SUM(CASE WHEN c.estado = 'Confirmada' THEN 1 ELSE 0 END) AS confirmadas,
                SUM(CASE WHEN c.estado = 'Atendida' THEN 1 ELSE 0 END) AS atendidas,
                SUM(CASE WHEN c.estado = 'Cancelada' THEN 1 ELSE 0 END) AS canceladas
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN citas c ON d.id_doctor = c.id_doctor
            GROUP BY d.id_doctor, u.nombres, u.apellidos, e.nombre_especialidad
            ORDER BY total_citas DESC, doctor ASC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_obtenerPagosPorMetodo() {
        String ML_sql = """
            SELECT
                metodo_pago,
                COUNT(*) AS total,
                COALESCE(SUM(monto), 0) AS monto_total
            FROM (
                SELECT p1.id_cita, p1.metodo_pago, p1.monto
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
                INNER JOIN citas c ON c.id_cita = p1.id_cita
                WHERE c.estado <> 'Cancelada'
            ) pagos_unicos
            GROUP BY metodo_pago
            ORDER BY monto_total DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    public int ML_contarPacientesActivos() {
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE u.estado = 'Activo'
        """);
    }

    public int ML_contarDoctoresActivos() {
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            WHERE d.estado = 'Activo'
              AND u.estado = 'Activo'
        """);
    }

    public int ML_contarPagosPendientes() {
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM pagos pg
            WHERE pg.estado_pago = 'Pendiente'
              AND NOT EXISTS (
                    SELECT 1
                    FROM pagos pgp
                    WHERE pgp.id_cita = pg.id_cita
                      AND pgp.estado_pago = 'Pagado'
              )
        """);
    }

    public int ML_contarPagosPagados() {
        return ML_obtenerEntero("SELECT COUNT(DISTINCT id_cita) FROM pagos WHERE estado_pago = 'Pagado'");
    }

    public List<Map<String, Object>> ML_obtenerCalendarioPorDiaSemana() {
        String ML_sql = """
            SELECT
                CASE DAYOFWEEK(fecha_cita)
                    WHEN 1 THEN 'Domingo'
                    WHEN 2 THEN 'Lunes'
                    WHEN 3 THEN 'Martes'
                    WHEN 4 THEN 'Miércoles'
                    WHEN 5 THEN 'Jueves'
                    WHEN 6 THEN 'Viernes'
                    WHEN 7 THEN 'Sábado'
                END AS dia,
                COUNT(*) AS total
            FROM citas
            GROUP BY DAYOFWEEK(fecha_cita)
            ORDER BY DAYOFWEEK(fecha_cita)
        """;
        return ML_consultarLista(ML_sql);
    }

    private Map<String, Object> ML_obtenerPagoBasico(Integer ML_idPago) {
        String ML_sql = """
            SELECT id_pago, id_cita, estado_pago
            FROM pagos
            WHERE id_pago = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPago);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private boolean ML_existePagoPagadoCita(Integer ML_idCita) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pagos WHERE id_cita = ? AND estado_pago = 'Pagado'", ML_idCita) > 0;
    }

    private Integer ML_obtenerIdPagoPendienteCita(Integer ML_idCita) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject("""
                SELECT id_pago
                FROM pagos
                WHERE id_cita = ?
                  AND estado_pago = 'Pendiente'
                ORDER BY id_pago DESC
                LIMIT 1
            """, Number.class, ML_idCita);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean ML_existeCodigoOperacionActivo(String ML_metodoPago, String ML_codigoOperacion) {
        return ML_existeCodigoOperacionActivo(ML_metodoPago, ML_codigoOperacion, null);
    }

    private boolean ML_existeCodigoOperacionActivo(String ML_metodoPago, String ML_codigoOperacion, Integer ML_idPagoIgnorado) {
        if (ML_codigoOperacion == null) {
            return false;
        }
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM pagos
            WHERE metodo_pago = ?
              AND LOWER(TRIM(codigo_operacion)) = LOWER(TRIM(?))
              AND estado_pago <> 'Anulado'
              AND (? IS NULL OR id_pago <> ?)
        """, ML_metodoPago, ML_codigoOperacion, ML_idPagoIgnorado, ML_idPagoIgnorado) > 0;
    }

    private String ML_obtenerEstadoCita(Integer ML_idCita) {
        try {
            return ML_jdbcTemplate.queryForObject("SELECT estado FROM citas WHERE id_cita = ?", String.class, ML_idCita);
        } catch (Exception e) {
            return null;
        }
    }

    private String ML_sqlIngresosPagadosUnicos() {
        return """
            SELECT COALESCE(SUM(monto), 0)
            FROM (
                SELECT p1.id_cita, p1.monto
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
                INNER JOIN citas c ON c.id_cita = p1.id_cita
                WHERE c.estado <> 'Cancelada'
            ) pagos_unicos
        """;
    }

    private String ML_normalizarTipoRegistroCita(String ML_tipoRegistro) {
        if (ML_tipoRegistro != null && ML_tipoRegistro.trim().equalsIgnoreCase("Invitado")) {
            return "Invitado";
        }
        return "Miembro";
    }

    private Integer ML_registrarPacienteInvitado(ML_SecretariaCitaFormDTO ML_form) {
        String ML_nombres = ML_limpiarTextoObligatorio(ML_form.getML_nombresInvitado(), "Debe ingresar los nombres del invitado.");
        String ML_apellidos = ML_limpiarTextoObligatorio(ML_form.getML_apellidosInvitado(), "Debe ingresar los apellidos del invitado.");
        String ML_dni = ML_limpiarTextoObligatorio(ML_form.getML_dniInvitado(), "Debe ingresar el DNI del invitado.");
        String ML_celular = ML_limpiarTextoObligatorio(ML_form.getML_celularInvitado(), "Debe ingresar el celular del invitado.");
        String ML_direccion = ML_limpiarTexto(ML_form.getML_direccionInvitado());
        String ML_correo = ML_limpiarTexto(ML_form.getML_correoInvitado());
        String ML_fechaNacimientoTexto = ML_limpiarTextoObligatorio(ML_form.getML_fechaNacimientoInvitado(), "Debe ingresar la fecha de nacimiento del invitado.");
        LocalDate ML_fechaNacimiento = ML_parsearFecha(ML_fechaNacimientoTexto);
        String ML_genero = ML_limpiarTexto(ML_form.getML_generoInvitado());
        String ML_grupoSanguineo = ML_limpiarTexto(ML_form.getML_grupoSanguineoInvitado());
        String ML_alergias = ML_limpiarTexto(ML_form.getML_alergiasInvitado());
        String ML_contactoEmergencia = ML_limpiarTexto(ML_form.getML_contactoEmergenciaInvitado());
        String ML_celularEmergencia = ML_limpiarTexto(ML_form.getML_celularEmergenciaInvitado());

        if (ML_existeDniUsuario(ML_dni)) {
            throw new IllegalArgumentException("El DNI del invitado ya pertenece a un usuario registrado. Usa la opción Registro miembro y selecciónalo de la tabla.");
        }

        if (ML_correo == null) {
            ML_correo = ML_generarCorreoInvitado(ML_dni);
        }

        if (ML_existeCorreoUsuario(ML_correo)) {
            throw new IllegalArgumentException("El correo ingresado ya pertenece a un usuario registrado. Usa otro correo o registra la cita como miembro.");
        }

        if (ML_genero == null) {
            ML_genero = "Masculino";
        }

        if (ML_grupoSanguineo == null) {
            ML_grupoSanguineo = "O+";
        }

        final String ML_correoFinal = ML_correo;
        final String ML_generoFinal = ML_genero;
        final String ML_grupoSanguineoFinal = ML_grupoSanguineo;

        KeyHolder ML_keyHolder = new GeneratedKeyHolder();
        String ML_sqlUsuario = """
            INSERT INTO usuarios
            (id_rol, nombres, apellidos, dni, celular, direccion, correo, `contraseña`, estado, fecha_registro)
            VALUES (4, ?, ?, ?, ?, ?, ?, ?, 'Activo', NOW())
        """;

        String ML_contrasenaInicial = ML_generarClaveInvitado(ML_dni, ML_celular);

        ML_jdbcTemplate.update(connection -> {
            PreparedStatement ML_ps = connection.prepareStatement(ML_sqlUsuario, Statement.RETURN_GENERATED_KEYS);
            ML_ps.setString(1, ML_nombres);
            ML_ps.setString(2, ML_apellidos);
            ML_ps.setString(3, ML_dni);
            ML_ps.setString(4, ML_celular);
            ML_ps.setString(5, ML_direccion);
            ML_ps.setString(6, ML_correoFinal.trim().toLowerCase());
            ML_ps.setString(7, ML_passwordEncoder.encode(ML_contrasenaInicial));
            return ML_ps;
        }, ML_keyHolder);

        Number ML_idUsuarioGenerado = ML_keyHolder.getKey();
        if (ML_idUsuarioGenerado == null) {
            throw new IllegalArgumentException("No se pudo registrar al invitado como paciente del sistema.");
        }

        KeyHolder ML_keyPaciente = new GeneratedKeyHolder();
        String ML_sqlPaciente = """
            INSERT INTO pacientes
            (id_usuario, fecha_nacimiento, genero, grupo_sanguineo, alergias, contacto_emergencia, celular_emergencia)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(connection -> {
            PreparedStatement ML_ps = connection.prepareStatement(ML_sqlPaciente, Statement.RETURN_GENERATED_KEYS);
            ML_ps.setInt(1, ML_idUsuarioGenerado.intValue());
            ML_ps.setDate(2, Date.valueOf(ML_fechaNacimiento));
            ML_ps.setString(3, ML_generoFinal);
            ML_ps.setString(4, ML_grupoSanguineoFinal);
            ML_ps.setString(5, ML_alergias);
            ML_ps.setString(6, ML_contactoEmergencia);
            ML_ps.setString(7, ML_celularEmergencia);
            return ML_ps;
        }, ML_keyPaciente);

        Number ML_idPacienteGenerado = ML_keyPaciente.getKey();
        if (ML_idPacienteGenerado == null) {
            throw new IllegalArgumentException("No se pudo crear el registro de paciente para el invitado.");
        }

        return ML_idPacienteGenerado.intValue();
    }

    private String ML_limpiarTextoObligatorio(String ML_texto, String ML_mensajeError) {
        String ML_limpio = ML_limpiarTexto(ML_texto);
        if (ML_limpio == null) {
            throw new IllegalArgumentException(ML_mensajeError);
        }
        return ML_limpio;
    }

    private String ML_generarCorreoInvitado(String ML_dni) {
        return "invitado." + ML_dni + "@medilink.local";
    }

    private String ML_generarClaveInvitado(String ML_dni, String ML_celular) {
        if (ML_dni != null && !ML_dni.isBlank()) {
            return ML_dni;
        }
        return ML_celular != null && !ML_celular.isBlank() ? ML_celular : "123456";
    }

    private boolean ML_existeDniUsuario(String ML_dni) {
        if (ML_dni == null || ML_dni.isBlank()) {
            return false;
        }
        return ML_obtenerEntero("SELECT COUNT(*) FROM usuarios WHERE TRIM(dni) = TRIM(?)", ML_dni.trim()) > 0;
    }

    private boolean ML_existeCorreoUsuario(String ML_correo) {
        if (ML_correo == null || ML_correo.isBlank()) {
            return false;
        }
        return ML_obtenerEntero("SELECT COUNT(*) FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(?))", ML_correo.trim()) > 0;
    }

    private String ML_sqlCitasBase() {
        return """
            SELECT
                c.id_cita AS id_cita,
                c.id_paciente AS id_paciente,
                c.id_doctor AS id_doctor,
                c.id_consultorio AS id_consultorio,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                DATE_FORMAT(c.fecha_cita, '%Y-%m-%d') AS fecha_cita_input,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                TIME_FORMAT(c.hora_inicio, '%H:%i') AS hora_inicio_input,
                TIME_FORMAT(c.hora_fin, '%h:%i %p') AS hora_fin_texto,
                TIME_FORMAT(c.hora_fin, '%H:%i') AS hora_fin_input,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                up.correo AS correo_paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                COALESCE(co.ubicacion, '-') AS ubicacion_consultorio,
                c.motivo AS motivo,
                c.estado AS estado,
                d.precio_consulta AS precio_consulta,
                COALESCE(pg.total_pagos, 0) AS total_pagos,
                COALESCE(pg.monto_pagado, 0) AS monto_pagado,
                COALESCE(pg.ultimo_estado_pago, 'Sin pago') AS ultimo_estado_pago,
                COALESCE(pg.ultimo_metodo_pago, '-') AS ultimo_metodo_pago
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            LEFT JOIN (
                SELECT
                    id_cita,
                    COUNT(*) AS total_pagos,
                    MAX(CASE WHEN estado_pago = 'Pagado' THEN monto ELSE 0 END) AS monto_pagado,
                    CASE
                        WHEN SUM(CASE WHEN estado_pago = 'Pagado' THEN 1 ELSE 0 END) > 0 THEN 'Pagado'
                        WHEN SUM(CASE WHEN estado_pago = 'Pendiente' THEN 1 ELSE 0 END) > 0 THEN 'Pendiente'
                        ELSE 'Sin pago'
                    END AS ultimo_estado_pago,
                    CASE
                        WHEN SUM(CASE WHEN estado_pago = 'Pagado' THEN 1 ELSE 0 END) > 0 THEN SUBSTRING_INDEX(GROUP_CONCAT(CASE WHEN estado_pago = 'Pagado' THEN metodo_pago END ORDER BY fecha_pago DESC, id_pago DESC), ',', 1)
                        WHEN SUM(CASE WHEN estado_pago = 'Pendiente' THEN 1 ELSE 0 END) > 0 THEN SUBSTRING_INDEX(GROUP_CONCAT(CASE WHEN estado_pago = 'Pendiente' THEN metodo_pago END ORDER BY fecha_pago DESC, id_pago DESC), ',', 1)
                        ELSE '-'
                    END AS ultimo_metodo_pago
                FROM pagos
                WHERE estado_pago <> 'Anulado'
                GROUP BY id_cita
            ) pg ON pg.id_cita = c.id_cita
        """;
    }

    private void ML_validarDisponibilidadDoctor(Integer ML_idDoctor, LocalDate ML_fecha, LocalTime ML_horaInicio, LocalTime ML_horaFin) {
        String ML_diaSemana = ML_obtenerDiaSemanaEspanol(ML_fecha);
        String ML_sql = """
            SELECT COUNT(*)
            FROM horarios
            WHERE id_doctor = ?
              AND dia_semana = ?
              AND estado = 'Activo'
              AND hora_inicio <= ?
              AND hora_fin >= ?
        """;

        int ML_total = ML_obtenerEntero(
                ML_sql,
                ML_idDoctor,
                ML_diaSemana,
                Time.valueOf(ML_horaInicio),
                Time.valueOf(ML_horaFin)
        );

        if (ML_total == 0) {
            throw new IllegalArgumentException("El doctor no tiene disponibilidad activa para ese día y rango de horas.");
        }
    }

    private void ML_validarCruceHorario(Integer ML_idCitaExcluir, Integer ML_idDoctor, Integer ML_idConsultorio, LocalDate ML_fecha, LocalTime ML_horaInicio, LocalTime ML_horaFin) {
        String ML_sqlDoctor = """
            SELECT COUNT(*)
            FROM citas
            WHERE id_doctor = ?
              AND fecha_cita = ?
              AND estado NOT IN ('Cancelada')
              AND (? IS NULL OR id_cita <> ?)
              AND hora_inicio < ?
              AND hora_fin > ?
        """;

        int ML_crucesDoctor = ML_obtenerEntero(
                ML_sqlDoctor,
                ML_idDoctor,
                Date.valueOf(ML_fecha),
                ML_idCitaExcluir,
                ML_idCitaExcluir,
                Time.valueOf(ML_horaFin),
                Time.valueOf(ML_horaInicio)
        );

        if (ML_crucesDoctor > 0) {
            throw new IllegalArgumentException("El doctor ya tiene una cita cruzada en esa fecha y horario.");
        }

        if (ML_idConsultorio == null) {
            return;
        }

        String ML_sqlConsultorio = """
            SELECT COUNT(*)
            FROM citas
            WHERE id_consultorio = ?
              AND fecha_cita = ?
              AND estado NOT IN ('Cancelada')
              AND (? IS NULL OR id_cita <> ?)
              AND hora_inicio < ?
              AND hora_fin > ?
        """;

        int ML_crucesConsultorio = ML_obtenerEntero(
                ML_sqlConsultorio,
                ML_idConsultorio,
                Date.valueOf(ML_fecha),
                ML_idCitaExcluir,
                ML_idCitaExcluir,
                Time.valueOf(ML_horaFin),
                Time.valueOf(ML_horaInicio)
        );

        if (ML_crucesConsultorio > 0) {
            throw new IllegalArgumentException("El consultorio asignado al doctor ya está ocupado en esa fecha y horario.");
        }
    }

    private String ML_obtenerDiaSemanaEspanol(LocalDate ML_fecha) {
        switch (ML_fecha.getDayOfWeek()) {
            case MONDAY:
                return "Lunes";
            case TUESDAY:
                return "Martes";
            case WEDNESDAY:
                return "Miércoles";
            case THURSDAY:
                return "Jueves";
            case FRIDAY:
                return "Viernes";
            case SATURDAY:
                return "Sábado";
            default:
                return "Domingo";
        }
    }

    private boolean ML_existePacienteActivo(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT COUNT(*)
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE p.id_paciente = ?
              AND u.estado = 'Activo'
        """;
        return ML_obtenerEntero(ML_sql, ML_idPaciente) > 0;
    }

    private Integer ML_obtenerConsultorioDoctor(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT id_consultorio
            FROM doctores
            WHERE id_doctor = ?
              AND estado = 'Activo'
            LIMIT 1
        """;

        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_idDoctor);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate ML_parsearFecha(String ML_fechaTexto) {
        if (ML_fechaTexto == null || ML_fechaTexto.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar la fecha de la cita.");
        }

        try {
            return LocalDate.parse(ML_fechaTexto.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("La fecha de la cita no tiene un formato válido.");
        }
    }

    private LocalTime ML_parsearHora(String ML_horaTexto, String ML_mensaje) {
        if (ML_horaTexto == null || ML_horaTexto.trim().isEmpty()) {
            throw new IllegalArgumentException(ML_mensaje);
        }

        try {
            return LocalTime.parse(ML_horaTexto.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("La hora ingresada no tiene un formato válido.");
        }
    }

    private LocalTime ML_parsearHoraFin(String ML_horaFinTexto, LocalTime ML_horaInicio) {
        LocalTime ML_horaFin = (ML_horaFinTexto == null || ML_horaFinTexto.trim().isEmpty())
                ? ML_horaInicio.plusMinutes(30)
                : ML_parsearHora(ML_horaFinTexto, "Debe seleccionar la hora de fin de la cita.");

        if (!ML_horaFin.isAfter(ML_horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        return ML_horaFin;
    }

    private String ML_normalizarEstadoCita(String ML_estado) {
        if (ML_estado == null || ML_estado.trim().isEmpty()) {
            return "Pendiente";
        }

        String ML_estadoLimpio = ML_estado.trim();

        if (ML_estadoLimpio.equalsIgnoreCase("Pendiente")) {
            return "Pendiente";
        }

        if (ML_estadoLimpio.equalsIgnoreCase("Confirmada")) {
            return "Confirmada";
        }

        if (ML_estadoLimpio.equalsIgnoreCase("Atendida")) {
            return "Atendida";
        }

        if (ML_estadoLimpio.equalsIgnoreCase("Cancelada")) {
            return "Cancelada";
        }

        return "Pendiente";
    }


    public boolean ML_pagoPertenecePaciente(Integer ML_idPago, String ML_correoPaciente) {
        if (ML_idPago == null || ML_correoPaciente == null || ML_correoPaciente.isBlank()) {
            return false;
        }

        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE pg.id_pago = ?
              AND LOWER(TRIM(u.correo)) = LOWER(TRIM(?))
        """, ML_idPago, ML_correoPaciente.trim()) > 0;
    }

    public Map<String, Object> ML_obtenerComprobantePago(Integer ML_idPago) {
        String ML_sql = """
            SELECT
                pg.id_pago,
                pg.id_cita,
                pg.monto,
                pg.metodo_pago,
                pg.estado_pago,
                COALESCE(pg.codigo_operacion, '-') AS codigo_operacion,
                COALESCE(pg.observacion, '-') AS observacion,
                DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %h:%i %p') AS fecha_pago_texto,
                'Recibo' AS tipo_comprobante,
                'PAGO' AS serie_comprobante,
                pg.id_pago AS numero_comprobante,
                'Generado por sistema' AS estado_comprobante,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE pg.id_pago = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPago);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void ML_exportarComprobantePagoPDF(Integer ML_idPago, HttpServletResponse ML_response) throws Exception {
        Map<String, Object> ML_comprobante = ML_obtenerComprobantePago(ML_idPago);

        if (ML_comprobante.isEmpty()) {
            throw new IllegalArgumentException("No se encontró el comprobante solicitado.");
        }

        String ML_tipo = ML_obtenerTextoDesdeMapa(ML_comprobante, "tipo_comprobante");
        String ML_serie = ML_obtenerTextoDesdeMapa(ML_comprobante, "serie_comprobante");
        int ML_numeroEntero = ML_obtenerEnteroDesdeMapa(ML_comprobante, "numero_comprobante");
        String ML_numeroFormateado = String.format("%08d", ML_numeroEntero);
        String ML_archivo = (ML_tipo + "_" + ML_serie + "-" + ML_numeroFormateado + ".pdf").replace(" ", "_");

        ML_response.setContentType("application/pdf");
        ML_response.setHeader("Content-Disposition", "attachment; filename=" + ML_archivo);

        com.lowagie.text.Document ML_documento = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 46, 46, 42, 42);
        com.lowagie.text.pdf.PdfWriter.getInstance(ML_documento, ML_response.getOutputStream());
        ML_documento.open();

        java.awt.Color ML_azul = new java.awt.Color(11, 99, 229);
        java.awt.Color ML_celeste = new java.awt.Color(18, 181, 203);
        java.awt.Color ML_fondo = new java.awt.Color(240, 247, 255);
        java.awt.Color ML_borde = new java.awt.Color(219, 226, 238);
        java.awt.Color ML_verde = new java.awt.Color(22, 163, 74);
        java.awt.Color ML_gris = new java.awt.Color(71, 85, 105);
        java.awt.Color ML_rojo = new java.awt.Color(220, 38, 38);

        com.lowagie.text.Font ML_marcaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
        com.lowagie.text.Font ML_tituloFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 15, com.lowagie.text.Font.BOLD, ML_azul);
        com.lowagie.text.Font ML_subFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, ML_gris);
        com.lowagie.text.Font ML_labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, ML_gris);
        com.lowagie.text.Font ML_valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, new java.awt.Color(15, 23, 42));
        com.lowagie.text.Font ML_valueBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, new java.awt.Color(15, 23, 42));
        com.lowagie.text.Font ML_estadoFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
        com.lowagie.text.Font ML_notaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, ML_gris);

        com.lowagie.text.pdf.PdfPTable ML_header = new com.lowagie.text.pdf.PdfPTable(2);
        ML_header.setWidthPercentage(100);
        ML_header.setWidths(new float[]{62, 38});

        com.lowagie.text.pdf.PdfPCell ML_marca = new com.lowagie.text.pdf.PdfPCell();
        ML_marca.setBackgroundColor(ML_azul);
        ML_marca.setPadding(18);
        ML_marca.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        ML_marca.addElement(new com.lowagie.text.Paragraph("MediLink", ML_marcaFont));
        ML_marca.addElement(new com.lowagie.text.Paragraph("Sistema de Gestión de Citas Médicas", new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, java.awt.Color.WHITE)));
        ML_header.addCell(ML_marca);

        com.lowagie.text.pdf.PdfPCell ML_numero = new com.lowagie.text.pdf.PdfPCell();
        ML_numero.setBackgroundColor(ML_fondo);
        ML_numero.setPadding(18);
        ML_numero.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        com.lowagie.text.Paragraph ML_recibo = new com.lowagie.text.Paragraph(ML_tipo.toUpperCase() + " DE PAGO", ML_tituloFont);
        ML_recibo.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        ML_numero.addElement(ML_recibo);
        com.lowagie.text.Paragraph ML_codigo = new com.lowagie.text.Paragraph(ML_serie + "-" + ML_numeroFormateado, ML_valueBold);
        ML_codigo.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        ML_numero.addElement(ML_codigo);
        com.lowagie.text.Paragraph ML_fecha = new com.lowagie.text.Paragraph("Fecha de emisión: " + ML_obtenerTextoDesdeMapa(ML_comprobante, "fecha_pago_texto"), ML_subFont);
        ML_fecha.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        ML_numero.addElement(ML_fecha);
        ML_header.addCell(ML_numero);
        ML_documento.add(ML_header);
        ML_documento.add(new com.lowagie.text.Paragraph(" "));

        String ML_estadoPago = ML_obtenerTextoDesdeMapa(ML_comprobante, "estado_pago");
        com.lowagie.text.pdf.PdfPTable ML_estadoTabla = new com.lowagie.text.pdf.PdfPTable(2);
        ML_estadoTabla.setWidthPercentage(100);
        ML_estadoTabla.setWidths(new float[]{68, 32});
        com.lowagie.text.pdf.PdfPCell ML_estadoTexto = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Comprobante generado desde la base de datos bd_citas_medicas.", ML_subFont));
        ML_estadoTexto.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        ML_estadoTexto.setPadding(14);
        ML_estadoTabla.addCell(ML_estadoTexto);
        com.lowagie.text.pdf.PdfPCell ML_estadoCelda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Estado: " + ML_estadoPago, ML_estadoFont));
        ML_estadoCelda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_estadoCelda.setBackgroundColor("Pagado".equalsIgnoreCase(ML_estadoPago) ? ML_verde : ML_rojo);
        ML_estadoCelda.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        ML_estadoCelda.setPadding(14);
        ML_estadoTabla.addCell(ML_estadoCelda);
        ML_documento.add(ML_estadoTabla);
        ML_documento.add(new com.lowagie.text.Paragraph(" "));

        com.lowagie.text.pdf.PdfPTable ML_datos = new com.lowagie.text.pdf.PdfPTable(2);
        ML_datos.setWidthPercentage(100);
        ML_datos.setWidths(new float[]{50, 50});
        ML_agregarBloqueComprobante(ML_datos, "DATOS DEL PACIENTE",
                "Paciente", ML_obtenerTextoDesdeMapa(ML_comprobante, "paciente"),
                "DNI", ML_obtenerTextoDesdeMapa(ML_comprobante, "dni_paciente"));
        ML_agregarBloqueComprobante(ML_datos, "DATOS DE LA ATENCIÓN",
                "Doctor", ML_obtenerTextoDesdeMapa(ML_comprobante, "doctor"),
                "Especialidad", ML_obtenerTextoDesdeMapa(ML_comprobante, "especialidad"),
                "Consultorio", ML_obtenerTextoDesdeMapa(ML_comprobante, "consultorio"),
                "Fecha de cita", ML_obtenerTextoDesdeMapa(ML_comprobante, "fecha_cita_texto") + " " + ML_obtenerTextoDesdeMapa(ML_comprobante, "hora_inicio_texto"));
        ML_documento.add(ML_datos);
        ML_documento.add(new com.lowagie.text.Paragraph(" "));

        com.lowagie.text.pdf.PdfPTable ML_pago = new com.lowagie.text.pdf.PdfPTable(4);
        ML_pago.setWidthPercentage(100);
        ML_pago.setWidths(new float[]{25, 25, 25, 25});
        ML_agregarEncabezadoTablaComprobante(ML_pago, "Método", "Código operación", "Fecha pago", "Monto");
        ML_agregarCeldaDatoComprobante(ML_pago, ML_obtenerTextoDesdeMapa(ML_comprobante, "metodo_pago"), ML_valueFont, ML_borde);
        ML_agregarCeldaDatoComprobante(ML_pago, ML_obtenerTextoDesdeMapa(ML_comprobante, "codigo_operacion"), ML_valueFont, ML_borde);
        ML_agregarCeldaDatoComprobante(ML_pago, ML_obtenerTextoDesdeMapa(ML_comprobante, "fecha_pago_texto"), ML_valueFont, ML_borde);
        ML_agregarCeldaDatoComprobante(ML_pago, "S/ " + ML_obtenerTextoDesdeMapa(ML_comprobante, "monto"), ML_valueBold, ML_borde);
        com.lowagie.text.Paragraph ML_tituloPago = new com.lowagie.text.Paragraph("Detalle del pago", ML_tituloFont);
        ML_tituloPago.setSpacingBefore(6);
        ML_tituloPago.setSpacingAfter(10);
        ML_documento.add(ML_tituloPago);
        ML_pago.setSpacingAfter(14);
        ML_documento.add(ML_pago);
        ML_documento.add(new com.lowagie.text.Paragraph(" "));

        com.lowagie.text.pdf.PdfPTable ML_totalBox = new com.lowagie.text.pdf.PdfPTable(2);
        ML_totalBox.setWidthPercentage(100);
        ML_totalBox.setWidths(new float[]{68, 32});
        com.lowagie.text.pdf.PdfPCell ML_totalTexto = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Importe total registrado por la consulta médica", ML_labelFont));
        ML_totalTexto.setBackgroundColor(new java.awt.Color(248, 250, 252));
        ML_totalTexto.setBorderColor(ML_borde);
        ML_totalTexto.setPadding(13);
        ML_totalBox.addCell(ML_totalTexto);
        com.lowagie.text.pdf.PdfPCell ML_totalMonto = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("S/ " + ML_obtenerTextoDesdeMapa(ML_comprobante, "monto"), new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, ML_azul)));
        ML_totalMonto.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        ML_totalMonto.setBackgroundColor(new java.awt.Color(239, 246, 255));
        ML_totalMonto.setBorderColor(ML_borde);
        ML_totalMonto.setPadding(13);
        ML_totalBox.addCell(ML_totalMonto);
        ML_totalBox.setSpacingBefore(8);
        ML_totalBox.setSpacingAfter(12);
        ML_documento.add(ML_totalBox);

        String ML_observacion = ML_obtenerTextoDesdeMapa(ML_comprobante, "observacion");
        if (ML_observacion != null && !ML_observacion.isBlank() && !"-".equals(ML_observacion)) {
            ML_documento.add(new com.lowagie.text.Paragraph(" "));
            com.lowagie.text.pdf.PdfPTable ML_obs = new com.lowagie.text.pdf.PdfPTable(1);
            ML_obs.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell ML_obsCelda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Observación: " + ML_observacion, ML_notaFont));
            ML_obsCelda.setBackgroundColor(ML_fondo);
            ML_obsCelda.setBorderColor(ML_borde);
            ML_obsCelda.setPadding(10);
            ML_obs.addCell(ML_obsCelda);
            ML_documento.add(ML_obs);
        }

        ML_documento.add(new com.lowagie.text.Paragraph(" "));
        com.lowagie.text.pdf.PdfPTable ML_footer = new com.lowagie.text.pdf.PdfPTable(1);
        ML_footer.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell ML_footerCelda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Este comprobante acredita el registro interno del pago de una cita médica en CONSULTAS - MediLink. No representa validación tributaria externa.", ML_notaFont));
        ML_footerCelda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_footerCelda.setBorder(com.lowagie.text.Rectangle.TOP);
        ML_footerCelda.setBorderColor(ML_borde);
        ML_footerCelda.setPadding(12);
        ML_footer.addCell(ML_footerCelda);
        ML_documento.add(ML_footer);
        ML_documento.close();
    }

    private void ML_agregarBloqueComprobante(com.lowagie.text.pdf.PdfPTable ML_tabla, String ML_titulo, String... ML_pares) {
        java.awt.Color ML_fondo = new java.awt.Color(248, 250, 252);
        java.awt.Color ML_borde = new java.awt.Color(219, 226, 238);
        java.awt.Color ML_azul = new java.awt.Color(11, 99, 229);
        com.lowagie.text.Font ML_tituloFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 11, com.lowagie.text.Font.BOLD, ML_azul);
        com.lowagie.text.Font ML_labelFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, new java.awt.Color(71, 85, 105));
        com.lowagie.text.Font ML_valueFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, new java.awt.Color(15, 23, 42));

        com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell();
        ML_celda.setPadding(18);
        ML_celda.setBorderColor(ML_borde);
        ML_celda.setBackgroundColor(ML_fondo);
        ML_celda.addElement(new com.lowagie.text.Paragraph(ML_titulo, ML_tituloFont));
        for (int i = 0; i + 1 < ML_pares.length; i += 2) {
            com.lowagie.text.Paragraph ML_linea = new com.lowagie.text.Paragraph();
            ML_linea.add(new com.lowagie.text.Chunk(ML_pares[i] + ": ", ML_labelFont));
            ML_linea.add(new com.lowagie.text.Chunk(ML_pares[i + 1] != null ? ML_pares[i + 1] : "-", ML_valueFont));
            ML_linea.setSpacingBefore(8);
            ML_linea.setLeading(13f);
            ML_celda.addElement(ML_linea);
        }
        ML_tabla.addCell(ML_celda);
    }

    private void ML_agregarEncabezadoTablaComprobante(com.lowagie.text.pdf.PdfPTable ML_tabla, String... ML_titulos) {
        java.awt.Color ML_azul = new java.awt.Color(11, 99, 229);
        com.lowagie.text.Font ML_headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
        for (String ML_titulo : ML_titulos) {
            com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_titulo, ML_headerFont));
            ML_celda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            ML_celda.setBackgroundColor(ML_azul);
            ML_celda.setPadding(11);
            ML_celda.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            ML_tabla.addCell(ML_celda);
        }
    }

    private void ML_agregarCeldaDatoComprobante(com.lowagie.text.pdf.PdfPTable ML_tabla, String ML_valor, com.lowagie.text.Font ML_fuente, java.awt.Color ML_borde) {
        com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_valor != null ? ML_valor : "-", ML_fuente));
        ML_celda.setPadding(12);
        ML_celda.setBorderColor(ML_borde);
        ML_celda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_tabla.addCell(ML_celda);
    }

    private String ML_normalizarTipoComprobante(String ML_tipoComprobante) {
        if (ML_tipoComprobante != null) {
            for (String ML_tipo : ML_listarTiposComprobantePermitidos()) {
                if (ML_tipo.equalsIgnoreCase(ML_tipoComprobante.trim())) {
                    return ML_tipo;
                }
            }
        }
        return "Recibo";
    }

    private Integer ML_obtenerUltimoIdInsertado() {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String ML_normalizarMetodoPago(String ML_metodoPago) {
        if (ML_metodoPago != null) {
            for (String ML_metodo : ML_listarMetodosPagoPermitidos()) {
                if (ML_metodo.equalsIgnoreCase(ML_metodoPago.trim())) {
                    return ML_metodo;
                }
            }
        }

        throw new IllegalArgumentException("Debe seleccionar un método de pago válido: Efectivo, Yape o Plin.");
    }

    private String ML_normalizarEstadoPago(String ML_estadoPago) {
        if (ML_estadoPago != null) {
            for (String ML_estado : ML_listarEstadosPagoPermitidos()) {
                if (ML_estado.equalsIgnoreCase(ML_estadoPago.trim())) {
                    return ML_estado;
                }
            }
        }

        return "Pagado";
    }

    private int ML_convertirDiaSemana(int ML_diaMysql) {
        return ML_diaMysql == 1 ? 7 : ML_diaMysql - 1;
    }

    private int ML_calcularTopCalendario(int ML_hora, int ML_minuto) {
        int ML_horaInicioCalendario = 8;
        int ML_altoHora = 92;
        int ML_top = ((ML_hora - ML_horaInicioCalendario) * ML_altoHora) + ((ML_minuto * ML_altoHora) / 60) + 14;

        if (ML_top < 14) {
            return 14;
        }

        if (ML_top > 880) {
            return 880;
        }

        return ML_top;
    }

    private String ML_obtenerClaseEstado(String ML_estado) {
        if (ML_estado == null) {
            return "ML_estado_default";
        }

        switch (ML_estado.toLowerCase()) {
            case "confirmada":
                return "ML_estado_confirmada";
            case "pendiente":
                return "ML_estado_pendiente";
            case "cancelada":
                return "ML_estado_cancelada";
            case "atendida":
                return "ML_estado_atendida";
            default:
                return "ML_estado_default";
        }
    }

    private String ML_obtenerClaseEspecialidad(String ML_especialidad) {
        if (ML_especialidad == null) {
            return "ML_evento_azul";
        }

        String ML_texto = ML_especialidad.toLowerCase();

        if (ML_texto.contains("cardio")) {
            return "ML_evento_verde";
        }
        if (ML_texto.contains("pedi")) {
            return "ML_evento_amarillo";
        }
        if (ML_texto.contains("derma")) {
            return "ML_evento_morado";
        }
        if (ML_texto.contains("gine")) {
            return "ML_evento_rojo";
        }
        if (ML_texto.contains("neuro")) {
            return "ML_evento_azul";
        }
        return "ML_evento_celeste";
    }

    private String ML_limpiarTexto(String ML_texto) {
        if (ML_texto == null) {
            return null;
        }
        String ML_limpio = ML_texto.trim();
        return ML_limpio.isEmpty() ? null : ML_limpio;
    }

    private Integer ML_obtenerEnteroDesdeMapa(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        if (ML_valor instanceof Number ML_numero) {
            return ML_numero.intValue();
        }
        if (ML_valor != null) {
            try {
                return Integer.parseInt(ML_valor.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public void ML_exportarReportesPDF(HttpServletResponse ML_response) throws Exception {
        ML_response.setContentType("application/pdf");
        ML_response.setHeader("Content-Disposition", "attachment; filename=reporte_secretaria.pdf");

        List<Map<String, Object>> ML_citasEstado = ML_obtenerCitasPorEstado();
        List<Map<String, Object>> ML_citasDoctor = ML_obtenerCitasPorDoctor();
        List<Map<String, Object>> ML_pagosMetodo = ML_obtenerPagosPorMetodo();

        com.lowagie.text.Document ML_documento = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate(), 42, 42, 38, 38);
        com.lowagie.text.pdf.PdfWriter ML_writer = com.lowagie.text.pdf.PdfWriter.getInstance(ML_documento, ML_response.getOutputStream());
        ML_documento.open();

        java.awt.Color ML_azulColor = new java.awt.Color(11, 99, 229);
        java.awt.Color ML_celesteColor = new java.awt.Color(18, 181, 203);
        java.awt.Color ML_grisColor = new java.awt.Color(51, 65, 85);
        java.awt.Color ML_fondoColor = new java.awt.Color(240, 247, 255);
        java.awt.Color ML_bordeColor = new java.awt.Color(219, 226, 238);

        com.lowagie.text.Font ML_tituloFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 21, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);
        com.lowagie.text.Font ML_subFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, ML_grisColor);
        com.lowagie.text.Font ML_descripcionFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(100, 116, 139));
        com.lowagie.text.Font ML_celdaFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new java.awt.Color(15, 23, 42));
        com.lowagie.text.Font ML_celdaBold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, java.awt.Color.WHITE);

        com.lowagie.text.pdf.PdfPTable ML_enc = new com.lowagie.text.pdf.PdfPTable(1);
        ML_enc.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell ML_celdaEnc = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("MediLink - Reporte de Secretaria", ML_tituloFont));
        ML_celdaEnc.setBackgroundColor(ML_azulColor);
        ML_celdaEnc.setPadding(18);
        ML_celdaEnc.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_celdaEnc.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        ML_enc.addCell(ML_celdaEnc);
        ML_documento.add(ML_enc);

        com.lowagie.text.Paragraph ML_intro = new com.lowagie.text.Paragraph("Resumen operativo de citas, doctores y pagos registrados en el sistema.", ML_descripcionFont);
        ML_intro.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        ML_intro.setSpacingBefore(8);
        ML_intro.setSpacingAfter(18);
        ML_documento.add(ML_intro);

        ML_agregarTituloReportePdf(ML_documento, "Citas por estado", "Distribución de citas según su estado actual.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_tabla1 = new com.lowagie.text.pdf.PdfPTable(2);
        ML_tabla1.setWidthPercentage(60);
        ML_tabla1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        ML_tabla1.setWidths(new float[]{70, 30});
        ML_tabla1.setSpacingAfter(18);
        ML_agregarHeaderReportePdf(ML_tabla1, ML_azulColor, ML_celdaBold, "Estado", "Total");
        for (Map<String, Object> ML_fila : ML_citasEstado) {
            ML_agregarCeldaReportePdf(ML_tabla1, ML_obtenerTextoDesdeMapa(ML_fila, "estado"), ML_celdaFont, ML_bordeColor);
            ML_agregarCeldaReportePdf(ML_tabla1, ML_obtenerTextoDesdeMapa(ML_fila, "total"), ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_tabla1);

        ML_agregarTituloReportePdf(ML_documento, "Citas por doctor", "Relación de citas asignadas, pendientes, confirmadas, atendidas y canceladas por médico.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_tabla2 = new com.lowagie.text.pdf.PdfPTable(7);
        ML_tabla2.setWidthPercentage(100);
        ML_tabla2.setWidths(new float[]{25, 22, 10, 11, 13, 11, 12});
        ML_tabla2.setSpacingAfter(18);
        ML_agregarHeaderReportePdf(ML_tabla2, ML_celesteColor, ML_celdaBold, "Doctor", "Especialidad", "Total", "Pend.", "Conf.", "Atend.", "Cancel.");
        for (Map<String, Object> ML_fila : ML_citasDoctor) {
            String[] ML_vals = {ML_obtenerTextoDesdeMapa(ML_fila, "doctor"), ML_obtenerTextoDesdeMapa(ML_fila, "especialidad"),
                ML_obtenerTextoDesdeMapa(ML_fila, "total_citas"), ML_obtenerTextoDesdeMapa(ML_fila, "pendientes"),
                ML_obtenerTextoDesdeMapa(ML_fila, "confirmadas"), ML_obtenerTextoDesdeMapa(ML_fila, "atendidas"),
                ML_obtenerTextoDesdeMapa(ML_fila, "canceladas")};
            for (String ML_valor : ML_vals) ML_agregarCeldaReportePdf(ML_tabla2, ML_valor, ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_tabla2);

        ML_agregarTituloReportePdf(ML_documento, "Pagos por método", "Montos recaudados agrupados por método de pago registrado.", ML_subFont, ML_descripcionFont);
        com.lowagie.text.pdf.PdfPTable ML_tabla3 = new com.lowagie.text.pdf.PdfPTable(3);
        ML_tabla3.setWidthPercentage(72);
        ML_tabla3.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_LEFT);
        ML_tabla3.setWidths(new float[]{45, 20, 35});
        ML_tabla3.setSpacingAfter(10);
        ML_agregarHeaderReportePdf(ML_tabla3, ML_azulColor, ML_celdaBold, "Método", "Total", "Monto total");
        for (Map<String, Object> ML_fila : ML_pagosMetodo) {
            String[] ML_vals = {ML_obtenerTextoDesdeMapa(ML_fila, "metodo_pago"), ML_obtenerTextoDesdeMapa(ML_fila, "total"), "S/ " + ML_fila.get("monto_total")};
            for (String ML_valor : ML_vals) ML_agregarCeldaReportePdf(ML_tabla3, ML_valor, ML_celdaFont, ML_bordeColor);
        }
        ML_documento.add(ML_tabla3);

        com.lowagie.text.Paragraph ML_nota = new com.lowagie.text.Paragraph("Reporte generado con información registrada en bd_citas_medicas.", ML_descripcionFont);
        ML_nota.setSpacingBefore(8);
        ML_documento.add(ML_nota);
        ML_documento.close();
        ML_writer.close();
    }

    private void ML_agregarTituloReportePdf(com.lowagie.text.Document ML_documento, String ML_titulo, String ML_descripcion, com.lowagie.text.Font ML_tituloFont, com.lowagie.text.Font ML_descripcionFont) throws Exception {
        com.lowagie.text.Paragraph ML_t = new com.lowagie.text.Paragraph(ML_titulo, ML_tituloFont);
        ML_t.setSpacingBefore(8);
        ML_t.setSpacingAfter(4);
        ML_documento.add(ML_t);
        com.lowagie.text.Paragraph ML_d = new com.lowagie.text.Paragraph(ML_descripcion, ML_descripcionFont);
        ML_d.setSpacingAfter(10);
        ML_documento.add(ML_d);
    }

    private void ML_agregarHeaderReportePdf(com.lowagie.text.pdf.PdfPTable ML_tabla, java.awt.Color ML_color, com.lowagie.text.Font ML_font, String... ML_headers) {
        for (String ML_h : ML_headers) {
            com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_h, ML_font));
            ML_celda.setBackgroundColor(ML_color);
            ML_celda.setPadding(9);
            ML_celda.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            ML_celda.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            ML_tabla.addCell(ML_celda);
        }
    }

    private void ML_agregarCeldaReportePdf(com.lowagie.text.pdf.PdfPTable ML_tabla, String ML_valor, com.lowagie.text.Font ML_font, java.awt.Color ML_borde) {
        com.lowagie.text.pdf.PdfPCell ML_celda = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(ML_valor != null ? ML_valor : "-", ML_font));
        ML_celda.setPadding(8);
        ML_celda.setBorderColor(ML_borde);
        ML_celda.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE);
        ML_tabla.addCell(ML_celda);
    }

    public void ML_exportarReportesExcel(HttpServletResponse ML_response) throws Exception {
        ML_response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ML_response.setHeader("Content-Disposition", "attachment; filename=reporte_secretaria.xlsx");

        List<Map<String, Object>> ML_citasEstado = ML_obtenerCitasPorEstado();
        List<Map<String, Object>> ML_citasDoctor = ML_obtenerCitasPorDoctor();
        List<Map<String, Object>> ML_pagosMetodo = ML_obtenerPagosPorMetodo();

        org.apache.poi.xssf.usermodel.XSSFWorkbook ML_libro = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.CellStyle ML_headerStyle = ML_libro.createCellStyle();
        org.apache.poi.ss.usermodel.Font ML_headerFont = ML_libro.createFont();
        ML_headerFont.setBold(true);
        ML_headerFont.setFontHeightInPoints((short) 12);
        ML_headerStyle.setFont(ML_headerFont);

        org.apache.poi.xssf.usermodel.XSSFSheet ML_hoja1 = ML_libro.createSheet("Citas por Estado");
        ML_hoja1.createRow(0).createCell(0).setCellValue("Estado");
        ML_hoja1.getRow(0).createCell(1).setCellValue("Total");
        ML_hoja1.getRow(0).getCell(0).setCellStyle(ML_headerStyle);
        ML_hoja1.getRow(0).getCell(1).setCellStyle(ML_headerStyle);
        int ML_fila = 1;
        for (Map<String, Object> ML_row : ML_citasEstado) {
            ML_hoja1.createRow(ML_fila).createCell(0).setCellValue(ML_obtenerTextoDesdeMapa(ML_row, "estado"));
            ML_hoja1.getRow(ML_fila).createCell(1).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "total"));
            ML_fila++;
        }

        org.apache.poi.xssf.usermodel.XSSFSheet ML_hoja2 = ML_libro.createSheet("Citas por Doctor");
        String[] ML_cols2 = {"Doctor", "Especialidad", "Total", "Pendientes", "Confirmadas", "Atendidas", "Canceladas"};
        ML_hoja2.createRow(0);
        for (int i = 0; i < ML_cols2.length; i++) {
            ML_hoja2.getRow(0).createCell(i).setCellValue(ML_cols2[i]);
            ML_hoja2.getRow(0).getCell(i).setCellStyle(ML_headerStyle);
        }
        ML_fila = 1;
        for (Map<String, Object> ML_row : ML_citasDoctor) {
            ML_hoja2.createRow(ML_fila).createCell(0).setCellValue(ML_obtenerTextoDesdeMapa(ML_row, "doctor"));
            ML_hoja2.getRow(ML_fila).createCell(1).setCellValue(ML_obtenerTextoDesdeMapa(ML_row, "especialidad"));
            ML_hoja2.getRow(ML_fila).createCell(2).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "total_citas"));
            ML_hoja2.getRow(ML_fila).createCell(3).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "pendientes"));
            ML_hoja2.getRow(ML_fila).createCell(4).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "confirmadas"));
            ML_hoja2.getRow(ML_fila).createCell(5).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "atendidas"));
            ML_hoja2.getRow(ML_fila).createCell(6).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "canceladas"));
            ML_fila++;
        }

        org.apache.poi.xssf.usermodel.XSSFSheet ML_hoja3 = ML_libro.createSheet("Pagos por Metodo");
        String[] ML_cols3 = {"Metodo", "Total", "Monto Total"};
        ML_hoja3.createRow(0);
        for (int i = 0; i < ML_cols3.length; i++) {
            ML_hoja3.getRow(0).createCell(i).setCellValue(ML_cols3[i]);
            ML_hoja3.getRow(0).getCell(i).setCellStyle(ML_headerStyle);
        }
        ML_fila = 1;
        for (Map<String, Object> ML_row : ML_pagosMetodo) {
            ML_hoja3.createRow(ML_fila).createCell(0).setCellValue(ML_obtenerTextoDesdeMapa(ML_row, "metodo_pago"));
            ML_hoja3.getRow(ML_fila).createCell(1).setCellValue(ML_obtenerDoubleDesdeMapa(ML_row, "total"));
            ML_hoja3.getRow(ML_fila).createCell(2).setCellValue("S/ " + ML_row.get("monto_total"));
            ML_fila++;
        }

        for (int i = 0; i < ML_libro.getNumberOfSheets(); i++) {
            ML_libro.getSheetAt(i).autoSizeColumn(0);
            ML_libro.getSheetAt(i).autoSizeColumn(1);
        }

        ML_libro.write(ML_response.getOutputStream());
        ML_libro.close();
    }

    private BigDecimal ML_obtenerBigDecimalDesdeMapa(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        if (ML_valor instanceof BigDecimal ML_decimal) {
            return ML_decimal;
        }
        if (ML_valor instanceof Number ML_numero) {
            return BigDecimal.valueOf(ML_numero.doubleValue());
        }
        if (ML_valor != null) {
            try {
                return new BigDecimal(ML_valor.toString());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private String ML_obtenerTextoDesdeMapa(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        return ML_valor != null ? ML_valor.toString() : "";
    }

    private double ML_obtenerDoubleDesdeMapa(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        if (ML_valor instanceof Number) return ((Number) ML_valor).doubleValue();
        return 0.0;
    }

    private int ML_obtenerEntero(String ML_sql, Object... ML_parametros) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_parametros);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private BigDecimal ML_obtenerDecimal(String ML_sql, Object... ML_parametros) {
        try {
            BigDecimal ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, BigDecimal.class, ML_parametros);
            return ML_resultado != null ? ML_resultado : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private List<Map<String, Object>> ML_consultarLista(String ML_sql, Object... ML_parametros) {
        try {
            return ML_jdbcTemplate.queryForList(ML_sql, ML_parametros);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
