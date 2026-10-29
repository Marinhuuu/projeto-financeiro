package com.gabriel.financeiro.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.gabriel.financeiro.entities.CicloFinanceiro;

public interface CicloFinanceiroRepository
        extends JpaRepository<CicloFinanceiro, UUID> {

    // Busca pelas datas exatas: ciclos gravados com outro dia de fechamento
    // (ex.: o padrão antigo 25) não são reaproveitados. findFirst tolera duplicados.
    Optional<CicloFinanceiro>
    findFirstByDataInicioAndDataFim(
            LocalDate dataInicio,
            LocalDate dataFim);

    // Um único UPDATE em vez de carregar e salvar ciclo por ciclo
    @Modifying
    @Query("""
        UPDATE CicloFinanceiro c
        SET c.fechado = true
        WHERE c.dataFim < :data
          AND c.fechado = false
    """)
    int fecharCiclosAntesDe(LocalDate data);

    // Duas requisições ao mesmo tempo podem tentar criar o mesmo ciclo:
    // a segunda não faz nada (em vez de violar a restrição única)
    @Modifying
    @Query(value = """
        INSERT INTO ciclo_financeiro (id, data_inicio, data_fim, fechado)
        VALUES (:id, :dataInicio, :dataFim, false)
        ON CONFLICT DO NOTHING
    """, nativeQuery = true)
    int inserirSeNaoExistir(UUID id, LocalDate dataInicio, LocalDate dataFim);
}
