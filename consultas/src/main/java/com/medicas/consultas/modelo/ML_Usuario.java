package com.medicas.consultas.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class ML_Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer ML_idUsuario;

    @Column(name = "id_rol")
    private Integer ML_idRol;

    @Column(name = "nombres")
    private String ML_nombres;

    @Column(name = "apellidos")
    private String ML_apellidos;

    @Column(name = "dni")
    private String ML_dni;

    @Column(name = "celular")
    private String ML_celular;

    @Column(name = "direccion")
    private String ML_direccion;

    @Column(name = "correo")
    private String ML_correo;

    @Column(name = "contraseña")
    private String ML_contrasena;

    @Column(name = "estado")
    private String ML_estado;

    @Column(name = "fecha_registro")
    private LocalDateTime ML_fechaRegistro;

    public Integer getML_idUsuario() {
        return ML_idUsuario;
    }

    public void setML_idUsuario(Integer ML_idUsuario) {
        this.ML_idUsuario = ML_idUsuario;
    }

    public Integer getML_idRol() {
        return ML_idRol;
    }

    public void setML_idRol(Integer ML_idRol) {
        this.ML_idRol = ML_idRol;
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

    public String getML_estado() {
        return ML_estado;
    }

    public void setML_estado(String ML_estado) {
        this.ML_estado = ML_estado;
    }

    public LocalDateTime getML_fechaRegistro() {
        return ML_fechaRegistro;
    }

    public void setML_fechaRegistro(LocalDateTime ML_fechaRegistro) {
        this.ML_fechaRegistro = ML_fechaRegistro;
    }
}