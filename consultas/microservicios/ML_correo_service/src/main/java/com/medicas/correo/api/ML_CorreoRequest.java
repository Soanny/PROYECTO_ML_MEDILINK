package com.medicas.correo.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ML_CorreoRequest {
    @Email
    @NotBlank
    private String destinatario;

    @NotBlank
    private String asunto;

    @NotBlank
    private String contenidoHtml;

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
    public String getContenidoHtml() { return contenidoHtml; }
    public void setContenidoHtml(String contenidoHtml) { this.contenidoHtml = contenidoHtml; }
}
