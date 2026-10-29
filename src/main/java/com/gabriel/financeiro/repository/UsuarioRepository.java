package com.gabriel.financeiro.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	Optional<Usuario> findByCpf(String cpf);

	boolean existsByCpf(String cpf);

	boolean existsByCpfAndIdNot(String cpf, UUID id);

	// O próprio usuário + os convites que ele fez e que ainda não foram usados
	@Query("""
		SELECT u
		FROM Usuario u
		WHERE u.id = :usuarioId
		   OR (u.convidadoPor = :usuarioId AND u.senha IS NULL)
	""")
	Page<Usuario> listarGerenciaveis(UUID usuarioId, Pageable pageable);

}
