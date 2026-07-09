package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_RegistroHistorialDoctorDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_DoctorPanelService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_DoctorPanelService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public Integer ML_obtenerIdDoctorPorCorreo(String ML_correo) {
        String ML_sql = """
            SELECT d.id_doctor
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            WHERE LOWER(TRIM(u.correo)) = LOWER(TRIM(?))
              AND d.estado = 'Activo'
              AND u.estado = 'Activo'
            LIMIT 1
        """;

        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_correo);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Object> ML_obtenerPerfilDoctor(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT
                d.id_doctor AS id_doctor,
                u.id_usuario AS id_usuario,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                u.nombres AS nombres,
                u.apellidos AS apellidos,
                u.dni AS dni,
                u.celular AS celular,
                u.correo AS correo,
                u.direccion AS direccion,
                d.nro_colegiatura AS nro_colegiatura,
                d.precio_consulta AS precio_consulta,
                d.estado AS estado_doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                COALESCE(co.ubicacion, '-') AS ubicacion_consultorio,
                COALESCE(co.piso, '-') AS piso_consultorio
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON d.id_consultorio = co.id_consultorio
            WHERE d.id_doctor = ?
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idDoctor);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public int ML_contarCitasAsignadas(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_doctor = ?", ML_idDoctor);
    }

    public int ML_contarCitasHoy(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_doctor = ? AND fecha_cita = CURDATE()", ML_idDoctor);
    }

    public int ML_contarCitasPendientes(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_doctor = ? AND estado = 'Pendiente'", ML_idDoctor);
    }

    public int ML_contarCitasConfirmadas(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_doctor = ? AND estado = 'Confirmada'", ML_idDoctor);
    }

    public int ML_contarCitasAtendidas(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE id_doctor = ? AND estado = 'Atendida'", ML_idDoctor);
    }

    public int ML_contarHistorialesRegistrados(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM historial_clinico WHERE id_doctor = ?", ML_idDoctor);
    }

    public int ML_contarPacientesConHistorial(Integer ML_idDoctor) {
        return ML_obtenerEntero("SELECT COUNT(DISTINCT id_paciente) FROM historial_clinico WHERE id_doctor = ?", ML_idDoctor);
    }

    public int ML_contarTratamientosEnSeguimiento(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT COUNT(*)
            FROM historial_clinico
            WHERE id_doctor = ?
              AND (
                    NULLIF(TRIM(COALESCE(tratamiento, '')), '') IS NOT NULL
                    OR NULLIF(TRIM(COALESCE(receta, '')), '') IS NOT NULL
                    OR NULLIF(TRIM(COALESCE(observaciones, '')), '') IS NOT NULL
              )
        """;
        return ML_obtenerEntero(ML_sql, ML_idDoctor);
    }

    public List<Map<String, Object>> ML_listarCitasAsignadas(Integer ML_idDoctor) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_doctor = ?
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }

    public List<Map<String, Object>> ML_listarProximasCitas(Integer ML_idDoctor) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_doctor = ?
              AND c.estado IN ('Pendiente', 'Confirmada')
              AND (
                    c.fecha_cita > CURDATE()
                    OR (c.fecha_cita = CURDATE() AND c.hora_inicio >= CURTIME())
              )
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
            LIMIT 5
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }


    public List<Map<String, Object>> ML_listarCitasParaRegistrarHistorial(Integer ML_idDoctor) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_doctor = ?
              AND c.estado IN ('Pendiente', 'Confirmada')
              AND COALESCE(h.total_historiales_cita, 0) = 0
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }

    public List<Map<String, Object>> ML_listarHistorialDoctor(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT
                h.id_historial AS id_historial,
                h.id_cita AS id_cita,
                h.id_paciente AS id_paciente,
                h.id_doctor AS id_doctor,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y %h:%i %p') AS fecha_registro_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                up.correo AS correo_paciente,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                c.motivo AS motivo,
                c.estado AS estado_cita,
                h.diagnostico AS diagnostico,
                h.tratamiento AS tratamiento,
                h.receta AS receta,
                h.observaciones AS observaciones,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                CASE WHEN h.id_historial = (
                    SELECT h2.id_historial
                    FROM historial_clinico h2
                    INNER JOIN citas c2 ON h2.id_cita = c2.id_cita
                    WHERE h2.id_doctor = h.id_doctor
                      AND h2.id_paciente = h.id_paciente
                    ORDER BY h2.fecha_registro DESC, c2.fecha_cita DESC, c2.hora_inicio DESC, h2.id_historial DESC
                    LIMIT 1
                ) THEN 1 ELSE 0 END AS puede_actualizar
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN pacientes p ON h.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE h.id_doctor = ?
            ORDER BY h.fecha_registro DESC, c.fecha_cita DESC, c.hora_inicio DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }

    public List<Map<String, Object>> ML_listarPacientesConHistorialDoctor(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT
                p.id_paciente AS id_paciente,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                up.correo AS correo_paciente,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                COUNT(h.id_historial) AS total_atenciones,
                DATE_FORMAT(MAX(h.fecha_registro), '%d/%m/%Y %h:%i %p') AS ultimo_registro_texto,
                (
                    SELECT h2.diagnostico
                    FROM historial_clinico h2
                    INNER JOIN citas c2 ON h2.id_cita = c2.id_cita
                    WHERE h2.id_doctor = h.id_doctor
                      AND h2.id_paciente = h.id_paciente
                    ORDER BY h2.fecha_registro DESC, c2.fecha_cita DESC, c2.hora_inicio DESC
                    LIMIT 1
                ) AS ultimo_diagnostico,
                (
                    SELECT DATE_FORMAT(c2.fecha_cita, '%d/%m/%Y')
                    FROM historial_clinico h2
                    INNER JOIN citas c2 ON h2.id_cita = c2.id_cita
                    WHERE h2.id_doctor = h.id_doctor
                      AND h2.id_paciente = h.id_paciente
                    ORDER BY h2.fecha_registro DESC, c2.fecha_cita DESC, c2.hora_inicio DESC
                    LIMIT 1
                ) AS ultima_cita_texto
            FROM historial_clinico h
            INNER JOIN pacientes p ON h.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            WHERE h.id_doctor = ?
            GROUP BY
                p.id_paciente,
                up.nombres,
                up.apellidos,
                up.dni,
                up.celular,
                up.correo,
                p.genero,
                p.grupo_sanguineo,
                p.alergias,
                h.id_doctor,
                h.id_paciente
            ORDER BY MAX(h.fecha_registro) DESC, paciente ASC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }

    public List<Map<String, Object>> ML_listarSeguimientoTratamientos(Integer ML_idDoctor) {
        String ML_sql = """
            SELECT
                h.id_historial AS id_historial,
                h.id_cita AS id_cita,
                h.id_paciente AS id_paciente,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y %h:%i %p') AS fecha_registro_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                p.alergias AS alergias,
                c.estado AS estado_cita,
                h.diagnostico AS diagnostico,
                h.tratamiento AS tratamiento,
                h.receta AS receta,
                h.observaciones AS observaciones
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN pacientes p ON h.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            WHERE h.id_doctor = ?
              AND (
                    NULLIF(TRIM(COALESCE(h.tratamiento, '')), '') IS NOT NULL
                    OR NULLIF(TRIM(COALESCE(h.receta, '')), '') IS NOT NULL
                    OR NULLIF(TRIM(COALESCE(h.observaciones, '')), '') IS NOT NULL
              )
            ORDER BY h.fecha_registro DESC, c.fecha_cita DESC, c.hora_inicio DESC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor);
    }

    public Map<String, Object> ML_obtenerPacienteDoctor(Integer ML_idDoctor, Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                p.id_paciente AS id_paciente,
                u.id_usuario AS id_usuario,
                CONCAT(u.nombres, ' ', u.apellidos) AS paciente,
                u.dni AS dni,
                u.celular AS celular,
                u.correo AS correo,
                u.direccion AS direccion,
                u.estado AS estado,
                DATE_FORMAT(p.fecha_nacimiento, '%d/%m/%Y') AS fecha_nacimiento_texto,
                TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) AS edad,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                p.contacto_emergencia AS contacto_emergencia,
                p.celular_emergencia AS celular_emergencia,
                COUNT(c.id_cita) AS total_citas_doctor,
                SUM(CASE WHEN c.estado = 'Atendida' THEN 1 ELSE 0 END) AS total_atendidas_doctor
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            INNER JOIN citas c ON p.id_paciente = c.id_paciente
            WHERE c.id_doctor = ?
              AND p.id_paciente = ?
            GROUP BY
                p.id_paciente,
                u.id_usuario,
                u.nombres,
                u.apellidos,
                u.dni,
                u.celular,
                u.correo,
                u.direccion,
                u.estado,
                p.fecha_nacimiento,
                p.genero,
                p.grupo_sanguineo,
                p.alergias,
                p.contacto_emergencia,
                p.celular_emergencia
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idDoctor, ML_idPaciente);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public List<Map<String, Object>> ML_obtenerHistorialPacienteDoctor(Integer ML_idDoctor, Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                h.id_historial AS id_historial,
                h.id_cita AS id_cita,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y %h:%i %p') AS fecha_registro_texto,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                c.motivo AS motivo,
                c.estado AS estado_cita,
                h.diagnostico AS diagnostico,
                h.tratamiento AS tratamiento,
                h.receta AS receta,
                h.observaciones AS observaciones,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                CASE WHEN h.id_historial = (
                    SELECT h2.id_historial
                    FROM historial_clinico h2
                    INNER JOIN citas c2 ON h2.id_cita = c2.id_cita
                    WHERE h2.id_doctor = h.id_doctor
                      AND h2.id_paciente = h.id_paciente
                    ORDER BY h2.fecha_registro DESC, c2.fecha_cita DESC, c2.hora_inicio DESC, h2.id_historial DESC
                    LIMIT 1
                ) THEN 1 ELSE 0 END AS puede_actualizar
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE h.id_doctor = ?
              AND h.id_paciente = ?
            ORDER BY h.fecha_registro ASC, c.fecha_cita ASC, c.hora_inicio ASC, h.id_historial ASC
        """;
        return ML_consultarLista(ML_sql, ML_idDoctor, ML_idPaciente);
    }


    public List<Map<String, Object>> ML_obtenerHistorialPacientePorCita(Integer ML_idDoctor, Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerCitaParaAtencion(ML_idDoctor, ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("No se encontró la cita seleccionada para el doctor autenticado.");
        }

        Integer ML_idPaciente = ML_obtenerEnteroDesdeMapa(ML_cita, "id_paciente");
        return ML_obtenerHistorialPacienteDoctor(ML_idDoctor, ML_idPaciente);
    }

    public Map<String, Object> ML_obtenerCitaParaAtencion(Integer ML_idDoctor, Integer ML_idCita) {
        String ML_sql = ML_sqlCitasBase() + """
            WHERE c.id_doctor = ?
              AND c.id_cita = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idDoctor, ML_idCita);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public ML_RegistroHistorialDoctorDTO ML_prepararHistorialCita(Integer ML_idDoctor, Integer ML_idCita) {
        Map<String, Object> ML_cita = ML_obtenerCitaParaAtencion(ML_idDoctor, ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe o no pertenece al doctor autenticado.");
        }

        Integer ML_tieneHistorial = ML_obtenerEnteroDesdeMapa(ML_cita, "tiene_historial");
        if ("Atendida".equalsIgnoreCase(ML_obtenerTextoDesdeMapa(ML_cita, "estado"))
                || (ML_tieneHistorial != null && ML_tieneHistorial == 1)) {
            throw new IllegalArgumentException("La cita ya está atendida o ya tiene historial registrado. No puede volver a modificarse ni registrarse como nueva atención.");
        }

        ML_RegistroHistorialDoctorDTO ML_historial = new ML_RegistroHistorialDoctorDTO();
        ML_historial.setML_idHistorial(null);
        ML_historial.setML_idCita(ML_idCita);
        ML_historial.setML_idDoctor(ML_idDoctor);
        ML_historial.setML_idPaciente(ML_obtenerEnteroDesdeMapa(ML_cita, "id_paciente"));
        return ML_historial;
    }

    public ML_RegistroHistorialDoctorDTO ML_obtenerHistorialPorCita(Integer ML_idDoctor, Integer ML_idCita) {
        String ML_sql = """
            SELECT
                id_historial,
                id_cita,
                id_paciente,
                id_doctor,
                diagnostico,
                tratamiento,
                receta,
                observaciones
            FROM historial_clinico
            WHERE id_doctor = ?
              AND id_cita = ?
            ORDER BY id_historial DESC
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForObject(ML_sql, (rs, rowNum) -> new ML_RegistroHistorialDoctorDTO(
                    rs.getInt("id_historial"),
                    rs.getInt("id_cita"),
                    rs.getInt("id_paciente"),
                    rs.getInt("id_doctor"),
                    rs.getString("diagnostico"),
                    rs.getString("tratamiento"),
                    rs.getString("receta"),
                    rs.getString("observaciones")
            ), ML_idDoctor, ML_idCita);
        } catch (Exception e) {
            return new ML_RegistroHistorialDoctorDTO();
        }
    }

    public ML_RegistroHistorialDoctorDTO ML_obtenerHistorialPorId(Integer ML_idDoctor, Integer ML_idHistorial) {
        String ML_sql = """
            SELECT
                id_historial,
                id_cita,
                id_paciente,
                id_doctor,
                diagnostico,
                tratamiento,
                receta,
                observaciones
            FROM historial_clinico
            WHERE id_doctor = ?
              AND id_historial = ?
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForObject(ML_sql, (rs, rowNum) -> new ML_RegistroHistorialDoctorDTO(
                    rs.getInt("id_historial"),
                    rs.getInt("id_cita"),
                    rs.getInt("id_paciente"),
                    rs.getInt("id_doctor"),
                    rs.getString("diagnostico"),
                    rs.getString("tratamiento"),
                    rs.getString("receta"),
                    rs.getString("observaciones")
            ), ML_idDoctor, ML_idHistorial);
        } catch (Exception e) {
            throw new IllegalArgumentException("El seguimiento seleccionado no existe o no pertenece al doctor autenticado.");
        }
    }

    public Map<String, Object> ML_obtenerCitaPorHistorial(Integer ML_idDoctor, Integer ML_idHistorial) {
        ML_RegistroHistorialDoctorDTO ML_historial = ML_obtenerHistorialPorId(ML_idDoctor, ML_idHistorial);
        return ML_obtenerCitaParaAtencion(ML_idDoctor, ML_historial.getML_idCita());
    }

    @Transactional
    public void ML_actualizarHistorialReciente(Integer ML_idDoctor,
                                               Integer ML_idHistorial,
                                               ML_RegistroHistorialDoctorDTO ML_historial) {
        ML_RegistroHistorialDoctorDTO ML_actual = ML_obtenerHistorialPorId(ML_idDoctor, ML_idHistorial);
        Map<String, Object> ML_citaActual = ML_obtenerCitaParaAtencion(ML_idDoctor, ML_actual.getML_idCita());
        if ("Atendida".equalsIgnoreCase(ML_obtenerTextoDesdeMapa(ML_citaActual, "estado"))) {
            throw new IllegalArgumentException("La cita ya está atendida. El historial queda solo para visualización y no puede modificarse.");
        }
        ML_validarHistorialActualizable(ML_idDoctor, ML_idHistorial);

        String ML_diagnostico = ML_limpiarTexto(ML_historial.getML_diagnostico());
        if (ML_diagnostico == null || ML_diagnostico.isBlank()) {
            throw new IllegalArgumentException("Debe registrar el diagnóstico del paciente.");
        }

        String ML_tratamiento = ML_limpiarTexto(ML_historial.getML_tratamiento());
        String ML_receta = ML_limpiarTexto(ML_historial.getML_receta());
        String ML_observaciones = ML_limpiarTexto(ML_historial.getML_observaciones());

        int ML_filas = ML_jdbcTemplate.update("""
            UPDATE historial_clinico
            SET diagnostico = ?, tratamiento = ?, receta = ?, observaciones = ?
            WHERE id_historial = ?
              AND id_doctor = ?
              AND id_cita = ?
        """,
                ML_diagnostico,
                ML_tratamiento,
                ML_receta,
                ML_observaciones,
                ML_idHistorial,
                ML_idDoctor,
                ML_actual.getML_idCita()
        );

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el seguimiento seleccionado.");
        }
    }

    private void ML_validarHistorialActualizable(Integer ML_idDoctor, Integer ML_idHistorial) {
        String ML_sql = """
            SELECT COUNT(*)
            FROM historial_clinico h
            WHERE h.id_doctor = ?
              AND h.id_historial = ?
              AND h.id_historial = (
                    SELECT h2.id_historial
                    FROM historial_clinico h2
                    INNER JOIN citas c2 ON h2.id_cita = c2.id_cita
                    WHERE h2.id_doctor = h.id_doctor
                      AND h2.id_paciente = h.id_paciente
                    ORDER BY h2.fecha_registro DESC, c2.fecha_cita DESC, c2.hora_inicio DESC, h2.id_historial DESC
                    LIMIT 1
              )
        """;

        if (ML_obtenerEntero(ML_sql, ML_idDoctor, ML_idHistorial) == 0) {
            throw new IllegalArgumentException("Solo se puede actualizar el último seguimiento realizado del paciente. Los anteriores son solo de visualización.");
        }
    }

    @Transactional
    public void ML_guardarHistorialCita(Integer ML_idDoctor,
                                        Integer ML_idCita,
                                        ML_RegistroHistorialDoctorDTO ML_historial) {

        Map<String, Object> ML_cita = ML_obtenerCitaParaAtencion(ML_idDoctor, ML_idCita);

        if (ML_cita.isEmpty()) {
            throw new IllegalArgumentException("La cita seleccionada no existe o no pertenece al doctor autenticado.");
        }

        if ("Atendida".equalsIgnoreCase(ML_obtenerTextoDesdeMapa(ML_cita, "estado"))
                || ML_obtenerEntero("SELECT COUNT(*) FROM historial_clinico WHERE id_cita = ? AND id_doctor = ?", ML_idCita, ML_idDoctor) > 0) {
            throw new IllegalArgumentException("La cita ya está atendida. No puede volver a registrarse ni modificarse como nueva atención.");
        }

        String ML_diagnostico = ML_limpiarTexto(ML_historial.getML_diagnostico());

        if (ML_diagnostico == null || ML_diagnostico.isBlank()) {
            throw new IllegalArgumentException("Debe registrar el diagnóstico del paciente.");
        }

        Integer ML_idPaciente = ML_obtenerEnteroDesdeMapa(ML_cita, "id_paciente");
        String ML_tratamiento = ML_limpiarTexto(ML_historial.getML_tratamiento());
        String ML_receta = ML_limpiarTexto(ML_historial.getML_receta());
        String ML_observaciones = ML_limpiarTexto(ML_historial.getML_observaciones());

        String ML_sqlInsertar = """
            INSERT INTO historial_clinico
            (id_cita, id_paciente, id_doctor, diagnostico, tratamiento, receta, observaciones)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(
                ML_sqlInsertar,
                ML_idCita,
                ML_idPaciente,
                ML_idDoctor,
                ML_diagnostico,
                ML_tratamiento,
                ML_receta,
                ML_observaciones
        );

        ML_jdbcTemplate.update(
                "UPDATE citas SET estado = 'Atendida' WHERE id_cita = ? AND id_doctor = ?",
                ML_idCita,
                ML_idDoctor
        );
    }

    @Transactional
    public void ML_actualizarEstadoCitaDoctor(Integer ML_idDoctor, Integer ML_idCita, String ML_estado) {
        String ML_estadoActual = ML_obtenerEstadoCitaDoctor(ML_idDoctor, ML_idCita);
        if (ML_estadoActual == null) {
            throw new IllegalArgumentException("La cita seleccionada no existe o no pertenece al doctor autenticado.");
        }
        if ("Atendida".equalsIgnoreCase(ML_estadoActual)) {
            throw new IllegalArgumentException("La cita ya está atendida y no puede modificarse.");
        }

        String ML_estadoNormalizado = ML_normalizarEstadoCita(ML_estado);
        if ("Atendida".equalsIgnoreCase(ML_estadoNormalizado)) {
            throw new IllegalArgumentException("La cita solo puede marcarse como Atendida al guardar el historial clínico desde Seguimiento.");
        }

        int ML_filas = ML_jdbcTemplate.update(
                "UPDATE citas SET estado = ? WHERE id_cita = ? AND id_doctor = ? AND estado <> 'Atendida'",
                ML_estadoNormalizado,
                ML_idCita,
                ML_idDoctor
        );

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar la cita. Verifica que pertenezca al doctor autenticado.");
        }
    }

    private String ML_obtenerEstadoCitaDoctor(Integer ML_idDoctor, Integer ML_idCita) {
        try {
            return ML_jdbcTemplate.queryForObject(
                    "SELECT estado FROM citas WHERE id_cita = ? AND id_doctor = ?",
                    String.class,
                    ML_idCita,
                    ML_idDoctor
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String ML_sqlCitasBase() {
        return """
            SELECT
                c.id_cita AS id_cita,
                c.id_paciente AS id_paciente,
                c.id_doctor AS id_doctor,
                c.id_consultorio AS id_consultorio,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                TIME_FORMAT(c.hora_fin, '%h:%i %p') AS hora_fin_texto,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                up.correo AS correo_paciente,
                p.fecha_nacimiento AS fecha_nacimiento,
                TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) AS edad,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                c.motivo AS motivo,
                c.estado AS estado,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                co.ubicacion AS ubicacion_consultorio,
                CASE
                    WHEN COALESCE(pg.total_pagados, 0) > 0 THEN 'Pagado'
                    ELSE 'Pendiente'
                END AS estado_pago,
                COALESCE(pg.metodo_pago, '-') AS metodo_pago,
                COALESCE(h.total_historiales_cita, 0) AS total_historiales_cita,
                CASE WHEN COALESCE(h.total_historiales_cita, 0) = 0 THEN 0 ELSE 1 END AS tiene_historial
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            LEFT JOIN (
                SELECT id_cita, id_doctor, COUNT(*) AS total_historiales_cita
                FROM historial_clinico
                GROUP BY id_cita, id_doctor
            ) h ON h.id_cita = c.id_cita AND h.id_doctor = c.id_doctor
            LEFT JOIN (
                SELECT
                    id_cita,
                    SUM(CASE WHEN estado_pago = 'Pagado' THEN 1 ELSE 0 END) AS total_pagados,
                    SUBSTRING_INDEX(GROUP_CONCAT(CASE WHEN estado_pago = 'Pagado' THEN metodo_pago END ORDER BY fecha_pago DESC, id_pago DESC), ',', 1) AS metodo_pago
                FROM pagos
                GROUP BY id_cita
            ) pg ON pg.id_cita = c.id_cita
        """;
    }

    private String ML_normalizarEstadoCita(String ML_estado) {
        if (ML_estado == null || ML_estado.trim().isEmpty()) {
            return "Pendiente";
        }

        String ML_estadoLimpio = ML_estado.trim();

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

    private int ML_obtenerEntero(String ML_sql, Object... ML_parametros) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_parametros);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
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
