/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ML_HistorialClinicoAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_HistorialClinicoAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public List<Map<String, Object>> ML_listarPacientesHistorial() {

        String ML_sql = """
            SELECT
                p.id_paciente AS id_paciente,
                u.id_usuario AS id_usuario,
                CONCAT(u.nombres, ' ', u.apellidos) AS paciente,
                u.nombres AS nombres,
                u.apellidos AS apellidos,
                u.dni AS dni,
                u.celular AS celular,
                u.correo AS correo,
                u.direccion AS direccion,
                u.estado AS estado,
                p.fecha_nacimiento AS fecha_nacimiento,
                DATE_FORMAT(p.fecha_nacimiento, '%d/%m/%Y') AS fecha_nacimiento_texto,
                TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) AS edad,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                p.contacto_emergencia AS contacto_emergencia,
                p.celular_emergencia AS celular_emergencia,
                COUNT(c.id_cita) AS total_citas
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            LEFT JOIN citas c ON p.id_paciente = c.id_paciente
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
            ORDER BY u.apellidos ASC, u.nombres ASC
        """;

        return ML_consultarLista(ML_sql);
    }

    public Map<String, Object> ML_obtenerPacienteHistorial(Integer ML_idPaciente) {

        String ML_sql = """
            SELECT
                p.id_paciente AS id_paciente,
                u.id_usuario AS id_usuario,
                CONCAT(u.nombres, ' ', u.apellidos) AS paciente,
                u.nombres AS nombres,
                u.apellidos AS apellidos,
                u.dni AS dni,
                u.celular AS celular,
                u.correo AS correo,
                u.direccion AS direccion,
                u.estado AS estado,
                p.fecha_nacimiento AS fecha_nacimiento,
                DATE_FORMAT(p.fecha_nacimiento, '%d/%m/%Y') AS fecha_nacimiento_texto,
                TIMESTAMPDIFF(YEAR, p.fecha_nacimiento, CURDATE()) AS edad,
                p.genero AS genero,
                p.grupo_sanguineo AS grupo_sanguineo,
                p.alergias AS alergias,
                p.contacto_emergencia AS contacto_emergencia,
                p.celular_emergencia AS celular_emergencia
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE p.id_paciente = ?
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPaciente);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public Map<String, Object> ML_obtenerResumenClinico(Integer ML_idPaciente) {

        String ML_sql = """
            SELECT
                COUNT(*) AS total_consultas,
                SUM(CASE WHEN estado = 'Atendida' THEN 1 ELSE 0 END) AS consultas_atendidas,
                SUM(CASE WHEN estado = 'Confirmada' THEN 1 ELSE 0 END) AS citas_confirmadas,
                SUM(CASE WHEN estado = 'Pendiente' THEN 1 ELSE 0 END) AS citas_pendientes,
                SUM(CASE WHEN estado = 'Cancelada' THEN 1 ELSE 0 END) AS citas_canceladas,
                DATE_FORMAT(MAX(fecha_cita), '%d/%m/%Y') AS ultima_cita
            FROM citas
            WHERE id_paciente = ?
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPaciente);
        } catch (Exception e) {
            Map<String, Object> ML_resumen = new HashMap<>();
            ML_resumen.put("total_consultas", 0);
            ML_resumen.put("consultas_atendidas", 0);
            ML_resumen.put("citas_confirmadas", 0);
            ML_resumen.put("citas_pendientes", 0);
            ML_resumen.put("citas_canceladas", 0);
            ML_resumen.put("ultima_cita", "-");
            return ML_resumen;
        }
    }

    public Map<String, Object> ML_obtenerProximaCita(Integer ML_idPaciente) {

        String ML_sql = """
            SELECT
                c.id_cita AS id_cita,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora,
                c.estado AS estado,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad
            FROM citas c
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE c.id_paciente = ?
              AND c.estado IN ('Pendiente', 'Confirmada')
              AND (
                    c.fecha_cita > CURDATE()
                    OR (c.fecha_cita = CURDATE() AND c.hora_inicio >= CURTIME())
              )
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
            LIMIT 1
        """;

        try {
            return ML_jdbcTemplate.queryForMap(ML_sql, ML_idPaciente);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public List<Map<String, Object>> ML_obtenerNotasClinicasRecientes(Integer ML_idPaciente) {

        String ML_sql = """
            SELECT
                c.id_cita AS id_cita,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora,
                c.estado AS estado,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                CASE
                    WHEN c.estado = 'Atendida' THEN CONCAT('Consulta finalizada en ', e.nombre_especialidad, '.')
                    WHEN c.estado = 'Confirmada' THEN CONCAT('Cita confirmada para atención en ', e.nombre_especialidad, '.')
                    WHEN c.estado = 'Pendiente' THEN CONCAT('Cita pendiente de confirmación en ', e.nombre_especialidad, '.')
                    WHEN c.estado = 'Cancelada' THEN CONCAT('Cita cancelada en ', e.nombre_especialidad, '.')
                    ELSE CONCAT('Registro de cita médica en ', e.nombre_especialidad, '.')
                END AS descripcion
            FROM citas c
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE c.id_paciente = ?
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
            LIMIT 5
        """;

        try {
            return ML_jdbcTemplate.queryForList(ML_sql, ML_idPaciente);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> ML_obtenerConsultasPaciente(Integer ML_idPaciente) {

        String ML_sql = """
            SELECT
                c.id_cita AS id_cita,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora,
                c.estado AS estado,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio
            FROM citas c
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE c.id_paciente = ?
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;

        try {
            return ML_jdbcTemplate.queryForList(ML_sql, ML_idPaciente);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> ML_obtenerDiagnosticosActivos(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                h.diagnostico AS diagnostico,
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y') AS fecha,
                c.estado AS estado,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                COALESCE(h.observaciones, '-') AS observaciones
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            WHERE h.id_paciente = ?
              AND NULLIF(TRIM(COALESCE(h.diagnostico, '')), '') IS NOT NULL
            ORDER BY h.fecha_registro DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_obtenerTratamientosPaciente(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y') AS fecha,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(h.tratamiento, '-') AS tratamiento,
                COALESCE(h.receta, '-') AS receta,
                COALESCE(h.observaciones, '-') AS observaciones
            FROM historial_clinico h
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE h.id_paciente = ?
              AND (
                    NULLIF(TRIM(COALESCE(h.tratamiento, '')), '') IS NOT NULL
                    OR NULLIF(TRIM(COALESCE(h.receta, '')), '') IS NOT NULL
              )
            ORDER BY h.fecha_registro DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    public List<Map<String, Object>> ML_obtenerExamenesPaciente(Integer ML_idPaciente) {
        String ML_sql = """
            SELECT
                DATE_FORMAT(h.fecha_registro, '%d/%m/%Y') AS fecha,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(h.observaciones, '-') AS examenes,
                c.estado AS estado
            FROM historial_clinico h
            INNER JOIN citas c ON h.id_cita = c.id_cita
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE h.id_paciente = ?
              AND NULLIF(TRIM(COALESCE(h.observaciones, '')), '') IS NOT NULL
            ORDER BY h.fecha_registro DESC, h.id_historial DESC
        """;
        return ML_consultarLista(ML_sql, ML_idPaciente);
    }

    private List<Map<String, Object>> ML_consultarLista(String ML_sql, Object... ML_parametros) {
        try {
            return ML_jdbcTemplate.queryForList(ML_sql, ML_parametros);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}