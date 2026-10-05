package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepo;

	public UsuarioService(UsuarioRepository usuarioRepo) {
		this.usuarioRepo = usuarioRepo;
	}

	public Page<Usuario> listUsuario(Pageable pageable) {
		return usuarioRepo.findAll(pageable);
	}

	public Usuario getById(UUID id) {
		return usuarioRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
	}

	public Usuario insertUsuario(Usuario usuario) {

		validar(usuario);

		if (usuarioRepo.existsByCpf(usuario.getCpf())) {
			throw new RuntimeException("Já existe um usuário cadastrado com este CPF");
		}

		usuario.setId(null);

		return usuarioRepo.save(usuario);
	}

	public Usuario updateUsuario(UUID id, Usuario usuario) {

		Usuario existente = getById(id);

		validar(usuario);

		if (usuarioRepo.existsByCpfAndIdNot(usuario.getCpf(), id)) {
			throw new RuntimeException("Já existe um usuário cadastrado com este CPF");
		}

		existente.setNome(usuario.getNome());
		existente.setSobrenome(usuario.getSobrenome());
		existente.setCpf(usuario.getCpf());
		existente.setDataNascimento(usuario.getDataNascimento());

		return usuarioRepo.save(existente);
	}

	public void deleteUsuario(UUID id) {
		usuarioRepo.delete(getById(id));
	}

	// =========================================================
	// VALIDAÇÃO
	// =========================================================

	// Normaliza os campos (trim, CPF só com dígitos) e valida
	private void validar(Usuario usuario) {

		if (usuario.getNome() == null || usuario.getNome().isBlank()) {
			throw new RuntimeException("Informe o nome");
		}

		if (usuario.getSobrenome() == null || usuario.getSobrenome().isBlank()) {
			throw new RuntimeException("Informe o sobrenome");
		}

		usuario.setNome(usuario.getNome().trim());
		usuario.setSobrenome(usuario.getSobrenome().trim());

		String cpf = usuario.getCpf() == null ? "" : usuario.getCpf().replaceAll("\\D", "");

		if (!cpfValido(cpf)) {
			throw new RuntimeException("CPF inválido");
		}

		usuario.setCpf(cpf);

		LocalDate dataNascimento = usuario.getDataNascimento();

		if (dataNascimento == null) {
			throw new RuntimeException("Informe a data de nascimento");
		}

		if (dataNascimento.isAfter(LocalDate.now())) {
			throw new RuntimeException("A data de nascimento não pode estar no futuro");
		}

		if (dataNascimento.isBefore(LocalDate.of(1900, 1, 1))) {
			throw new RuntimeException("Data de nascimento inválida");
		}
	}

	// Valida os dois dígitos verificadores do CPF
	static boolean cpfValido(String cpf) {

		if (cpf == null || !cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
			return false;
		}

		for (int posicao = 9; posicao <= 10; posicao++) {

			int soma = 0;

			for (int i = 0; i < posicao; i++) {
				soma += Character.getNumericValue(cpf.charAt(i)) * (posicao + 1 - i);
			}

			int digito = (soma * 10) % 11 % 10;

			if (digito != Character.getNumericValue(cpf.charAt(posicao))) {
				return false;
			}
		}

		return true;
	}
}
