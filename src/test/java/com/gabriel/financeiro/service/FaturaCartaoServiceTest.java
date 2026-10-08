package com.gabriel.financeiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;

class FaturaCartaoServiceTest {

	private final FaturaCartaoService service = new FaturaCartaoService(null, null, null, null);

	private CartaoCredito cartao(int diaFechamento) {
		return cartao(diaFechamento, 10);
	}

	private CartaoCredito cartao(int diaFechamento, int diaVencimento) {
		return new CartaoCredito(null, "Teste", BigDecimal.ZERO, diaFechamento, diaVencimento, null, null);
	}

	/* Vencimento da fatura em que uma compra feita em "data" entra */
	private LocalDate vencimentoDaCompra(CartaoCredito cartao, LocalDate data) {
		return service.calcularDataVencimento(cartao, service.calcularReferencia(cartao, data));
	}

	private FaturaCartao fatura(CartaoCredito cartao, YearMonth referencia) {
		return new FaturaCartao(null, referencia.getMonthValue(), referencia.getYear(),
				service.calcularDataFechamento(cartao, referencia),
				service.calcularDataVencimento(cartao, referencia), BigDecimal.ZERO, null, cartao);
	}

	private static LocalDate d(int dia, int mes, int ano) {
		return LocalDate.of(ano, mes, dia);
	}

	// =========================================================
	// CICLO DO CARTÃO (fecha dia 5, vence dia 10)
	// =========================================================

	@Test
	void compraAteOFechamentoEntraNaFaturaDoMes() {
		CartaoCredito c = cartao(5, 10);

		assertEquals(d(10, 10, 2026), vencimentoDaCompra(c, d(2, 10, 2026)));
		assertEquals(d(10, 10, 2026), vencimentoDaCompra(c, d(4, 10, 2026)));
		assertEquals(d(10, 10, 2026), vencimentoDaCompra(c, d(5, 10, 2026)));
		assertEquals(d(10, 10, 2026), vencimentoDaCompra(c, d(6, 9, 2026)));
	}

	@Test
	void compraAposOFechamentoEntraNaFaturaSeguinte() {
		CartaoCredito c = cartao(5, 10);

		assertEquals(d(10, 11, 2026), vencimentoDaCompra(c, d(6, 10, 2026)));
		assertEquals(d(10, 11, 2026), vencimentoDaCompra(c, d(31, 10, 2026)));
		assertEquals(d(10, 11, 2026), vencimentoDaCompra(c, d(5, 11, 2026)));
	}

	@Test
	void periodoDaFaturaVaiDoDiaSeguinteAoFechamentoAnteriorAteOFechamento() {
		CartaoCredito c = cartao(5, 10);

		FaturaCartao outubro = fatura(c, YearMonth.of(2026, 10));
		assertEquals(d(6, 9, 2026), outubro.getDataInicioPeriodo());
		assertEquals(d(5, 10, 2026), outubro.getDataFechamento());
		assertEquals(d(10, 10, 2026), outubro.getDataVencimento());

		FaturaCartao novembro = fatura(c, YearMonth.of(2026, 11));
		assertEquals(d(6, 10, 2026), novembro.getDataInicioPeriodo());
		assertEquals(d(5, 11, 2026), novembro.getDataFechamento());
		assertEquals(d(10, 11, 2026), novembro.getDataVencimento());
	}

	@Test
	void viradaDeAno() {
		CartaoCredito c = cartao(5, 10);

		assertEquals(d(10, 12, 2026), vencimentoDaCompra(c, d(5, 12, 2026)));
		assertEquals(d(10, 1, 2027), vencimentoDaCompra(c, d(6, 12, 2026)));
		assertEquals(d(10, 1, 2027), vencimentoDaCompra(c, d(31, 12, 2026)));
		assertEquals(d(10, 1, 2027), vencimentoDaCompra(c, d(5, 1, 2027)));
		assertEquals(d(10, 2, 2027), vencimentoDaCompra(c, d(6, 1, 2027)));

		FaturaCartao janeiro = fatura(c, YearMonth.of(2027, 1));
		assertEquals(d(6, 12, 2026), janeiro.getDataInicioPeriodo());
		assertEquals(d(5, 1, 2027), janeiro.getDataFechamento());

		FaturaCartao fevereiro = fatura(c, YearMonth.of(2027, 2));
		assertEquals(d(6, 1, 2027), fevereiro.getDataInicioPeriodo());
		assertEquals(d(5, 2, 2027), fevereiro.getDataFechamento());
	}

