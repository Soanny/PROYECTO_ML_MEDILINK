/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_RegistroSecretariaDTO;
import com.medicas.consultas.dto.ML_SecretariaAdminDTO;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_SecretariaAdminService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final PasswordEncoder ML_passwordEncoder;

    public ML_SecretariaAdminService(JdbcTemplate ML_jdbcTemplate, PasswordEncoder ML_passwordEncoder) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_passwordEncoder = ML_passwordEncoder;
    }

    public int ML_contarSecretarias() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM secretarias");
    }

    public int ML_contarSecretariasActivas() {
        return ML_obtenerEntero("""
            SELECT COUNT(*)
            FROM secretarias s
            INNER JOIN usuarios u ON s.id_usuario = u.id_usuario
            WHERE u.estado = 'Activo'
        """);
    }

    public int ML_contarTurnoManana() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM secretarias WHERE turno = 'Mañana'");
    }

    public int ML_contarTurnoTarde() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM secretarias WHERE turno = 'Tarde'");
    }

    public List<ML_SecretariaAdminDTO> ML_listarSecretarias() {

        String ML_sql = """
            SELECT 
                s.id_secretaria,
                u.id_usuario,
                u.nombres,
                u.apellidos,
                u.dni,
                u.celular,
                u.correo,
                u.direccion,
                u.estado,
                s.fecha_ingreso,
                s.turno
            FROM secretarias s
            INNER JOIN usuarios u ON s.id_usuario = u.id_usuario
            ORDER BY s.id_secretaria ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> {

                Date ML_fechaIngreso = rs.getDate("fecha_ingreso");
                String ML_fechaTexto = ML_fechaIngreso != null ? ML_fechaIngreso.toString() : "";

                return new ML_SecretariaAdminDTO(
                        rs.getInt("id_secretaria"),
                        rs.getInt("id_usuario"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("dni"),
                        rs.getString("celular"),
                        rs.getString("correo"),
                        rs.getString("direccion"),
                        ML_fechaTexto,
                        rs.getString("turno"),
                        rs.getString("estado")
                );
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void ML_registrarSecretaria(ML_RegistroSecretariaDTO ML_secretaria) {

        if (ML_existeCorreo(ML_secretaria.getML_correo())) {
            throw new IllegalArgumentException("El correo ya existe en la base de datos.");
        }

        if (ML_existeDni(ML_secretaria.getML_dni())) {
            throw new IllegalArgumentException("El DNI ya existe en la base de datos.");
        }

        String ML_estadoInicial = "Activo";

        KeyHolder ML_keyHolder = new GeneratedKeyHolder();

        String ML_sqlUsuario = """
            INSERT INTO usuarios
            (id_rol, nombres, apellidos, dni, celular, direccion, correo, `contraseña`, estado, fecha_registro)
            VALUES (2, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """;

        ML_jdbcTemplate.update(connection -> {
            PreparedStatement ML_ps = connection.prepareStatement(
                    ML_sqlUsuario,
                    Statement.RETURN_GENERATED_KEYS
            );

            ML_ps.setString(1, ML_secretaria.getML_nombres());
            ML_ps.setString(2, ML_secretaria.getML_apellidos());
            ML_ps.setString(3, ML_secretaria.getML_dni());
            ML_ps.setString(4, ML_secretaria.getML_celular());
            ML_ps.setString(5, ML_secretaria.getML_direccion());
            ML_ps.setString(6, ML_secretaria.getML_correo().trim().toLowerCase());
            ML_ps.setString(7, ML_passwordEncoder.encode(ML_secretaria.getML_contrasena()));
            ML_ps.setString(8, ML_estadoInicial);

            return ML_ps;
        }, ML_keyHolder);

        Number ML_idUsuarioGenerado = ML_keyHolder.getKey();

        if (ML_idUsuarioGenerado == null) {
            throw new RuntimeException("No se pudo crear el usuario secretaria.");
        }

        LocalDate ML_fechaIngreso = LocalDate.now();

        if (ML_secretaria.getML_fechaIngreso() != null && !ML_secretaria.getML_fechaIngreso().trim().isEmpty()) {
            ML_fechaIngreso = LocalDate.parse(ML_secretaria.getML_fechaIngreso().trim());
        }

        String ML_turno = ML_secretaria.getML_turno();

        if (ML_turno == null || ML_turno.trim().isEmpty()) {
            ML_turno = "Mañana";
        }

        String ML_sqlSecretaria = """
            INSERT INTO secretarias
            (id_usuario, fecha_ingreso, turno)
            VALUES (?, ?, ?)
        """;

        ML_jdbcTemplate.update(
                ML_sqlSecretaria,
                ML_idUsuarioGenerado.intValue(),
                Date.valueOf(ML_fechaIngreso),
                ML_turno
        );
    }

    @Transactional
    public void ML_actualizarEstadoSecretaria(Integer ML_idUsuario, String ML_estado) {

        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);

        int ML_filasUsuario = ML_jdbcTemplate.update(
                "UPDATE usuarios SET estado = ? WHERE id_usuario = ? AND id_rol = 2",
                ML_estadoNormalizado,
                ML_idUsuario
        );

        if (ML_filasUsuario == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el estado de la secretaria.");
        }
    }

    private String ML_normalizarEstado(String ML_estado) {
        if (ML_estado == null || ML_estado.trim().isEmpty()) {
            return "Activo";
        }

        String ML_estadoLimpio = ML_estado.trim();

        if (ML_estadoLimpio.equalsIgnoreCase("Inactivo")
                || ML_estadoLimpio.equalsIgnoreCase("Desactivado")
                || ML_estadoLimpio.equalsIgnoreCase("Desactivo")) {
            return "Inactivo";
        }

        return "Activo";
    }

    private boolean ML_existeCorreo(String ML_correo) {
        return ML_existeDato(
                "SELECT COUNT(*) FROM usuarios WHERE LOWER(TRIM(correo)) = LOWER(TRIM(?))",
                ML_correo
        );
    }

    private boolean ML_existeDni(String ML_dni) {
        return ML_existeDato("SELECT COUNT(*) FROM usuarios WHERE TRIM(dni) = TRIM(?)", ML_dni);
    }

    private boolean ML_existeDato(String ML_sql, String ML_valor) {
        try {
            Number ML_resultado = ML_jdbcTemplate.queryForObject(ML_sql, Number.class, ML_valor);
            return ML_resultado != null && ML_resultado.intValue() > 0;
        } catch (Exception e) {
            return false;
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
}