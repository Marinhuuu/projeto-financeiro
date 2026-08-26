package com.gabriel.financeiro.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.ConfiguracaoFinanceira;

public interface ConfiguracaoFinanceiraRepository
        extends JpaRepository<ConfiguracaoFinanceira, UUID> {
}