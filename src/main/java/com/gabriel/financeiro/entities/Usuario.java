package com.gabriel.financeiro.entities;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table
public class Usuario {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(nullable = false)
	private String nome;

	@Column(nullable = false)
	private String sobrenome;

	// Armazenado somente com os 11 dígitos, sem pontuação
	@Column(nullable = false, unique = true, length = 11)
	private String cpf;

	// ISO (yyyy-MM-dd) para o <input type="date"> vir preenchido na edição
	@Column(nullable = false)
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dataNascimento;

	// Somente o hash BCrypt da senha (60 caracteres); nunca a senha em texto.
	// Nulo enquanto o usuário não fez o primeiro acesso.
	@Column(length = 60)
	private String senha;

	public Usuario() {
	}

	public Usuario(UUID id, String nome, String sobrenome, String cpf, LocalDate dataNascimento) {
		this.id = id;
		this.nome = nome;
		this.sobrenome = sobrenome;
		this.cpf = cpf;
		this.dataNascimento = dataNascimento;
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

	public String getSobrenome() {
		return sobrenome;
	}

	public void setSobrenome(String sobrenome) {
		this.sobrenome = sobrenome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public LocalDate getDataNascimento() {
		return dataNascimento;
	}

	public void setDataNascimento(LocalDate dataNascimento) {
		this.dataNascimento = dataNascimento;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public boolean isPrimeiroAcessoPendente() {
		return senha == null;
	}

	public Integer getIdade() {
		if (dataNascimento == null) {
			return null;
		}
		return Period.between(dataNascimento, LocalDate.now()).getYears();
	}

	// CPF no formato 000.000.000-00 para exibição
	public String getCpfFormatado() {
		if (cpf == null || cpf.length() != 11) {
			return cpf;
		}
		return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
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
		Usuario other = (Usuario) obj;
		return Objects.equals(id, other.id);
	}

}
