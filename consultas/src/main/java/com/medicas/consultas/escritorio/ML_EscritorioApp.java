package com.medicas.consultas.escritorio;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Versión de escritorio del sistema CONSULTAS.
 * Usa la misma base bd_citas_medicas y las mismas tablas del proyecto web.
 */
public class ML_EscritorioApp {

    private String ML_url;
    private String ML_usuarioDb;
    private String ML_claveDb;
    private ML_Sesion ML_sesion;
    private JTable ML_tablaCitas;
    private JTable ML_tablaHorarios;
    private JTable ML_tablaPagos;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ML_EscritorioApp().ML_iniciar());
    }

    private void ML_iniciar() {
        ML_cargarConfiguracion();
        ML_mostrarLogin();
    }

    private void ML_cargarConfiguracion() {
        try {
            Properties ML_props = new Properties();
            ML_props.load(ML_EscritorioApp.class.getClassLoader().getResourceAsStream("application.properties"));
            ML_url = ML_resolverPropiedad(ML_props.getProperty("ml.desktop.db.url", ML_props.getProperty("spring.datasource.url")), "ML_DB_URL");
            ML_usuarioDb = ML_resolverPropiedad(ML_props.getProperty("ml.desktop.db.user", ML_props.getProperty("spring.datasource.username")), "ML_DB_USER");
            ML_claveDb = ML_resolverPropiedad(ML_props.getProperty("ml.desktop.db.password", ML_props.getProperty("spring.datasource.password")), "ML_DB_PASSWORD");
        } catch (Exception e) {
            ML_url = "jdbc:mysql://localhost:3306/bd_citas_medicas?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Lima";
            ML_usuarioDb = "root";
            ML_claveDb = "";
        }
    }

    private String ML_resolverPropiedad(String ML_valor, String ML_variableEntorno) {
        String ML_env = System.getenv(ML_variableEntorno);
        if (ML_env != null && !ML_env.isBlank()) {
            return ML_env.trim();
        }
        if (ML_valor == null) {
            return "";
        }
        String ML_limpio = ML_valor.trim();
        if (ML_limpio.startsWith("${") && ML_limpio.endsWith("}")) {
            int ML_indice = ML_limpio.indexOf(':');
            if (ML_indice >= 0) {
                return ML_limpio.substring(ML_indice + 1, ML_limpio.length() - 1);
            }
            return "";
        }
        return ML_limpio;
    }

    private Connection ML_conectar() throws Exception {
        return DriverManager.getConnection(ML_url, ML_usuarioDb, ML_claveDb);
    }

    private void ML_mostrarLogin() {
        JFrame ML_frame = new JFrame("CONSULTAS - Escritorio");
        ML_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ML_frame.setSize(420, 230);
        ML_frame.setLocationRelativeTo(null);

        JPanel ML_panel = new JPanel(new GridLayout(4, 2, 10, 10));
        ML_panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTextField ML_correo = new JTextField("secretaria1@gmail.com");
        JPasswordField ML_clave = new JPasswordField("123456");
        JButton ML_ingresar = new JButton("Ingresar");

        ML_panel.add(new JLabel("Correo:"));
        ML_panel.add(ML_correo);
        ML_panel.add(new JLabel("Clave:"));
        ML_panel.add(ML_clave);
        ML_panel.add(new JLabel("Base:"));
        ML_panel.add(new JLabel("bd_citas_medicas"));
        ML_panel.add(new JLabel(""));
        ML_panel.add(ML_ingresar);
        ML_frame.add(ML_panel);

        ML_ingresar.addActionListener(e -> {
            try {
                ML_sesion = ML_autenticar(ML_correo.getText(), new String(ML_clave.getPassword()));
                ML_frame.dispose();
                ML_mostrarPanelPrincipal();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(ML_frame, ex.getMessage(), "No se pudo iniciar sesión", JOptionPane.ERROR_MESSAGE);
            }
        });

        ML_frame.setVisible(true);
    }

    private ML_Sesion ML_autenticar(String ML_correo, String ML_clave) throws Exception {
        String ML_sql = """
            SELECT u.id_usuario, u.nombres, u.apellidos, u.correo, u.`contraseña`, u.estado, r.id_rol, r.nombre_rol,
                   p.id_paciente, d.id_doctor
            FROM usuarios u
            INNER JOIN roles r ON u.id_rol = r.id_rol
            LEFT JOIN pacientes p ON p.id_usuario = u.id_usuario
            LEFT JOIN doctores d ON d.id_usuario = u.id_usuario
            WHERE LOWER(TRIM(u.correo)) = LOWER(TRIM(?))
            LIMIT 1
        """;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql)) {
            ML_ps.setString(1, ML_correo);
            try (ResultSet ML_rs = ML_ps.executeQuery()) {
                if (!ML_rs.next()) {
                    throw new IllegalArgumentException("Usuario no encontrado.");
                }
                if (!"Activo".equalsIgnoreCase(ML_rs.getString("estado"))) {
                    throw new IllegalArgumentException("Usuario inactivo.");
                }
                if (!ML_claveCoincide(ML_clave, ML_rs.getString("contraseña"))) {
                    throw new IllegalArgumentException("Contraseña incorrecta.");
                }
                return new ML_Sesion(
                        ML_rs.getInt("id_usuario"),
                        ML_rs.getInt("id_rol"),
                        ML_rs.getString("nombre_rol"),
                        ML_rs.getString("nombres") + " " + ML_rs.getString("apellidos"),
                        ML_rs.getObject("id_paciente") != null ? ML_rs.getInt("id_paciente") : null,
                        ML_rs.getObject("id_doctor") != null ? ML_rs.getInt("id_doctor") : null
                );
            }
        }
    }


    private boolean ML_claveCoincide(String ML_claveIngresada, String ML_claveGuardada) {
        if (ML_claveIngresada == null || ML_claveGuardada == null) {
            return false;
        }
        String ML_raw = ML_claveIngresada.trim();
        String ML_guardada = ML_claveGuardada.trim();
        if (ML_guardada.startsWith("{noop}")) {
            return ML_raw.equals(ML_guardada.substring(6).trim());
        }
        if (ML_guardada.startsWith("{bcrypt}")) {
            return ML_verificarBcryptPorReflexion(ML_raw, ML_guardada.substring(8).trim());
        }
        if (ML_guardada.startsWith("$2a$") || ML_guardada.startsWith("$2b$") || ML_guardada.startsWith("$2y$")) {
            return ML_verificarBcryptPorReflexion(ML_raw, ML_guardada);
        }
        return ML_raw.equals(ML_guardada);
    }

    private boolean ML_verificarBcryptPorReflexion(String ML_raw, String ML_hash) {
        try {
            Class<?> ML_clase = Class.forName("org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder");
            Object ML_encoder = ML_clase.getConstructor().newInstance();
            Object ML_resultado = ML_clase.getMethod("matches", CharSequence.class, String.class).invoke(ML_encoder, ML_raw, ML_hash);
            return Boolean.TRUE.equals(ML_resultado);
        } catch (Exception e) {
            return false;
        }
    }

    private void ML_mostrarPanelPrincipal() {
        JFrame ML_frame = new JFrame("CONSULTAS - Escritorio - " + ML_sesion.ML_rol);
        ML_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ML_frame.setSize(1100, 650);
        ML_frame.setLocationRelativeTo(null);

        JLabel ML_titulo = new JLabel("  Usuario: " + ML_sesion.ML_nombre + " | Rol: " + ML_sesion.ML_rol);
        JTabbedPane ML_tabs = new JTabbedPane();
        ML_tabs.addTab("Citas", ML_crearPanelCitas());
        ML_tabs.addTab("Horarios doctores", ML_crearPanelHorarios());
        ML_tabs.addTab("Pagos y comprobantes", ML_crearPanelPagos());

        ML_frame.add(ML_titulo, BorderLayout.NORTH);
        ML_frame.add(ML_tabs, BorderLayout.CENTER);
        ML_refrescarTodo();
        ML_frame.setVisible(true);
    }

    private JPanel ML_crearPanelCitas() {
        JPanel ML_panel = new JPanel(new BorderLayout());
        ML_tablaCitas = new JTable();
        JPanel ML_acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ML_refrescar = new JButton("Actualizar");
        JButton ML_nueva = new JButton("Registrar cita");
        JButton ML_confirmar = new JButton("Confirmar");
        JButton ML_cancelar = new JButton("Cancelar");

        ML_acciones.add(ML_refrescar);
        if (ML_esSecretariaOAdmin()) {
            ML_acciones.add(ML_nueva);
            ML_acciones.add(ML_confirmar);
            ML_acciones.add(ML_cancelar);
        }
        ML_refrescar.addActionListener(e -> ML_cargarCitas());
        ML_nueva.addActionListener(e -> ML_mostrarDialogoCita());
        ML_confirmar.addActionListener(e -> ML_cambiarEstadoCitaSeleccionada("Confirmada"));
        ML_cancelar.addActionListener(e -> ML_cambiarEstadoCitaSeleccionada("Cancelada"));

        ML_panel.add(ML_acciones, BorderLayout.NORTH);
        ML_panel.add(new JScrollPane(ML_tablaCitas), BorderLayout.CENTER);
        return ML_panel;
    }

    private JPanel ML_crearPanelHorarios() {
        JPanel ML_panel = new JPanel(new BorderLayout());
        ML_tablaHorarios = new JTable();
        JPanel ML_acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ML_refrescar = new JButton("Actualizar");
        JButton ML_nuevo = new JButton("Asignar horario");
        JButton ML_activar = new JButton("Activar");
        JButton ML_inactivar = new JButton("Inactivar");

        ML_acciones.add(ML_refrescar);
        if (ML_esSecretariaOAdmin()) {
            ML_acciones.add(ML_nuevo);
            ML_acciones.add(ML_activar);
            ML_acciones.add(ML_inactivar);
        }
        ML_refrescar.addActionListener(e -> ML_cargarHorarios());
        ML_nuevo.addActionListener(e -> ML_mostrarDialogoHorario());
        ML_activar.addActionListener(e -> ML_cambiarEstadoHorarioSeleccionado("Activo"));
        ML_inactivar.addActionListener(e -> ML_cambiarEstadoHorarioSeleccionado("Inactivo"));

        ML_panel.add(ML_acciones, BorderLayout.NORTH);
        ML_panel.add(new JScrollPane(ML_tablaHorarios), BorderLayout.CENTER);
        return ML_panel;
    }

    private JPanel ML_crearPanelPagos() {
        JPanel ML_panel = new JPanel(new BorderLayout());
        ML_tablaPagos = new JTable();
        JPanel ML_acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton ML_refrescar = new JButton("Actualizar");
        JButton ML_comprobante = new JButton("Ver comprobante");
        ML_acciones.add(ML_refrescar);
        ML_acciones.add(ML_comprobante);
        ML_refrescar.addActionListener(e -> ML_cargarPagos());
        ML_comprobante.addActionListener(e -> ML_verComprobanteSeleccionado());
        ML_panel.add(ML_acciones, BorderLayout.NORTH);
        ML_panel.add(new JScrollPane(ML_tablaPagos), BorderLayout.CENTER);
        return ML_panel;
    }

    private void ML_refrescarTodo() {
        ML_cargarCitas();
        ML_cargarHorarios();
        ML_cargarPagos();
    }

    private void ML_cargarCitas() {
        DefaultTableModel ML_modelo = new DefaultTableModel(new Object[]{"ID", "Paciente", "Doctor", "Especialidad", "Consultorio", "Fecha", "Inicio", "Fin", "Estado", "Motivo"}, 0);
        String ML_sql = """
            SELECT c.id_cita, CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                   CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                   e.nombre_especialidad, co.nombre_consultorio, c.fecha_cita,
                   TIME_FORMAT(c.hora_inicio, '%H:%i') AS hora_inicio,
                   TIME_FORMAT(c.hora_fin, '%H:%i') AS hora_fin,
                   c.estado, COALESCE(c.motivo, '-') AS motivo
            FROM citas c
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
        """ + ML_filtroRolCitas() + " ORDER BY c.fecha_cita DESC, c.hora_inicio DESC";
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_prepararFiltroRol(ML_cn, ML_sql, "citas"); ResultSet ML_rs = ML_ps.executeQuery()) {
            while (ML_rs.next()) {
                ML_modelo.addRow(new Object[]{ML_rs.getInt("id_cita"), ML_rs.getString("paciente"), ML_rs.getString("doctor"), ML_rs.getString("nombre_especialidad"), ML_rs.getString("nombre_consultorio"), ML_rs.getDate("fecha_cita"), ML_rs.getString("hora_inicio"), ML_rs.getString("hora_fin"), ML_rs.getString("estado"), ML_rs.getString("motivo")});
            }
        } catch (Exception e) {
            ML_error(e);
        }
        ML_tablaCitas.setModel(ML_modelo);
    }

    private void ML_cargarHorarios() {
        DefaultTableModel ML_modelo = new DefaultTableModel(new Object[]{"ID", "Doctor", "Especialidad", "Consultorio", "Día", "Inicio", "Fin", "Estado"}, 0);
        String ML_sql = """
            SELECT h.id_horario, CONCAT('Dr. ', u.nombres, ' ', u.apellidos) AS doctor,
                   e.nombre_especialidad, co.nombre_consultorio, h.dia_semana,
                   TIME_FORMAT(h.hora_inicio, '%H:%i') AS hora_inicio,
                   TIME_FORMAT(h.hora_fin, '%H:%i') AS hora_fin, h.estado
            FROM horarios h
            INNER JOIN doctores d ON h.id_doctor = d.id_doctor
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON d.id_consultorio = co.id_consultorio
        """ + ML_filtroRolHorarios() + " ORDER BY FIELD(h.dia_semana,'Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'), h.hora_inicio";
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_prepararFiltroRol(ML_cn, ML_sql, "horarios"); ResultSet ML_rs = ML_ps.executeQuery()) {
            while (ML_rs.next()) {
                ML_modelo.addRow(new Object[]{ML_rs.getInt("id_horario"), ML_rs.getString("doctor"), ML_rs.getString("nombre_especialidad"), ML_rs.getString("nombre_consultorio"), ML_rs.getString("dia_semana"), ML_rs.getString("hora_inicio"), ML_rs.getString("hora_fin"), ML_rs.getString("estado")});
            }
        } catch (Exception e) {
            ML_error(e);
        }
        ML_tablaHorarios.setModel(ML_modelo);
    }

    private void ML_cargarPagos() {
        DefaultTableModel ML_modelo = new DefaultTableModel(new Object[]{"ID", "Cita", "Paciente", "Doctor", "Monto", "Método", "Estado", "Operación", "Fecha"}, 0);
        String ML_sql = """
            SELECT pg.id_pago, pg.id_cita, CONCAT(up.nombres, ' ', up.apellidos) AS paciente,
                   CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor, pg.monto,
                   pg.metodo_pago, pg.estado_pago, COALESCE(pg.codigo_operacion, '-') AS codigo_operacion,
                   DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %H:%i') AS fecha_pago
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
        """ + ML_filtroRolPagos() + " ORDER BY pg.fecha_pago DESC, pg.id_pago DESC";
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_prepararFiltroRol(ML_cn, ML_sql, "pagos"); ResultSet ML_rs = ML_ps.executeQuery()) {
            while (ML_rs.next()) {
                ML_modelo.addRow(new Object[]{ML_rs.getInt("id_pago"), ML_rs.getInt("id_cita"), ML_rs.getString("paciente"), ML_rs.getString("doctor"), ML_rs.getBigDecimal("monto"), ML_rs.getString("metodo_pago"), ML_rs.getString("estado_pago"), ML_rs.getString("codigo_operacion"), ML_rs.getString("fecha_pago")});
            }
        } catch (Exception e) {
            ML_error(e);
        }
        ML_tablaPagos.setModel(ML_modelo);
    }

    private String ML_filtroRolCitas() {
        if (ML_sesion.ML_idRol == 3) return " WHERE c.id_doctor = ? ";
        if (ML_sesion.ML_idRol == 4) return " WHERE c.id_paciente = ? ";
        return " ";
    }

    private String ML_filtroRolHorarios() {
        if (ML_sesion.ML_idRol == 3) return " WHERE h.id_doctor = ? ";
        return " ";
    }

    private String ML_filtroRolPagos() {
        if (ML_sesion.ML_idRol == 3) return " WHERE c.id_doctor = ? ";
        if (ML_sesion.ML_idRol == 4) return " WHERE c.id_paciente = ? AND pg.estado_pago = 'Pagado' ";
        return " WHERE NOT (pg.estado_pago = 'Pendiente' AND EXISTS (SELECT 1 FROM pagos px WHERE px.id_cita = pg.id_cita AND px.estado_pago = 'Pagado')) ";
    }

    private PreparedStatement ML_prepararFiltroRol(Connection ML_cn, String ML_sql, String ML_tipo) throws Exception {
        PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql);
        if ("citas".equals(ML_tipo)) {
            if (ML_sesion.ML_idRol == 3) ML_ps.setInt(1, ML_sesion.ML_idDoctor);
            if (ML_sesion.ML_idRol == 4) ML_ps.setInt(1, ML_sesion.ML_idPaciente);
        } else if ("horarios".equals(ML_tipo)) {
            if (ML_sesion.ML_idRol == 3) ML_ps.setInt(1, ML_sesion.ML_idDoctor);
        } else if ("pagos".equals(ML_tipo)) {
            if (ML_sesion.ML_idRol == 3) ML_ps.setInt(1, ML_sesion.ML_idDoctor);
            if (ML_sesion.ML_idRol == 4) ML_ps.setInt(1, ML_sesion.ML_idPaciente);
        }
        return ML_ps;
    }

    private void ML_mostrarDialogoHorario() {
        try {
            List<ML_Opcion> ML_doctores = ML_listarDoctores();
            JComboBox<ML_Opcion> ML_doctor = new JComboBox<>(ML_doctores.toArray(new ML_Opcion[0]));
            JComboBox<String> ML_dia = new JComboBox<>(new String[]{"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"});
            JTextField ML_inicio = new JTextField("08:00");
            JTextField ML_fin = new JTextField("12:00");
            JComboBox<String> ML_estado = new JComboBox<>(new String[]{"Activo", "Inactivo"});
            JPanel ML_form = new JPanel(new GridLayout(5, 2, 8, 8));
            ML_form.add(new JLabel("Doctor:")); ML_form.add(ML_doctor);
            ML_form.add(new JLabel("Día:")); ML_form.add(ML_dia);
            ML_form.add(new JLabel("Hora inicio:")); ML_form.add(ML_inicio);
            ML_form.add(new JLabel("Hora fin:")); ML_form.add(ML_fin);
            ML_form.add(new JLabel("Estado:")); ML_form.add(ML_estado);
            int ML_ok = JOptionPane.showConfirmDialog(null, ML_form, "Asignar horario", JOptionPane.OK_CANCEL_OPTION);
            if (ML_ok == JOptionPane.OK_OPTION) {
                ML_guardarHorario((ML_Opcion) ML_doctor.getSelectedItem(), (String) ML_dia.getSelectedItem(), ML_inicio.getText(), ML_fin.getText(), (String) ML_estado.getSelectedItem());
                ML_cargarHorarios();
            }
        } catch (Exception e) {
            ML_error(e);
        }
    }

    private void ML_guardarHorario(ML_Opcion ML_doctor, String ML_dia, String ML_inicio, String ML_fin, String ML_estado) throws Exception {
        LocalTime ML_horaInicio = LocalTime.parse(ML_inicio.trim());
        LocalTime ML_horaFin = LocalTime.parse(ML_fin.trim());
        if (!ML_horaFin.isAfter(ML_horaInicio)) {
            throw new IllegalArgumentException("La hora fin debe ser posterior a la hora inicio.");
        }
        try (Connection ML_cn = ML_conectar()) {
            int ML_cruces = ML_contar(ML_cn, "SELECT COUNT(*) FROM horarios WHERE id_doctor=? AND dia_semana=? AND estado='Activo' AND hora_inicio < ? AND hora_fin > ?", ML_doctor.ML_id, ML_dia, Time.valueOf(ML_horaFin), Time.valueOf(ML_horaInicio));
            if (ML_cruces > 0 && "Activo".equals(ML_estado)) {
                throw new IllegalArgumentException("El doctor ya tiene un horario cruzado ese día.");
            }
            try (PreparedStatement ML_ps = ML_cn.prepareStatement("INSERT INTO horarios (id_doctor, dia_semana, hora_inicio, hora_fin, estado) VALUES (?, ?, ?, ?, ?)")) {
                ML_ps.setInt(1, ML_doctor.ML_id);
                ML_ps.setString(2, ML_dia);
                ML_ps.setTime(3, Time.valueOf(ML_horaInicio));
                ML_ps.setTime(4, Time.valueOf(ML_horaFin));
                ML_ps.setString(5, ML_estado);
                ML_ps.executeUpdate();
            }
        }
    }

    private void ML_mostrarDialogoCita() {
        try {
            List<ML_Opcion> ML_pacientes = ML_listarPacientes();
            List<ML_Opcion> ML_doctores = ML_listarDoctores();
            JComboBox<ML_Opcion> ML_paciente = new JComboBox<>(ML_pacientes.toArray(new ML_Opcion[0]));
            JComboBox<ML_Opcion> ML_doctor = new JComboBox<>(ML_doctores.toArray(new ML_Opcion[0]));
            JTextField ML_fecha = new JTextField(LocalDate.now().toString());
            JTextField ML_inicio = new JTextField("09:00");
            JTextField ML_fin = new JTextField("09:30");
            JTextArea ML_motivo = new JTextArea(3, 20);
            JPanel ML_form = new JPanel(new GridLayout(6, 2, 8, 8));
            ML_form.add(new JLabel("Paciente:")); ML_form.add(ML_paciente);
            ML_form.add(new JLabel("Doctor:")); ML_form.add(ML_doctor);
            ML_form.add(new JLabel("Fecha yyyy-mm-dd:")); ML_form.add(ML_fecha);
            ML_form.add(new JLabel("Hora inicio:")); ML_form.add(ML_inicio);
            ML_form.add(new JLabel("Hora fin:")); ML_form.add(ML_fin);
            ML_form.add(new JLabel("Motivo:")); ML_form.add(new JScrollPane(ML_motivo));
            int ML_ok = JOptionPane.showConfirmDialog(null, ML_form, "Registrar cita", JOptionPane.OK_CANCEL_OPTION);
            if (ML_ok == JOptionPane.OK_OPTION) {
                ML_guardarCita((ML_Opcion) ML_paciente.getSelectedItem(), (ML_Opcion) ML_doctor.getSelectedItem(), ML_fecha.getText(), ML_inicio.getText(), ML_fin.getText(), ML_motivo.getText());
                ML_cargarCitas();
            }
        } catch (Exception e) {
            ML_error(e);
        }
    }

    private void ML_guardarCita(ML_Opcion ML_paciente, ML_Opcion ML_doctor, String ML_fechaTexto, String ML_inicioTexto, String ML_finTexto, String ML_motivo) throws Exception {
        LocalDate ML_fecha = LocalDate.parse(ML_fechaTexto.trim());
        LocalTime ML_inicio = LocalTime.parse(ML_inicioTexto.trim());
        LocalTime ML_fin = LocalTime.parse(ML_finTexto.trim());
        if (!ML_fin.isAfter(ML_inicio)) {
            throw new IllegalArgumentException("La hora fin debe ser posterior a la hora inicio.");
        }
        try (Connection ML_cn = ML_conectar()) {
            Integer ML_consultorio = ML_obtenerConsultorioDoctor(ML_cn, ML_doctor.ML_id);
            String ML_dia = ML_diaEspanol(ML_fecha.getDayOfWeek());
            int ML_disponible = ML_contar(ML_cn, "SELECT COUNT(*) FROM horarios WHERE id_doctor=? AND dia_semana=? AND estado='Activo' AND hora_inicio <= ? AND hora_fin >= ?", ML_doctor.ML_id, ML_dia, Time.valueOf(ML_inicio), Time.valueOf(ML_fin));
            if (ML_disponible == 0) {
                throw new IllegalArgumentException("El doctor no tiene disponibilidad activa para esa fecha y hora.");
            }
            int ML_cruceDoctor = ML_contar(ML_cn, "SELECT COUNT(*) FROM citas WHERE id_doctor=? AND fecha_cita=? AND estado <> 'Cancelada' AND hora_inicio < ? AND hora_fin > ?", ML_doctor.ML_id, Date.valueOf(ML_fecha), Time.valueOf(ML_fin), Time.valueOf(ML_inicio));
            if (ML_cruceDoctor > 0) {
                throw new IllegalArgumentException("El doctor ya tiene una cita cruzada.");
            }
            int ML_cruceConsultorio = ML_contar(ML_cn, "SELECT COUNT(*) FROM citas WHERE id_consultorio=? AND fecha_cita=? AND estado <> 'Cancelada' AND hora_inicio < ? AND hora_fin > ?", ML_consultorio, Date.valueOf(ML_fecha), Time.valueOf(ML_fin), Time.valueOf(ML_inicio));
            if (ML_cruceConsultorio > 0) {
                throw new IllegalArgumentException("El consultorio ya está ocupado en ese horario.");
            }
            try (PreparedStatement ML_ps = ML_cn.prepareStatement("INSERT INTO citas (id_paciente, id_doctor, id_consultorio, fecha_cita, hora_inicio, hora_fin, motivo, estado) VALUES (?, ?, ?, ?, ?, ?, ?, 'Confirmada')")) {
                ML_ps.setInt(1, ML_paciente.ML_id);
                ML_ps.setInt(2, ML_doctor.ML_id);
                ML_ps.setInt(3, ML_consultorio);
                ML_ps.setDate(4, Date.valueOf(ML_fecha));
                ML_ps.setTime(5, Time.valueOf(ML_inicio));
                ML_ps.setTime(6, Time.valueOf(ML_fin));
                ML_ps.setString(7, ML_motivo == null || ML_motivo.isBlank() ? "Consulta registrada por secretaria" : ML_motivo.trim());
                ML_ps.executeUpdate();
            }
        }
    }

    private void ML_cambiarEstadoCitaSeleccionada(String ML_estado) {
        Integer ML_id = ML_idSeleccionado(ML_tablaCitas, 0);
        if (ML_id == null) return;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement("UPDATE citas SET estado=? WHERE id_cita=? AND estado <> 'Atendida'")) {
            if ("Cancelada".equals(ML_estado)) {
                int ML_pagada = ML_contar(ML_cn, "SELECT COUNT(*) FROM pagos WHERE id_cita=? AND estado_pago='Pagado'", ML_id);
                if (ML_pagada > 0) throw new IllegalArgumentException("No se puede cancelar una cita con pago confirmado.");
            }
            ML_ps.setString(1, ML_estado);
            ML_ps.setInt(2, ML_id);
            ML_ps.executeUpdate();
            ML_cargarCitas();
        } catch (Exception e) {
            ML_error(e);
        }
    }

    private void ML_cambiarEstadoHorarioSeleccionado(String ML_estado) {
        Integer ML_id = ML_idSeleccionado(ML_tablaHorarios, 0);
        if (ML_id == null) return;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement("UPDATE horarios SET estado=? WHERE id_horario=?")) {
            ML_ps.setString(1, ML_estado);
            ML_ps.setInt(2, ML_id);
            ML_ps.executeUpdate();
            ML_cargarHorarios();
        } catch (Exception e) {
            ML_error(e);
        }
    }

    private void ML_verComprobanteSeleccionado() {
        Integer ML_id = ML_idSeleccionado(ML_tablaPagos, 0);
        if (ML_id == null) return;
        String ML_sql = """
            SELECT pg.id_pago, pg.id_cita, pg.monto, pg.metodo_pago, pg.estado_pago,
                   COALESCE(pg.codigo_operacion, '-') AS codigo_operacion,
                   DATE_FORMAT(pg.fecha_pago, '%d/%m/%Y %H:%i') AS fecha_pago,
                   CONCAT(up.nombres, ' ', up.apellidos) AS paciente, up.dni,
                   CONCAT('Dr. ', ud.nombres, ' ', ud.apellidos) AS doctor,
                   e.nombre_especialidad, co.nombre_consultorio, c.fecha_cita, TIME_FORMAT(c.hora_inicio, '%H:%i') AS hora_cita
            FROM pagos pg
            INNER JOIN citas c ON pg.id_cita = c.id_cita
            INNER JOIN pacientes p ON c.id_paciente = p.id_paciente
            INNER JOIN usuarios up ON p.id_usuario = up.id_usuario
            INNER JOIN doctores d ON c.id_doctor = d.id_doctor
            INNER JOIN usuarios ud ON d.id_usuario = ud.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            LEFT JOIN consultorios co ON c.id_consultorio = co.id_consultorio
            WHERE pg.id_pago = ?
        """;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql)) {
            ML_ps.setInt(1, ML_id);
            try (ResultSet ML_rs = ML_ps.executeQuery()) {
                if (!ML_rs.next()) throw new IllegalArgumentException("No se encontró el pago.");
                String ML_texto = "Recibo de pago PAGO-" + String.format("%08d", ML_rs.getInt("id_pago")) + "\n\n"
                        + "Paciente: " + ML_rs.getString("paciente") + "\n"
                        + "DNI: " + ML_rs.getString("dni") + "\n"
                        + "Doctor: " + ML_rs.getString("doctor") + "\n"
                        + "Especialidad: " + ML_rs.getString("nombre_especialidad") + "\n"
                        + "Consultorio: " + ML_rs.getString("nombre_consultorio") + "\n"
                        + "Cita: " + ML_rs.getDate("fecha_cita") + " " + ML_rs.getString("hora_cita") + "\n"
                        + "Método: " + ML_rs.getString("metodo_pago") + "\n"
                        + "Operación: " + ML_rs.getString("codigo_operacion") + "\n"
                        + "Estado pago: " + ML_rs.getString("estado_pago") + "\n"
                        + "Monto: S/ " + ML_rs.getBigDecimal("monto") + "\n"
                        + "Fecha pago: " + ML_rs.getString("fecha_pago") + "\n";
                JTextArea ML_area = new JTextArea(ML_texto, 16, 52);
                ML_area.setEditable(false);
                JOptionPane.showMessageDialog(null, new JScrollPane(ML_area), "Comprobante de pago", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            ML_error(e);
        }
    }

    private List<ML_Opcion> ML_listarDoctores() throws Exception {
        List<ML_Opcion> ML_lista = new ArrayList<>();
        String ML_sql = """
            SELECT d.id_doctor, CONCAT('Dr. ', u.nombres, ' ', u.apellidos, ' - ', e.nombre_especialidad) AS texto
            FROM doctores d
            INNER JOIN usuarios u ON d.id_usuario = u.id_usuario
            INNER JOIN especialidades e ON d.id_especialidad = e.id_especialidad
            WHERE d.estado='Activo' AND u.estado='Activo'
            ORDER BY u.apellidos, u.nombres
        """;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql); ResultSet ML_rs = ML_ps.executeQuery()) {
            while (ML_rs.next()) ML_lista.add(new ML_Opcion(ML_rs.getInt("id_doctor"), ML_rs.getString("texto")));
        }
        return ML_lista;
    }

    private List<ML_Opcion> ML_listarPacientes() throws Exception {
        List<ML_Opcion> ML_lista = new ArrayList<>();
        String ML_sql = """
            SELECT p.id_paciente, CONCAT(u.nombres, ' ', u.apellidos, ' - DNI: ', COALESCE(u.dni, '-')) AS texto
            FROM pacientes p
            INNER JOIN usuarios u ON p.id_usuario = u.id_usuario
            WHERE u.estado='Activo'
            ORDER BY u.apellidos, u.nombres
        """;
        try (Connection ML_cn = ML_conectar(); PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql); ResultSet ML_rs = ML_ps.executeQuery()) {
            while (ML_rs.next()) ML_lista.add(new ML_Opcion(ML_rs.getInt("id_paciente"), ML_rs.getString("texto")));
        }
        return ML_lista;
    }

    private Integer ML_obtenerConsultorioDoctor(Connection ML_cn, int ML_idDoctor) throws Exception {
        try (PreparedStatement ML_ps = ML_cn.prepareStatement("SELECT id_consultorio FROM doctores WHERE id_doctor=? AND estado='Activo'")) {
            ML_ps.setInt(1, ML_idDoctor);
            try (ResultSet ML_rs = ML_ps.executeQuery()) {
                if (ML_rs.next()) return ML_rs.getInt("id_consultorio");
            }
        }
        throw new IllegalArgumentException("El doctor no tiene consultorio activo asignado.");
    }

    private int ML_contar(Connection ML_cn, String ML_sql, Object... ML_params) throws Exception {
        try (PreparedStatement ML_ps = ML_cn.prepareStatement(ML_sql)) {
            for (int i = 0; i < ML_params.length; i++) ML_ps.setObject(i + 1, ML_params[i]);
            try (ResultSet ML_rs = ML_ps.executeQuery()) {
                return ML_rs.next() ? ML_rs.getInt(1) : 0;
            }
        }
    }

    private String ML_diaEspanol(DayOfWeek ML_dia) {
        return switch (ML_dia) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            default -> "Domingo";
        };
    }

    private Integer ML_idSeleccionado(JTable ML_tabla, int ML_columna) {
        int ML_fila = ML_tabla.getSelectedRow();
        if (ML_fila < 0) {
            JOptionPane.showMessageDialog(null, "Selecciona una fila primero.");
            return null;
        }
        Object ML_valor = ML_tabla.getValueAt(ML_fila, ML_columna);
        if (ML_valor instanceof Integer) return (Integer) ML_valor;
        return Integer.valueOf(ML_valor.toString());
    }

    private boolean ML_esSecretariaOAdmin() {
        return ML_sesion.ML_idRol == 1 || ML_sesion.ML_idRol == 2;
    }

    private void ML_error(Exception e) {
        JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ML_Sesion {
        final int ML_idUsuario;
        final int ML_idRol;
        final String ML_rol;
        final String ML_nombre;
        final Integer ML_idPaciente;
        final Integer ML_idDoctor;

        ML_Sesion(int ML_idUsuario, int ML_idRol, String ML_rol, String ML_nombre, Integer ML_idPaciente, Integer ML_idDoctor) {
            this.ML_idUsuario = ML_idUsuario;
            this.ML_idRol = ML_idRol;
            this.ML_rol = ML_rol;
            this.ML_nombre = ML_nombre;
            this.ML_idPaciente = ML_idPaciente;
            this.ML_idDoctor = ML_idDoctor;
        }
    }

    private static class ML_Opcion {
        final int ML_id;
        final String ML_texto;

        ML_Opcion(int ML_id, String ML_texto) {
            this.ML_id = ML_id;
            this.ML_texto = ML_texto;
        }

        @Override
        public String toString() {
            return ML_texto;
        }
    }
}
