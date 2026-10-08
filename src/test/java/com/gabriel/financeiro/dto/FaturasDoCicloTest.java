package com.gabriel.financeiro.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.ConfiguracaoFinanceira;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.repository.CartaoRepository;
import com.gabriel.financeiro.service.CicloFinanceiroService;
import com.gabriel.financeiro.service.ConfiguracaoFinanceiraService;
import com.gabriel.financeiro.service.FaturaCartaoService;

class FaturasDoCicloTest {

	private final FaturaCartaoService faturaService = new FaturaCartaoService(null, null, null, null);

	/* Ciclo financeiro real (CicloFinanceiroService) para o dia de fechamento global informado */
	private CicloFinanceiroService cicloService(int diaFechamentoGlobal) {
		ConfiguracaoFinanceiraService config = mock(ConfiguracaoFinanceiraService.class);
		when(config.getConfiguracao()).thenReturn(new ConfiguracaoFinanceira(null, diaFechamentoGlobal));
		return new CicloFinanceiroService(null, config, semCartoes());
	}

	private CartaoRepository semCartoes() {
		CartaoRepository cartaoRepo = mock(CartaoRepository.class);
		when(cartaoRepo.findAll()).thenReturn(List.of());
		return cartaoRepo;
	}

	// =========================================================
	// DIA DE FECHAMENTO DO CICLO = DIA DOS CARTÕES
	// =========================================================

	@Test
	void cicloSegueODiaDeFechamentoDoCartao() {
		CicloFinanceiroService service = cicloService(25); // configuração antiga (padrão 25)

		assertEquals(5, service.getDiaFechamento(List.of(cartao(5))));
		assertEquals(25, service.getDiaFechamento(List.of())); // sem cartão: configuração
		assertEquals(5, service.getDiaFechamento(List.of(cartao(5), cartao(5), cartao(20)))); // mais comum
		assertEquals(5, service.getDiaFechamento(List.of(cartao(20), cartao(5)))); // empate: menor
	}

	@Test
	void comCartaoQueFechaDia5OCicloVaiDo6AoDia5() {
		ConfiguracaoFinanceiraService config = mock(ConfiguracaoFinanceiraService.class);
		when(config.getConfiguracao()).thenReturn(new ConfiguracaoFinanceira(null, 25));
		CartaoRepository cartaoRepo = mock(CartaoRepository.class);
		when(cartaoRepo.findAll()).thenReturn(List.of(cartao(5)));
		CicloFinanceiroService service = new CicloFinanceiroService(null, config, cartaoRepo);

		CicloFinanceiro hoje = service.calcularCiclo(LocalDate.of(2026, 10, 8));
		assertEquals(LocalDate.of(2026, 10, 6), hoje.getDataInicio());
		assertEquals(LocalDate.of(2026, 11, 5), hoje.getDataFim());

		CicloFinanceiro dezembro = service.calcularCiclo(LocalDate.of(2026, 12, 5));
		assertEquals(LocalDate.of(2026, 11, 6), dezembro.getDataInicio());
		assertEquals(LocalDate.of(2026, 12, 5), dezembro.getDataFim());

		CicloFinanceiro janeiro = service.calcularCiclo(LocalDate.of(2026, 12, 6));
		assertEquals(LocalDate.of(2026, 12, 6), janeiro.getDataInicio());
		assertEquals(LocalDate.of(2027, 1, 5), janeiro.getDataFim());

		// A fatura do cartão no ciclo é a que tem o mesmo período
		assertEquals(YearMonth.of(2026, 12), service.faturasDoCiclo(dezembro).referenciaDaFatura(5));
	}

	private CicloFinanceiro ciclo(int diaFechamentoGlobal, YearMonth referencia) {
		return ciclo(cicloService(diaFechamentoGlobal), diaFechamentoGlobal, referencia);
	}

	private CicloFinanceiro ciclo(CicloFinanceiroService service, int diaFechamentoGlobal, YearMonth referencia) {
		return service.calcularCiclo(referencia.atDay(Math.min(diaFechamentoGlobal, referencia.lengthOfMonth())));
	}

