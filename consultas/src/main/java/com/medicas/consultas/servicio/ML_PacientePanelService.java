package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_PacientePagoVirtualDTO;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_PacientePanelService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final ML_CorreoAutomaticoService ML_correoAutomaticoService;

    public ML_PacientePanelService(JdbcTemplate ML_jdbcTemplate,
                                   ML_CorreoAutomaticoService ML_correoAutomaticoService) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_correoAutomaticoService = ML_correoAutomaticoService;
    }

    public Integer ML_obtenerIdPacientePorCorreo(String ML_correo) {
        String ML_sql = """
            SELECT p.id_paciente
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE u.correo = ?
              AND u.estado = 'Activo'
            LIMIT 1
        """;

        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_correo);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("No se encontró el paciente autenticado.");
        }
    }

    public Map<String, Object> ML_obtenerPerfilPaciente(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                p.id_paciente,
                CONCAT(u.nombres, ' ', u.apellidos) AS paciente,
                u.nombres,
                u.apellidos,
                u.dni,
                u.celular,
                u.correo,
                u.direccion,
                DATE_FORMAT(p.fecha_nacimiento, '%d/%m/%Y') AS fecha_nacimiento_texto,
                p.genero,
                p.grupo_sanguineo,
                p.alergias,
                p.contacto_emergencia,
                p.celular_emergencia
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE p.id_paciente = ?
            LIMIT 1
        """;
        return ML_consultarMapa(ML_sql, ML_idPaciente);
    }

    public int ML_contarCitasAsignadas(Integer ML_idPaciente) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_paciente = ?", ML_idPaciente);
    }

    public int ML_contarCitasPendientes(Integer ML_idPaciente) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_paciente = ? AND estado = 'Pendiente'", ML_idPaciente);
    }

    public int ML_contarCitasConfirmadas(Integer ML_idPaciente) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_paciente = ? AND estado = 'Confirmada'", ML_idPaciente);
    }

    public int ML_contarCitasAtendidas(Integer ML_idPaciente) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_paciente = ? AND estado = 'Atendida'", ML_idPaciente);
    }

    public int ML_contarHistorialClinico(Integer ML_idPaciente) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM historial_clinico WHERE id_paciente = ?", ML_idPaciente);
    }

    public int ML_contarSeguimientos(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT COUNT(*)
            FROM historial_clinico
            WHERE id_paciente = ?
              AND (
                    NULLIF(TRIM(COALESCE(tratamiento, '')), '') IS NOT NULL
                 OR NULLIF(TRIM(COALESCE(receta, '')), '') IS NOT NULL
                 OR NULLIF(TRIM(COALESCE(observaciones, '')), '') IS NOT NULL
              )
        """;
        return ML_obtenerEntero(ML_sql, ML_idPaciente);
    }

    public String ML_obtenerTotalPagadoFormateado(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT COALESCE(SUM(pu.monto), 0)
            FROM (
                SELECT p1.id_cita, p1.monto
                FROM pagos p1
                INNER JOIN (
                    SELECT id_cita, MAX(id_pago) AS id_pago
                    FROM pagos
                    WHERE estado_pago = 'Pagado'
                    GROUP BY id_cita
                ) ult ON ult.id_pago = p1.id_pago
            ) pu
            INNER JOIN citas c ON pu.id_cita = c.id_cita
            WHERE c.id_paciente = ?
              AND c.estado <> 'Cancelada'
        """;

        try {
            BigDecimal ML_total = ML_jdbcTemplate.queryForObject(ML_sql, BigDecimal.class, ML_idPaciente);
            if (ML_total == null) {
                ML_total = BigDecimal.ZERO;
            }
            return "S/ " + ML_total.setScale(2).toPlainString();
        } catch (Exception e) {
            return "S/ 0.00";
        }
    }

    public List<Map<String, Object>> ML_listarProximasCitas(Integer ML_idPaciente) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_paciente = ?
              AND c.estado NOT IN ('Cancelada', 'Atendida')
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
            LIMIT 5
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_listarCitasAsignadas(Integer ML_idPaciente) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_paciente = ?
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC, c.id_cita DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_listarHistorialClinico(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                h.id_historial,
                h.id_cita,
                h.id_paciente,
                h.id_doctor,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y %h:%i %p') AS fecha_registro_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                TIME_FORMAT(c.hora_fin, '%h:%i %p') AS hora_fin_texto,
                c.motivo,
                c.estado AS estado_cita,
                h.diagnostico,
                h.tratamiento,
                h.receta,
                h.observaciones,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE h.id_paciente = ?
            ORDER BY h.fecha_registro DESC, c.fecha_cita DESC, c.hora_inicio DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_listarSeguimientoTratamiento(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                h.id_historial,
                h.id_cita,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y %h:%i %p') AS fecha_registro_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                c.motivo,
                c.estado AS estado_cita,
                h.diagnostico,
                h.tratamiento,
                h.receta,
                h.observaciones,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE h.id_paciente = ?
              AND (
                    NULLIF(TRIM(COALESCE(h.tratamiento, '')), '') IS NOT NULL
                 OR NULLIF(TRIM(COALESCE(h.receta, '')), '') IS NOT NULL
                 OR NULLIF(TRIM(COALESCE(h.observaciones, '')), '') IS NOT NULL
              )
            ORDER BY h.fecha_registro DESC, c.fecha_cita DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_listarPagosPaciente(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                pg.id_pago,
                pg.id_cita,
                pg.monto,
                pg.metodo_pago,
                pg.estado_pago,
                pg.codigo_operacion,
                DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %h:%i %p') AS fecha_pago_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                c.estado AS estado_cita
            FROM pagos pg
            INNER JOIN (
                SELECT id_cita, MAX(id_pago) AS id_pago
                FROM pagos
                WHERE estado_pago = 'Pagado'
                GROUP BY id_cita
            ) ult ON ult.id_pago = pg.id_pago
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE c.id_paciente = ?
              AND c.estado <> 'Cancelada'
            ORDER BY pg.fecha_pago DESC, pg.id_pago DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_listarCitasParaPagoVirtual(Integer ML_idPaciente) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_paciente = ?
              AND c.estado <> 'Cancelada'
              AND (d.precio_consulta - COALESCE(pg.monto_pagado, 0)) > 0
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public Map<String, Object> ML_obtenerCitaPaciente(Integer ML_idPaciente, Integer ML_idCita) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_paciente = ?
              AND c.id_cita = ?
            LIMIT 1
        """;
        return ML_consultarMapa(ML_sql, ML_idPaciente, ML_idCita);
    }

    public BigDecimal ML_obtenerTotalPendientePago(Integer ML_idPaciente) {
        BigDecimal ML_total = BigDecimal.ZERO;
        for (Map<String, Object> ML_cita : ML_listarCitasParaPagoVirtual(ML_idPaciente)) {
            BigDecimal ML_saldo = ML_obtenerBigDecimalDesdeMapa(ML_cita, "saldo_pendiente");
            if (ML_saldo != null && ML_saldo.compareTo(BigDecimal.ZERO) > 0) {
                ML_total = ML_total.add(ML_saldo);
            }
        }
        return ML_total.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public String ML_obtenerTotalPendientePagoFormateado(Integer ML_idPaciente) {
        return "S/ " + ML_obtenerTotalPendientePago(ML_idPaciente).toPlainString();
    }

    public ML_PacientePagoVirtualDTO ML_prepararPagoSeleccion(Integer ML_idPaciente) {
        ML_PacientePagoVirtualDTO ML_pago = new ML_PacientePagoVirtualDTO();
        ML_pago.setML_seleccionPago("");
        ML_pago.setML_monto(ML_obtenerTotalPendientePago(ML_idPaciente));
        ML_pago.setML_metodoPago("Yape");
        return ML_pago;
    }

    public ML_PacientePagoVirtualDTO ML_prepararPagoVirtual(Integer ML_idPaciente, Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerCitaPaciente(ML_idPaciente, ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no pertenece al paciente autenticado.");
        }

        String ML_estadoCita = ML_obtenerTextoDesdeMapa(ML_cita, "estado");
        if (ML_estadoCita != null && ML_estadoCita.equalsIgnoreCase("Cancelada")) {
            throw new IllegalArgumentException("No se puede pagar una cita cancelada.");
        }

        BigDecimal ML_saldo = ML_obtenerBigDecimalDesdeMapa(ML_cita, "saldo_pendiente");
        if (ML_saldo == null || ML_saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cita seleccionada ya no tiene saldo pendiente.");
        }

        ML_PacientePagoVirtualDTO ML_pago = new ML_PacientePagoVirtualDTO();
        ML_pago.setML_idCita(ML_idCita);
        ML_pago.setML_monto(ML_saldo);
        ML_pago.setML_metodoPago("Yape");
        return ML_pago;
    }

    @Transactional
    public void ML_registrarPagoSeleccionPaciente(Integer ML_idPaciente, ML_PacientePagoVirtualDTO ML_pagoForm) {
        if (ML_pagoForm == null || ML_pagoForm.getML_seleccionPago() == null || ML_pagoForm.getML_seleccionPago().trim().isEmpty()) {
            throw new IllegalArgumentException("Selecciona una cita para pagar o elige pagar todo lo pendiente.");
        }

        String ML_seleccion = ML_pagoForm.getML_seleccionPago().trim();
        String ML_metodo = ML_normalizarMetodoPagoVirtual(ML_pagoForm.getML_metodoPago());
        String ML_codigoBase = ML_limpiarTexto(ML_pagoForm.getML_codigoOperacion());

        if (ML_seleccion.equalsIgnoreCase("TODAS")) {
            List<Map<String, Object>> ML_citasPendientes = ML_listarCitasParaPagoVirtual(ML_idPaciente);
            if (ML_citasPendientes.isEmpty()) {
                throw new IllegalArgumentException("No tienes citas pendientes de pago.");
            }

            String ML_codigoOperacion = ML_codigoBase != null ? ML_codigoBase : ML_generarCodigoOperacion(ML_metodo);
            for (Map<String, Object> ML_cita : ML_citasPendientes) {
                Object ML_idCitaObjeto = ML_cita.get("id_cita");
                if (!(ML_idCitaObjeto instanceof Number)) {
                    continue;
                }

                Integer ML_idCita = ((Number) ML_idCitaObjeto).intValue();
                BigDecimal ML_saldo = ML_obtenerBigDecimalDesdeMapa(ML_cita, "saldo_pendiente");
                if (ML_saldo != null && ML_saldo.compareTo(BigDecimal.ZERO) > 0) {
                    ML_insertarPagoVirtual(ML_idCita, ML_saldo, ML_metodo, ML_codigoOperacion + "-C" + ML_idCita);
                }
            }
            return;
        }

        try {
            Integer ML_idCita = Integer.valueOf(ML_seleccion);
            ML_registrarPagoVirtual(ML_idPaciente, ML_idCita, ML_pagoForm);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La opción seleccionada para pagar no es válida.");
        }
    }

    @Transactional
    public void ML_registrarPagoVirtual(Integer ML_idPaciente, Integer ML_idCita, ML_PacientePagoVirtualDTO ML_pagoForm) {
        Map<String, Object> ML_cita = ML_obtenerCitaPaciente(ML_idPaciente, ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no pertenece al paciente autenticado.");
        }

        String ML_estadoCita = ML_obtenerTextoDesdeMapa(ML_cita, "estado");
        if (ML_estadoCita != null && ML_estadoCita.equalsIgnoreCase("Cancelada")) {
            throw new IllegalArgumentException("No se puede pagar una cita cancelada.");
        }

        String ML_metodo = ML_normalizarMetodoPagoVirtual(ML_pagoForm.getML_metodoPago());
        String ML_codigoOperacion = ML_limpiarTexto(ML_pagoForm.getML_codigoOperacion());

        if (ML_codigoOperacion == null) {
            ML_codigoOperacion = ML_generarCodigoOperacion(ML_metodo);
        }

        BigDecimal ML_saldo = ML_obtenerBigDecimalDesdeMapa(ML_cita, "saldo_pendiente");
        if (ML_saldo == null || ML_saldo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cita seleccionada ya se encuentra pagada o no tiene saldo pendiente.");
        }

        ML_insertarPagoVirtual(ML_idCita, ML_saldo, ML_metodo, ML_codigoOperacion);
    }

    private void ML_insertarPagoVirtual(Integer ML_idCita, BigDecimal ML_monto, String ML_metodo, String ML_codigoOperacion) {
        if (ML_obtenerEntero("SELECT COUNT(*) FROM pagos WHERE id_cita = ? AND estado_pago = 'Pagado'", ML_idCita) > 0) {
            throw new IllegalArgumentException("Esta cita ya tiene un pago en estado Pagado. No se puede registrar otro pago.");
        }

        String ML_sql = """
            INSERT INTO pagos (id_cita, monto, metodo_pago, estado_pago, codigo_operacion, fecha_pago)
            VALUES (?, ?, ?, 'Pagado', ?, ?)
        """;

        Timestamp ML_fechaPago = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("America/Lima")));
        ML_jdbcTemplate.update(ML_sql, ML_idCita, ML_monto, ML_metodo, ML_codigoOperacion, ML_fechaPago);
        Integer ML_idPago = ML_obtenerUltimoIdInsertado();
        ML_correoAutomaticoService.ML_programarComprobantePago(ML_idPago);
    }

    private Integer ML_obtenerUltimoIdInsertado() {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> ML_listarMetodosVirtualesPermitidos() {
        return List.of("Yape", "Plin");
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
                TIME_FORMAT(c.hora_fin, '%h:%i %p') AS hora_fin_texto,
                c.motivo,
                c.estado,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                COALESCE(co.ubicacion, '-') AS ubicacion_consultorio,
                d.precio_consulta AS precio_consulta,
                COALESCE(pg.total_pagos, 0) AS total_pagos,
                COALESCE(pg.monto_pagado, 0) AS monto_pagado,
                GREATEST(d.precio_consulta - COALESCE(pg.monto_pagado, 0), 0) AS saldo_pendiente,
                COALESCE(pg.ultimo_estado_pago, 'Sin pago') AS ultimo_estado_pago,
                COALESCE(pg.ultimo_metodo_pago, '-') AS ultimo_metodo_pago,
                COALESCE(h.total_historiales, 0) AS total_historiales
            FROM citas c
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
            LEFT JOIN (
                SELECT id_cita, COUNT(*) AS total_historiales
                FROM historial_clinico
                GROUP BY id_cita
            ) h ON h.id_cita = c.id_cita
        """;
    }

    private String ML_normalizarMetodoPagoVirtual(String ML_metodo) {
        if (ML_metodo == null || ML_metodo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un método de pago.");
        }

        String ML_metodoLimpio = ML_metodo.trim();
        for (String ML_permitido : ML_listarMetodosVirtualesPermitidos()) {
            if (ML_permitido.equalsIgnoreCase(ML_metodoLimpio)) {
                return ML_permitido;
            }
        }

        throw new IllegalArgumentException("El método de pago seleccionado no es válido para pago.");
    }

    private String ML_generarCodigoOperacion(String ML_metodo) {
        String ML_prefijo = ML_metodo == null || ML_metodo.isBlank() ? "PV" : ML_metodo.substring(0, Math.min(3, ML_metodo.length())).toUpperCase();
        return ML_prefijo + "-" + System.currentTimeMillis();
    }

    private String ML_limpiarTexto(String ML_texto) {
        if (ML_texto == null) {
            return null;
        }
        String ML_limpio = ML_texto.trim();
        return ML_limpio.isEmpty() ? null : ML_limpio;
    }

    private String ML_obtenerTextoDesdeMapa(Map<String, Object> ML_mapa, String ML_clave) {
        Object ML_valor = ML_mapa.get(ML_clave);
        return ML_valor != null ? ML_valor.toString() : null;
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
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private int ML_obtenerEntero(String ML_sql, Object... ML_parametros) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_parametros);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> ML_consultarMapa(String ML_sql, Object... ML_parametros) {
        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_parametros);
        } catch (Exception e) {
            return new HashMap<>();
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
