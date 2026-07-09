/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_CitaAdminDTO;
import com.medicas.consultas.dto.ML_OpcionDTO;
import com.medicas.consultas.dto.ML_RegistroCitaDTO;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_CitaAdminService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final ML_CorreoAutomaticoService ML_correoAutomaticoService;

    public ML_CitaAdminService(JdbcTemplate ML_jdbcTemplate,
                               ML_CorreoAutomaticoService ML_correoAutomaticoService) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_correoAutomaticoService = ML_correoAutomaticoService;
    }

    public int ML_contarCitas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas");
    }

    public int ML_contarCitasPendientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Pendiente'");
    }

    public int ML_contarCitasConfirmadas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Confirmada'");
    }

    public int ML_contarCitasCanceladas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Cancelada'");
    }

    public int ML_contarCitasAtendidas() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM citas WHERE estado = 'Atendida'");
    }

    public List<ML_CitaAdminDTO> ML_listarCitas() {

        String ML_sql = """
            SELECT 
                c.id_cita,
                c.id_paciente,
                c.id_doctor,
                DATE_FORMAT(c.fecha_cita, '%d/%m/%Y') AS fecha_cita_texto,
                TIME_FORMAT(c.hora_inicio, '%h:%i %p') AS hora_inicio_texto,
                CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                up.dni AS dni_paciente,
                up.celular AS celular_paciente,
                CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                e.nombre_especialidad,
                COALESCE(co.nombre_consultorio, 'Sin consultorio asignado') AS nombre_consultorio,
                c.estado
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            ORDER BY c.fecha_cita DESC, c.hora_inicio DESC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> new ML_CitaAdminDTO(
                    rs.getInt("id_cita"),
                    rs.getInt("id_paciente"),
                    rs.getInt("id_doctor"),
                    rs.getString("fecha_cita_texto"),
                    rs.getString("hora_inicio_texto"),
                    rs.getString("paciente"),
                    rs.getString("dni_paciente"),
                    rs.getString("celular_paciente"),
                    rs.getString("doctor"),
                    rs.getString("nombre_especialidad"),
                    rs.getString("nombre_consultorio"),
                    rs.getString("estado")
            ));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ML_OpcionDTO> ML_listarPacientesParaCita() {

        String ML_sql = """
            SELECT 
                p.id_paciente,
                CONCAT(u.nombres, ' ', u.apellidos, ' - DNI: ', u.dni) AS paciente
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

    public List<ML_OpcionDTO> ML_listarDoctoresParaCita() {

        String ML_sql = """
            SELECT 
                d.id_doctor,
                CONCAT('Dr. ', u.nombres, ' ', u.apellidos, ' - ', e.nombre_especialidad) AS doctor
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
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
    public void ML_registrarCita(ML_RegistroCitaDTO ML_cita) {

        if (ML_cita.getML_idPaciente() == null) {
            throw new IllegalArgumentException("Debe seleccionar un paciente.");
        }

        if (ML_cita.getML_idDoctor() == null) {
            throw new IllegalArgumentException("Debe seleccionar un doctor.");
        }

        if (ML_cita.getML_fechaCita() == null || ML_cita.getML_fechaCita().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar la fecha de la cita.");
        }

        if (ML_cita.getML_horaInicio() == null || ML_cita.getML_horaInicio().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar la hora de la cita.");
        }

        LocalDate ML_fecha = LocalDate.parse(ML_cita.getML_fechaCita().trim());
        LocalTime ML_hora = LocalTime.parse(ML_cita.getML_horaInicio().trim());
        LocalTime ML_horaFin = ML_hora.plusMinutes(30);
        Integer ML_idConsultorio = ML_obtenerConsultorioDoctor(ML_cita.getML_idDoctor());

        ML_validarDisponibilidadDoctor(ML_cita.getML_idDoctor(), ML_fecha, ML_hora, ML_horaFin);
        ML_validarCruceHorario(null, ML_cita.getML_idDoctor(), ML_idConsultorio, ML_fecha, ML_hora, ML_horaFin);

        String ML_estado = ML_normalizarEstadoCita(ML_cita.getML_estado());
        if ("Atendida".equalsIgnoreCase(ML_estado)) {
            throw new IllegalArgumentException("La cita solo puede marcarse como Atendida cuando el Doctor registra el historial clínico.");
        }

        String ML_sql = """
            INSERT INTO citas
            (id_paciente, id_doctor, id_consultorio, fecha_cita, hora_inicio, hora_fin, motivo, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(
                ML_sql,
                ML_cita.getML_idPaciente(),
                ML_cita.getML_idDoctor(),
                ML_idConsultorio,
                Date.valueOf(ML_fecha),
                Time.valueOf(ML_hora),
                Time.valueOf(ML_horaFin),
                null,
                ML_estado
        );

        Integer ML_idCitaRegistrada = ML_obtenerUltimoIdInsertado();
        ML_correoAutomaticoService.ML_programarConfirmacionCita(ML_idCitaRegistrada);
    }

    @Transactional
    public void ML_actualizarEstadoCita(Integer ML_idCita, String ML_estado) {

        String ML_estadoActual = ML_obtenerEstadoCita(ML_idCita);
        if (ML_estadoActual == null) {
            throw new IllegalArgumentException("La cita seleccionada no existe.");
        }

        if (ML_estadoActual.equalsIgnoreCase("Atendida")) {
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

    private boolean ML_existePagoPagadoCita(Integer ML_idCita) {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pagos WHERE id_cita = ? AND estado_pago = 'Pagado'", ML_idCita) > 0;
    }

    private String ML_obtenerEstadoCita(Integer ML_idCita) {
        try {
            return ML_jdbcTemplate.queryForObject("SELECT estado FROM citas WHERE id_cita = ?", String.class, ML_idCita);
        } catch (Exception e) {
            return null;
        }
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

    private String ML_normalizarEstadoCita(String ML_estado) {

        if (ML_estado == null || ML_estado.trim().isEmpty()) {
            return "Pendiente";
        }

        String ML_estadoLimpio = ML_estado.trim();

        if (ML_estadoLimpio.equalsIgnoreCase("Confirmada")) {
            return "Confirmada";
        }

        if (ML_estadoLimpio.equalsIgnoreCase("Cancelada")) {
            return "Cancelada";
        }

        if (ML_estadoLimpio.equalsIgnoreCase("Atendida")) {
            return "Atendida";
        }

        return "Pendiente";
    }

    private Integer ML_obtenerUltimoIdInsertado() {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : null;
        } catch (Exception e) {
            return null;
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
}
