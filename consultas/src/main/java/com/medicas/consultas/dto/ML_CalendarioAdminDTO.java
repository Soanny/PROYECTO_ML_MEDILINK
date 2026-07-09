/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

/**
 *
 * @author Windows
 */
public class ML_CalendarioAdminDTO {

    private Integer ML_idCita;
    private String ML_fechaCita;
    private String ML_horaInicio;
    private String ML_estado;
    private String ML_estadoClase;
    private String ML_paciente;
    private String ML_doctor;
    private String ML_especialidad;
    private String ML_consultorio;

    private Integer ML_diaIndex;
    private Integer ML_topPx;
    private String ML_colorClase;

    public ML_CalendarioAdminDTO() {
    }

    public ML_CalendarioAdminDTO(Integer ML_idCita, String ML_fechaCita, String ML_horaInicio,
                                 String ML_estado, String ML_estadoClase, String ML_paciente,
                                 String ML_doctor, String ML_especialidad, String ML_consultorio,
                                 Integer ML_diaIndex, Integer ML_topPx, String ML_colorClase) {
        this.ML_idCita = ML_idCita;
        this.ML_fechaCita = ML_fechaCita;
        this.ML_horaInicio = ML_horaInicio;
        this.ML_estado = ML_estado;
        this.ML_estadoClase = ML_estadoClase;
        this.ML_paciente = ML_paciente;
        this.ML_doctor = ML_doctor;
        this.ML_especialidad = ML_especialidad;
        this.ML_consultorio = ML_consultorio;
        this.ML_diaIndex = ML_diaIndex;
        this.ML_topPx = ML_topPx;
        this.ML_colorClase = ML_colorClase;
    }

    public Integer getML_idCita() {
        return ML_idCita;
    }

    public void setML_idCita(Integer ML_idCita) {
        this.ML_idCita = ML_idCita;
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

    public String getML_estadoClase() {
        return ML_estadoClase;
    }

    public void setML_estadoClase(String ML_estadoClase) {
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

    public String getML_especialidad() {
        return ML_especialidad;
    }

    public void setML_especialidad(String ML_especialidad) {
        this.ML_especialidad = ML_especialidad;
    }

    public String getML_consultorio() {
        return ML_consultorio;
    }

    public void setML_consultorio(String ML_consultorio) {
        this.ML_consultorio = ML_consultorio;
    }

    public Integer getML_diaIndex() {
        return ML_diaIndex;
    }

    public void setML_diaIndex(Integer ML_diaIndex) {
        this.ML_diaIndex = ML_diaIndex;
    }

    public Integer getML_topPx() {
        return ML_topPx;
    }

    public void setML_topPx(Integer ML_topPx) {
        this.ML_topPx = ML_topPx;
    }

    public String getML_colorClase() {
        return ML_colorClase;
    }

    public void setML_colorClase(String ML_colorClase) {
        this.ML_colorClase = ML_colorClase;
    }
}