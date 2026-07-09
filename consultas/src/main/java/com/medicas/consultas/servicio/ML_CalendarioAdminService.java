/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;
import com.medicas.consultas.dto.ML_CalendarioAdminDTO;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
/**
 *
 * @author Windows
 */
@Service
public class ML_CalendarioAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_CalendarioAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public int ML_contarCitas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public int ML_contarCitasHoy() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE fecha_cita = CURDATE()");
    }

    public int ML_contarCitasConfirmadas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Confirmada'");
    }

    public int ML_contarCitasPendientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Pendiente'");
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

                int ML_diaMysql = rs.getInt("dia_semana");
                int ML_diaIndex = ML_convertirDiaSemana(ML_diaMysql);

                int ML_hora = rs.getInt("hora_numero");
                int ML_minuto = rs.getInt("minuto_numero");
                int ML_topPx = ML_calcularTopCalendario(ML_hora, ML_minuto);

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

    private int ML_convertirDiaSemana(int ML_diaMysql) {
        /*
         MySQL DAYOFWEEK:
         1 Domingo, 2 Lunes, 3 Martes, 4 Miércoles, 5 Jueves, 6 Viernes, 7 Sábado

         Vista:
         1 Lunes, 2 Martes, 3 Miércoles, 4 Jueves, 5 Viernes, 6 Sábado, 7 Domingo
        */
        if (ML_diaMysql == 1) {
            return 7;
        }

        return ML_diaMysql - 1;
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

    private int ML_obtenerEntero(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
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
}
