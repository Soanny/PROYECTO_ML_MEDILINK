package com.medicas.consultas.controlador;

import com.medicas.consultas.dto.ML_PacientePagoVirtualDTO;
import com.medicas.consultas.servicio.ML_PacientePanelService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ML_PacienteController {

    private final ML_PacientePanelService ML_pacientePanelService;

    public ML_PacienteController(ML_PacientePanelService ML_pacientePanelService) {
        this.ML_pacientePanelService = ML_pacientePanelService;
    }

    @GetMapping("/paciente")
    public String ML_redirigirDashboardPaciente() {
        return "redirect:/paciente/dashboard";
    }

    @GetMapping("/paciente/dashboard")
    public String ML_mostrarDashboardPaciente(Authentication authentication, Model model) {
        Integer ML_idPaciente = ML_obtenerIdPacienteAutenticado(authentication);
        ML_cargarPerfilPaciente(ML_idPaciente, model);
        model.addAttribute("ML_tituloPagina", "Panel del Paciente");
        model.addAttribute("ML_totalCitas", ML_pacientePanelService.ML_contarCitasAsignadas(ML_idPaciente));
        model.addAttribute("ML_citasPendientes", ML_pacientePanelService.ML_contarCitasPendientes(ML_idPaciente));
        model.addAttribute("ML_citasConfirmadas", ML_pacientePanelService.ML_contarCitasConfirmadas(ML_idPaciente));
        model.addAttribute("ML_citasAtendidas", ML_pacientePanelService.ML_contarCitasAtendidas(ML_idPaciente));
        model.addAttribute("ML_totalHistorial", ML_pacientePanelService.ML_contarHistorialClinico(ML_idPaciente));
        model.addAttribute("ML_totalSeguimientos", ML_pacientePanelService.ML_contarSeguimientos(ML_idPaciente));
        model.addAttribute("ML_totalPagado", ML_pacientePanelService.ML_obtenerTotalPagadoFormateado(ML_idPaciente));
        model.addAttribute("ML_proximasCitas", ML_pacientePanelService.ML_listarProximasCitas(ML_idPaciente));
        model.addAttribute("ML_citasParaPago", ML_pacientePanelService.ML_listarCitasParaPagoVirtual(ML_idPaciente));
        return "paciente/ML_dashboardPaciente";
    }

    @GetMapping("/paciente/citas")
    public String ML_mostrarCitasPaciente(Authentication authentication, Model model) {
        Integer ML_idPaciente = ML_obtenerIdPacienteAutenticado(authentication);
        ML_cargarPerfilPaciente(ML_idPaciente, model);
        model.addAttribute("ML_tituloPagina", "Mis Citas Asignadas");
        model.addAttribute("ML_citas", ML_pacientePanelService.ML_listarCitasAsignadas(ML_idPaciente));
        model.addAttribute("ML_totalCitas", ML_pacientePanelService.ML_contarCitasAsignadas(ML_idPaciente));
        model.addAttribute("ML_citasPendientes", ML_pacientePanelService.ML_contarCitasPendientes(ML_idPaciente));
        model.addAttribute("ML_citasConfirmadas", ML_pacientePanelService.ML_contarCitasConfirmadas(ML_idPaciente));
        model.addAttribute("ML_citasAtendidas", ML_pacientePanelService.ML_contarCitasAtendidas(ML_idPaciente));
        return "paciente/ML_citasPaciente";
    }

    @GetMapping("/paciente/historial")
    public String ML_mostrarHistorialPaciente(Authentication authentication, Model model) {
        Integer ML_idPaciente = ML_obtenerIdPacienteAutenticado(authentication);
        ML_cargarPerfilPaciente(ML_idPaciente, model);
        model.addAttribute("ML_tituloPagina", "Historial Clínico y Seguimiento");
        model.addAttribute("ML_historial", ML_pacientePanelService.ML_listarHistorialClinico(ML_idPaciente));
        model.addAttribute("ML_totalHistorial", ML_pacientePanelService.ML_contarHistorialClinico(ML_idPaciente));
        model.addAttribute("ML_totalSeguimientos", ML_pacientePanelService.ML_contarSeguimientos(ML_idPaciente));
        return "paciente/ML_historialPaciente";
    }

    @GetMapping("/paciente/seguimiento")
    public String ML_redirigirSeguimientoAHistorial() {
        return "redirect:/paciente/historial";
    }

    @GetMapping("/paciente/pagos")
    public String ML_mostrarPagosPaciente(Authentication authentication, Model model) {
        Integer ML_idPaciente = ML_obtenerIdPacienteAutenticado(authentication);
        ML_cargarPerfilPaciente(ML_idPaciente, model);
        model.addAttribute("ML_tituloPagina", "Pagos");
        model.addAttribute("ML_citasParaPago", ML_pacientePanelService.ML_listarCitasParaPagoVirtual(ML_idPaciente));
        model.addAttribute("ML_totalPendiente", ML_pacientePanelService.ML_obtenerTotalPendientePagoFormateado(ML_idPaciente));
        model.addAttribute("ML_pagos", ML_pacientePanelService.ML_listarPagosPaciente(ML_idPaciente));
        model.addAttribute("ML_metodosPago", ML_pacientePanelService.ML_listarMetodosVirtualesPermitidos());
        model.addAttribute("ML_totalPagado", ML_pacientePanelService.ML_obtenerTotalPagadoFormateado(ML_idPaciente));
        return "paciente/ML_pagosPaciente";
    }

    @GetMapping("/paciente/pagos/realizar")
    public String ML_mostrarFormularioPagoSeleccion(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("ML_mensajeError", "El paciente solo puede consultar el estado de sus pagos. La Secretaria confirma los pagos en recepción por efectivo, Yape o Plin.");
        return "redirect:/paciente/pagos";
    }

    @GetMapping("/paciente/pagos/{ML_idCita}/realizar")
    public String ML_mostrarFormularioPagoVirtual(@PathVariable Integer ML_idCita,
                                                  RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("ML_mensajeError", "El paciente no puede registrar ni editar pagos. La Secretaria confirma el pago de la cita seleccionada.");
        return "redirect:/paciente/pagos";
    }

    @PostMapping("/paciente/pagos/guardar")
    public String ML_guardarPagoSeleccionPaciente(@ModelAttribute("ML_pagoForm") ML_PacientePagoVirtualDTO ML_pagoForm,
                                                  RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("ML_mensajeError", "Operación no permitida. Los pagos son registrados y confirmados por Secretaria.");
        return "redirect:/paciente/pagos";
    }

    @PostMapping("/paciente/pagos/{ML_idCita}/guardar")
    public String ML_guardarPagoVirtualPaciente(@PathVariable Integer ML_idCita,
                                                @ModelAttribute("ML_pagoForm") ML_PacientePagoVirtualDTO ML_pagoForm,
                                                RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("ML_mensajeError", "Operación no permitida. Los pagos son registrados y confirmados por Secretaria.");
        return "redirect:/paciente/pagos";
    }

    private void ML_cargarPerfilPaciente(Integer ML_idPaciente, Model model) {
        model.addAttribute("ML_perfilPaciente", ML_pacientePanelService.ML_obtenerPerfilPaciente(ML_idPaciente));
    }

    private Integer ML_obtenerIdPacienteAutenticado(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("No existe una sesión de paciente activa.");
        }
        return ML_pacientePanelService.ML_obtenerIdPacientePorCorreo(authentication.getName());
    }
}
