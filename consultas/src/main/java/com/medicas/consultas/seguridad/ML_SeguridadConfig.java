/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.seguridad;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ML_SeguridadConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .addFilterBefore(new ML_CaptchaFiltro(), UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    "/",
                    "/login",
                    "/registro/paciente",
                    "/error",
                    "/api/salud",
                    "/api/catalogo",
                    "/api/correos/estado",
                    "/css/**",
                    "/images/**",
                    "/js/**"
                ).permitAll()

                .requestMatchers("/api/comprobantes/**").hasAnyRole("ADMIN", "SECRETARIA", "PACIENTE")
                .requestMatchers("/api/correos/**").hasAnyRole("ADMIN", "SECRETARIA")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/doctor/**").hasRole("DOCTOR")
                .requestMatchers("/secretaria/**").hasRole("SECRETARIA")
                .requestMatchers("/paciente/**").hasRole("PACIENTE")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(ML_successHandler())
                .failureUrl("/login?error=true")
                .permitAll()
            )

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler ML_successHandler() {

        return new AuthenticationSuccessHandler() {

            @Override
            public void onAuthenticationSuccess(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication
            ) throws IOException, ServletException {

                for (GrantedAuthority authority : authentication.getAuthorities()) {

                    String rol = authority.getAuthority();

                    if (rol.equals("ROLE_ADMIN")) {
                        response.sendRedirect("/admin/dashboard");
                        return;
                    }

                    if (rol.equals("ROLE_DOCTOR")) {
                        response.sendRedirect("/doctor/dashboard");
                        return;
                    }

                    if (rol.equals("ROLE_SECRETARIA")) {
                        response.sendRedirect("/secretaria/dashboard");
                        return;
                    }

                    if (rol.equals("ROLE_PACIENTE")) {
                        response.sendRedirect("/paciente/dashboard");
                        return;
                    }
                }

                response.sendRedirect("/login?error=true");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new ML_PasswordEncoder();
    }
}