	private CartaoCredito cartao(int diaFechamento) {
		return new CartaoCredito(null, "Teste", BigDecimal.ZERO, diaFechamento, 10, null, null);
	}

	private FaturaCartao fatura(CartaoCredito cartao, YearMonth referencia) {
		return new FaturaCartao(null, referencia.getMonthValue(), referencia.getYear(),
				faturaService.calcularDataFechamento(cartao, referencia),
				faturaService.calcularDataVencimento(cartao, referencia), BigDecimal.ZERO, null, cartao);
	}

	@Test
	void cartaoQueFechaNoMesmoDiaDoCicloUsaAFaturaDoMes() {
		// Ciclo 06/09/2026 a 05/10/2026
		CicloFinanceiro c = ciclo(5, YearMonth.of(2026, 10));
		assertEquals(LocalDate.of(2026, 9, 6), c.getDataInicio());

		FaturasDoCiclo faturas = FaturasDoCiclo.de(c, 5);

		assertEquals(YearMonth.of(2026, 10), faturas.referenciaDaFatura(5));
		assertEquals(YearMonth.of(2026, 10), faturas.referenciaDaFatura(1)); // fecha 01/10
	}

	@Test
	void cartaoQueFechaDepoisDoCicloUsaAFaturaQueFechaDentroDele() {
		// Ciclo 06/09 a 05/10: o cartão que fecha dia 25 entra com a fatura que fecha em 25/09
		FaturasDoCiclo faturas = FaturasDoCiclo.de(ciclo(5, YearMonth.of(2026, 10)), 5);

		assertEquals(YearMonth.of(2026, 9), faturas.referenciaDaFatura(25));
		assertTrue(faturas.contem(fatura(cartao(25), YearMonth.of(2026, 9))));
		assertFalse(faturas.contem(fatura(cartao(25), YearMonth.of(2026, 10))));
		assertTrue(faturas.contem(fatura(cartao(5), YearMonth.of(2026, 10))));
	}

	@Test
	void viradaDeAno() {
		// Ciclo 06/12/2026 a 05/01/2027
		FaturasDoCiclo faturas = FaturasDoCiclo.de(ciclo(5, YearMonth.of(2027, 1)), 5);

		assertEquals(YearMonth.of(2027, 1), faturas.referenciaDaFatura(5));
		assertEquals(YearMonth.of(2026, 12), faturas.referenciaDaFatura(25));
		assertEquals(12, faturas.getMesAnterior());
		assertEquals(2026, faturas.getAnoAnterior());
	}

	/*
	 * Para todo fechamento global e de cartão (1–31) e todo ciclo de 2026 a 2028:
	 * - cada ciclo tem exatamente uma fatura de cada cartão (ciclos seguidos → faturas seguidas);
	 * - sem ajuste de fim de mês (dias até 28), a fatura fecha dentro do ciclo.
	 */
	@Test
	void cadaCicloTemUmaFaturaDeCadaCartaoQueFechaDentroDele() {

		for (int global = 1; global <= 31; global++) {

			CicloFinanceiroService cicloService = cicloService(global);

			for (int diaCartao = 1; diaCartao <= 31; diaCartao++) {

				CartaoCredito cartao = cartao(diaCartao);
				YearMonth anterior = null;

				for (YearMonth ref = YearMonth.of(2026, 1); ref.isBefore(YearMonth.of(2029, 1)); ref = ref.plusMonths(1)) {

					CicloFinanceiro c = ciclo(cicloService, global, ref);
					YearMonth refFatura = FaturasDoCiclo.de(c, global).referenciaDaFatura(diaCartao);
					String msg = "global " + global + ", cartão " + diaCartao + ", ciclo " + ref;

					if (anterior != null) {
						assertEquals(anterior.plusMonths(1), refFatura, msg);
					}
					anterior = refFatura;

					if (global <= 28 && diaCartao <= 28) {
						LocalDate fechamento = faturaService.calcularDataFechamento(cartao, refFatura);
						assertFalse(fechamento.isBefore(c.getDataInicio()), msg);
						assertFalse(fechamento.isAfter(c.getDataFim()), msg);
					}
				}
			}
		}
	}
}
