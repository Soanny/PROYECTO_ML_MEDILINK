/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_CitaDashboardDTO;
import com.medicas.consultas.dto.ML_ResumenSemanalDTO;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
/**
 *
 * @author Windows
 */
@Service
public class ML_AdminDashboardService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_AdminDashboardService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }


    public int ML_contarUsuarios() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM usuarios");
    }

    public int ML_contarSecretarias() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM secretarias");
    }

    public int ML_contarConsultorios() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios");
    }

    public int ML_contarReservasRegistradas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public int ML_contarPacientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pacientes");
    }

    public int ML_contarDoctores() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM doctores");
    }

    public int ML_contarCitas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public String ML_obtenerPagosFormateado() {
        double ML_total = ML_obtenerDecimal(ML_sqlIngresosPagadosUnicos());
        DecimalFormat ML_formato = new DecimalFormat("#,##0.00");
        return "S/ " + ML_formato.format(ML_total);
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
            ) pagos_unicos
        """;
    }


    public List<Map<String, Object>> ML_obtenerRankingDoctoresHoras() {
        String ML_sql = """
            SELECT
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COUNT(c.id_cita) AS total_citas,
                SUM(CASE WHEN c.estado = 'Atendida' THEN 1 ELSE 0 END) AS citas_atendidas,
                SUM(CASE WHEN c.estado = 'Pendiente' THEN 1 ELSE 0 END) AS citas_pendientes,
                SUM(CASE WHEN c.estado = 'Confirmada' THEN 1 ELSE 0 END) AS citas_confirmadas,
                SUM(CASE WHEN c.estado = 'Cancelada' THEN 1 ELSE 0 END) AS citas_canceladas,
                COALESCE(SUM(CASE
                    WHEN c.id_cita IS NOT NULL AND c.estado <> 'Cancelada'
                    THEN GREATEST(TIME_TO_SEC(TIMEDIFF(c.hora_fin, c.hora_inicio)) / 60, 0)
                    ELSE 0
                END), 0) AS minutos_trabajados,
                CONCAT(
                    FLOOR(COALESCE(SUM(CASE
                        WHEN c.id_cita IS NOT NULL AND c.estado <> 'Cancelada'
                        THEN GREATEST(TIME_TO_SEC(TIMEDIFF(c.hora_fin, c.hora_inicio)) / 60, 0)
                        ELSE 0
                    END), 0) / 60),
                    ' h ',
                    MOD(FLOOR(COALESCE(SUM(CASE
                        WHEN c.id_cita IS NOT NULL AND c.estado <> 'Cancelada'
                        THEN GREATEST(TIME_TO_SEC(TIMEDIFF(c.hora_fin, c.hora_inicio)) / 60, 0)
                        ELSE 0
                    END), 0)), 60),
                    ' min'
                ) AS horas_trabajadas
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN citas c ON d.id_doctor = c.id_doctor
            WHERE u.estado = 'Activo'
            GROUP BY d.id_doctor, u.nombres, u.apellidos, e.nombre_especialidad
            ORDER BY minutos_trabajados DESC, total_citas DESC, doctor ASC
            LIMIT 10
        """;

        try {
            return ML_jdbcTemplate.queryForList(ML_sql);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ML_CitaDashboardDTO> ML_listarProximasCitas() {

        String ML_sql = """
            SELECT 
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora,
                c.estado AS estado
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            ORDER BY c.fecha_cita ASC, c.hora_inicio ASC
            LIMIT 4
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> {
                String ML_estado = rs.getString("estado");

                return new ML_CitaDashboardDTO(
                    rs.getString("paciente"),
                    rs.getString("doctor"),
                    rs.getString("hora"),
                    ML_estado,
                    ML_obtenerClaseEstado(ML_estado)
                );
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ML_ResumenSemanalDTO> ML_obtenerResumenSemanal() {

        String ML_sql = """
            SELECT DAYOFWEEK(fecha_cita) AS dia, COUNT(*) AS total
            FROM citas
            GROUP BY DAYOFWEEK(fecha_cita)
        """;

        Map<Integer, Integer> ML_conteos = new HashMap<>();

        try {
            List<Map<String, Object>> ML_resultados = ML_jdbcTemplate.queryForList(ML_sql);

            for (Map<String, Object> ML_fila : ML_resultados) {
                Integer ML_dia = ((Number) ML_fila.get("dia")).intValue();
                Integer ML_total = ((Number) ML_fila.get("total")).intValue();
                ML_conteos.put(ML_dia, ML_total);
            }
        } catch (Exception e) {
        }

        int ML_maximo = 1;

        for (Integer ML_total : ML_conteos.values()) {
            if (ML_total > ML_maximo) {
                ML_maximo = ML_total;
            }
        }

        List<ML_ResumenSemanalDTO> ML_resumen = new ArrayList<>();

        ML_resumen.add(new ML_ResumenSemanalDTO("Lun", ML_conteos.getOrDefault(2, 0), ML_calcularAltura(ML_conteos.getOrDefault(2, 0), ML_maximo)));
        ML_resumen.add(new ML_ResumenSemanalDTO("Mar", ML_conteos.getOrDefault(3, 0), ML_calcularAltura(ML_conteos.getOrDefault(3, 0), ML_maximo)));
        ML_resumen.add(new ML_ResumenSemanalDTO("Mié", ML_conteos.getOrDefault(4, 0), ML_calcularAltura(ML_conteos.getOrDefault(4, 0), ML_maximo)));
        ML_resumen.add(new ML_ResumenSemanalDTO("Jue", ML_conteos.getOrDefault(5, 0), ML_calcularAltura(ML_conteos.getOrDefault(5, 0), ML_maximo)));
        ML_resumen.add(new ML_ResumenSemanalDTO("Vie", ML_conteos.getOrDefault(6, 0), ML_calcularAltura(ML_conteos.getOrDefault(6, 0), ML_maximo)));
        ML_resumen.add(new ML_ResumenSemanalDTO("Sáb", ML_conteos.getOrDefault(7, 0), ML_calcularAltura(ML_conteos.getOrDefault(7, 0), ML_maximo)));

        return ML_resumen;
    }

    private int ML_obtenerEntero(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double ML_obtenerDecimal(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int ML_calcularAltura(Integer ML_total, Integer ML_maximo) {
        if (ML_total == null || ML_total == 0) {
            return 35;
        }

        return Math.max(55, (ML_total * 185) / ML_maximo);
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
}
