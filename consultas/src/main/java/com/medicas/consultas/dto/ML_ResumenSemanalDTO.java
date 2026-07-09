/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.dto;

/**
 *
 * @author Windows
 */
public class ML_ResumenSemanalDTO {

    private String ML_dia;
    private Integer ML_total;
    private Integer ML_altura;

    public ML_ResumenSemanalDTO() {
    }

    public ML_ResumenSemanalDTO(String ML_dia, Integer ML_total, Integer ML_altura) {
        this.ML_dia = ML_dia;
        this.ML_total = ML_total;
        this.ML_altura = ML_altura;
    }

    public String getML_dia() {
        return ML_dia;
    }

    public void setML_dia(String ML_dia) {
        this.ML_dia = ML_dia;
    }

    public Integer getML_total() {
        return ML_total;
    }

    public void setML_total(Integer ML_total) {
        this.ML_total = ML_total;
    }

    public Integer getML_altura() {
        return ML_altura;
    }

    public void setML_altura(Integer ML_altura) {
        this.ML_altura = ML_altura;
    }
}