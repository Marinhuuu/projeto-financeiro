package com.gabriel.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.CicloFinanceiro;

public interface CicloFinanceiroRepository
        extends JpaRepository<CicloFinanceiro, UUID> {

    // Busca pelas datas exatas: ciclos gravados com outro dia de fechamento
    // (ex.: o padrão antigo 25) não são reaproveitados. findFirst tolera duplicados.
    Optional<CicloFinanceiro>
    findFirstByDataInicioAndDataFim(
            LocalDate dataInicio,
            LocalDate dataFim);

    List<CicloFinanceiro>
    findByDataFimBeforeAndFechadoFalse(LocalDate data);
}
