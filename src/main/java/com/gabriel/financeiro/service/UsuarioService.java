package com.gabriel.financeiro.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private static final int SENHA_MIN = 8;

	// Limite do BCrypt: bytes além de 72 são ignorados
	private static final int SENHA_MAX_BYTES = 72;

	private final UsuarioRepository usuarioRepo;
	private final PasswordEncoder passwordEncoder;

	public UsuarioService(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
		this.usuarioRepo = usuarioRepo;
		this.passwordEncoder = passwordEncoder;
	}

	public Page<Usuario> listUsuario(Pageable pageable) {
		return usuarioRepo.findAll(pageable);
	}

	public Usuario getById(UUID id) {
		return usuarioRepo.findById(id)
				.orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
	}

	public boolean existe(UUID id) {
		return usuarioRepo.existsById(id);
	}

	public Optional<Usuario> buscarPorCpf(String cpf) {
		return usuarioRepo.findByCpf(normalizarCpf(cpf));
	}

	public Usuario insertUsuario(Usuario usuario) {

		validar(usuario);

		if (usuarioRepo.existsByCpf(usuario.getCpf())) {
			throw new RuntimeException("Já existe um usuário cadastrado com este CPF");
		}

		usuario.setId(null);

		// A senha é definida pelo próprio usuário no primeiro acesso
		usuario.setSenha(null);

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
	// AUTENTICAÇÃO
	// =========================================================

	/*
	 * Retorna o usuário se o CPF existir, já tiver senha e a senha conferir com o hash.
	 */
	public Optional<Usuario> autenticar(String cpf, String senha) {

		if (senha == null || senha.isEmpty()) {
			return Optional.empty();
		}

		return buscarPorCpf(cpf)
				.filter(u -> u.getSenha() != null)
				.filter(u -> passwordEncoder.matches(senha, u.getSenha()));
	}

	/*
	 * Primeiro acesso (somente uma vez por CPF): completa/cria o cadastro
	 * e grava apenas o hash da senha escolhida.
	 */
	@Transactional
	public Usuario concluirPrimeiroAcesso(String cpf, Usuario dados, String senha, String confirmacaoSenha) {

		Usuario usuario = buscarPorCpf(cpf).orElseGet(Usuario::new);

		if (!usuario.isPrimeiroAcessoPendente()) {
			throw new RuntimeException("O primeiro acesso deste CPF já foi realizado. Entre com a sua senha.");
		}

		usuario.setNome(dados.getNome());
		usuario.setSobrenome(dados.getSobrenome());
		usuario.setCpf(cpf);
		usuario.setDataNascimento(dados.getDataNascimento());

		validar(usuario);
		validarSenha(senha, confirmacaoSenha);

		usuario.setSenha(passwordEncoder.encode(senha));

		return usuarioRepo.save(usuario);
	}

	private void validarSenha(String senha, String confirmacaoSenha) {

		if (senha == null || senha.length() < SENHA_MIN) {
			throw new RuntimeException("A senha deve ter pelo menos " + SENHA_MIN + " caracteres");
		}

		if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
			throw new RuntimeException("A senha é longa demais (máximo de " + SENHA_MAX_BYTES + " caracteres)");
		}

		if (!senha.matches(".*\\p{L}.*") || !senha.matches(".*\\d.*")) {
			throw new RuntimeException("A senha deve conter letras e números");
		}

		if (!senha.equals(confirmacaoSenha)) {
			throw new RuntimeException("A confirmação não confere com a senha");
		}
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

		String cpf = normalizarCpf(usuario.getCpf());

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

	public static String normalizarCpf(String cpf) {
		return cpf == null ? "" : cpf.replaceAll("\\D", "");
	}

	// Valida os dois dígitos verificadores do CPF
	public static boolean cpfValido(String cpf) {

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
