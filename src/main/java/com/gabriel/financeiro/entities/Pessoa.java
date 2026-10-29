package com.gabriel.financeiro.entities;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class Pessoa {

	@Id
	@GeneratedValue
	private UUID id;

	// Dono do registro: preenchido e filtrado pelo Hibernate a partir do usuário logado (UsuarioTenantResolver)
	@TenantId
	@Column(name = "usuario_id")
	private UUID usuarioId;
	private String nome;

	public Pessoa() {
	}

	public Pessoa(UUID id, String nome) {
		this.id = id;
		this.nome = nome;
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
		Pessoa other = (Pessoa) obj;
		return Objects.equals(id, other.id);
	}

}
