package com.medicas.consultas.controlador;

import com.medicas.consultas.dto.ML_RegistroHistorialDoctorDTO;
import com.medicas.consultas.servicio.ML_DoctorPanelService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ML_DoctorController {

    private final ML_DoctorPanelService ML_doctorPanelService;

    public ML_DoctorController(ML_DoctorPanelService ML_doctorPanelService) {
        this.ML_doctorPanelService = ML_doctorPanelService;
    }

    @GetMapping("/doctor")
    public String ML_redirigirDashboardDoctor() {
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/doctor/dashboard")
    public String ML_mostrarDashboardDoctor(Authentication authentication, Model model) {
        Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);

        model.addAttribute("ML_tituloPagina", "Panel del Doctor");
        model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
        model.addAttribute("ML_totalCitas", ML_doctorPanelService.ML_contarCitasAsignadas(ML_idDoctor));
        model.addAttribute("ML_citasHoy", ML_doctorPanelService.ML_contarCitasHoy(ML_idDoctor));
        model.addAttribute("ML_citasPendientes", ML_doctorPanelService.ML_contarCitasPendientes(ML_idDoctor));
        model.addAttribute("ML_citasAtendidas", ML_doctorPanelService.ML_contarCitasAtendidas(ML_idDoctor));
        model.addAttribute("ML_historialesRegistrados", ML_doctorPanelService.ML_contarHistorialesRegistrados(ML_idDoctor));
        model.addAttribute("ML_proximasCitas", ML_doctorPanelService.ML_listarProximasCitas(ML_idDoctor));

        return "doctor/ML_dashboardDoctor";
    }

    @GetMapping("/doctor/citas")
    public String ML_mostrarCitasDoctor(Authentication authentication, Model model) {
        Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);

        model.addAttribute("ML_tituloPagina", "Citas Asignadas");
        model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
        model.addAttribute("ML_citas", ML_doctorPanelService.ML_listarCitasAsignadas(ML_idDoctor));
        model.addAttribute("ML_totalCitas", ML_doctorPanelService.ML_contarCitasAsignadas(ML_idDoctor));
        model.addAttribute("ML_citasPendientes", ML_doctorPanelService.ML_contarCitasPendientes(ML_idDoctor));
        model.addAttribute("ML_citasConfirmadas", ML_doctorPanelService.ML_contarCitasConfirmadas(ML_idDoctor));
        model.addAttribute("ML_citasAtendidas", ML_doctorPanelService.ML_contarCitasAtendidas(ML_idDoctor));

        return "doctor/ML_citasDoctor";
    }

    @PostMapping("/doctor/citas/{ML_idCita}/estado")
    public String ML_actualizarEstadoCitaDoctor(@PathVariable Integer ML_idCita,
                                                @RequestParam("ML_estado") String ML_estado,
                                                Authentication authentication,
                                                RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            ML_doctorPanelService.ML_actualizarEstadoCitaDoctor(ML_idDoctor, ML_idCita, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado de la cita actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }

        return "redirect:/doctor/citas";
    }

    @GetMapping("/doctor/seguimiento")
    public String ML_mostrarSeguimientoTratamiento(Authentication authentication, Model model) {
        Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);

        model.addAttribute("ML_tituloPagina", "Seguimiento e Historial Clínico");
        model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
        model.addAttribute("ML_citasParaSeguimiento", ML_doctorPanelService.ML_listarCitasParaRegistrarHistorial(ML_idDoctor));
        model.addAttribute("ML_totalHistoriales", ML_doctorPanelService.ML_contarHistorialesRegistrados(ML_idDoctor));
        model.addAttribute("ML_totalPacientesHistorial", ML_doctorPanelService.ML_contarPacientesConHistorial(ML_idDoctor));
        model.addAttribute("ML_totalSeguimientos", ML_doctorPanelService.ML_contarTratamientosEnSeguimiento(ML_idDoctor));
        model.addAttribute("ML_pacientesHistorial", ML_doctorPanelService.ML_listarPacientesConHistorialDoctor(ML_idDoctor));
        model.addAttribute("ML_historialCompleto", ML_doctorPanelService.ML_listarHistorialDoctor(ML_idDoctor));

        return "doctor/ML_seguimientoDoctor";
    }


    @GetMapping("/doctor/seguimiento/registros/{ML_idCita}")
    public String ML_mostrarRegistrosAntesDeNuevoSeguimiento(@PathVariable Integer ML_idCita,
                                                             Authentication authentication,
                                                             Model model,
                                                             RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            Map<String, Object> ML_cita = ML_doctorPanelService.ML_obtenerCitaParaAtencion(ML_idDoctor, ML_idCita);

            if (ML_cita.isEmpty()) {
                redirectAttributes.addFlashAttribute("ML_mensajeError", "No se encontró la cita seleccionada para el doctor autenticado.");
                return "redirect:/doctor/seguimiento";
            }

            model.addAttribute("ML_tituloPagina", "Registros del paciente");
            model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
            model.addAttribute("ML_cita", ML_cita);
            model.addAttribute("ML_historialPaciente", ML_doctorPanelService.ML_obtenerHistorialPacientePorCita(ML_idDoctor, ML_idCita));

            return "doctor/ML_registrosCitaDoctor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/doctor/seguimiento";
        }
    }

    @GetMapping("/doctor/seguimiento/registrar/{ML_idCita}")
    public String ML_mostrarFormularioSeguimiento(@PathVariable Integer ML_idCita,
                                                  Authentication authentication,
                                                  Model model,
                                                  RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            Map<String, Object> ML_cita = ML_doctorPanelService.ML_obtenerCitaParaAtencion(ML_idDoctor, ML_idCita);
            ML_RegistroHistorialDoctorDTO ML_historial = ML_doctorPanelService.ML_prepararHistorialCita(ML_idDoctor, ML_idCita);

            model.addAttribute("ML_tituloPagina", "Agregar seguimiento clínico");
            model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
            model.addAttribute("ML_cita", ML_cita);
            model.addAttribute("ML_historial", ML_historial);
            model.addAttribute("ML_modoEdicion", false);

            return "doctor/ML_registrarHistorialDoctor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/doctor/seguimiento";
        }
    }

    @PostMapping("/doctor/seguimiento/registrar/{ML_idCita}/guardar")
    public String ML_guardarSeguimientoDoctor(@PathVariable Integer ML_idCita,
                                              @ModelAttribute("ML_historial") ML_RegistroHistorialDoctorDTO ML_historial,
                                              Authentication authentication,
                                              RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            ML_doctorPanelService.ML_guardarHistorialCita(ML_idDoctor, ML_idCita, ML_historial);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Nuevo registro guardado correctamente. Se agregó al seguimiento acumulado del paciente.");
            return "redirect:/doctor/seguimiento";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/doctor/seguimiento/registrar/" + ML_idCita;
        }
    }

    @GetMapping("/doctor/seguimiento/actualizar/{ML_idHistorial}")
    public String ML_mostrarFormularioActualizarSeguimiento(@PathVariable Integer ML_idHistorial,
                                                            Authentication authentication,
                                                            Model model,
                                                            RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            ML_RegistroHistorialDoctorDTO ML_historial = ML_doctorPanelService.ML_obtenerHistorialPorId(ML_idDoctor, ML_idHistorial);
            Map<String, Object> ML_cita = ML_doctorPanelService.ML_obtenerCitaPorHistorial(ML_idDoctor, ML_idHistorial);

            model.addAttribute("ML_tituloPagina", "Actualizar registro clínico");
            model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
            model.addAttribute("ML_cita", ML_cita);
            model.addAttribute("ML_historial", ML_historial);
            model.addAttribute("ML_modoEdicion", true);

            return "doctor/ML_registrarHistorialDoctor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/doctor/seguimiento";
        }
    }

    @PostMapping("/doctor/seguimiento/actualizar/{ML_idHistorial}/guardar")
    public String ML_actualizarSeguimientoDoctor(@PathVariable Integer ML_idHistorial,
                                                 @ModelAttribute("ML_historial") ML_RegistroHistorialDoctorDTO ML_historial,
                                                 Authentication authentication,
                                                 RedirectAttributes redirectAttributes) {
        try {
            Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
            ML_doctorPanelService.ML_actualizarHistorialReciente(ML_idDoctor, ML_idHistorial, ML_historial);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Último seguimiento actualizado correctamente.");
            return "redirect:/doctor/seguimiento";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/doctor/seguimiento/actualizar/" + ML_idHistorial;
        }
    }

    @GetMapping("/doctor/seguimiento/paciente/{ML_idPaciente}")
    public String ML_mostrarDetalleSeguimientoPaciente(@PathVariable Integer ML_idPaciente,
                                                       Authentication authentication,
                                                       Model model,
                                                       RedirectAttributes redirectAttributes) {
        Integer ML_idDoctor = ML_obtenerIdDoctorAutenticado(authentication);
        Map<String, Object> ML_paciente = ML_doctorPanelService.ML_obtenerPacienteDoctor(ML_idDoctor, ML_idPaciente);

        if (ML_paciente.isEmpty()) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", "El paciente no tiene atenciones registradas con el doctor autenticado.");
            return "redirect:/doctor/seguimiento";
        }

        model.addAttribute("ML_tituloPagina", "Seguimiento acumulado del paciente");
        model.addAttribute("ML_perfilDoctor", ML_doctorPanelService.ML_obtenerPerfilDoctor(ML_idDoctor));
        model.addAttribute("ML_paciente", ML_paciente);
        model.addAttribute("ML_historialPaciente", ML_doctorPanelService.ML_obtenerHistorialPacienteDoctor(ML_idDoctor, ML_idPaciente));

        return "doctor/ML_historialDetalleDoctor";
    }

    @GetMapping("/doctor/historial")
    public String ML_redirigirHistorialASeguimiento() {
        return "redirect:/doctor/seguimiento";
    }

    @GetMapping("/doctor/historial/paciente/{ML_idPaciente}")
    public String ML_redirigirDetalleHistorialASeguimiento(@PathVariable Integer ML_idPaciente) {
        return "redirect:/doctor/seguimiento/paciente/" + ML_idPaciente;
    }

    @GetMapping("/doctor/historial/registrar/{ML_idCita}")
    public String ML_redirigirRegistrarHistorialASeguimiento(@PathVariable Integer ML_idCita) {
        return "redirect:/doctor/seguimiento/registrar/" + ML_idCita;
    }

    @PostMapping("/doctor/historial/registrar/{ML_idCita}/guardar")
    public String ML_guardarHistorialDoctorRutaAntigua(@PathVariable Integer ML_idCita,
                                                       @ModelAttribute("ML_historial") ML_RegistroHistorialDoctorDTO ML_historial,
                                                       Authentication authentication,
                                                       RedirectAttributes redirectAttributes) {
        return ML_guardarSeguimientoDoctor(ML_idCita, ML_historial, authentication, redirectAttributes);
    }

    @GetMapping("/doctor/citas/{ML_idCita}/atencion")
    public String ML_redirigirAtencionAntigua(@PathVariable Integer ML_idCita) {
        return "redirect:/doctor/seguimiento/registrar/" + ML_idCita;
    }

    @PostMapping("/doctor/citas/{ML_idCita}/historial/guardar")
    public String ML_guardarHistorialDoctorRutaCitasAntigua(@PathVariable Integer ML_idCita,
                                                            @ModelAttribute("ML_historial") ML_RegistroHistorialDoctorDTO ML_historial,
                                                            Authentication authentication,
                                                            RedirectAttributes redirectAttributes) {
        return ML_guardarSeguimientoDoctor(ML_idCita, ML_historial, authentication, redirectAttributes);
    }

    private Integer ML_obtenerIdDoctorAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("No se pudo identificar al doctor autenticado.");
        }

        Integer ML_idDoctor = ML_doctorPanelService.ML_obtenerIdDoctorPorCorreo(authentication.getName());

        if (ML_idDoctor == null) {
            throw new IllegalArgumentException("El usuario autenticado no tiene un registro activo como doctor.");
        }

        return ML_idDoctor;
    }
}
