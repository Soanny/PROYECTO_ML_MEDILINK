package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_OpcionDTO;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_HorarioAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_HorarioAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public List<Map<String, Object>> ML_listarHorarios() {
        String ML_sql = """
            SELECT
                h.id_horario,
                h.id_doctor,
                h.dia_semana,
                TIME_FORMAT(h.hora_inicio, '%H:%i') AS hora_inicio,
                TIME_FORMAT(h.hora_fin, '%H:%i') AS hora_fin,
                h.estado,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio
            FROM horarios h
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON d.id_consultorio = co.id_consultorio
            ORDER BY FIELD(h.dia_semana, 'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'), h.hora_inicio ASC, doctor ASC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_listarResumenHorariosPorDoctor() {
        String ML_sql = """
            SELECT
                d.id_doctor,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                e.nombre_especialidad AS especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS consultorio,
                COUNT(h.id_horario) AS total_horarios,
                SUM(CASE WHEN h.estado = 'Activo' THEN 1 ELSE 0 END) AS horarios_activos,
                MIN(TIME_FORMAT(h.hora_inicio, '%H:%i')) AS primera_hora,
                MAX(TIME_FORMAT(h.hora_fin, '%H:%i')) AS ultima_hora
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON d.id_consultorio = co.id_consultorio
            LEFT JOIN horarios h ON d.id_doctor = h.id_doctor
            WHERE d.estado = 'Activo'
              AND u.estado = 'Activo'
            GROUP BY d.id_doctor, doctor, e.nombre_especialidad, co.nombre_consultorio
            ORDER BY u.apellidos ASC, u.nombres ASC
        """;
        return ML_consultarLista(ML_sql);
    }

    public List<Map<String, Object>> ML_listarResumenHorariosPorDia() {
        String ML_sql = """
            SELECT
                h.dia_semana,
                COUNT(*) AS total_horarios,
                SUM(CASE WHEN h.estado = 'Activo' THEN 1 ELSE 0 END) AS activos
            FROM horarios h
            GROUP BY h.dia_semana
            ORDER BY FIELD(h.dia_semana, 'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo')
        """;
        return ML_consultarLista(ML_sql);
    }


    public Map<String, List<Map<String, Object>>> ML_agruparHorariosPorDia() {
        Map<String, List<Map<String, Object>>> ML_mapa = new LinkedHashMap<>();
        for (String ML_dia : ML_listarDiasSemana()) {
            ML_mapa.put(ML_dia, new ArrayList<>());
        }
        for (Map<String, Object> ML_horario : ML_listarHorarios()) {
            String ML_dia = String.valueOf(ML_horario.get("dia_semana"));
            ML_mapa.computeIfAbsent(ML_dia, k -> new ArrayList<>()).add(ML_horario);
        }
        return ML_mapa;
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

    public int ML_contarHorarios() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM horarios");
    }

    public int ML_contarHorariosActivos() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM horarios WHERE estado = 'Activo'");
    }

    public int ML_contarHorariosInactivos() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM horarios WHERE estado = 'Inactivo'");
    }

    public List<String> ML_listarDiasSemana() {
        return Arrays.asList("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");
    }

    public List<String> ML_listarEstadosHorario() {
        return Arrays.asList("Activo", "Inactivo");
    }

    @Transactional
    public void ML_guardarHorario(Integer ML_idDoctor, String ML_diaSemana, String ML_horaInicioTexto, String ML_horaFinTexto, String ML_estado) {
        if (ML_idDoctor == null) {
            throw new IllegalArgumentException("Debe seleccionar un doctor.");
        }

        String ML_dia = ML_normalizarDia(ML_diaSemana);
        LocalTime ML_horaInicio = ML_parsearHora(ML_horaInicioTexto, "Debe ingresar la hora de inicio.");
        LocalTime ML_horaFin = ML_parsearHora(ML_horaFinTexto, "Debe ingresar la hora de fin.");

        if (!ML_horaFin.isAfter(ML_horaInicio)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio.");
        }

        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);
        ML_validarCruceHorarioDoctor(null, ML_idDoctor, ML_dia, ML_horaInicio, ML_horaFin);

        ML_jdbcTemplate.update(
                "INSERT INTO horarios (id_doctor, dia_semana, hora_inicio, hora_fin, estado) VALUES (?, ?, ?, ?, ?)",
                ML_idDoctor,
                ML_dia,
                Time.valueOf(ML_horaInicio),
                Time.valueOf(ML_horaFin),
                ML_estadoNormalizado
        );
    }

    @Transactional
    public void ML_actualizarEstadoHorario(Integer ML_idHorario, String ML_estado) {
        if (ML_idHorario == null) {
            throw new IllegalArgumentException("No se recibió el horario.");
        }
        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);
        int ML_filas = ML_jdbcTemplate.update(
                "UPDATE horarios SET estado = ? WHERE id_horario = ?",
                ML_estadoNormalizado,
                ML_idHorario
        );
        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el horario.");
        }
    }

    private void ML_validarCruceHorarioDoctor(Integer ML_idHorarioExcluir, Integer ML_idDoctor, String ML_dia, LocalTime ML_inicio, LocalTime ML_fin) {
        String ML_sql = """
            SELECT COUNT(*)
            FROM horarios
            WHERE id_doctor = ?
              AND dia_semana = ?
              AND estado = 'Activo'
              AND (? IS NULL OR id_horario <> ?)
              AND hora_inicio < ?
              AND hora_fin > ?
        """;

        int ML_total = ML_obtenerEntero(
                ML_sql,
                ML_idDoctor,
                ML_dia,
                ML_idHorarioExcluir,
                ML_idHorarioExcluir,
                Time.valueOf(ML_fin),
                Time.valueOf(ML_inicio)
        );

        if (ML_total > 0) {
            throw new IllegalArgumentException("El doctor ya tiene un horario cruzado ese día.");
        }
    }

    private String ML_normalizarDia(String ML_diaSemana) {
        if (ML_diaSemana != null) {
            for (String ML_dia : ML_listarDiasSemana()) {
                if (ML_dia.equalsIgnoreCase(ML_diaSemana.trim())) {
                    return ML_dia;
                }
            }
        }
        throw new IllegalArgumentException("Debe seleccionar un día válido.");
    }

    private String ML_normalizarEstado(String ML_estado) {
        if (ML_estado != null) {
            for (String ML_opcion : ML_listarEstadosHorario()) {
                if (ML_opcion.equalsIgnoreCase(ML_estado.trim())) {
                    return ML_opcion;
                }
            }
        }
        return "Activo";
    }

    private LocalTime ML_parsearHora(String ML_texto, String ML_mensaje) {
        if (ML_texto == null || ML_texto.trim().isEmpty()) {
            throw new IllegalArgumentException(ML_mensaje);
        }
        try {
            return LocalTime.parse(ML_texto.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("La hora ingresada no tiene un formato válido.");
        }
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
