/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

public class ML_RegistroSecretariaDTO {

    private String ML_nombres;
    private String ML_apellidos;
    private String ML_dni;
    private String ML_celular;
    private String ML_direccion;
    private String ML_correo;
    private String ML_contrasena;

    private String ML_fechaIngreso;
    private String ML_turno;
    private String ML_estado;

    public ML_RegistroSecretariaDTO() {
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

    public String getML_fechaIngreso() {
        return ML_fechaIngreso;
    }

    public void setML_fechaIngreso(String ML_fechaIngreso) {
        this.ML_fechaIngreso = ML_fechaIngreso;
    }

    public String getML_turno() {
        return ML_turno;
    }

    public void setML_turno(String ML_turno) {
        this.ML_turno = ML_turno;
    }

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }
}