/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

/**
 *
 * @author Windows
 */
public class ML_CitaDashboardDTO {

    private String ML_paciente;
    private String ML_doctor;
    private String ML_hora;
    private String ML_estado;
    private String ML_estadoClase;

    public ML_CitaDashboardDTO() {
    }

    public ML_CitaDashboardDTO(String ML_paciente, String ML_doctor, String ML_hora, String ML_estado, String ML_estadoClase) {
        this.ML_paciente = ML_paciente;
        this.ML_doctor = ML_doctor;
        this.ML_hora = ML_hora;
        this.ML_estado = ML_estado;
        this.ML_estadoClase = ML_estadoClase;
    }

    public String getML_paciente() {
        return ML_paciente;
    }

    public void setML_paciente(String ML_paciente) {
        this.ML_paciente = ML_paciente;
    }

    public String getML_doctor() {
        return ML_doctor;
    }

    public void setML_doctor(String ML_doctor) {
        this.ML_doctor = ML_doctor;
    }

    public String getML_hora() {
        return ML_hora;
    }

    public void setML_hora(String ML_hora) {
        this.ML_hora = ML_hora;
    }

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }

    public String getML_estadoClase() {
        return ML_estadoClase;
    }

    public void setML_estadoClase(String ML_estadoClase) {
        this.ML_estadoClase = ML_estadoClase;
    }
}
