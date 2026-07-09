package com.medicas.consultas.dto;

public class ML_RegistroDoctorDTO {

    private String ML_nombres;
    private String ML_apellidos;
    private String ML_dni;
    private String ML_celular;
    private String ML_direccion;
    private String ML_correo;
    private String ML_contrasena;

    private Integer ML_idEspecialidad;
    private Integer ML_idConsultorio;
    private String ML_nroColegiatura;
    private String ML_precioConsulta;
    private String ML_estado;

    public ML_RegistroDoctorDTO() {
        this.ML_estado = "Activo";
    }

    public String getML_nombres() {
        return ML_nombres;
    }

    public void setML_nombres(String ML_nombres) {
        this.ML_nombres = ML_nombres;
    }

    public String getML_apellidos() {
        return ML_apellidos;
    }

    public void setML_apellidos(String ML_apellidos) {
        this.ML_apellidos = ML_apellidos;
    }

    public String getML_dni() {
        return ML_dni;
    }

    public void setML_dni(String ML_dni) {
        this.ML_dni = ML_dni;
    }

    public String getML_celular() {
        return ML_celular;
    }

    public void setML_celular(String ML_celular) {
        this.ML_celular = ML_celular;
    }

    public String getML_direccion() {
        return ML_direccion;
    }

    public void setML_direccion(String ML_direccion) {
        this.ML_direccion = ML_direccion;
    }

    public String getML_correo() {
        return ML_correo;
    }

    public void setML_correo(String ML_correo) {
        this.ML_correo = ML_correo;
    }

    public String getML_contrasena() {
        return ML_contrasena;
    }

    public void setML_contrasena(String ML_contrasena) {
        this.ML_contrasena = ML_contrasena;
    }

    public Integer getML_idEspecialidad() {
        return ML_idEspecialidad;
    }

    public void setML_idEspecialidad(Integer ML_idEspecialidad) {
        this.ML_idEspecialidad = ML_idEspecialidad;
    }

    public Integer getML_idConsultorio() {
        return ML_idConsultorio;
    }

    public void setML_idConsultorio(Integer ML_idConsultorio) {
        this.ML_idConsultorio = ML_idConsultorio;
    }

    public String getML_nroColegiatura() {
        return ML_nroColegiatura;
    }

    public void setML_nroColegiatura(String ML_nroColegiatura) {
        this.ML_nroColegiatura = ML_nroColegiatura;
    }

    public String getML_precioConsulta() {
        return ML_precioConsulta;
    }

    public void setML_precioConsulta(String ML_precioConsulta) {
        this.ML_precioConsulta = ML_precioConsulta;
    }

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }
}