package com.medicas.consultas.servicio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ML_ConsultorioAdminService {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_ConsultorioAdminService(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    public List<Map<String, Object>> ML_listarConsultorios() {
        String ML_sql = """
            SELECT
                co.id_consultorio,
                co.nombre_consultorio,
                COALESCE(co.ubicacion, '-') AS ubicacion,
                COALESCE(co.piso, 1) AS piso,
                COALESCE(co.estado, 'Disponible') AS estado,
                COUNT(d.id_doctor) AS doctores_asignados
            FROM consultorios co
            LEFT JOIN doctores d ON d.id_consultorio = co.id_consultorio
            GROUP BY co.id_consultorio, co.nombre_consultorio, co.ubicacion, co.piso, co.estado
            ORDER BY co.id_consultorio DESC
        """;
        return ML_consultarLista(ML_sql);
    }

    public int ML_contarConsultorios() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios");
    }

    public int ML_contarDisponibles() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios WHERE COALESCE(estado, 'Disponible') = 'Disponible'");
    }

    public int ML_contarOcupados() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios WHERE estado = 'Ocupado'");
    }

    public int ML_contarMantenimiento() {
        return ML_obtenerEntero("SELECT COUNT(*) FROM consultorios WHERE estado = 'Mantenimiento'");
    }

    public List<String> ML_listarEstadosConsultorio() {
        return Arrays.asList("Disponible", "Ocupado", "Mantenimiento", "Inactivo");
    }

    @Transactional
    public void ML_guardarConsultorio(String ML_nombre, String ML_ubicacion, Integer ML_piso, String ML_estado) {
        String ML_nombreLimpio = ML_limpiarTexto(ML_nombre);
        if (ML_nombreLimpio == null) {
            throw new IllegalArgumentException("Debe ingresar el nombre del consultorio.");
        }

        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);
        String ML_ubicacionLimpia = ML_limpiarTexto(ML_ubicacion);
        Integer ML_pisoSeguro = ML_piso != null && ML_piso > 0 ? ML_piso : 1;

        int ML_repetidos = ML_obtenerEntero(
                "SELECT COUNT(*) FROM consultorios WHERE LOWER(TRIM(nombre_consultorio)) = LOWER(TRIM(?))",
                ML_nombreLimpio
        );

        if (ML_repetidos > 0) {
            throw new IllegalArgumentException("Ya existe un consultorio con ese nombre.");
        }

        ML_jdbcTemplate.update(
                "INSERT INTO consultorios (nombre_consultorio, ubicacion, piso, estado) VALUES (?, ?, ?, ?)",
                ML_nombreLimpio,
                ML_ubicacionLimpia,
                ML_pisoSeguro,
                ML_estadoNormalizado
        );
    }

    @Transactional
    public void ML_actualizarEstadoConsultorio(Integer ML_idConsultorio, String ML_estado) {
        if (ML_idConsultorio == null) {
            throw new IllegalArgumentException("No se recibió el consultorio.");
        }

        String ML_estadoNormalizado = ML_normalizarEstado(ML_estado);
        int ML_filas = ML_jdbcTemplate.update(
                "UPDATE consultorios SET estado = ? WHERE id_consultorio = ?",
                ML_estadoNormalizado,
                ML_idConsultorio
        );

        if (ML_filas == 0) {
            throw new IllegalArgumentException("No se pudo actualizar el estado del consultorio.");
        }
    }

    private String ML_normalizarEstado(String ML_estado) {
        if (ML_estado == null) {
            return "Disponible";
        }
        for (String ML_opcion : ML_listarEstadosConsultorio()) {
            if (ML_opcion.equalsIgnoreCase(ML_estado.trim())) {
                return ML_opcion;
            }
        }
        return "Disponible";
    }

    private String ML_limpiarTexto(String ML_texto) {
        if (ML_texto == null) {
            return null;
        }
        String ML_limpio = ML_texto.trim();
        return ML_limpio.isEmpty() ? null : ML_limpio;
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
