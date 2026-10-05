package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.Pessoa;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {

    // =========================================================
    // TOTAL DE DESPESAS PESSOAIS FORA DO CARTÃO NO PERÍODO
    // Pessoa = null
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(d.valorTotal), 0)
        FROM Despesa d
        WHERE d.pessoa IS NULL
          AND d.cartao IS NULL
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
