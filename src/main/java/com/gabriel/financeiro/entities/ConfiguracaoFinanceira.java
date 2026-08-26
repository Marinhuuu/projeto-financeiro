package com.gabriel.financeiro.entities;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_financeira")
public class ConfiguracaoFinanceira {

    @Id
    @GeneratedValue
    private UUID id;

    private Integer diaFechamento;

    public ConfiguracaoFinanceira() {
    }

    public ConfiguracaoFinanceira(UUID id, Integer diaFechamento) {
        this.id = id;
        this.diaFechamento = diaFechamento;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getDiaFechamento() {
        return diaFechamento;
    }

    public void setDiaFechamento(Integer diaFechamento) {
        this.diaFechamento = diaFechamento;
    }
}