	@Test
	void vencimentoAntesDoDiaDeFechamentoCaiNoMesSeguinte() {
		// Fecha 25, vence 5: a fatura que fecha em 25/10 vence em 05/11
		CartaoCredito c = cartao(25, 5);

		assertEquals(d(5, 11, 2026), vencimentoDaCompra(c, d(20, 10, 2026)));
		assertEquals(d(5, 12, 2026), vencimentoDaCompra(c, d(26, 10, 2026)));
		assertEquals(d(5, 1, 2027), vencimentoDaCompra(c, d(26, 11, 2026)));
	}

	@Test
	void parcelasDeCompraAposOFechamentoCaemEmFaturasConsecutivas() {
		CartaoCredito c = cartao(5, 10);
		YearMonth primeira = service.calcularReferencia(c, d(6, 11, 2026));

		assertEquals(d(10, 12, 2026), service.calcularDataVencimento(c, primeira));
		assertEquals(d(10, 1, 2027), service.calcularDataVencimento(c, primeira.plusMonths(1)));
		assertEquals(d(10, 2, 2027), service.calcularDataVencimento(c, primeira.plusMonths(2)));
	}

	// =========================================================
	// MESES COM 28/29/30/31 DIAS
	// =========================================================

	@Test
	void compraAteOFechamentoFicaNoMes() {
		assertEquals(YearMonth.of(2026, 1), service.calcularReferencia(cartao(30), d(30, 1, 2026)));
	}

	@Test
	void compraAposOFechamentoVaiParaOMesSeguinte() {
		assertEquals(YearMonth.of(2026, 2), service.calcularReferencia(cartao(30), d(31, 1, 2026)));
	}

	@Test
	void fechamentoMaiorQueOMesUsaOUltimoDia() {
		assertEquals(YearMonth.of(2026, 2), service.calcularReferencia(cartao(31), d(28, 2, 2026)));
		assertEquals(YearMonth.of(2026, 3), service.calcularReferencia(cartao(31), d(1, 3, 2026)));
	}

	@Test
	void fevereiroBissextoENaoBissexto() {
		CartaoCredito c = cartao(30, 10);

		// 2026: fevereiro fecha em 28/02; março começa em 01/03
		assertEquals(d(28, 2, 2026), fatura(c, YearMonth.of(2026, 2)).getDataFechamento());
		assertEquals(d(1, 3, 2026), fatura(c, YearMonth.of(2026, 3)).getDataInicioPeriodo());
		assertEquals(YearMonth.of(2026, 3), service.calcularReferencia(c, d(1, 3, 2026)));

		// 2028 (bissexto): fevereiro fecha em 29/02
		assertEquals(YearMonth.of(2028, 2), service.calcularReferencia(c, d(29, 2, 2028)));
		assertEquals(d(29, 2, 2028), fatura(c, YearMonth.of(2028, 2)).getDataFechamento());
		assertEquals(d(1, 3, 2028), fatura(c, YearMonth.of(2028, 3)).getDataInicioPeriodo());
	}

