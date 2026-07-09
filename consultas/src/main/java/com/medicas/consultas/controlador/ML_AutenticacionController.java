package com.medicas.consultas.controlador;

import com.medicas.consultas.seguridad.ML_CaptchaFiltro;
import com.medicas.consultas.seguridad.ML_CaptchaUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ML_AutenticacionController {

    @GetMapping("/")
    public String redirigirALogin() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model, HttpSession session, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        String ML_captcha = ML_CaptchaUtil.ML_generarCodigo();
        session.setAttribute(ML_CaptchaFiltro.ML_CAPTCHA_SESION, ML_captcha);
        model.addAttribute("ML_captchaCodigo", ML_captcha);
        model.addAttribute("ML_captchaToken", ML_CaptchaUtil.ML_crearToken(ML_captcha));
        return "autentiacion/ML_login";
    }

    @GetMapping("/index")
    public String mostrarIndex() {
        return "redirect:/login";
    }
}
