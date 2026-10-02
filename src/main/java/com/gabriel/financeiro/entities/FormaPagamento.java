package com.gabriel.financeiro.entities;

import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "forma_pagamento")
public class FormaPagamento {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false, unique = true)
	private String nome;

	@Column(nullable = false, unique = true)
	private String codigo;

	private Boolean permiteParcelamento = false;

	public FormaPagamento() {
	}

	public FormaPagamento(UUID id, String nome, String codigo, Boolean permiteParcelamento) {
		this.id = id;
		this.nome = nome;
		this.codigo = codigo;
		this.permiteParcelamento = permiteParcelamento != null ? permiteParcelamento : false;
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

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public Boolean getPermiteParcelamento() {
		return permiteParcelamento;
	}

	public void setPermiteParcelamento(Boolean permiteParcelamento) {
		this.permiteParcelamento = permiteParcelamento;
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
		FormaPagamento other = (FormaPagamento) obj;
		return Objects.equals(id, other.id);
	}
}
