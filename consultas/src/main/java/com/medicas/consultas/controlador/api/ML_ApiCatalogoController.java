package com.medicas.consultas.controlador.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ML_ApiCatalogoController {

    private final JdbcTemplate ML_jdbcTemplate;

    public ML_ApiCatalogoController(JdbcTemplate ML_jdbcTemplate) {
        this.ML_jdbcTemplate = ML_jdbcTemplate;
    }

    @GetMapping("/salud")
    public ResponseEntity<Map<String, Object>> ML_saludApi() {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("sistema", "CONSULTAS - MediLink");
        ML_respuesta.put("estado", "activo");
        ML_respuesta.put("version", "4.2.5");
        return ResponseEntity.ok(ML_respuesta);
    }

    @GetMapping("/catalogo")
    public ResponseEntity<Map<String, Object>> ML_catalogoApis() {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("sistema", "CONSULTAS - MediLink");
        ML_respuesta.put("version", "4.2.5");
        ML_respuesta.put("roles", ML_consultarRoles());
        ML_respuesta.put("estadosCita", List.of("Pendiente", "Confirmada", "Atendida", "Cancelada"));
        ML_respuesta.put("estadosPago", List.of("Pendiente", "Pagado", "Anulado"));
        ML_respuesta.put("metodosPago", ML_consultarMetodosPago());
        return ResponseEntity.ok(ML_respuesta);
    }

    private List<Map<String, Object>> ML_consultarRoles() {
        try {
            return ML_jdbcTemplate.queryForList(
                "SELECT id_rol, nombre_rol, COALESCE(descripcion_rol, '') AS descripcion_rol FROM roles ORDER BY id_rol"
            );
        } catch (Exception e) {
            return List.of(
                Map.of("id_rol", 1, "nombre_rol", "Administrador"),
                Map.of("id_rol", 2, "nombre_rol", "Secretaria"),
                Map.of("id_rol", 3, "nombre_rol", "Doctor"),
                Map.of("id_rol", 4, "nombre_rol", "Paciente")
            );
        }
    }

    private List<String> ML_consultarMetodosPago() {
        try {
            return ML_jdbcTemplate.queryForList(
                "SELECT DISTINCT metodo_pago FROM pagos WHERE metodo_pago IS NOT NULL ORDER BY metodo_pago", String.class
            );
        } catch (Exception e) {
            return List.of("Efectivo", "Yape", "Plin");
        }
    }
}
