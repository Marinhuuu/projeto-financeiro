package com.gabriel.financeiro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.gabriel.financeiro.enums.StatusFatura;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table
public class FaturaCartao {

	@Id
	@GeneratedValue
	private UUID id;

	private Integer mesReferencia;
	private Integer anoReferencia;
	private LocalDate dataFechamento;
	private LocalDate dataVencimento;
	private BigDecimal valorTotal;

	@Enumerated(EnumType.STRING)
	private StatusFatura statusFatura;

	@ManyToOne
	@JoinColumn(name = "cartaoId", nullable = false)
	private CartaoCredito cartao;

	@OneToMany(mappedBy = "fatura")
	private List<Parcela> parcelas = new ArrayList<>();

	public FaturaCartao() {
	}

	public FaturaCartao(UUID id, Integer mesReferencia, Integer anoReferencia, LocalDate dataFechamento, LocalDate dataVencimento,
			BigDecimal valorTotal, StatusFatura statusFatura, CartaoCredito cartao) {
		this.id = id;
		this.mesReferencia = mesReferencia;
		this.anoReferencia = anoReferencia;
		this.dataFechamento = dataFechamento;
		this.dataVencimento = dataVencimento;
		this.valorTotal = valorTotal;
		this.statusFatura = statusFatura;
		this.cartao = cartao;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Integer getMesReferencia() {
		return mesReferencia;
	}

	public void setMesReferencia(Integer mesReferencia) {
		this.mesReferencia = mesReferencia;
	}

	public Integer getAnoReferencia() {
		return anoReferencia;
	}

	public void setAnoReferencia(Integer anoReferencia) {
		this.anoReferencia = anoReferencia;
	}

	public LocalDate getDataFechamento() {
		return dataFechamento;
	}

	public void setDataFechamento(LocalDate dataFechamento) {
		this.dataFechamento = dataFechamento;
	}

	public LocalDate getDataVencimento() {
		return dataVencimento;
	}

	public void setDataVencimento(LocalDate dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public StatusFatura getStatusFatura() {
		return statusFatura;
	}

	public void setStatusFatura(StatusFatura statusFatura) {
		this.statusFatura = statusFatura;
	}

	public CartaoCredito getCartao() {
		return cartao;
	}

	public void setCartao(CartaoCredito cartao) {
		this.cartao = cartao;
	}

	public List<Parcela> getParcelas() {
		return parcelas;
	}

	public void setParcelas(List<Parcela> parcelas) {
		this.parcelas = parcelas;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FaturaCartao other = (FaturaCartao) obj;
		return Objects.equals(id, other.id);
	}

}
