package com.medicas.consultas.dto;

import java.math.BigDecimal;

public class ML_SecretariaPagoDTO {

    private Integer ML_idCita;
    private BigDecimal ML_monto;
    private String ML_metodoPago;
    private String ML_estadoPago;
    private String ML_codigoOperacion;
    private String ML_observacion;
    private String ML_tipoComprobante;

    public ML_SecretariaPagoDTO() {
        this.ML_estadoPago = "Pagado";
        this.ML_metodoPago = "Efectivo";
        this.ML_tipoComprobante = "Recibo";
    }

    public Integer getML_idCita() {
        return ML_idCita;
    }

    public void setML_idCita(Integer ML_idCita) {
        this.ML_idCita = ML_idCita;
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

    public String getML_estadoPago() {
        return ML_estadoPago;
    }

    public void setML_estadoPago(String ML_estadoPago) {
        this.ML_estadoPago = ML_estadoPago;
    }

    public String getML_codigoOperacion() {
        return ML_codigoOperacion;
    }

    public void setML_codigoOperacion(String ML_codigoOperacion) {
        this.ML_codigoOperacion = ML_codigoOperacion;
    }

    public String getML_observacion() {
        return ML_observacion;
    }

    public void setML_observacion(String ML_observacion) {
        this.ML_observacion = ML_observacion;
    }

    public String getML_tipoComprobante() {
        return ML_tipoComprobante;
    }

    public void setML_tipoComprobante(String ML_tipoComprobante) {
        this.ML_tipoComprobante = ML_tipoComprobante;
    }
}
