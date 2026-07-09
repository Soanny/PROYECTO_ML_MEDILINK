package com.medicas.consultas.seguridad;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class ML_CaptchaUtil {

    public static final String ML_CAPTCHA_SESION = "ML_CAPTCHA_SESION";
    public static final String ML_CAPTCHA_TOKEN = "ML_captchaToken";

    private static final String ML_CARACTERES_CAPTCHA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom ML_RANDOM = new SecureRandom();
    private static final long ML_DURACION_TOKEN_SEGUNDOS = 10 * 60;
    private static final String ML_SEPARADOR = ":";

    private ML_CaptchaUtil() {
    }

    public static String ML_generarCodigo() {
        StringBuilder ML_codigo = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int ML_indice = ML_RANDOM.nextInt(ML_CARACTERES_CAPTCHA.length());
            ML_codigo.append(ML_CARACTERES_CAPTCHA.charAt(ML_indice));
        }
        return ML_codigo.toString();
    }

    public static String ML_crearToken(String ML_codigo) {
        String ML_codigoNormalizado = ML_normalizar(ML_codigo);
        long ML_fecha = Instant.now().getEpochSecond();
        String ML_nonce = ML_generarNonce();
        String ML_payload = ML_codigoNormalizado + ML_SEPARADOR + ML_fecha + ML_SEPARADOR + ML_nonce;
        String ML_payloadBase64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(ML_payload.getBytes(StandardCharsets.UTF_8));
        String ML_firma = ML_firmar(ML_payloadBase64);
        return ML_payloadBase64 + "." + ML_firma;
    }

    public static boolean ML_validarCaptcha(String ML_codigoSesion,
                                            String ML_codigoIngresado,
                                            String ML_token) {
        String ML_ingresado = ML_normalizar(ML_codigoIngresado);

        if (ML_ingresado.isBlank()) {
            return false;
        }

        String ML_sesion = ML_normalizar(ML_codigoSesion);
        if (!ML_sesion.isBlank() && ML_sesion.equals(ML_ingresado)) {
            return true;
        }

        return ML_validarToken(ML_token, ML_ingresado);
    }

    private static boolean ML_validarToken(String ML_token, String ML_codigoIngresado) {
        if (ML_token == null || ML_token.isBlank() || !ML_token.contains(".")) {
            return false;
        }

        String[] ML_partes = ML_token.split("\\.", 2);
        if (ML_partes.length != 2) {
            return false;
        }

        String ML_payloadBase64 = ML_partes[0];
        String ML_firmaRecibida = ML_partes[1];
        String ML_firmaCalculada = ML_firmar(ML_payloadBase64);

        if (!MessageDigest.isEqual(
                ML_firmaCalculada.getBytes(StandardCharsets.UTF_8),
                ML_firmaRecibida.getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            String ML_payload = new String(
                    Base64.getUrlDecoder().decode(ML_payloadBase64),
                    StandardCharsets.UTF_8
            );
            String[] ML_datos = ML_payload.split(ML_SEPARADOR, 3);
            if (ML_datos.length != 3) {
                return false;
            }

            String ML_codigoToken = ML_normalizar(ML_datos[0]);
            long ML_fechaToken = Long.parseLong(ML_datos[1]);
            long ML_ahora = Instant.now().getEpochSecond();

            if ((ML_ahora - ML_fechaToken) > ML_DURACION_TOKEN_SEGUNDOS) {
                return false;
            }

            return ML_codigoToken.equals(ML_codigoIngresado);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static String ML_normalizar(String ML_valor) {
        if (ML_valor == null) {
            return "";
        }
        return ML_valor.replaceAll("\\s+", "").trim().toUpperCase();
    }

    private static String ML_generarNonce() {
        byte[] ML_bytes = new byte[12];
        ML_RANDOM.nextBytes(ML_bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ML_bytes);
    }

    private static String ML_firmar(String ML_texto) {
        try {
            Mac ML_mac = Mac.getInstance("HmacSHA256");
            ML_mac.init(new SecretKeySpec(ML_obtenerSecreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] ML_firma = ML_mac.doFinal(ML_texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(ML_firma);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar el captcha", ex);
        }
    }

    private static String ML_obtenerSecreto() {
        String ML_secreto = System.getenv("ML_CAPTCHA_SECRET");
        if (ML_secreto != null && !ML_secreto.isBlank()) {
            return ML_secreto;
        }
        return "MediLink-Captcha-Secret-2026-Cambiar-En-Produccion";
    }
}
