package com.medicas.consultas.controlador.api;

import com.medicas.consultas.servicio.ML_SecretariaPanelService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comprobantes")
public class ML_ComprobantePagoApiController {

    private final ML_SecretariaPanelService ML_secretariaPanelService;

    public ML_ComprobantePagoApiController(ML_SecretariaPanelService ML_secretariaPanelService) {
        this.ML_secretariaPanelService = ML_secretariaPanelService;
    }

    @GetMapping("/pagos/{ML_idPago}")
    public ResponseEntity<Map<String, Object>> ML_obtenerComprobantePago(@PathVariable Integer ML_idPago,
                                                                         Authentication authentication) {
        if (!ML_puedeConsultarComprobante(ML_idPago, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ML_respuestaError("No tienes permiso para consultar este comprobante."));
        }

        Map<String, Object> ML_comprobante = ML_secretariaPanelService.ML_obtenerComprobantePago(ML_idPago);
        if (ML_comprobante.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ML_respuestaError("No se encontró el comprobante solicitado."));
        }

        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("ok", true);
        ML_respuesta.put("mensaje", "Comprobante encontrado");
        ML_respuesta.put("comprobante", ML_comprobante);
        return ResponseEntity.ok(ML_respuesta);
    }

    @GetMapping("/pagos/{ML_idPago}/pdf")
    public void ML_descargarComprobantePagoPdf(@PathVariable Integer ML_idPago,
                                               Authentication authentication,
                                               HttpServletResponse ML_response) {
        try {
            if (!ML_puedeConsultarComprobante(ML_idPago, authentication)) {
                ML_response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            ML_secretariaPanelService.ML_exportarComprobantePagoPDF(ML_idPago, ML_response);
        } catch (Exception e) {
            ML_response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean ML_puedeConsultarComprobante(Integer ML_idPago, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (ML_tieneRol(authentication, "ROLE_ADMIN") || ML_tieneRol(authentication, "ROLE_SECRETARIA")) {
            return true;
        }

        if (ML_tieneRol(authentication, "ROLE_PACIENTE")) {
            return ML_secretariaPanelService.ML_pagoPertenecePaciente(ML_idPago, authentication.getName());
        }

        return false;
    }

    private boolean ML_tieneRol(Authentication authentication, String ML_rol) {
        for (GrantedAuthority ML_authority : authentication.getAuthorities()) {
            if (ML_rol.equals(ML_authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> ML_respuestaError(String ML_mensaje) {
        Map<String, Object> ML_respuesta = new LinkedHashMap<>();
        ML_respuesta.put("ok", false);
        ML_respuesta.put("mensaje", ML_mensaje);
        return ML_respuesta;
    }
}
