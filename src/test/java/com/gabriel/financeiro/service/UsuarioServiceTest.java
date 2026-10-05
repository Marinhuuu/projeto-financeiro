package com.gabriel.financeiro.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioServiceTest {

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
}
