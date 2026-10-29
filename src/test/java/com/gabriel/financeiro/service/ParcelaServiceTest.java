package com.gabriel.financeiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.ParcelaRepository;

class ParcelaServiceTest {

	@Test
	void totalPendenteIncluiParcelasAtrasadas() {
		ParcelaRepository repo = mock(ParcelaRepository.class);
		when(repo.somarPorStatus(StatusParcela.PENDENTE)).thenReturn(new BigDecimal("100.00"));
		when(repo.somarPorStatus(StatusParcela.ATRASADA)).thenReturn(new BigDecimal("50.00"));

		ParcelaService service = new ParcelaService(repo, null, null);

		assertEquals(new BigDecimal("150.00"), service.calcularTotalPendente(null));
	}
}
