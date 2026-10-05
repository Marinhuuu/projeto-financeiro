package com.gabriel.financeiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.gabriel.financeiro.entities.CartaoCredito;

class FaturaCartaoServiceTest {

	private final FaturaCartaoService service = new FaturaCartaoService(null, null, null);

	private CartaoCredito cartao(int diaFechamento) {
		return new CartaoCredito(null, "Teste", BigDecimal.ZERO, diaFechamento, 10, null, null);
	}

	@Test
	void compraAteOFechamentoFicaNoMes() {
		assertEquals(YearMonth.of(2026, 1), service.calcularReferencia(cartao(30), LocalDate.of(2026, 1, 30)));
	}

	@Test
	void compraAposOFechamentoVaiParaOMesSeguinte() {
		assertEquals(YearMonth.of(2026, 2), service.calcularReferencia(cartao(30), LocalDate.of(2026, 1, 31)));
	}

	@Test
	void fechamentoMaiorQueOMesUsaOUltimoDia() {
		assertEquals(YearMonth.of(2026, 2), service.calcularReferencia(cartao(31), LocalDate.of(2026, 2, 28)));
	}

	@Test
	void parcelasDeCompraNoFimDoMesCaemEmFaturasDistintas() {
		// Antes: a 2ª parcela usava 31/01 + 1 mês = 28/02 e também caía em fevereiro
		YearMonth primeira = service.calcularReferencia(cartao(30), LocalDate.of(2026, 1, 31));

		assertEquals(YearMonth.of(2026, 2), primeira);
		assertEquals(YearMonth.of(2026, 3), primeira.plusMonths(1));
	}
}
