package com.gabriel.financeiro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table
public class Despesa {

	@Id
	@GeneratedValue
	private UUID id;

	private String descricao;

	private BigDecimal valorTotal;

	private LocalDate dataCompra;

	@ManyToOne
	@JoinColumn(name = "forma_pagamento_id", nullable = true)
	private FormaPagamento formaPagamento;

	private Integer qtdParcelas;
	
	private Boolean devolvido = false;

	@ManyToOne
	@JoinColumn(name = "categoriaId", nullable = false)
	private Categoria categoria;

	@ManyToOne
	@JoinColumn(name = "cartaoId", nullable = true)
	private CartaoCredito cartao;

	@ManyToOne
	@JoinColumn(name = "ciclo_id")
	private CicloFinanceiro ciclo;

	@OneToMany(mappedBy = "despesa")
	private List<Parcela> parcelas = new ArrayList<>();
	
	@ManyToOne
	@JoinColumn(name = "pessoa_id", nullable = true)
	private Pessoa pessoa;

	// ===== RECORRÊNCIA =====
	// Despesas geradas juntas por "repetir mensalmente" compartilham o mesmo grupo.
	private UUID grupoRecorrencia;

	// Posição desta despesa na recorrência (1..totalOcorrencias)
	private Integer ocorrencia;

	private Integer totalOcorrencias;

	// Só para o formulário: marca que a despesa deve se repetir por totalOcorrencias meses
	@Transient
	private Boolean recorrente;

	public Despesa() {
	}

	public Despesa(UUID id, String descricao, BigDecimal valorTotal, LocalDate dataCompra,
			FormaPagamento formaPagamento, Integer qtdParcelas, Categoria categoria) {

		this.id = id;
		this.descricao = descricao;
		this.valorTotal = valorTotal;
		this.dataCompra = dataCompra;
		this.formaPagamento = formaPagamento;
		this.qtdParcelas = qtdParcelas;
		this.categoria = categoria;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public BigDecimal getValorTotal() {
		return valorTotal;
	}

	public void setValorTotal(BigDecimal valorTotal) {
		this.valorTotal = valorTotal;
	}

	public LocalDate getDataCompra() {
		return dataCompra;
	}

	public void setDataCompra(LocalDate dataCompra) {
		this.dataCompra = dataCompra;
	}

	public FormaPagamento getFormaPagamento() {
		return formaPagamento;
	}

	public void setFormaPagamento(FormaPagamento formaPagamento) {
		this.formaPagamento = formaPagamento;
	}

	public Integer getQtdParcelas() {
		return qtdParcelas;
	}

	public void setQtdParcelas(Integer qtdParcelas) {
		this.qtdParcelas = qtdParcelas;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public CartaoCredito getCartao() {
		return cartao;
	}

	public void setCartao(CartaoCredito cartao) {
		this.cartao = cartao;
	}

	public CicloFinanceiro getCiclo() {
		return ciclo;
	}

	public void setCiclo(CicloFinanceiro ciclo) {
		this.ciclo = ciclo;
	}

	public List<Parcela> getParcelas() {
		return parcelas;
	}

	public void setParcelas(List<Parcela> parcelas) {
		this.parcelas = parcelas;
	}
	
	public Pessoa getPessoa() {
	    return pessoa;
	}

	public void setPessoa(Pessoa pessoa) {
	    this.pessoa = pessoa;
	}
	
	public Boolean getDevolvido() {
	    return devolvido;
	}

	public void setDevolvido(Boolean devolvido) {
	    this.devolvido = devolvido;
	}

	public UUID getGrupoRecorrencia() {
		return grupoRecorrencia;
	}

	public void setGrupoRecorrencia(UUID grupoRecorrencia) {
		this.grupoRecorrencia = grupoRecorrencia;
	}

	public Integer getOcorrencia() {
		return ocorrencia;
	}

	public void setOcorrencia(Integer ocorrencia) {
		this.ocorrencia = ocorrencia;
	}

	public Integer getTotalOcorrencias() {
		return totalOcorrencias;
	}

	public void setTotalOcorrencias(Integer totalOcorrencias) {
		this.totalOcorrencias = totalOcorrencias;
	}

	public Boolean getRecorrente() {
		return recorrente;
	}

	public void setRecorrente(Boolean recorrente) {
		this.recorrente = recorrente;
	}

	public boolean isParteDeRecorrencia() {
		return grupoRecorrencia != null;
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

		Despesa other = (Despesa) obj;

		return Objects.equals(id, other.id);
	}
}