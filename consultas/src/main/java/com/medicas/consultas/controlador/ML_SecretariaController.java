package com.medicas.consultas.controlador;

import com.medicas.consultas.dto.ML_SecretariaCitaFormDTO;
import com.medicas.consultas.dto.ML_SecretariaPagoDTO;
import com.medicas.consultas.servicio.ML_SecretariaPanelService;
import com.medicas.consultas.servicio.ML_HorarioAdminService;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ML_SecretariaController {

    private final ML_SecretariaPanelService ML_secretariaPanelService;
    private final ML_HorarioAdminService ML_horarioAdminService;

    public ML_SecretariaController(ML_SecretariaPanelService ML_secretariaPanelService,
                                   ML_HorarioAdminService ML_horarioAdminService) {
        this.ML_secretariaPanelService = ML_secretariaPanelService;
        this.ML_horarioAdminService = ML_horarioAdminService;
    }

    @GetMapping("/secretaria")
    public String ML_redirigirDashboardSecretaria() {
        return "redirect:/secretaria/dashboard";
    }

    @GetMapping("/secretaria/dashboard")
    public String ML_mostrarDashboardSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Panel de Secretaria");
        model.addAttribute("ML_totalCitas", ML_secretariaPanelService.ML_contarTotalCitas());
        model.addAttribute("ML_citasHoy", ML_secretariaPanelService.ML_contarCitasHoy());
        model.addAttribute("ML_citasPendientes", ML_secretariaPanelService.ML_contarCitasPendientes());
        model.addAttribute("ML_citasConfirmadas", ML_secretariaPanelService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_ingresosPagados", ML_secretariaPanelService.ML_obtenerIngresosPagadosFormateado());
        model.addAttribute("ML_proximasCitas", ML_secretariaPanelService.ML_listarProximasCitas());
        return "secretaria/ML_dashboardSecretaria";
    }

    @GetMapping("/secretaria/citas/registro-presencial")
    public String ML_mostrarRegistroPresencial(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Registro Presencial de Cita");

        if (!model.containsAttribute("ML_citaForm")) {
            model.addAttribute("ML_citaForm", ML_secretariaPanelService.ML_prepararNuevaCita());
        }

        ML_cargarOpcionesCita(model);
        return "secretaria/ML_registroPresencialCita";
    }

    @PostMapping("/secretaria/citas/registro-presencial/guardar")
    public String ML_guardarRegistroPresencial(@ModelAttribute("ML_citaForm") ML_SecretariaCitaFormDTO ML_citaForm,
                                               RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaPanelService.ML_registrarCitaPresencial(ML_citaForm);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Cita presencial registrada correctamente.");
            return "redirect:/secretaria/citas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_citaForm", ML_citaForm);
            return "redirect:/secretaria/citas/registro-presencial";
        }
    }

    @GetMapping("/secretaria/citas")
    public String ML_mostrarGestionCitas(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Gestionar Citas");
        model.addAttribute("ML_citas", ML_secretariaPanelService.ML_listarCitas());
        model.addAttribute("ML_totalCitas", ML_secretariaPanelService.ML_contarTotalCitas());
        model.addAttribute("ML_citasPendientes", ML_secretariaPanelService.ML_contarCitasPendientes());
        model.addAttribute("ML_citasConfirmadas", ML_secretariaPanelService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasAtendidas", ML_secretariaPanelService.ML_contarCitasAtendidas());
        model.addAttribute("ML_citasCanceladas", ML_secretariaPanelService.ML_contarCitasCanceladas());
        return "secretaria/ML_gestionCitasSecretaria";
    }

    @GetMapping("/secretaria/citas/{ML_idCita}/editar")
    public String ML_mostrarEditarCita(@PathVariable Integer ML_idCita,
                                       Authentication authentication,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        try {
            ML_cargarPerfilSecretaria(authentication, model);
            Map<String, Object> ML_cita = ML_secretariaPanelService.ML_obtenerCitaDetalle(ML_idCita);

            if (ML_cita.isEmpty()) {
                throw new IllegalArgumentException("La cita seleccionada no existe.");
            }

            Object ML_estadoActual = ML_cita.get("estado");
            if (ML_estadoActual != null && "Atendida".equalsIgnoreCase(ML_estadoActual.toString())) {
                throw new IllegalArgumentException("La cita ya fue atendida y no puede modificarse.");
            }

            if (!model.containsAttribute("ML_citaForm")) {
                model.addAttribute("ML_citaForm", ML_secretariaPanelService.ML_obtenerFormularioCita(ML_idCita));
            }

            ML_cargarOpcionesCita(model);
            model.addAttribute("ML_tituloPagina", "Editar Cita");
            model.addAttribute("ML_cita", ML_cita);
            return "secretaria/ML_editarCitaSecretaria";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/secretaria/citas";
        }
    }

    @PostMapping("/secretaria/citas/{ML_idCita}/actualizar")
    public String ML_actualizarCitaSecretaria(@PathVariable Integer ML_idCita,
                                              @ModelAttribute("ML_citaForm") ML_SecretariaCitaFormDTO ML_citaForm,
                                              RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaPanelService.ML_actualizarDatosCita(ML_idCita, ML_citaForm);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Datos de la cita actualizados correctamente. No se modificaron datos personales del paciente.");
            return "redirect:/secretaria/citas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_citaForm", ML_citaForm);
            return "redirect:/secretaria/citas/" + ML_idCita + "/editar";
        }
    }

    @PostMapping("/secretaria/citas/{ML_idCita}/estado")
    public String ML_actualizarEstadoCita(@PathVariable Integer ML_idCita,
                                          @RequestParam("ML_estado") String ML_estado,
                                          RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaPanelService.ML_actualizarEstadoCita(ML_idCita, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado de la cita actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/secretaria/citas";
    }

    @GetMapping("/secretaria/calendario")
    public String ML_mostrarCalendarioSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Calendario de Citas");
        model.addAttribute("ML_citasCalendario", ML_secretariaPanelService.ML_listarCitasCalendario());
        model.addAttribute("ML_totalCitas", ML_secretariaPanelService.ML_contarTotalCitas());
        model.addAttribute("ML_citasHoy", ML_secretariaPanelService.ML_contarCitasHoy());
        model.addAttribute("ML_citasConfirmadas", ML_secretariaPanelService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasPendientes", ML_secretariaPanelService.ML_contarCitasPendientes());
        model.addAttribute("ML_calendarioPorDia", ML_secretariaPanelService.ML_obtenerCalendarioPorDiaSemana());
        return "secretaria/ML_calendarioSecretaria";
    }


    @GetMapping("/secretaria/disponibilidad")
    public String ML_mostrarDisponibilidadSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Disponibilidad Médica");
        model.addAttribute("ML_horarios", ML_horarioAdminService.ML_listarHorarios());
        model.addAttribute("ML_doctores", ML_horarioAdminService.ML_listarDoctoresActivos());
        model.addAttribute("ML_diasSemana", ML_horarioAdminService.ML_listarDiasSemana());
        model.addAttribute("ML_horariosPorDiaMapa", ML_horarioAdminService.ML_agruparHorariosPorDia());
        model.addAttribute("ML_estadosHorario", ML_horarioAdminService.ML_listarEstadosHorario());
        model.addAttribute("ML_totalHorarios", ML_horarioAdminService.ML_contarHorarios());
        model.addAttribute("ML_horariosActivos", ML_horarioAdminService.ML_contarHorariosActivos());
        model.addAttribute("ML_horariosInactivos", ML_horarioAdminService.ML_contarHorariosInactivos());
        return "secretaria/ML_disponibilidadSecretaria";
    }

    @PostMapping("/secretaria/disponibilidad/guardar")
    public String ML_guardarDisponibilidadSecretaria(@RequestParam("ML_idDoctor") Integer ML_idDoctor,
                                                     @RequestParam("ML_diaSemana") String ML_diaSemana,
                                                     @RequestParam("ML_horaInicio") String ML_horaInicio,
                                                     @RequestParam("ML_horaFin") String ML_horaFin,
                                                     @RequestParam(value = "ML_estado", defaultValue = "Activo") String ML_estado,
                                                     RedirectAttributes redirectAttributes) {
        try {
            ML_horarioAdminService.ML_guardarHorario(ML_idDoctor, ML_diaSemana, ML_horaInicio, ML_horaFin, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Horario médico registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/secretaria/disponibilidad";
    }

    @PostMapping("/secretaria/disponibilidad/{ML_idHorario}/estado")
    public String ML_cambiarEstadoDisponibilidadSecretaria(@PathVariable Integer ML_idHorario,
                                                           @RequestParam("ML_estado") String ML_estado,
                                                           RedirectAttributes redirectAttributes) {
        try {
            ML_horarioAdminService.ML_actualizarEstadoHorario(ML_idHorario, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado del horario actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/secretaria/disponibilidad";
    }

    @GetMapping("/secretaria/pagos")
    public String ML_mostrarPagosSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Pagos");
        model.addAttribute("ML_pagos", ML_secretariaPanelService.ML_listarPagos());
        model.addAttribute("ML_citasParaPago", ML_secretariaPanelService.ML_listarCitasParaPago());
        model.addAttribute("ML_metodosPago", ML_secretariaPanelService.ML_listarMetodosPagoPermitidos());
        model.addAttribute("ML_estadosPago", ML_secretariaPanelService.ML_listarEstadosPagoPermitidos());
        model.addAttribute("ML_tiposComprobante", ML_secretariaPanelService.ML_listarTiposComprobantePermitidos());

        if (!model.containsAttribute("ML_pagoForm")) {
            model.addAttribute("ML_pagoForm", new ML_SecretariaPagoDTO());
        }

        return "secretaria/ML_pagosSecretaria";
    }

    @GetMapping("/secretaria/pagos/realizar")
    public String ML_mostrarRealizarPagoSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Realizar pago");
        model.addAttribute("ML_citasParaPago", ML_secretariaPanelService.ML_listarCitasParaPago());
        model.addAttribute("ML_metodosPago", ML_secretariaPanelService.ML_listarMetodosPagoPermitidos());
        model.addAttribute("ML_estadosPago", ML_secretariaPanelService.ML_listarEstadosPagoPermitidos());
        model.addAttribute("ML_tiposComprobante", ML_secretariaPanelService.ML_listarTiposComprobantePermitidos());

        if (!model.containsAttribute("ML_pagoForm")) {
            model.addAttribute("ML_pagoForm", new ML_SecretariaPagoDTO());
        }

        return "secretaria/ML_realizarPagoSecretaria";
    }

    @PostMapping("/secretaria/pagos/guardar")
    public String ML_guardarPagoDesdeModulo(@ModelAttribute("ML_pagoForm") ML_SecretariaPagoDTO ML_pagoForm,
                                            RedirectAttributes redirectAttributes) {
        try {
            if (ML_pagoForm.getML_idCita() == null) {
                throw new IllegalArgumentException("Selecciona una cita para registrar el pago.");
            }

            ML_secretariaPanelService.ML_registrarPagoCita(ML_pagoForm.getML_idCita(), ML_pagoForm);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Pago registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_pagoForm", ML_pagoForm);
        }

        return "redirect:/secretaria/pagos/realizar";
    }

    @GetMapping("/secretaria/pagos/{ML_idCita}/registrar")
    public String ML_mostrarFormularioPago(@PathVariable Integer ML_idCita,
                                           Authentication authentication,
                                           Model model,
                                           RedirectAttributes redirectAttributes) {
        try {
            ML_cargarPerfilSecretaria(authentication, model);
            Map<String, Object> ML_cita = ML_secretariaPanelService.ML_obtenerCitaDetalle(ML_idCita);

            if (ML_cita.isEmpty()) {
                throw new IllegalArgumentException("La cita seleccionada no existe.");
            }

            if (!model.containsAttribute("ML_pagoForm")) {
                model.addAttribute("ML_pagoForm", ML_secretariaPanelService.ML_prepararPagoCita(ML_idCita));
            }

            model.addAttribute("ML_tituloPagina", "Registrar Pago");
            model.addAttribute("ML_cita", ML_cita);
            model.addAttribute("ML_metodosPago", ML_secretariaPanelService.ML_listarMetodosPagoPermitidos());
            model.addAttribute("ML_estadosPago", ML_secretariaPanelService.ML_listarEstadosPagoPermitidos());
            model.addAttribute("ML_tiposComprobante", ML_secretariaPanelService.ML_listarTiposComprobantePermitidos());
            return "secretaria/ML_registrarPagoSecretaria";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            return "redirect:/secretaria/pagos";
        }
    }

    @PostMapping("/secretaria/pagos/{ML_idCita}/guardar")
    public String ML_guardarPagoCita(@PathVariable Integer ML_idCita,
                                     @ModelAttribute("ML_pagoForm") ML_SecretariaPagoDTO ML_pagoForm,
                                     RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaPanelService.ML_registrarPagoCita(ML_idCita, ML_pagoForm);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Pago registrado correctamente.");
            return "redirect:/secretaria/pagos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_pagoForm", ML_pagoForm);
            return "redirect:/secretaria/pagos/" + ML_idCita + "/registrar";
        }
    }

    @GetMapping("/secretaria/pagos/{ML_idPago}/comprobante")
    public void ML_descargarComprobantePago(@PathVariable Integer ML_idPago, HttpServletResponse ML_response) {
        try {
            ML_secretariaPanelService.ML_exportarComprobantePagoPDF(ML_idPago, ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    @GetMapping("/secretaria/reportes")
    public String ML_mostrarReportesSecretaria(Authentication authentication, Model model) {
        ML_cargarPerfilSecretaria(authentication, model);
        model.addAttribute("ML_tituloPagina", "Reportes de Citas y Pagos");
        model.addAttribute("ML_totalCitas", ML_secretariaPanelService.ML_contarTotalCitas());
        model.addAttribute("ML_citasPendientes", ML_secretariaPanelService.ML_contarCitasPendientes());
        model.addAttribute("ML_citasConfirmadas", ML_secretariaPanelService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasAtendidas", ML_secretariaPanelService.ML_contarCitasAtendidas());
        model.addAttribute("ML_citasCanceladas", ML_secretariaPanelService.ML_contarCitasCanceladas());
        model.addAttribute("ML_ingresosPagados", ML_secretariaPanelService.ML_obtenerIngresosPagadosFormateado());
        model.addAttribute("ML_citasPorEstado", ML_secretariaPanelService.ML_obtenerCitasPorEstado());
        model.addAttribute("ML_citasPorDoctor", ML_secretariaPanelService.ML_obtenerCitasPorDoctor());
        model.addAttribute("ML_pagosPorMetodo", ML_secretariaPanelService.ML_obtenerPagosPorMetodo());
        model.addAttribute("ML_totalPacientesActivos", ML_secretariaPanelService.ML_contarPacientesActivos());
        model.addAttribute("ML_totalDoctoresActivos", ML_secretariaPanelService.ML_contarDoctoresActivos());
        model.addAttribute("ML_pagosPendientes", ML_secretariaPanelService.ML_contarPagosPendientes());
        model.addAttribute("ML_pagosPagados", ML_secretariaPanelService.ML_contarPagosPagados());
        model.addAttribute("ML_calendarioPorDia", ML_secretariaPanelService.ML_obtenerCalendarioPorDiaSemana());
        return "secretaria/ML_reportesSecretaria";
    }

    @GetMapping("/secretaria/reportes/exportar/pdf")
    public void ML_exportarReportesSecretariaPDF(HttpServletResponse ML_response) {
        try {
            ML_secretariaPanelService.ML_exportarReportesPDF(ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    @GetMapping("/secretaria/reportes/exportar/excel")
    public void ML_exportarReportesSecretariaExcel(HttpServletResponse ML_response) {
        try {
            ML_secretariaPanelService.ML_exportarReportesExcel(ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    private void ML_cargarPerfilSecretaria(Authentication authentication, Model model) {
        String ML_correo = authentication != null ? authentication.getName() : "";
        model.addAttribute("ML_perfilSecretaria", ML_secretariaPanelService.ML_obtenerPerfilSecretaria(ML_correo));
    }

    private void ML_cargarOpcionesCita(Model model) {
        model.addAttribute("ML_pacientes", ML_secretariaPanelService.ML_listarPacientesActivos());
        model.addAttribute("ML_pacientesTabla", ML_secretariaPanelService.ML_listarPacientesActivosTabla());
        model.addAttribute("ML_doctores", ML_secretariaPanelService.ML_listarDoctoresActivos());
    }
}
