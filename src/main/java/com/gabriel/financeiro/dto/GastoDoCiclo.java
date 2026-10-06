package com.gabriel.financeiro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.Parcela;

/*
 * Linha da listagem consolidada de gastos do ciclo: uma despesa fora do cartão
 * (valor total) ou uma parcela do cartão (valor da parcela) da fatura do ciclo.
 */
public class GastoDoCiclo {

	private final Despesa despesa;
	private final Parcela parcela;

	private GastoDoCiclo(Despesa despesa, Parcela parcela) {
		this.despesa = despesa;
		this.parcela = parcela;
	}

	public static GastoDoCiclo deDespesa(Despesa despesa) {
		return new GastoDoCiclo(despesa, null);
	}

	public static GastoDoCiclo deParcela(Parcela parcela) {
		return new GastoDoCiclo(parcela.getDespesa(), parcela);
	}

	public Despesa getDespesa() {
		return despesa;
	}

	public Parcela getParcela() {
		return parcela;
	}

	public boolean isDeParcela() {
		return parcela != null;
	}

	public BigDecimal getValor() {
		return isDeParcela() ? parcela.getValorParcela() : despesa.getValorTotal();
	}

	public LocalDate getData() {
		return despesa.getDataCompra();
	}

	public Integer getNumeroParcela() {
		return isDeParcela() ? parcela.getQtdParcela() : null;
	}
}
