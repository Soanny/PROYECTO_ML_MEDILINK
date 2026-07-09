/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.medicas.consultas.repositorio;

import com.medicas.consultas.modelo.ML_Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ML_UsuarioRepository extends JpaRepository<ML_Usuario, Integer> {

    @Query("SELECT u FROM ML_Usuario u WHERE LOWER(TRIM(u.ML_correo)) = LOWER(TRIM(:correo))")
    Optional<ML_Usuario> ML_buscarPorCorreo(@Param("correo") String correo);
}