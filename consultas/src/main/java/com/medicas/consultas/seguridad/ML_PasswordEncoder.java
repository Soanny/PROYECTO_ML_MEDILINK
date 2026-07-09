package com.medicas.consultas.seguridad;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class ML_PasswordEncoder implements PasswordEncoder {

    private static final String ML_PREFIJO_BCRYPT = "{bcrypt}";
    private static final String ML_PREFIJO_NOOP = "{noop}";

    private final BCryptPasswordEncoder ML_bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return ML_bcrypt.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        String ML_raw = rawPassword.toString();
        String ML_guardada = encodedPassword.trim();

        if (ML_guardada.isBlank()) {
            return false;
        }

        // Compatibilidad con claves guardadas por DelegatingPasswordEncoder: {bcrypt}$2a$...
        if (ML_guardada.startsWith(ML_PREFIJO_BCRYPT)) {
            String ML_hash = ML_guardada.substring(ML_PREFIJO_BCRYPT.length()).trim();
            return ML_esHashBcrypt(ML_hash) && ML_bcrypt.matches(ML_raw, ML_hash);
        }

        // Compatibilidad con claves antiguas en texto plano con prefijo {noop}.
        if (ML_guardada.startsWith(ML_PREFIJO_NOOP)) {
            String ML_textoPlano = ML_guardada.substring(ML_PREFIJO_NOOP.length()).trim();
            return ML_raw.trim().equals(ML_textoPlano);
        }

        // Compatibilidad con claves BCrypt guardadas sin prefijo.
        if (ML_esHashBcrypt(ML_guardada)) {
            return ML_bcrypt.matches(ML_raw, ML_guardada);
        }

        // Compatibilidad con usuarios antiguos creados antes de cifrar claves.
        // Se usa trim para evitar errores por espacios accidentales en la base de datos.
        return ML_raw.trim().equals(ML_guardada);
    }

    private boolean ML_esHashBcrypt(String ML_valor) {
        return ML_valor.startsWith("$2a$")
                || ML_valor.startsWith("$2b$")
                || ML_valor.startsWith("$2y$");
    }
}
