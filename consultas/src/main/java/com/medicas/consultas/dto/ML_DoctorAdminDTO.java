/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

/**
 *
 * @author Windows
 */
public class ML_DoctorAdminDTO {

    private Integer ML_idDoctor;
    private Integer ML_idUsuario;
    private String ML_nombres;
    private String ML_apellidos;
    private String ML_dni;
    private String ML_celular;
    private String ML_correo;
    private String ML_direccion;
    private String ML_especialidad;
    private String ML_consultorio;
    private String ML_ubicacionConsultorio;
    private String ML_piso;
    private String ML_nroColegiatura;
    private String ML_precioConsulta;
    private String ML_estado;

    public ML_DoctorAdminDTO() {
    }

    public ML_DoctorAdminDTO(Integer ML_idDoctor, Integer ML_idUsuario, String ML_nombres,
                             String ML_apellidos, String ML_dni, String ML_celular,
                             String ML_correo, String ML_direccion, String ML_especialidad,
                             String ML_consultorio, String ML_ubicacionConsultorio, String ML_piso,
                             String ML_nroColegiatura, String ML_precioConsulta, String ML_estado) {
        this.ML_idDoctor = ML_idDoctor;
        this.ML_idUsuario = ML_idUsuario;
        this.ML_nombres = ML_nombres;
        this.ML_apellidos = ML_apellidos;
        this.ML_dni = ML_dni;
        this.ML_celular = ML_celular;
        this.ML_correo = ML_correo;
        this.ML_direccion = ML_direccion;
        this.ML_especialidad = ML_especialidad;
        this.ML_consultorio = ML_consultorio;
        this.ML_ubicacionConsultorio = ML_ubicacionConsultorio;
        this.ML_piso = ML_piso;
        this.ML_nroColegiatura = ML_nroColegiatura;
        this.ML_precioConsulta = ML_precioConsulta;
        this.ML_estado = ML_estado;
    }

    public Integer getML_idDoctor() {
        return ML_idDoctor;
    }

    public void setML_idDoctor(Integer ML_idDoctor) {
        this.ML_idDoctor = ML_idDoctor;
    }

    public Integer getML_idUsuario() {
        return ML_idUsuario;
    }

    public void setML_idUsuario(Integer ML_idUsuario) {
        this.ML_idUsuario = ML_idUsuario;
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

    public String getML_correo() {
        return ML_correo;
    }

    public void setML_correo(String ML_correo) {
        this.ML_correo = ML_correo;
    }

    public String getML_direccion() {
        return ML_direccion;
    }

    public void setML_direccion(String ML_direccion) {
        this.ML_direccion = ML_direccion;
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

    public String getML_ubicacionConsultorio() {
        return ML_ubicacionConsultorio;
    }

    public void setML_ubicacionConsultorio(String ML_ubicacionConsultorio) {
        this.ML_ubicacionConsultorio = ML_ubicacionConsultorio;
    }

    public String getML_piso() {
        return ML_piso;
    }

    public void setML_piso(String ML_piso) {
        this.ML_piso = ML_piso;
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

    public String getML_nombreCompleto() {
        return ML_nombres + " " + ML_apellidos;
    }
}