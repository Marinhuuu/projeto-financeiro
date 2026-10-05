package com.gabriel.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gabriel.financeiro.entities.CicloFinanceiro;

public interface CicloFinanceiroRepository
        extends JpaRepository<CicloFinanceiro, UUID> {

    // findFirst: tolera ciclos sobrepostos criados antes da correção do cálculo
    Optional<CicloFinanceiro>
    findFirstByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
            LocalDate dataInicio,
            LocalDate dataFim);

    List<CicloFinanceiro>
    findByDataFimBeforeAndFechadoFalse(LocalDate data);
}
