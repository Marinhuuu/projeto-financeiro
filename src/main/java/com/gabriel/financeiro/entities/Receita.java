package com.gabriel.financeiro.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.TenantId;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table
public class Receita {

	@Id
	@GeneratedValue
	private UUID id;

	// Dono do registro: preenchido e filtrado pelo Hibernate a partir do usuário logado (UsuarioTenantResolver)
	@TenantId
	@Column(name = "usuario_id")
	private UUID usuarioId;

	private String descricao;

	private BigDecimal valor;

	// ISO (yyyy-MM-dd) para o <input type="date"> vir preenchido na edição
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate dataEntrada;

	private Boolean recorrente;

	// ===== RECORRÊNCIA =====
	// Receitas geradas juntas por "repetir mensalmente" compartilham o mesmo grupo.
	private UUID grupoRecorrencia;

	// Posição desta receita na recorrência (1..totalOcorrencias)
	private Integer ocorrencia;

	private Integer totalOcorrencias;

	@ManyToOne
	@JoinColumn(name = "categoriaId", nullable = false)
	private Categoria categoria;

	@ManyToOne
	@JoinColumn(name = "ciclo_id")
	private CicloFinanceiro ciclo;

	public Receita() {
	}

	public Receita(UUID id, String descricao, BigDecimal valor, LocalDate dataEntrada, Boolean recorrente,
			Categoria categoria, CicloFinanceiro ciclo) {
		this.id = id;
		this.descricao = descricao;
		this.valor = valor;
		this.dataEntrada = dataEntrada;
		this.recorrente = recorrente;
		this.categoria = categoria;
		this.ciclo = ciclo;
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

	public BigDecimal getValor() {
		return valor;
	}

	public void setValor(BigDecimal valor) {
		this.valor = valor;
	}

	public LocalDate getDataEntrada() {
		return dataEntrada;
	}

	public void setDataEntrada(LocalDate dataEntrada) {
		this.dataEntrada = dataEntrada;
	}

	public Boolean getRecorrente() {
		return recorrente;
	}

	public void setRecorrente(Boolean recorrente) {
		this.recorrente = recorrente;
	}

	public Categoria getCategoria() {
		return categoria;
	}

	public void setCategoria(Categoria categoria) {
		this.categoria = categoria;
	}

	public CicloFinanceiro getCiclo() {
		return ciclo;
	}

	public void setCiclo(CicloFinanceiro ciclo) {
		this.ciclo = ciclo;
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

	public boolean isParteDeRecorrencia() {
		return grupoRecorrencia != null;
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

		Receita other = (Receita) obj;

		return Objects.equals(id, other.id);
	}
}