	@Test
	void mesesDe30E31Dias() {
		CartaoCredito c = cartao(31, 10);

		// Abril (30 dias) fecha em 30/04; maio vai de 01/05 a 31/05
		assertEquals(YearMonth.of(2026, 4), service.calcularReferencia(c, d(30, 4, 2026)));
		assertEquals(YearMonth.of(2026, 5), service.calcularReferencia(c, d(1, 5, 2026)));
		assertEquals(d(1, 5, 2026), fatura(c, YearMonth.of(2026, 5)).getDataInicioPeriodo());
		assertEquals(d(31, 5, 2026), fatura(c, YearMonth.of(2026, 5)).getDataFechamento());
	}

	@Test
	void parcelasDeCompraNoFimDoMesCaemEmFaturasDistintas() {
		// Antes: a 2ª parcela usava 31/01 + 1 mês = 28/02 e também caía em fevereiro
		YearMonth primeira = service.calcularReferencia(cartao(30), d(31, 1, 2026));

		assertEquals(YearMonth.of(2026, 2), primeira);
		assertEquals(YearMonth.of(2026, 3), primeira.plusMonths(1));
	}

	/*
	 * Para todo dia de fechamento (1–31) e toda data de 2026 a 2028:
	 * a compra fica dentro do período da fatura escolhida e os períodos
	 * são contínuos (sem buraco nem sobreposição).
	 */
	@Test
	void todaCompraCaiDentroDoPeriodoDaSuaFatura() {

		for (int dia = 1; dia <= 31; dia++) {

			CartaoCredito c = cartao(dia, 10);

			for (LocalDate data = d(1, 1, 2026); data.isBefore(d(1, 1, 2029)); data = data.plusDays(1)) {

				YearMonth referencia = service.calcularReferencia(c, data);
				FaturaCartao f = fatura(c, referencia);
				FaturaCartao anterior = fatura(c, referencia.minusMonths(1));

				String msg = "fechamento " + dia + ", compra " + data;
				assertFalse(data.isBefore(f.getDataInicioPeriodo()), msg);
				assertFalse(data.isAfter(f.getDataFechamento()), msg);
				assertEquals(anterior.getDataFechamento().plusDays(1), f.getDataInicioPeriodo(), msg);
			}
		}
	}

	// =========================================================
	// PAGAMENTO DA FATURA
	// =========================================================

	private static class Cenario {
		final FaturaCartaoRepository faturaRepo = mock(FaturaCartaoRepository.class);
		final ParcelaRepository parcelaRepo = mock(ParcelaRepository.class);
		final FaturaCartaoService service = new FaturaCartaoService(faturaRepo, parcelaRepo, null, null);
		final FaturaCartao fatura = new FaturaCartao();
		final List<Parcela> parcelas;

		/* Fatura com duas parcelas pendentes (750,00 + 500,00) e o vencimento informado */
		Cenario(LocalDate vencimento) {
			fatura.setId(UUID.randomUUID());
			fatura.setCartao(new CartaoCredito(null, "Nubank", BigDecimal.ZERO, 5, 10, null, null));
			fatura.setMesReferencia(vencimento.getMonthValue());
			fatura.setAnoReferencia(vencimento.getYear());
			fatura.setDataFechamento(vencimento.minusDays(5));
			fatura.setDataVencimento(vencimento);
			fatura.setValorTotal(new BigDecimal("1250.00"));

			parcelas = List.of(parcela("750.00", vencimento), parcela("500.00", vencimento));

			when(faturaRepo.findById(fatura.getId())).thenReturn(Optional.of(fatura));
			when(faturaRepo.save(any(FaturaCartao.class))).thenAnswer(inv -> inv.getArgument(0));
			when(parcelaRepo.findByFatura(fatura)).thenReturn(parcelas);
			when(parcelaRepo.countByFatura(fatura)).thenAnswer(inv -> (long) parcelas.size());
			when(parcelaRepo.countByFaturaAndStatusParcelaNot(eq(fatura), any(StatusParcela.class)))
					.thenAnswer(inv -> parcelas.stream()
							.filter(p -> p.getStatusParcela() != inv.getArgument(1, StatusParcela.class)).count());
			when(parcelaRepo.somarPorFatura(fatura)).thenAnswer(inv -> parcelas.stream()
					.map(Parcela::getValorParcela).reduce(BigDecimal.ZERO, BigDecimal::add));

			service.atualizarStatus(List.of(fatura));
		}

