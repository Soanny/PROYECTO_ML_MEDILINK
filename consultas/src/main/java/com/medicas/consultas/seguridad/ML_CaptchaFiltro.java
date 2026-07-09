package com.medicas.consultas.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class ML_CaptchaFiltro extends OncePerRequestFilter {

    public static final String ML_CAPTCHA_SESION = ML_CaptchaUtil.ML_CAPTCHA_SESION;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!ML_esPeticionLogin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        String ML_captchaSesion = session != null ? (String) session.getAttribute(ML_CAPTCHA_SESION) : null;
        String ML_captchaIngresado = request.getParameter("ML_captchaRespuesta");
        String ML_captchaToken = request.getParameter(ML_CaptchaUtil.ML_CAPTCHA_TOKEN);

        boolean ML_captchaValido = ML_CaptchaUtil.ML_validarCaptcha(
                ML_captchaSesion,
                ML_captchaIngresado,
                ML_captchaToken
        );

        if (!ML_captchaValido) {
            if (session != null) {
                session.removeAttribute(ML_CAPTCHA_SESION);
            }
            response.sendRedirect(request.getContextPath() + "/login?captcha=true");
            return;
        }

        if (session != null) {
            session.removeAttribute(ML_CAPTCHA_SESION);
        }
        filterChain.doFilter(request, response);
    }

    private boolean ML_esPeticionLogin(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/login".equals(request.getServletPath());
    }
}
