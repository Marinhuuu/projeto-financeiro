package com.gabriel.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.FaturaCartao;

public interface FaturaCartaoRepository extends JpaRepository<FaturaCartao, UUID> {

    Optional<FaturaCartao> findByCartaoAndMesReferenciaAndAnoReferencia(
            CartaoCredito cartao,
            Integer mesReferencia,
            Integer anoReferencia
    );

    List<FaturaCartao> findByMesReferenciaAndAnoReferencia(
            Integer mesReferencia,
            Integer anoReferencia
    );

    List<FaturaCartao> findByCartao(CartaoCredito cartao);

    List<FaturaCartao> findAllByOrderByAnoReferenciaDescMesReferenciaDesc();
}
