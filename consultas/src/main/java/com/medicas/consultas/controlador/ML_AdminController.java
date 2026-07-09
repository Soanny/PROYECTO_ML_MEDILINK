package com.medicas.consultas.controlador;

import com.medicas.consultas.dto.ML_RegistroCitaDTO;
import com.medicas.consultas.dto.ML_RegistroDoctorDTO;
import com.medicas.consultas.dto.ML_RegistroSecretariaDTO;
import com.medicas.consultas.servicio.ML_AdminDashboardService;
import com.medicas.consultas.servicio.ML_CalendarioAdminService;
import com.medicas.consultas.servicio.ML_CitaAdminService;
import com.medicas.consultas.servicio.ML_ConsultorioAdminService;
import com.medicas.consultas.servicio.ML_DoctorAdminService;
import com.medicas.consultas.servicio.ML_HistorialClinicoAdminService;
import com.medicas.consultas.servicio.ML_HorarioAdminService;
import com.medicas.consultas.servicio.ML_PacienteAdminService;
import com.medicas.consultas.servicio.ML_ReporteAdminService;
import com.medicas.consultas.servicio.ML_SecretariaAdminService;
import com.medicas.consultas.servicio.ML_SecretariaPanelService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ML_AdminController {

    private final ML_AdminDashboardService ML_adminDashboardService;
    private final ML_PacienteAdminService ML_pacienteAdminService;
    private final ML_DoctorAdminService ML_doctorAdminService;
    private final ML_SecretariaAdminService ML_secretariaAdminService;
    private final ML_CalendarioAdminService ML_calendarioAdminService;
    private final ML_CitaAdminService ML_citaAdminService;
    private final ML_ReporteAdminService ML_reporteAdminService;
    private final ML_HistorialClinicoAdminService ML_historialClinicoAdminService;
    private final ML_ConsultorioAdminService ML_consultorioAdminService;
    private final ML_HorarioAdminService ML_horarioAdminService;
    private final ML_SecretariaPanelService ML_secretariaPanelService;

    public ML_AdminController(ML_AdminDashboardService ML_adminDashboardService,
                              ML_PacienteAdminService ML_pacienteAdminService,
                              ML_DoctorAdminService ML_doctorAdminService,
                              ML_SecretariaAdminService ML_secretariaAdminService,
                              ML_CalendarioAdminService ML_calendarioAdminService,
                              ML_CitaAdminService ML_citaAdminService,
                              ML_ReporteAdminService ML_reporteAdminService,
                              ML_HistorialClinicoAdminService ML_historialClinicoAdminService,
                              ML_ConsultorioAdminService ML_consultorioAdminService,
                              ML_HorarioAdminService ML_horarioAdminService,
                              ML_SecretariaPanelService ML_secretariaPanelService) {
        this.ML_adminDashboardService = ML_adminDashboardService;
        this.ML_pacienteAdminService = ML_pacienteAdminService;
        this.ML_doctorAdminService = ML_doctorAdminService;
        this.ML_secretariaAdminService = ML_secretariaAdminService;
        this.ML_calendarioAdminService = ML_calendarioAdminService;
        this.ML_citaAdminService = ML_citaAdminService;
        this.ML_reporteAdminService = ML_reporteAdminService;
        this.ML_historialClinicoAdminService = ML_historialClinicoAdminService;
        this.ML_consultorioAdminService = ML_consultorioAdminService;
        this.ML_horarioAdminService = ML_horarioAdminService;
        this.ML_secretariaPanelService = ML_secretariaPanelService;
    }

    @GetMapping("/admin")
    public String ML_redirigirDashboardAdmin() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String ML_mostrarDashboardAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Bienvenido Administrador");
        model.addAttribute("ML_totalUsuarios", ML_adminDashboardService.ML_contarUsuarios());
        model.addAttribute("ML_totalSecretarias", ML_adminDashboardService.ML_contarSecretarias());
        model.addAttribute("ML_totalPacientes", ML_adminDashboardService.ML_contarPacientes());
        model.addAttribute("ML_totalConsultorios", ML_adminDashboardService.ML_contarConsultorios());
        model.addAttribute("ML_totalDoctores", ML_adminDashboardService.ML_contarDoctores());
        model.addAttribute("ML_totalReservas", ML_adminDashboardService.ML_contarReservasRegistradas());
        model.addAttribute("ML_totalCitas", ML_adminDashboardService.ML_contarCitas());
        model.addAttribute("ML_totalPagos", ML_adminDashboardService.ML_obtenerPagosFormateado());
        model.addAttribute("ML_proximasCitas", ML_adminDashboardService.ML_listarProximasCitas());
        model.addAttribute("ML_resumenSemanal", ML_adminDashboardService.ML_obtenerResumenSemanal());
        model.addAttribute("ML_rankingDoctoresHoras", ML_adminDashboardService.ML_obtenerRankingDoctoresHoras());
        return "admin/ML_dashboardAdmin";
    }

    @GetMapping("/admin/ranking-doctores")
    public String ML_mostrarRankingDoctoresAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Ranking de Doctores");
        model.addAttribute("ML_rankingDoctoresHoras", ML_adminDashboardService.ML_obtenerRankingDoctoresHoras());
        return "admin/ML_ARankingDoctores";
    }

    @GetMapping("/admin/usuarios")
    public String ML_mostrarUsuariosAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Usuarios");
        model.addAttribute("ML_totalUsuarios", ML_adminDashboardService.ML_contarUsuarios());
        model.addAttribute("ML_totalPacientes", ML_adminDashboardService.ML_contarPacientes());
        model.addAttribute("ML_totalDoctores", ML_adminDashboardService.ML_contarDoctores());
        model.addAttribute("ML_totalSecretarias", ML_adminDashboardService.ML_contarSecretarias());
        return "admin/ML_AUsuarios";
    }

    @GetMapping("/admin/pacientes")
    public String ML_mostrarPacientesAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Gestión de Pacientes");
        model.addAttribute("ML_pacientes", ML_pacienteAdminService.ML_listarPacientes());
        model.addAttribute("ML_totalPacientes", ML_pacienteAdminService.ML_contarPacientes());
        model.addAttribute("ML_pacientesMasculinos", ML_pacienteAdminService.ML_contarPacientesMasculinos());
        model.addAttribute("ML_pacientesFemeninos", ML_pacienteAdminService.ML_contarPacientesFemeninos());
        return "admin/ML_APacientes";
    }

    @PostMapping("/admin/pacientes/{ML_idUsuario}/estado")
    public String ML_cambiarEstadoPaciente(@PathVariable Integer ML_idUsuario,
                                           @RequestParam("ML_estado") String ML_estado,
                                           RedirectAttributes redirectAttributes) {
        try {
            ML_pacienteAdminService.ML_actualizarEstadoPaciente(ML_idUsuario, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado del paciente actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/pacientes";
    }

    @GetMapping("/admin/doctores")
    public String ML_mostrarDoctoresAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Gestión de Doctores");
        model.addAttribute("ML_doctores", ML_doctorAdminService.ML_listarDoctores());
        model.addAttribute("ML_totalDoctores", ML_doctorAdminService.ML_contarDoctores());
        model.addAttribute("ML_doctoresActivos", ML_doctorAdminService.ML_contarDoctoresActivos());
        model.addAttribute("ML_totalEspecialidades", ML_doctorAdminService.ML_contarEspecialidades());
        model.addAttribute("ML_consultoriosDisponibles", ML_doctorAdminService.ML_contarConsultoriosDisponibles());
        return "admin/ML_ADoctores";
    }

    @GetMapping("/admin/doctores/nuevo")
    public String ML_mostrarFormularioNuevoDoctor(Model model) {
        model.addAttribute("ML_tituloPagina", "Crear Usuario Doctor");
        if (!model.containsAttribute("ML_doctorNuevo")) {
            model.addAttribute("ML_doctorNuevo", new ML_RegistroDoctorDTO());
        }
        model.addAttribute("ML_especialidades", ML_doctorAdminService.ML_listarEspecialidadesActivas());
        model.addAttribute("ML_consultorios", ML_doctorAdminService.ML_listarConsultoriosDisponibles());
        return "admin/ML_ARegistrarDoctor";
    }

    @PostMapping("/admin/doctores/guardar")
    public String ML_guardarDoctorAdmin(@ModelAttribute("ML_doctorNuevo") ML_RegistroDoctorDTO ML_doctorNuevo,
                                        RedirectAttributes redirectAttributes) {
        try {
            ML_doctorAdminService.ML_registrarDoctor(ML_doctorNuevo);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Doctor creado correctamente.");
            return "redirect:/admin/doctores";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_errorRegistro", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_doctorNuevo", ML_doctorNuevo);
            return "redirect:/admin/doctores/nuevo";
        }
    }

    @PostMapping("/admin/doctores/{ML_idUsuario}/estado")
    public String ML_cambiarEstadoDoctor(@PathVariable Integer ML_idUsuario,
                                         @RequestParam("ML_estado") String ML_estado,
                                         RedirectAttributes redirectAttributes) {
        try {
            ML_doctorAdminService.ML_actualizarEstadoDoctor(ML_idUsuario, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado del doctor actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/doctores";
    }

    @GetMapping("/admin/secretarias")
    public String ML_mostrarSecretariasAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Gestión de Secretarias");
        model.addAttribute("ML_secretarias", ML_secretariaAdminService.ML_listarSecretarias());
        model.addAttribute("ML_totalSecretarias", ML_secretariaAdminService.ML_contarSecretarias());
        model.addAttribute("ML_secretariasActivas", ML_secretariaAdminService.ML_contarSecretariasActivas());
        model.addAttribute("ML_turnoManana", ML_secretariaAdminService.ML_contarTurnoManana());
        model.addAttribute("ML_turnoTarde", ML_secretariaAdminService.ML_contarTurnoTarde());
        return "admin/ML_ASecretarias";
    }

    @GetMapping("/admin/secretarias/nuevo")
    public String ML_mostrarFormularioNuevaSecretaria(Model model) {
        model.addAttribute("ML_tituloPagina", "Crear Usuario Secretaria");
        if (!model.containsAttribute("ML_secretariaNueva")) {
            model.addAttribute("ML_secretariaNueva", new ML_RegistroSecretariaDTO());
        }
        return "admin/ML_ARegistrarSecretaria";
    }

    @PostMapping("/admin/secretarias/guardar")
    public String ML_guardarSecretariaAdmin(@ModelAttribute("ML_secretariaNueva") ML_RegistroSecretariaDTO ML_secretariaNueva,
                                            RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaAdminService.ML_registrarSecretaria(ML_secretariaNueva);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Secretaria creada correctamente.");
            return "redirect:/admin/secretarias";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_errorRegistro", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_secretariaNueva", ML_secretariaNueva);
            return "redirect:/admin/secretarias/nuevo";
        }
    }

    @PostMapping("/admin/secretarias/{ML_idUsuario}/estado")
    public String ML_cambiarEstadoSecretaria(@PathVariable Integer ML_idUsuario,
                                             @RequestParam("ML_estado") String ML_estado,
                                             RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaAdminService.ML_actualizarEstadoSecretaria(ML_idUsuario, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado de la secretaria actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/secretarias";
    }

    @GetMapping("/admin/citas")
    public String ML_mostrarCitasAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Gestión de Citas");
        model.addAttribute("ML_citas", ML_citaAdminService.ML_listarCitas());
        model.addAttribute("ML_totalCitas", ML_citaAdminService.ML_contarCitas());
        model.addAttribute("ML_citasPendientes", ML_citaAdminService.ML_contarCitasPendientes());
        model.addAttribute("ML_citasConfirmadas", ML_citaAdminService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasCanceladas", ML_citaAdminService.ML_contarCitasCanceladas());
        model.addAttribute("ML_citasAtendidas", ML_citaAdminService.ML_contarCitasAtendidas());
        return "admin/ML_ACitas";
    }

    @GetMapping("/admin/citas/nueva")
    public String ML_mostrarFormularioNuevaCita(Model model) {
        model.addAttribute("ML_tituloPagina", "Nueva Cita");
        if (!model.containsAttribute("ML_citaNueva")) {
            model.addAttribute("ML_citaNueva", new ML_RegistroCitaDTO());
        }
        model.addAttribute("ML_pacientes", ML_citaAdminService.ML_listarPacientesParaCita());
        model.addAttribute("ML_doctores", ML_citaAdminService.ML_listarDoctoresParaCita());
        return "admin/ML_ARegistrarCita";
    }

    @PostMapping("/admin/citas/guardar")
    public String ML_guardarCitaAdmin(@ModelAttribute("ML_citaNueva") ML_RegistroCitaDTO ML_citaNueva,
                                      RedirectAttributes redirectAttributes) {
        try {
            ML_citaAdminService.ML_registrarCita(ML_citaNueva);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Cita registrada correctamente.");
            return "redirect:/admin/citas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_errorRegistro", e.getMessage());
            redirectAttributes.addFlashAttribute("ML_citaNueva", ML_citaNueva);
            return "redirect:/admin/citas/nueva";
        }
    }

    @PostMapping("/admin/citas/{ML_idCita}/estado")
    public String ML_actualizarEstadoCita(@PathVariable Integer ML_idCita,
                                          @RequestParam("ML_estado") String ML_estado,
                                          RedirectAttributes redirectAttributes) {
        try {
            ML_citaAdminService.ML_actualizarEstadoCita(ML_idCita, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado de la cita actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/citas";
    }

    @GetMapping("/admin/horarios")
    public String ML_mostrarCalendarioAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Calendario");
        model.addAttribute("ML_citasCalendario", ML_calendarioAdminService.ML_listarCitasCalendario());
        model.addAttribute("ML_totalCitas", ML_calendarioAdminService.ML_contarCitas());
        model.addAttribute("ML_citasHoy", ML_calendarioAdminService.ML_contarCitasHoy());
        model.addAttribute("ML_citasConfirmadas", ML_calendarioAdminService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasPendientes", ML_calendarioAdminService.ML_contarCitasPendientes());
        return "admin/ML_ACalendario";
    }

    @GetMapping("/admin/horarios-doctores")
    public String ML_mostrarHorariosDoctoresAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Horarios de Doctores");
        model.addAttribute("ML_horarios", ML_horarioAdminService.ML_listarHorarios());
        model.addAttribute("ML_horariosPorDoctor", ML_horarioAdminService.ML_listarResumenHorariosPorDoctor());
        model.addAttribute("ML_horariosPorDia", ML_horarioAdminService.ML_listarResumenHorariosPorDia());
        model.addAttribute("ML_horariosPorDiaMapa", ML_horarioAdminService.ML_agruparHorariosPorDia());
        model.addAttribute("ML_diasSemana", ML_horarioAdminService.ML_listarDiasSemana());
        model.addAttribute("ML_estadosHorario", ML_horarioAdminService.ML_listarEstadosHorario());
        model.addAttribute("ML_totalHorarios", ML_horarioAdminService.ML_contarHorarios());
        model.addAttribute("ML_horariosActivos", ML_horarioAdminService.ML_contarHorariosActivos());
        model.addAttribute("ML_horariosInactivos", ML_horarioAdminService.ML_contarHorariosInactivos());
        return "admin/ML_AHorariosDoctores";
    }

    @GetMapping("/admin/disponibilidad")
    public String ML_mostrarDisponibilidadAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Disponibilidad Médica");
        model.addAttribute("ML_horarios", ML_horarioAdminService.ML_listarHorarios());
        model.addAttribute("ML_doctores", ML_horarioAdminService.ML_listarDoctoresActivos());
        model.addAttribute("ML_diasSemana", ML_horarioAdminService.ML_listarDiasSemana());
        model.addAttribute("ML_horariosPorDiaMapa", ML_horarioAdminService.ML_agruparHorariosPorDia());
        model.addAttribute("ML_estadosHorario", ML_horarioAdminService.ML_listarEstadosHorario());
        model.addAttribute("ML_totalHorarios", ML_horarioAdminService.ML_contarHorarios());
        model.addAttribute("ML_horariosActivos", ML_horarioAdminService.ML_contarHorariosActivos());
        model.addAttribute("ML_horariosInactivos", ML_horarioAdminService.ML_contarHorariosInactivos());
        return "admin/ML_disponibilidad";
    }

    @PostMapping("/admin/disponibilidad/guardar")
    public String ML_guardarDisponibilidadAdmin(@RequestParam("ML_idDoctor") Integer ML_idDoctor,
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
        return "redirect:/admin/disponibilidad";
    }

    @PostMapping("/admin/disponibilidad/{ML_idHorario}/estado")
    public String ML_cambiarEstadoDisponibilidad(@PathVariable Integer ML_idHorario,
                                                 @RequestParam("ML_estado") String ML_estado,
                                                 RedirectAttributes redirectAttributes) {
        try {
            ML_horarioAdminService.ML_actualizarEstadoHorario(ML_idHorario, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado del horario actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/disponibilidad";
    }

    @GetMapping("/admin/historial")
    public String ML_mostrarHistorialClinico(Model model) {
        List<Map<String, Object>> ML_pacientesHistorial = ML_historialClinicoAdminService.ML_listarPacientesHistorial();
        model.addAttribute("ML_tituloPagina", "Historial Clínico");
        model.addAttribute("ML_pacientesHistorial", ML_pacientesHistorial);
        return "admin/ML_historial";
    }

    @GetMapping("/admin/historial/{ML_idPaciente}")
    public String ML_mostrarDetalleHistorialClinico(@PathVariable Integer ML_idPaciente, Model model) {
        model.addAttribute("ML_tituloPagina", "Historial Clínico");
        model.addAttribute("ML_paciente", ML_historialClinicoAdminService.ML_obtenerPacienteHistorial(ML_idPaciente));
        model.addAttribute("ML_resumenClinico", ML_historialClinicoAdminService.ML_obtenerResumenClinico(ML_idPaciente));
        model.addAttribute("ML_proximaCita", ML_historialClinicoAdminService.ML_obtenerProximaCita(ML_idPaciente));
        model.addAttribute("ML_notasClinicas", ML_historialClinicoAdminService.ML_obtenerNotasClinicasRecientes(ML_idPaciente));
        model.addAttribute("ML_consultasPaciente", ML_historialClinicoAdminService.ML_obtenerConsultasPaciente(ML_idPaciente));
        model.addAttribute("ML_diagnosticosActivos", ML_historialClinicoAdminService.ML_obtenerDiagnosticosActivos(ML_idPaciente));
        model.addAttribute("ML_tratamientosPaciente", ML_historialClinicoAdminService.ML_obtenerTratamientosPaciente(ML_idPaciente));
        model.addAttribute("ML_examenesPaciente", ML_historialClinicoAdminService.ML_obtenerExamenesPaciente(ML_idPaciente));
        return "admin/ML_historialDetalle";
    }

    @GetMapping("/admin/reportes")
    public String ML_mostrarReportesAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Reportes");
        model.addAttribute("ML_totalCitas", ML_reporteAdminService.ML_contarTotalCitas());
        model.addAttribute("ML_citasConfirmadas", ML_reporteAdminService.ML_contarCitasConfirmadas());
        model.addAttribute("ML_citasPendientes", ML_reporteAdminService.ML_contarCitasPendientes());
        model.addAttribute("ML_citasCanceladas", ML_reporteAdminService.ML_contarCitasCanceladas());
        model.addAttribute("ML_citasAtendidas", ML_reporteAdminService.ML_contarCitasAtendidas());
        model.addAttribute("ML_ingresosTotales", ML_reporteAdminService.ML_obtenerIngresosTotales());
        model.addAttribute("ML_citasPorEstado", ML_reporteAdminService.ML_obtenerCitasPorEstado());
        model.addAttribute("ML_citasPorDiaSemana", ML_reporteAdminService.ML_obtenerCitasPorDiaSemana());
        model.addAttribute("ML_rendimientoEspecialidades", ML_reporteAdminService.ML_obtenerRendimientoPorEspecialidad());
        model.addAttribute("ML_rendimientoDoctores", ML_reporteAdminService.ML_obtenerRendimientoPorDoctor());
        model.addAttribute("ML_doctoresMasAtenciones", ML_reporteAdminService.ML_obtenerDoctoresMasAtenciones());
        model.addAttribute("ML_metodosPago", ML_reporteAdminService.ML_obtenerMetodosPago());
        return "admin/ML_AReportes";
    }

    @GetMapping("/admin/reportes/exportar/pdf")
    public void ML_exportarReportesAdminPDF(HttpServletResponse ML_response) {
        try {
            ML_reporteAdminService.ML_exportarReportesPDF(ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    @GetMapping("/admin/reportes/exportar/excel")
    public void ML_exportarReportesAdminExcel(HttpServletResponse ML_response) {
        try {
            ML_reporteAdminService.ML_exportarReportesExcel(ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    @GetMapping("/admin/pagos")
    public String ML_mostrarPagosAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Pagos por Consulta");
        model.addAttribute("ML_pagos", ML_secretariaPanelService.ML_listarPagos());
        model.addAttribute("ML_reportePagosDiarios", ML_secretariaPanelService.ML_obtenerReportePagosDiarios());
        model.addAttribute("ML_montoPagosHoy", ML_secretariaPanelService.ML_obtenerMontoPagosHoyFormateado());
        model.addAttribute("ML_pagosHoy", ML_secretariaPanelService.ML_contarPagosHoy());
        model.addAttribute("ML_pagosAnulados", ML_secretariaPanelService.ML_contarPagosAnulados());
        return "admin/ML_pagos";
    }

    @PostMapping("/admin/pagos/{ML_idPago}/anular")
    public String ML_anularPagoAdmin(@PathVariable Integer ML_idPago,
                                     @RequestParam(value = "ML_observacion", required = false) String ML_observacion,
                                     RedirectAttributes redirectAttributes) {
        try {
            ML_secretariaPanelService.ML_anularPagoAdmin(ML_idPago, ML_observacion);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Pago anulado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/pagos";
    }

    @GetMapping("/admin/pagos/{ML_idPago}/comprobante")
    public void ML_descargarComprobanteAdmin(@PathVariable Integer ML_idPago, HttpServletResponse ML_response) {
        try {
            ML_secretariaPanelService.ML_exportarComprobantePagoPDF(ML_idPago, ML_response);
        } catch (Exception e) {
            ML_response.setStatus(500);
        }
    }

    @GetMapping("/admin/especialidades")
    public String ML_mostrarEspecialidadesAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Especialidades Medicas");
        model.addAttribute("ML_especialidades", ML_doctorAdminService.ML_listarEspecialidadesActivas());
        return "admin/ML_especialidades";
    }

    @PostMapping("/admin/especialidades/guardar")
    public String ML_guardarEspecialidad(@RequestParam("ML_nombre") String ML_nombre,
                                          RedirectAttributes redirectAttributes) {
        try {
            ML_doctorAdminService.ML_registrarEspecialidad(ML_nombre);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Especialidad creada correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/especialidades";
    }

    @GetMapping("/admin/consultorios")
    public String ML_mostrarConsultoriosAdmin(Model model) {
        model.addAttribute("ML_tituloPagina", "Consultorios");
        model.addAttribute("ML_consultorios", ML_consultorioAdminService.ML_listarConsultorios());
        model.addAttribute("ML_estadosConsultorio", ML_consultorioAdminService.ML_listarEstadosConsultorio());
        model.addAttribute("ML_totalConsultorios", ML_consultorioAdminService.ML_contarConsultorios());
        model.addAttribute("ML_consultoriosDisponibles", ML_consultorioAdminService.ML_contarDisponibles());
        model.addAttribute("ML_consultoriosOcupados", ML_consultorioAdminService.ML_contarOcupados());
        model.addAttribute("ML_consultoriosMantenimiento", ML_consultorioAdminService.ML_contarMantenimiento());
        return "admin/ML_consultorios";
    }

    @PostMapping("/admin/consultorios/guardar")
    public String ML_guardarConsultorioAdmin(@RequestParam("ML_nombre") String ML_nombre,
                                             @RequestParam(value = "ML_ubicacion", required = false) String ML_ubicacion,
                                             @RequestParam(value = "ML_piso", required = false) Integer ML_piso,
                                             @RequestParam(value = "ML_estado", defaultValue = "Disponible") String ML_estado,
                                             RedirectAttributes redirectAttributes) {
        try {
            ML_consultorioAdminService.ML_guardarConsultorio(ML_nombre, ML_ubicacion, ML_piso, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Consultorio registrado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/consultorios";
    }

    @PostMapping("/admin/consultorios/{ML_idConsultorio}/estado")
    public String ML_cambiarEstadoConsultorio(@PathVariable Integer ML_idConsultorio,
                                              @RequestParam("ML_estado") String ML_estado,
                                              RedirectAttributes redirectAttributes) {
        try {
            ML_consultorioAdminService.ML_actualizarEstadoConsultorio(ML_idConsultorio, ML_estado);
            redirectAttributes.addFlashAttribute("ML_mensajeExito", "Estado del consultorio actualizado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("ML_mensajeError", e.getMessage());
        }
        return "redirect:/admin/consultorios";
    }
}
