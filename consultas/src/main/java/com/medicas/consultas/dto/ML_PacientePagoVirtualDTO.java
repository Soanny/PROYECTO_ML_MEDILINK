package com.medicas.consultas.dto;

import java.math.BigDecimal;

public class ML_PacientePagoVirtualDTO {

    private Integer ML_idCita;
    private String ML_seleccionPago;
    private BigDecimal ML_monto;
    private String ML_metodoPago;
    private String ML_codigoOperacion;

    public ML_PacientePagoVirtualDTO() {
    }

    public ML_PacientePagoVirtualDTO(Integer ML_idCita, BigDecimal ML_monto, String ML_metodoPago, String ML_codigoOperacion) {
        this.ML_idCita = ML_idCita;
        this.ML_seleccionPago = ML_idCita != null ? String.valueOf(ML_idCita) : null;
        this.ML_monto = ML_monto;
        this.ML_metodoPago = ML_metodoPago;
        this.ML_codigoOperacion = ML_codigoOperacion;
    }

    public Integer getML_idCita() {
        return ML_idCita;
    }

    public void setML_idCita(Integer ML_idCita) {
        this.ML_idCita = ML_idCita;
    }

    public String getML_seleccionPago() {
        return ML_seleccionPago;
    }

    public void setML_seleccionPago(String ML_seleccionPago) {
        this.ML_seleccionPago = ML_seleccionPago;
    }

    public BigDecimal getML_monto() {
        return ML_monto;
    }

    public void setML_monto(BigDecimal ML_monto) {
        this.ML_monto = ML_monto;
    }

    public String getML_metodoPago() {
        return ML_metodoPago;
    }

    public void setML_metodoPago(String ML_metodoPago) {
        this.ML_metodoPago = ML_metodoPago;
    }

    public String getML_codigoOperacion() {
        return ML_codigoOperacion;
    }

    public void setML_codigoOperacion(String ML_codigoOperacion) {
        this.ML_codigoOperacion = ML_codigoOperacion;
    }
}
