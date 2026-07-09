/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.controlador;

import com.medicas.consultas.dto.ML_RegistroPacienteDTO;
import com.medicas.consultas.servicio.ML_RegistroPacienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ML_RegistroPacienteController {

    private final ML_RegistroPacienteService ML_registroPacienteService;

    public ML_RegistroPacienteController(ML_RegistroPacienteService ML_registroPacienteService) {
        this.ML_registroPacienteService = ML_registroPacienteService;
    }

    @GetMapping("/registro/paciente")
    public String ML_mostrarRegistroPaciente(Model model) {

        model.addAttribute("ML_paciente", new ML_RegistroPacienteDTO());

        return "autentiacion/ML_registroPaciente";
    }

    @PostMapping("/registro/paciente")
    public String ML_guardarRegistroPaciente(ML_RegistroPacienteDTO ML_paciente, Model model) {

        if (ML_registroPacienteService.ML_existeCorreo(ML_paciente.getML_correo())) {
            model.addAttribute("ML_error", "El correo electrónico ya está registrado.");
            model.addAttribute("ML_paciente", ML_paciente);

            return "autentiacion/ML_registroPaciente";
        }

        if (ML_registroPacienteService.ML_existeDni(ML_paciente.getML_dni())) {
            model.addAttribute("ML_error", "El DNI ya está registrado.");
            model.addAttribute("ML_paciente", ML_paciente);

            return "autentiacion/ML_registroPaciente";
        }

        ML_registroPacienteService.ML_registrarPaciente(ML_paciente);

        return "redirect:/login?registro=ok";
    }
}