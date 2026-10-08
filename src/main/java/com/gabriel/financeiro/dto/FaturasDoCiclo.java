package com.gabriel.financeiro.dto;

import java.time.YearMonth;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.FaturaCartao;

/*
 * Quais faturas de cartão pertencem a um ciclo financeiro.
 *
 * Cada cartão tem o próprio ciclo (dia seguinte ao fechamento anterior até o
 * fechamento). A fatura entra no ciclo financeiro em que ela fecha:
 * - cartão que fecha até o dia de fechamento do ciclo → fatura do mesmo mês do ciclo;
 * - cartão que fecha depois → fatura do mês anterior (fecha dentro deste ciclo).
 * Ex.: ciclo 06/09 a 05/10 (fecha dia 5) → cartão que fecha dia 5: fatura de 05/10;
 *      cartão que fecha dia 25: fatura de 25/09.
 * Compara os dias configurados (sem o ajuste ao fim do mês) para que cada ciclo
 * tenha exatamente uma fatura de cada cartão, inclusive em fevereiro.
 *
 * Os getters são usados nas queries JPQL via SpEL (:#{#faturas.mes}).
 */
public final class FaturasDoCiclo {

	private final YearMonth referencia;
	private final int diaFechamento;

	private FaturasDoCiclo(YearMonth referencia, int diaFechamento) {
		this.referencia = referencia;
		this.diaFechamento = diaFechamento;
	}

	/*
	 * ciclo: o ciclo selecionado (referência = mês em que termina);
	 * diaFechamento: dia de fechamento da ConfiguracaoFinanceira.
	 */
	public static FaturasDoCiclo de(CicloFinanceiro ciclo, int diaFechamento) {
		return new FaturasDoCiclo(YearMonth.from(ciclo.getDataFim()), diaFechamento);
	}

	/*
	 * Mês de referência da fatura do cartão que pertence a este ciclo.
	 */
	public YearMonth referenciaDaFatura(int diaFechamentoCartao) {
		return diaFechamentoCartao <= diaFechamento ? referencia : referencia.minusMonths(1);
	}

	public boolean contem(FaturaCartao fatura) {
		return YearMonth.of(fatura.getAnoReferencia(), fatura.getMesReferencia())
				.equals(referenciaDaFatura(fatura.getCartao().getDiaFechamento()));
	}

	public int getMes() {
		return referencia.getMonthValue();
	}

	public int getAno() {
		return referencia.getYear();
	}

	public int getMesAnterior() {
		return referencia.minusMonths(1).getMonthValue();
	}

	public int getAnoAnterior() {
		return referencia.minusMonths(1).getYear();
	}

	public int getDiaFechamento() {
		return diaFechamento;
	}
}
