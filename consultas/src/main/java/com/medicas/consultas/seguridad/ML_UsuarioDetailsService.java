/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.seguridad;

import com.medicas.consultas.modelo.ML_Usuario;
import com.medicas.consultas.repositorio.ML_UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ML_UsuarioDetailsService implements UserDetailsService {

    private final ML_UsuarioRepository ML_usuarioRepository;

    public ML_UsuarioDetailsService(ML_UsuarioRepository ML_usuarioRepository) {
        this.ML_usuarioRepository = ML_usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {

        ML_Usuario usuario = ML_usuarioRepository.ML_buscarPorCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        if (usuario.getML_estado() == null || !usuario.getML_estado().equalsIgnoreCase("Activo")) {
            throw new UsernameNotFoundException("Usuario inactivo: " + correo);
        }

        return User.builder()
                .username(usuario.getML_correo())
                .password(usuario.getML_contrasena())
                .roles(ML_obtenerRol(usuario.getML_idRol()))
                .build();
    }

    private String ML_obtenerRol(Integer idRol) {

        if (idRol == null) {
            return "PACIENTE";
        }

        return switch (idRol) {
            case 1 -> "ADMIN";
            case 2 -> "SECRETARIA";
            case 3 -> "DOCTOR";
            case 4 -> "PACIENTE";
            default -> "PACIENTE";
        };
    }
}