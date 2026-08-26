package com.gabriel.financeiro.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table
public class CartaoCredito {

	@Id
	@GeneratedValue
	private UUID id;
	private String nome;
	private BigDecimal limite;
	private Integer diaFechamento;
	private Integer diaVencimento;

	@OneToMany(mappedBy = "cartao")
	List<Despesa> despesas = new ArrayList<>();
	
	@OneToMany(mappedBy = "cartao")
	private List<FaturaCartao> faturas = new ArrayList<>();

	public CartaoCredito() {
	}

	public CartaoCredito(UUID id, String nome, BigDecimal limite, Integer diaFechamento, Integer diaVencimento,
			List<Despesa> despesas, List<FaturaCartao> faturas) {
		this.id = id;
		this.nome = nome;
		this.limite = limite;
		this.diaFechamento = diaFechamento;
		this.diaVencimento = diaVencimento;
		this.despesas = despesas;
		this.faturas = faturas;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public BigDecimal getLimite() {
		return limite;
	}

	public void setLimite(BigDecimal limite) {
		this.limite = limite;
	}

	public Integer getDiaFechamento() {
		return diaFechamento;
	}

	public void setDiaFechamento(Integer diaFechamento) {
		this.diaFechamento = diaFechamento;
	}

	public Integer getDiaVencimento() {
		return diaVencimento;
	}

	public void setDiaVencimento(Integer diaVencimento) {
		this.diaVencimento = diaVencimento;
	}

	public List<Despesa> getDespesas() {
		return despesas;
	}

	public void setDespesas(List<Despesa> despesas) {
		this.despesas = despesas;
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
		CartaoCredito other = (CartaoCredito) obj;
		return Objects.equals(id, other.id);
	}

}
