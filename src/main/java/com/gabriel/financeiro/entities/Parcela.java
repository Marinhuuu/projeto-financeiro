package com.gabriel.financeiro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.gabriel.financeiro.enums.StatusParcela;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table
public class Parcela {

	@Id
	@GeneratedValue
	private UUID id;

	// Dono do registro: preenchido e filtrado pelo Hibernate a partir do usuário logado (UsuarioTenantResolver)
	@TenantId
	@Column(name = "usuario_id")
	private UUID usuarioId;
	private Integer qtdParcela;
	private BigDecimal valorParcela;
	private LocalDate dataVencimento;

	@Enumerated(EnumType.STRING)
	private StatusParcela statusParcela;

	@ManyToOne
	@JoinColumn(name = "despesaId", nullable = false)
	private Despesa despesa;

	@ManyToOne
	@JoinColumn(name = "fatura_id", nullable = true)
	private FaturaCartao fatura;

	public Parcela() {
	}

	public Parcela(UUID id, Integer qtdParcela, BigDecimal valorParcela, LocalDate dataVencimento,
			StatusParcela statusParcela) {
		this.id = id;
		this.qtdParcela = qtdParcela;
		this.valorParcela = valorParcela;
		this.dataVencimento = dataVencimento;
		this.statusParcela = statusParcela;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Integer getQtdParcela() {
		return qtdParcela;
	}

	public void setQtdParcela(Integer qtdParcela) {
		this.qtdParcela = qtdParcela;
	}

	public BigDecimal getValorParcela() {
		return valorParcela;
	}

	public void setValorParcela(BigDecimal valorParcela) {
		this.valorParcela = valorParcela;
	}

	public LocalDate getDataVencimento() {
		return dataVencimento;
	}

	public void setDataVencimento(LocalDate dataVencimento) {
		this.dataVencimento = dataVencimento;
	}

	public StatusParcela getStatusParcela() {
		return statusParcela;
	}

	public void setStatusParcela(StatusParcela statusParcela) {
		this.statusParcela = statusParcela;
	}

	public Despesa getDespesa() {
		return despesa;
	}

	public void setDespesa(Despesa despesa) {
		this.despesa = despesa;
	}

	public FaturaCartao getFatura() {
		return fatura;
	}

	public void setFatura(FaturaCartao fatura) {
		this.fatura = fatura;
	}

	public UUID getUsuarioId() {
		return usuarioId;
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
		Parcela other = (Parcela) obj;
		return Objects.equals(id, other.id);
	}

}
