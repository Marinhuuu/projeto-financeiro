package com.gabriel.financeiro.entities;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ciclo_financeiro")
public class CicloFinanceiro {

    @Id
    @GeneratedValue
    private UUID id;

    private LocalDate dataInicio;

    private LocalDate dataFim;

    private Boolean fechado;

    public CicloFinanceiro() {
    }

    public CicloFinanceiro(
            UUID id,
            LocalDate dataInicio,
            LocalDate dataFim,
            Boolean fechado) {

        this.id = id;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.fechado = fechado;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public Boolean getFechado() {
        return fechado;
    }

    public void setFechado(Boolean fechado) {
        this.fechado = fechado;
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

        CicloFinanceiro other = (CicloFinanceiro) obj;

        return Objects.equals(id, other.id);
    }
}