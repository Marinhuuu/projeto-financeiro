package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.Receita;

public interface ReceitaRepository extends JpaRepository<Receita, UUID> {

    @Query("SELECT COALESCE(SUM(r.valor), 0) FROM Receita r")
    BigDecimal somarReceitas();
    
    List<Receita> findByDataEntradaBetween(LocalDate inicio, LocalDate fim);

}