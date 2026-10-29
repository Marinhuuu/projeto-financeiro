package com.gabriel.financeiro.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PaginacaoTest {

	@Test
	void corrigePaginaETamanhoInvalidos() {
		Pageable negativo = Paginacao.de(-3, -1);
		assertEquals(0, negativo.getPageNumber());
		assertEquals(1, negativo.getPageSize());

		assertEquals(Paginacao.TAMANHO_MAXIMO, Paginacao.de(0, 1_000_000).getPageSize());
		assertEquals(10, Paginacao.de(2, 10).getPageSize());
	}
}
