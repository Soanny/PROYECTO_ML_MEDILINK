package com.medicas.consultas.dto;

public class ML_RegistroHistorialDoctorDTO {

    private Integer ML_idHistorial;
    private Integer ML_idCita;
    private Integer ML_idPaciente;
    private Integer ML_idDoctor;
    private String ML_diagnostico;
    private String ML_tratamiento;
    private String ML_receta;
    private String ML_observaciones;

    public ML_RegistroHistorialDoctorDTO() {
    }

    public ML_RegistroHistorialDoctorDTO(Integer ML_idHistorial,
                                         Integer ML_idCita,
                                         Integer ML_idPaciente,
                                         Integer ML_idDoctor,
                                         String ML_diagnostico,
                                         String ML_tratamiento,
                                         String ML_receta,
                                         String ML_observaciones) {
        this.ML_idHistorial = ML_idHistorial;
        this.ML_idCita = ML_idCita;
        this.ML_idPaciente = ML_idPaciente;
        this.ML_idDoctor = ML_idDoctor;
        this.ML_diagnostico = ML_diagnostico;
        this.ML_tratamiento = ML_tratamiento;
        this.ML_receta = ML_receta;
        this.ML_observaciones = ML_observaciones;
    }

    public Integer getML_idHistorial() {
        return ML_idHistorial;
    }

    public void setML_idHistorial(Integer ML_idHistorial) {
        this.ML_idHistorial = ML_idHistorial;
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

    public String getML_diagnostico() {
        return ML_diagnostico;
    }

    public void setML_diagnostico(String ML_diagnostico) {
        this.ML_diagnostico = ML_diagnostico;
    }

    public String getML_tratamiento() {
        return ML_tratamiento;
    }

    public void setML_tratamiento(String ML_tratamiento) {
        this.ML_tratamiento = ML_tratamiento;
    }

    public String getML_receta() {
        return ML_receta;
    }

    public void setML_receta(String ML_receta) {
        this.ML_receta = ML_receta;
    }

    public String getML_observaciones() {
        return ML_observaciones;
    }

    public void setML_observaciones(String ML_observaciones) {
        this.ML_observaciones = ML_observaciones;
    }
}
