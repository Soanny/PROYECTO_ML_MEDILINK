package com.medicas.consultas.dto;

public class ML_CitaAdminDTO {

    private Integer ML_idCita;
    private Integer ML_idPaciente;
    private Integer ML_idDoctor;

    private String ML_fechaCita;
    private String ML_horaInicio;
    private String ML_paciente;
    private String ML_dniPaciente;
    private String ML_celularPaciente;
    private String ML_doctor;
    private String ML_especialidad;
    private String ML_consultorio;
    private String ML_estado;

    public ML_CitaAdminDTO() {
    }

    public ML_CitaAdminDTO(Integer ML_idCita,
                           Integer ML_idPaciente,
                           Integer ML_idDoctor,
                           String ML_fechaCita,
                           String ML_horaInicio,
                           String ML_paciente,
                           String ML_dniPaciente,
                           String ML_celularPaciente,
                           String ML_doctor,
                           String ML_especialidad,
                           String ML_consultorio,
                           String ML_estado) {
        this.ML_idCita = ML_idCita;
        this.ML_idPaciente = ML_idPaciente;
        this.ML_idDoctor = ML_idDoctor;
        this.ML_fechaCita = ML_fechaCita;
        this.ML_horaInicio = ML_horaInicio;
        this.ML_paciente = ML_paciente;
        this.ML_dniPaciente = ML_dniPaciente;
        this.ML_celularPaciente = ML_celularPaciente;
        this.ML_doctor = ML_doctor;
        this.ML_especialidad = ML_especialidad;
        this.ML_consultorio = ML_consultorio;
        this.ML_estado = ML_estado;
    }

    public Integer getML_idCita() {
        return ML_idCita;
    }

    public void setML_idCita(Integer ML_idCita) {
        this.ML_idCita = ML_idCita;
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

    public String getML_paciente() {
        return ML_paciente;
    }

    public void setML_paciente(String ML_paciente) {
        this.ML_paciente = ML_paciente;
    }

    public String getML_dniPaciente() {
        return ML_dniPaciente;
    }

    public void setML_dniPaciente(String ML_dniPaciente) {
        this.ML_dniPaciente = ML_dniPaciente;
    }

    public String getML_celularPaciente() {
        return ML_celularPaciente;
    }

    public void setML_celularPaciente(String ML_celularPaciente) {
        this.ML_celularPaciente = ML_celularPaciente;
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

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }
}