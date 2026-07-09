/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.servicio;

import com.medicas.consultas.dto.ML_DoctorAdminDTO;
import com.medicas.consultas.dto.ML_OpcionDTO;
import com.medicas.consultas.dto.ML_RegistroDoctorDTO;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_DoctorAdminService {

    private final JdbcTemplate ML_jdbcTemplate;
    private final PasswordEncoder ML_passwordEncoder;

    public ML_DoctorAdminService(JdbcTemplate ML_jdbcTemplate, PasswordEncoder ML_passwordEncoder) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
        this.ML_passwordEncoder = ML_passwordEncoder;
    }

    public int ML_contarDoctores() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM doctores");
    }

    public int ML_contarDoctoresActivos() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM doctores WHERE estado = 'Activo'");
    }

    public int ML_contarEspecialidades() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM especialidades WHERE estado = 'Activo'");
    }

    public int ML_contarConsultoriosDisponibles() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios WHERE estado = 'Disponible'");
    }

    public List<ML_DoctorAdminDTO> ML_listarDoctores() {

        String ML_sql = """
            SELECT 
                d.id_doctor,
                u.id_usuario,
                u.nombres,
                u.apellidos,
                u.dni,
                u.celular,
                u.correo,
                u.direccion,
                e.nombre_especialidad,
                COALESCE(c.nombre_consultorio, 'Sin consultorio asignado') AS nombre_consultorio,
                COALESCE(c.ubicacion, '-') AS ubicacion,
                COALESCE(c.piso, '-') AS piso,
                d.nro_colegiatura,
                d.precio_consulta,
                d.estado
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios c ON d.id_consultorio = c.id_consultorio
            ORDER BY d.id_doctor ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> {

                DecimalFormat ML_formato = new DecimalFormat("#,##0.00");
                String ML_precio = "S/ " + ML_formato.format(rs.getDouble("precio_consulta"));

                return new ML_DoctorAdminDTO(
                        rs.getInt("id_doctor"),
                        rs.getInt("id_usuario"),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("dni"),
                        rs.getString("celular"),
                        rs.getString("correo"),
                        rs.getString("direccion"),
                        rs.getString("nombre_especialidad"),
                        rs.getString("nombre_consultorio"),
                        rs.getString("ubicacion"),
                        rs.getString("piso"),
                        rs.getString("nro_colegiatura"),
                        ML_precio,
                        rs.getString("estado")
                );
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ML_OpcionDTO> ML_listarEspecialidadesActivas() {
        String ML_sql = """
            SELECT id_especialidad, nombre_especialidad
            FROM especialidades
            WHERE estado = 'Activo'
            ORDER BY nombre_especialidad ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> new ML_OpcionDTO(
                    rs.getInt("id_especialidad"),
                    rs.getString("nombre_especialidad")
            ));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<ML_OpcionDTO> ML_listarConsultoriosDisponibles() {
        String ML_sql = """
            SELECT id_consultorio,
                   CONCAT(nombre_consultorio, ' - ', ubicacion, ' - Piso ', piso) AS nombre
            FROM consultorios
            WHERE estado = 'Disponible'
            ORDER BY nombre_consultorio ASC
        """;

        try {
            return ML_jdbcTemplate.query(ML_sql, (rs, rowNum) -> new ML_OpcionDTO(
                    rs.getInt("id_consultorio"),
                    rs.getString("nombre")
            ));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void ML_registrarDoctor(ML_RegistroDoctorDTO ML_doctor) {

        if (ML_existeCorreo(ML_doctor.getML_correo())) {
            throw new IllegalArgumentException("El correo ya existe en la base de datos.");
        }

        if (ML_existeDni(ML_doctor.getML_dni())) {
            throw new IllegalArgumentException("El DNI ya existe en la base de datos.");
        }

        if (ML_existeColegiatura(ML_doctor.getML_nroColegiatura())) {
            throw new IllegalArgumentException("El número de colegiatura ya existe.");
        }

        String ML_estadoInicial = "Activo";

        KeyHolder ML_keyHolder = new GeneratedKeyHolder();

        String ML_sqlUsuario = """
            INSERT INTO usuarios
            (id_rol, nombres, apellidos, dni, celular, direccion, correo, `contraseña`, estado, fecha_registro)
            VALUES (3, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """;

        ML_jdbcTemplate.update(connection -> {
            PreparedStatement ML_ps = connection.prepareStatement(
                    ML_sqlUsuario,
                    Statement.RETURN_GENERATED_KEYS
            );

            ML_ps.setString(1, ML_doctor.getML_nombres());
            ML_ps.setString(2, ML_doctor.getML_apellidos());
            ML_ps.setString(3, ML_doctor.getML_dni());
            ML_ps.setString(4, ML_doctor.getML_celular());
            ML_ps.setString(5, ML_doctor.getML_direccion());
            ML_ps.setString(6, ML_doctor.getML_correo().trim().toLowerCase());
            ML_ps.setString(7, ML_passwordEncoder.encode(ML_doctor.getML_contrasena()));
            ML_ps.setString(8, ML_estadoInicial);

            return ML_ps;
        }, ML_keyHolder);

        Number ML_idUsuarioGenerado = ML_keyHolder.getKey();

        if (ML_idUsuarioGenerado == null) {
            throw new RuntimeException("No se pudo crear el usuario doctor.");
        }

        BigDecimal ML_precio = BigDecimal.ZERO;

        if (ML_doctor.getML_precioConsulta() != null && !ML_doctor.getML_precioConsulta().trim().isEmpty()) {
            String ML_precioTexto = ML_doctor.getML_precioConsulta().trim().replace(",", ".");
            ML_precio = new BigDecimal(ML_precioTexto);
        }

        String ML_sqlDoctor = """
            INSERT INTO doctores
            (id_usuario, id_especialidad, id_consultorio, nro_colegiatura, precio_consulta, estado)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        ML_jdbcTemplate.update(
                ML_sqlDoctor,
                ML_idUsuarioGenerado.intValue(),
                ML_doctor.getML_idEspecialidad(),
                ML_doctor.getML_idConsultorio(),
                ML_doctor.getML_nroColegiatura(),
                ML_precio,
                ML_estadoInicial
        );
    }

    @Transactional
    public void ML_actualizarEstadoDoctor(Integer ML_idUsuario, String ML_estado) {

        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);

        int ML_filasUsuario = ML_jdbcTemplate.update(
                "UPDATE usuarios SET estado = ? WHERE id_usuario = ? AND id_rol = 3",
                ML_estadoNormalizado,
                ML_idUsuario
        );

        int ML_filasDoctor = ML_jdbcTemplate.update(
                "UPDATE doctores SET estado = ? WHERE id_usuario = ?",
                ML_estadoNormalizado,
                ML_idUsuario
        );

        if (ML_filasUsuario == 0 || ML_filasDoctor == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el estado del doctor.");
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

    private boolean ML_existeColegiatura(String ML_nroColegiatura) {
        return ML_existeDato("SELECT COUNT(*) FROM doctores WHERE TRIM(nro_colegiatura) = TRIM(?)", ML_nroColegiatura);
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

    @Transactional
    public void ML_registrarEspecialidad(String ML_nombre) {
        if (ML_nombre == null || ML_nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la especialidad es obligatorio.");
        }
        String ML_existe = "SELECT COUNT(*) FROM especialidades WHERE LOWER(TRIM(nombre_especialidad)) = LOWER(TRIM(?))";
        if (ML_existeDato(ML_existe, ML_nombre.trim())) {
            throw new IllegalArgumentException("Ya existe una especialidad con ese nombre.");
        }
        ML_jdbcTemplate.update(
            "INSERT INTO especialidades (nombre_especialidad, estado) VALUES (?, 'Activo')",
            ML_nombre.trim()
        );
    }
}