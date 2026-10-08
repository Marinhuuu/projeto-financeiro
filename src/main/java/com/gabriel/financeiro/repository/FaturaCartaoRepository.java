package com.gabriel.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.dto.FaturasDoCiclo;
import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.FaturaCartao;

public interface FaturaCartaoRepository extends JpaRepository<FaturaCartao, UUID> {

    /*
     * Condição "a fatura f pertence ao ciclo" (regra em FaturasDoCiclo).
     * Exige um parâmetro FaturasDoCiclo chamado "faturas" e a fatura com alias "f".
     */
    String FATURA_DO_CICLO = """
        ((f.cartao.diaFechamento <= :#{#faturas.diaFechamento}
            AND f.mesReferencia = :#{#faturas.mes}
            AND f.anoReferencia = :#{#faturas.ano})
         OR (f.cartao.diaFechamento > :#{#faturas.diaFechamento}
            AND f.mesReferencia = :#{#faturas.mesAnterior}
            AND f.anoReferencia = :#{#faturas.anoAnterior}))
    """;

    Optional<FaturaCartao> findByCartaoAndMesReferenciaAndAnoReferencia(
            CartaoCredito cartao,
            Integer mesReferencia,
            Integer anoReferencia
    );

    // =========================================================
    // FATURAS DO CICLO SELECIONADO
    // =========================================================

    @Query("""
        SELECT f
        FROM FaturaCartao f
        WHERE """ + FATURA_DO_CICLO + """
        ORDER BY f.dataVencimento, f.cartao.nome
    """)
    List<FaturaCartao> listarDoCiclo(FaturasDoCiclo faturas);

    List<FaturaCartao> findByCartao(CartaoCredito cartao);

    List<FaturaCartao> findAllByOrderByAnoReferenciaDescMesReferenciaDesc();
}