		private Parcela parcela(String valor, LocalDate vencimento) {
			Parcela p = new Parcela(UUID.randomUUID(), 1, new BigDecimal(valor), vencimento, StatusParcela.PENDENTE);
			p.setFatura(fatura);
			return p;
		}
	}

	@Test
	void pagarFaturaAbertaMarcaComoPagaComDataDeHoje() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));
		assertEquals(StatusFatura.EM_ABERTO, c.fatura.getStatusFatura());
		assertNull(c.fatura.getDataPagamento());

		c.service.pagarFatura(c.fatura.getId());

		assertEquals(StatusFatura.PAGA, c.fatura.getStatusFatura());
		assertEquals(LocalDate.now(), c.fatura.getDataPagamento());
		assertTrue(c.parcelas.stream().allMatch(p -> p.getStatusParcela() == StatusParcela.PAGA));
	}

	@Test
	void pagarNaoAlteraOValorDaFatura() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));

		c.service.pagarFatura(c.fatura.getId());

		assertEquals(new BigDecimal("1250.00"), c.fatura.getValorTotal());
		assertEquals(new BigDecimal("750.00"), c.parcelas.get(0).getValorParcela());
		assertEquals(new BigDecimal("500.00"), c.parcelas.get(1).getValorParcela());
	}

	@Test
	void naoPermitePagarFaturaJaPaga() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));
		c.service.pagarFatura(c.fatura.getId());

		assertThrows(RuntimeException.class, () -> c.service.pagarFatura(c.fatura.getId()));
	}

	@Test
	void desfazerPagamentoVoltaParaAbertaSemData() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));
		c.service.pagarFatura(c.fatura.getId());

		c.service.desfazerPagamento(c.fatura.getId());

		assertEquals(StatusFatura.EM_ABERTO, c.fatura.getStatusFatura());
		assertNull(c.fatura.getDataPagamento());
		assertEquals(new BigDecimal("1250.00"), c.fatura.getValorTotal());
		assertTrue(c.parcelas.stream().allMatch(p -> p.getStatusParcela() == StatusParcela.PENDENTE));
	}

	@Test
	void desfazerPagamentoDeFaturaVencidaVoltaParaVencida() {
		Cenario c = new Cenario(LocalDate.now().minusDays(3));
		c.service.pagarFatura(c.fatura.getId());

		c.service.desfazerPagamento(c.fatura.getId());

		assertEquals(StatusFatura.VENCIDA, c.fatura.getStatusFatura());
		assertNull(c.fatura.getDataPagamento());
		assertTrue(c.parcelas.stream().allMatch(p -> p.getStatusParcela() == StatusParcela.ATRASADA));
	}

	@Test
	void naoPermiteDesfazerPagamentoDeFaturaNaoPaga() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));

		assertThrows(RuntimeException.class, () -> c.service.desfazerPagamento(c.fatura.getId()));
	}

	@Test
	void faturaQuitadaParcelaAParcelaRecebeDataDePagamento() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));
		c.parcelas.forEach(p -> p.setStatusParcela(StatusParcela.PAGA));

		c.service.atualizarStatus(List.of(c.fatura));

		assertEquals(StatusFatura.PAGA, c.fatura.getStatusFatura());
		assertNotNull(c.fatura.getDataPagamento());
	}

	@Test
	void faturaNaoPagaNuncaFicaComDataDePagamento() {
		Cenario c = new Cenario(LocalDate.now().plusDays(20));
		c.fatura.setDataPagamento(LocalDate.now()); // estado inconsistente gravado antes

		c.service.atualizarStatus(List.of(c.fatura));

		assertEquals(StatusFatura.EM_ABERTO, c.fatura.getStatusFatura());
		assertNull(c.fatura.getDataPagamento());
	}
}
