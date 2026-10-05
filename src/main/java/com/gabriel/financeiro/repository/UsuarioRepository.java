package com.gabriel.financeiro.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	boolean existsByCpf(String cpf);

	boolean existsByCpfAndIdNot(String cpf, UUID id);

}
