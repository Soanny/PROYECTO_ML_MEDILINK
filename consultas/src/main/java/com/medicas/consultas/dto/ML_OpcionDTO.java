/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

public class ML_OpcionDTO {

    private Integer ML_id;
    private String ML_nombre;

    public ML_OpcionDTO() {
    }

    public ML_OpcionDTO(Integer ML_id, String ML_nombre) {
        this.ML_id = ML_id;
        this.ML_nombre = ML_nombre;
    }

    public Integer getML_id() {
        return ML_id;
    }

    public void setML_id(Integer ML_id) {
        this.ML_id = ML_id;
    }

    public String getML_nombre() {
        return ML_nombre;
    }

    public void setML_nombre(String ML_nombre) {
        this.ML_nombre = ML_nombre;
    }
}