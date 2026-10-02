package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusParcela;

public interface ParcelaRepository extends JpaRepository<Parcela, UUID> {

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.statusParcela = :status
    """)
    BigDecimal somarPorStatus(StatusParcela status);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.statusParcela = :status
        AND p.despesa.pessoa IS NULL
        AND p.despesa.devolvido = false
    """)
    BigDecimal somarParcelasMinhas(StatusParcela status);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.statusParcela = :status
        AND p.despesa.pessoa IS NOT NULL
        AND p.despesa.devolvido = false
    """)
    BigDecimal somarParcelasTerceiros(StatusParcela status);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
        AND p.despesa.pessoa IS NULL
    """)
    BigDecimal somarParcelasPropriasPorMesEAno(Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
        AND p.despesa.pessoa IS NOT NULL
    """)
    BigDecimal somarParcelasTerceirosPorMesEAno(Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
    """)
    BigDecimal somarTodasParcelasPorMesEAno(Integer mes, Integer ano);

    List<Parcela> findByDespesa(Despesa despesa);
}