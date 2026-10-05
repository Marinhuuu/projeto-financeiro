package com.gabriel.financeiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.repository.UsuarioRepository;

class UsuarioServiceTest {

	private static final String CPF = "52998224725";

	private UsuarioRepository repo;
	private PasswordEncoder encoder;
	private UsuarioService service;

	@BeforeEach
	void setUp() {
		repo = mock(UsuarioRepository.class);
		encoder = new BCryptPasswordEncoder(4); // custo baixo só para o teste ser rápido
		service = new UsuarioService(repo, encoder);
		when(repo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private Usuario dados() {
		return new Usuario(null, "Gabriel", "Silva", null, LocalDate.of(1990, 5, 20));
	}

	// ===== CPF =====

	@Test
	void aceitaCpfValido() {
		assertTrue(UsuarioService.cpfValido("52998224725"));
		assertTrue(UsuarioService.cpfValido("11144477735"));
	}

	@Test
	void rejeitaDigitoVerificadorErrado() {
		assertFalse(UsuarioService.cpfValido("52998224724"));
		assertFalse(UsuarioService.cpfValido("11144477705"));
	}

	@Test
	void rejeitaDigitosRepetidosETamanhoErrado() {
		assertFalse(UsuarioService.cpfValido("11111111111"));
		assertFalse(UsuarioService.cpfValido("1234567890"));
		assertFalse(UsuarioService.cpfValido("529.982.247-25"));
		assertFalse(UsuarioService.cpfValido(null));
	}

	// ===== PRIMEIRO ACESSO =====

	@Test
	void primeiroAcessoGravaSomenteOHash() {
		when(repo.findByCpf(CPF)).thenReturn(Optional.empty());

		Usuario salvo = service.concluirPrimeiroAcesso(CPF, dados(), "senhaForte1", "senhaForte1");

		assertEquals(CPF, salvo.getCpf());
		assertNotEquals("senhaForte1", salvo.getSenha());
		assertTrue(salvo.getSenha().startsWith("$2"));
		assertTrue(encoder.matches("senhaForte1", salvo.getSenha()));
	}

	@Test
	void primeiroAcessoCompletaCadastroExistenteSemSenha() {
		Usuario existente = new Usuario(null, "Antigo", "Nome", CPF, LocalDate.of(1980, 1, 1));
		when(repo.findByCpf(CPF)).thenReturn(Optional.of(existente));

		Usuario salvo = service.concluirPrimeiroAcesso(CPF, dados(), "senhaForte1", "senhaForte1");

		assertEquals("Gabriel", salvo.getNome());
		assertFalse(salvo.isPrimeiroAcessoPendente());
	}

	@Test
	void primeiroAcessoSoPodeAcontecerUmaVez() {
		Usuario comSenha = new Usuario(null, "Gabriel", "Silva", CPF, LocalDate.of(1990, 5, 20));
		comSenha.setSenha(encoder.encode("senhaForte1"));
		when(repo.findByCpf(CPF)).thenReturn(Optional.of(comSenha));

		assertThrows(RuntimeException.class,
				() -> service.concluirPrimeiroAcesso(CPF, dados(), "outraSenha2", "outraSenha2"));
	}

	@Test
	void rejeitaSenhaFracaOuConfirmacaoDiferente() {
		when(repo.findByCpf(CPF)).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> service.concluirPrimeiroAcesso(CPF, dados(), "curta1", "curta1"));
		assertThrows(RuntimeException.class, () -> service.concluirPrimeiroAcesso(CPF, dados(), "somenteletras", "somenteletras"));
		assertThrows(RuntimeException.class, () -> service.concluirPrimeiroAcesso(CPF, dados(), "senhaForte1", "senhaForte2"));
	}

	// ===== LOGIN =====

	@Test
	void autenticaSomenteComSenhaCorreta() {
		Usuario comSenha = new Usuario(null, "Gabriel", "Silva", CPF, LocalDate.of(1990, 5, 20));
		comSenha.setSenha(encoder.encode("senhaForte1"));
		when(repo.findByCpf(CPF)).thenReturn(Optional.of(comSenha));

		assertTrue(service.autenticar("529.982.247-25", "senhaForte1").isPresent());
		assertTrue(service.autenticar(CPF, "senhaErrada1").isEmpty());
		assertTrue(service.autenticar(CPF, "").isEmpty());
	}

	@Test
	void naoAutenticaUsuarioSemSenha() {
		when(repo.findByCpf(CPF)).thenReturn(Optional.of(new Usuario(null, "G", "S", CPF, LocalDate.of(1990, 1, 1))));

		assertTrue(service.autenticar(CPF, "qualquer1").isEmpty());
	}
}
