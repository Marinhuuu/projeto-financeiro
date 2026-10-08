package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.dto.FaturasDoCiclo;
import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.Pessoa;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {

    // =========================================================
    // TOTAL DE DESPESAS PESSOAIS FORA DO CARTÃO NO PERÍODO
    // Pessoa = null. "Fora do cartão" = despesa sem parcelas (as do cartão
    // entram pelas parcelas da fatura do mês, não pela data da compra).
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(d.valorTotal), 0)
        FROM Despesa d
        WHERE d.pessoa IS NULL
          AND NOT EXISTS (SELECT 1 FROM Parcela p WHERE p.despesa = d)
          AND d.dataCompra BETWEEN :inicio AND :fim
    """)
    BigDecimal somarDespesasPessoaisNaoCartaoPorPeriodo(LocalDate inicio, LocalDate fim);

    // =========================================================
    // LISTAR DESPESAS DE TERCEIROS
    // Pessoa preenchida
    // =========================================================

    @Query("""
        SELECT d
        FROM Despesa d
        WHERE d.pessoa IS NOT NULL
        ORDER BY d.dataCompra DESC
    """)
    List<Despesa> listarDespesasDeTerceiros();

    @Query("""
        SELECT d
        FROM Despesa d
        WHERE d.pessoa IS NOT NULL
          AND """ + DESPESA_DO_CICLO + """
        ORDER BY d.dataCompra DESC
    """)
    List<Despesa> listarDespesasDeTerceirosDoCiclo(LocalDate inicio, LocalDate fim, FaturasDoCiclo faturas);

    // =========================================================
    // DESPESAS DO CICLO SELECIONADO
    // Fora do cartão (sem parcelas): pela data da compra.
    // No cartão: pela fatura da 1ª parcela, que segue o ciclo do cartão
    // (regra em FaturasDoCiclo). Exige :inicio, :fim e "faturas".
    // =========================================================

    String DESPESA_DO_CICLO = """
        ((NOT EXISTS (SELECT 1 FROM Parcela p0 WHERE p0.despesa = d)
            AND d.dataCompra BETWEEN :inicio AND :fim)
         OR EXISTS (SELECT 1 FROM Parcela p1 JOIN p1.fatura f
            WHERE p1.despesa = d AND p1.qtdParcela = 1
            AND """ + FaturaCartaoRepository.FATURA_DO_CICLO + """
         ))
    """;

    @Query(value = """
        SELECT d
        FROM Despesa d
        WHERE """ + DESPESA_DO_CICLO,
        countQuery = """
        SELECT COUNT(d)
        FROM Despesa d
        WHERE """ + DESPESA_DO_CICLO)
    Page<Despesa> listarDoCiclo(LocalDate inicio, LocalDate fim, FaturasDoCiclo faturas, Pageable pageable);

    // =========================================================
    // DESPESAS FORA DO CARTÃO (sem parcelas) — gastos do ciclo
    // =========================================================

    @Query("""
        SELECT d
        FROM Despesa d
        WHERE NOT EXISTS (SELECT 1 FROM Parcela p WHERE p.despesa = d)
          AND d.dataCompra BETWEEN :inicio AND :fim
    """)
    List<Despesa> listarDespesasSemParcelasPorPeriodo(LocalDate inicio, LocalDate fim);

    @Query("""
        SELECT d
        FROM Despesa d
        WHERE NOT EXISTS (SELECT 1 FROM Parcela p WHERE p.despesa = d)
    """)
    List<Despesa> listarDespesasSemParcelas();

    // =========================================================
    // VÍNCULOS (usados antes de excluir cadastros)
    // =========================================================

    List<Despesa> findByCartao(CartaoCredito cartao);

    // =========================================================
    // RECORRÊNCIA
    // =========================================================

    List<Despesa> findByGrupoRecorrenciaAndOcorrenciaGreaterThanEqualOrderByOcorrenciaAsc(
            UUID grupoRecorrencia, Integer ocorrencia);

    boolean existsByCartao(CartaoCredito cartao);

    boolean existsByCategoria(Categoria categoria);

    boolean existsByPessoa(Pessoa pessoa);
}
