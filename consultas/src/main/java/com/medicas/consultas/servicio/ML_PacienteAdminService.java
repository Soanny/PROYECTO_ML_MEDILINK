/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_PacienteAdminDTO;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 *
 * @author Windows
 */
@Service
public class ML_PacienteAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_PacienteAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public int ML_contarPacientes() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pacientes");
    }

    public int ML_contarPacientesMasculinos() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pacientes WHERE genero = 'Masculino'");
    }

    public int ML_contarPacientesFemeninos() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM pacientes WHERE genero = 'Femenino'");
    }

    public List<ML_PacienteAdminDTO> ML_listarPacientes() {

        String ML_sql = """
            SELECT 
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
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            ORDER BY p.id_paciente ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> {

                Date ML_fechaNacimiento = rs.getDate("fecha_nacimiento");
                String ML_fechaTexto = ML_fechaNacimiento != null ? ML_fechaNacimiento.toString() : "";

                return new ML_PacienteAdminDTO(
                    rs.getInt("id_paciente"),
                    rs.getInt("id_usuario"),
                    rs.getString("nombres"),
                    rs.getString("apellidos"),
                    rs.getString("dni"),
                    rs.getString("celular"),
                    rs.getString("correo"),
                    rs.getString("direccion"),
                    ML_fechaTexto,
                    rs.getString("genero"),
                    rs.getString("grupo_sanguineo"),
                    rs.getString("alergias"),
                    rs.getString("contacto_emergencia"),
                    rs.getString("celular_emergencia"),
                    rs.getString("estado")
                );
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private int ML_obtenerEntero(String ML_sql) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class);
            return ML_resultado != null ? ML_resultado.intValue() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Transactional
    public void ML_actualizarEstadoPaciente(Integer ML_idUsuario, String ML_estado) {
        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);
        int ML_filasUsuario = ML_jdbcTemplate.update(
                "UPDATE usuarios SET estado = ? WHERE id_usuario = ? AND id_rol = 4",
                ML_estadoNormalizado,
                ML_idUsuario
        );
        if (ML_filasUsuario == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el estado del paciente.");
        }
    }

    private String ML_normalizarEstado(String ML_estado) {
        if (ML_estado == null || ML_estado.trim().isEmpty()) {
            return "Activo";
        }
        String ML_estadoLimpio = ML_estado.trim();
        if (ML_estadoLimpio.equalsIgnoreCase("Inactivo") || ML_estadoLimpio.equalsIgnoreCase("Desactivado") || ML_estadoLimpio.equalsIgnoreCase("Desactivo")) {
            return "Inactivo";
        }
        return "Activo";
    }
}
