package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.FaturaCartao;
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
    """)
    BigDecimal somarTodas();

    // =========================================================
    // PARCELAS DAS FATURAS DE UM MÊS DE REFERÊNCIA (ciclo selecionado)
    // =========================================================

    Page<Parcela> findByFaturaMesReferenciaAndFaturaAnoReferencia(Integer mes, Integer ano, Pageable pageable);

    List<Parcela> findByFaturaMesReferenciaAndFaturaAnoReferencia(Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.statusParcela = :status
        AND p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
    """)
    BigDecimal somarPorStatusEMesEAno(StatusParcela status, Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
    """)
    BigDecimal somarPorMesEAno(Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
        AND p.despesa.pessoa IS NULL
    """)
    BigDecimal somarParcelasPropriasPorMesEAno(Integer mes, Integer ano);

    @Query("""
        SELECT COUNT(p)
        FROM Parcela p
        WHERE p.fatura.mesReferencia = :mes
        AND p.fatura.anoReferencia = :ano
        AND p.despesa.pessoa IS NULL
    """)
    long contarParcelasPropriasPorMesEAno(Integer mes, Integer ano);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        WHERE p.fatura = :fatura
    """)
    BigDecimal somarPorFatura(FaturaCartao fatura);

    // =========================================================
    // ATUALIZA PENDENTES VENCIDAS PARA ATRASADA
    // =========================================================

    @Modifying
    @Query("""
        UPDATE Parcela p
        SET p.statusParcela = :atrasada
        WHERE p.statusParcela = :pendente
        AND p.dataVencimento < :hoje
    """)
    int marcarAtrasadas(StatusParcela pendente, StatusParcela atrasada, LocalDate hoje);

    List<Parcela> findByDespesa(Despesa despesa);

    List<Parcela> findByDespesaOrderByQtdParcelaAsc(Despesa despesa);

    List<Parcela> findByFatura(FaturaCartao fatura);

    long countByFatura(FaturaCartao fatura);

    long countByFaturaAndStatusParcelaNot(FaturaCartao fatura, StatusParcela status);
}
