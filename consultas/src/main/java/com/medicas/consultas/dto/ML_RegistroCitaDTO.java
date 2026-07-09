/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

public class ML_RegistroCitaDTO {

    private Integer ML_idPaciente;
    private Integer ML_idDoctor;
    private String ML_fechaCita;
    private String ML_horaInicio;
    private String ML_estado;

    public ML_RegistroCitaDTO() {
        this.ML_estado = "Pendiente";
    }

    public Integer getML_idPaciente() {
        return ML_idPaciente;
    }

    public void setML_idPaciente(Integer ML_idPaciente) {
        this.ML_idPaciente = ML_idPaciente;
    }

    public Integer getML_idDoctor() {
        return ML_idDoctor;
    }

    public void setML_idDoctor(Integer ML_idDoctor) {
        this.ML_idDoctor = ML_idDoctor;
    }

    public String getML_fechaCita() {
        return ML_fechaCita;
    }

    public void setML_fechaCita(String ML_fechaCita) {
        this.ML_fechaCita = ML_fechaCita;
    }

    public String getML_horaInicio() {
        return ML_horaInicio;
    }

    public void setML_horaInicio(String ML_horaInicio) {
        this.ML_horaInicio = ML_horaInicio;
    }

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }
}