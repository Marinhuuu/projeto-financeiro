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

import com.gabriel.financeiro.dto.FaturasDoCiclo;
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
    // PARCELAS DAS FATURAS DO CICLO SELECIONADO
    // Regra em FaturasDoCiclo / FaturaCartaoRepository.FATURA_DO_CICLO
    // =========================================================

    @Query(value = """
        SELECT p
        FROM Parcela p
        JOIN p.fatura f
        WHERE """ + FaturaCartaoRepository.FATURA_DO_CICLO,
        countQuery = """
        SELECT COUNT(p)
        FROM Parcela p
        JOIN p.fatura f
        WHERE """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    Page<Parcela> listarDoCiclo(FaturasDoCiclo faturas, Pageable pageable);

    @Query("""
        SELECT p
        FROM Parcela p
        JOIN p.fatura f
        WHERE """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    List<Parcela> listarDoCiclo(FaturasDoCiclo faturas);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        JOIN p.fatura f
        WHERE p.statusParcela = :status
        AND """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    BigDecimal somarPorStatusDoCiclo(StatusParcela status, FaturasDoCiclo faturas);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        JOIN p.fatura f
        WHERE """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    BigDecimal somarDoCiclo(FaturasDoCiclo faturas);

    @Query("""
        SELECT COALESCE(SUM(p.valorParcela), 0)
        FROM Parcela p
        JOIN p.fatura f
        WHERE p.despesa.pessoa IS NULL
        AND """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    BigDecimal somarParcelasPropriasDoCiclo(FaturasDoCiclo faturas);

    @Query("""
        SELECT COUNT(p)
        FROM Parcela p
        JOIN p.fatura f
        WHERE p.despesa.pessoa IS NULL
        AND """ + FaturaCartaoRepository.FATURA_DO_CICLO)
    long contarParcelasPropriasDoCiclo(FaturasDoCiclo faturas);

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
