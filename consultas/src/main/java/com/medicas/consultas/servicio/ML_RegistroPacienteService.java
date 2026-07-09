/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_RegistroPacienteDTO;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Windows
 */
@Service
public class ML_RegistroPacienteService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final PasswordEncoder ML_passwordEncoder;

    public ML_RegistroPacienteService(JdbcTemplate ML_jdbcTemplate, PasswordEncoder ML_passwordEncoder) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_passwordEncoder = ML_passwordEncoder;
    }

    @Transactional
    public void ML_registrarPaciente(ML_RegistroPacienteDTO ML_paciente) {

        KeyHolder ML_keyHolder = new GeneratedKeyHolder();

        String ML_sqlUsuario = """
            INSERT INTO usuarios
            (id_rol, nombres, apellidos, dni, celular, direccion, correo, `contraseña`, estado, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Activo', NOW())
        """;

        ML_jdbcTemplate.update(connection -> {
            PreparedStatement ML_ps = connection.prepareStatement(ML_sqlUsuario, Statement.RETURN_GENERATED_KEYS);
            ML_ps.setInt(1, 4);
            ML_ps.setString(2, ML_paciente.getML_nombres());
            ML_ps.setString(3, ML_paciente.getML_apellidos());
            ML_ps.setString(4, ML_paciente.getML_dni());
            ML_ps.setString(5, ML_paciente.getML_celular());
            ML_ps.setString(6, ML_paciente.getML_direccion());
            ML_ps.setString(7, ML_paciente.getML_correo().trim().toLowerCase());
            ML_ps.setString(8, ML_passwordEncoder.encode(ML_paciente.getML_contrasena()));
            return ML_ps;
        }, ML_keyHolder);

        Number ML_idUsuarioGenerado = ML_keyHolder.getKey();

        if (ML_idUsuarioGenerado == null) {
            throw new RuntimeException("No se pudo registrar el usuario paciente.");
        }

        String ML_sqlPaciente = """
            INSERT INTO pacientes
            (id_usuario, fecha_nacimiento, genero, grupo_sanguineo, alergias, contacto_emergencia, celular_emergencia)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(
            ML_sqlPaciente,
            ML_idUsuarioGenerado.intValue(),
            Date.valueOf(ML_paciente.getML_fechaNacimiento()),
            ML_paciente.getML_genero(),
            ML_paciente.getML_grupoSanguineo(),
            ML_paciente.getML_alergias(),
            ML_paciente.getML_contactoEmergencia(),
            ML_paciente.getML_celularEmergencia()
        );
    }

    public boolean ML_existeCorreo(String ML_correo) {
        String ML_sql = "SELECT COUNT(*) FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(?))";

        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_correo);
            return ML_resultado != null && ML_resultado.intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean ML_existeDni(String ML_dni) {
        String ML_sql = "SELECT COUNT(*) FROM usuarios WHERE TRIM(dni) = TRIM(?)";

        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_dni);
            return ML_resultado != null && ML_resultado.intValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
