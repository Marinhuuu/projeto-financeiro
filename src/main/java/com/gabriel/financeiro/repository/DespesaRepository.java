package com.gabriel.financeiro.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.Despesa;

public interface DespesaRepository extends JpaRepository<Despesa, UUID> {

    // =========================================================
    // TOTAL DE TODAS AS DESPESAS
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(d.valorTotal), 0)
        FROM Despesa d
    """)
    BigDecimal somarDespesas();


    // =========================================================
    // TOTAL DE DESPESAS PESSOAIS
    // Pessoa = null
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(d.valorTotal), 0)
        FROM Despesa d
        WHERE d.pessoa IS NULL
    """)
    BigDecimal somarDespesasPessoais();


    // =========================================================
    // TOTAL DE DESPESAS DE TERCEIROS
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
    // TOTAL QUE TERCEIROS AINDA DEVEM
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(d.valorTotal), 0)
        FROM Despesa d
        WHERE d.pessoa IS NOT NULL
          AND d.devolvido = false
    """)
    BigDecimal somarTerceirosPendentes();


    
    @Query("""
            SELECT COALESCE(SUM(d.valorTotal), 0)
            FROM Despesa d
            WHERE d.pessoa IS NOT NULL
        """)
        BigDecimal somarDespesasDeTerceiros();
    
    
    @Query("""
            SELECT COUNT(d)
            FROM Despesa d
            WHERE d.pessoa IS NOT NULL
            AND d.devolvido = false
        """)
        long contarDespesasDeTerceiros();
    // =========================================================
    // BUSCAR DESPESA PELA DESCRIÇÃO
    // =========================================================

    Optional<Despesa> findByDescricaoIgnoreCase(
            String descricao
    );


    // =========================================================
    // LISTAR DESPESAS DE TERCEIROS
    // =========================================================

    List<Despesa>
    findByPessoaIsNotNullOrderByDataCompraDesc();
}