/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

/**
 *
 * @author Windows
 */
public class ML_PacienteAdminDTO {

    private Integer ML_idPaciente;
    private Integer ML_idUsuario;
    private String ML_nombres;
    private String ML_apellidos;
    private String ML_dni;
    private String ML_celular;
    private String ML_correo;
    private String ML_direccion;
    private String ML_fechaNacimiento;
    private String ML_genero;
    private String ML_grupoSanguineo;
    private String ML_alergias;
    private String ML_contactoEmergencia;
    private String ML_celularEmergencia;
    private String ML_estado;

    public ML_PacienteAdminDTO() {
    }

    public ML_PacienteAdminDTO(Integer ML_idPaciente, Integer ML_idUsuario, String ML_nombres,
                               String ML_apellidos, String ML_dni, String ML_celular,
                               String ML_correo, String ML_direccion, String ML_fechaNacimiento,
                               String ML_genero, String ML_grupoSanguineo, String ML_alergias,
                               String ML_contactoEmergencia, String ML_celularEmergencia,
                               String ML_estado) {
        this.ML_idPaciente = ML_idPaciente;
        this.ML_idUsuario = ML_idUsuario;
        this.ML_nombres = ML_nombres;
        this.ML_apellidos = ML_apellidos;
        this.ML_dni = ML_dni;
        this.ML_celular = ML_celular;
        this.ML_correo = ML_correo;
        this.ML_direccion = ML_direccion;
        this.ML_fechaNacimiento = ML_fechaNacimiento;
        this.ML_genero = ML_genero;
        this.ML_grupoSanguineo = ML_grupoSanguineo;
        this.ML_alergias = ML_alergias;
        this.ML_contactoEmergencia = ML_contactoEmergencia;
        this.ML_celularEmergencia = ML_celularEmergencia;
        this.ML_estado = ML_estado;
    }

    public Integer getML_idPaciente() {
        return ML_idPaciente;
    }

    public void setML_idPaciente(Integer ML_idPaciente) {
        this.ML_idPaciente = ML_idPaciente;
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

    public String getML_fechaNacimiento() {
        return ML_fechaNacimiento;
    }

    public void setML_fechaNacimiento(String ML_fechaNacimiento) {
        this.ML_fechaNacimiento = ML_fechaNacimiento;
    }

    public String getML_genero() {
        return ML_genero;
    }

    public void setML_genero(String ML_genero) {
        this.ML_genero = ML_genero;
    }

    public String getML_grupoSanguineo() {
        return ML_grupoSanguineo;
    }

    public void setML_grupoSanguineo(String ML_grupoSanguineo) {
        this.ML_grupoSanguineo = ML_grupoSanguineo;
    }

    public String getML_alergias() {
        return ML_alergias;
    }

    public void setML_alergias(String ML_alergias) {
        this.ML_alergias = ML_alergias;
    }

    public String getML_contactoEmergencia() {
        return ML_contactoEmergencia;
    }

    public void setML_contactoEmergencia(String ML_contactoEmergencia) {
        this.ML_contactoEmergencia = ML_contactoEmergencia;
    }

    public String getML_celularEmergencia() {
        return ML_celularEmergencia;
    }

    public void setML_celularEmergencia(String ML_celularEmergencia) {
        this.ML_celularEmergencia = ML_celularEmergencia;
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
