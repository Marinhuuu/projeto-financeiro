package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.repository.CicloFinanceiroRepository;

@Service
public class CicloFinanceiroService {

    private final CicloFinanceiroRepository cicloRepo;
    private final ConfiguracaoFinanceiraService configService;

    public CicloFinanceiroService(
            CicloFinanceiroRepository cicloRepo,
            ConfiguracaoFinanceiraService configService) {

        this.cicloRepo = cicloRepo;
        this.configService = configService;
    }

    @Transactional
    public CicloFinanceiro getCicloAtual() {

        LocalDate hoje = LocalDate.now();

        // Fecha ciclos anteriores
        fecharCiclosAnteriores(hoje);

        int diaFechamento =
                configService.getConfiguracao().getDiaFechamento();

        LocalDate inicio;
        LocalDate fim;

        if (hoje.getDayOfMonth() <= diaFechamento) {

            fim = ajustarDia(
                    YearMonth.from(hoje),
                    diaFechamento
            );

            inicio = fim
                    .minusMonths(1)
                    .plusDays(1);

        } else {

            inicio = ajustarDia(
                    YearMonth.from(hoje),
                    diaFechamento
            ).plusDays(1);

            fim = ajustarDia(
                    YearMonth.from(hoje).plusMonths(1),
                    diaFechamento
            );
        }

        return cicloRepo
                .findByDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        hoje,
                        hoje
                )
                .orElseGet(() -> criarCiclo(inicio, fim));
    }

    private void fecharCiclosAnteriores(LocalDate hoje) {

        List<CicloFinanceiro> ciclos =
                cicloRepo.findByDataFimBeforeAndFechadoFalse(hoje);

        for (CicloFinanceiro ciclo : ciclos) {

            ciclo.setFechado(true);

            cicloRepo.save(ciclo);
        }
    }

    private LocalDate ajustarDia(
            YearMonth mes,
            int dia) {

        int ultimoDia =
                mes.lengthOfMonth();

        int diaReal =
                Math.min(dia, ultimoDia);

        return mes.atDay(diaReal);
    }

    private CicloFinanceiro criarCiclo(
            LocalDate inicio,
            LocalDate fim) {

        CicloFinanceiro ciclo =
                new CicloFinanceiro();

        ciclo.setDataInicio(inicio);
        ciclo.setDataFim(fim);
        ciclo.setFechado(false);

        return cicloRepo.save(ciclo);
    }
    
    public CicloFinanceiro calcularCiclo(LocalDate data) {

        int diaFechamento =
                configService.getConfiguracao().getDiaFechamento();

        LocalDate inicio;
        LocalDate fim;

        if (data.getDayOfMonth() <= diaFechamento) {

            fim = ajustarDia(
                    YearMonth.from(data),
                    diaFechamento
            );

            inicio = fim
                    .minusMonths(1)
                    .plusDays(1);

        } else {

            inicio = ajustarDia(
                    YearMonth.from(data),
                    diaFechamento
            ).plusDays(1);

            fim = ajustarDia(
                    YearMonth.from(data).plusMonths(1),
                    diaFechamento
            );
        }

        return new CicloFinanceiro(
                null,
                inicio,
                fim,
                false
        );
    }
    @Transactional
    public CicloFinanceiro getOuCriarCiclo(LocalDate data) {

        return cicloRepo
                .findByDataInicioLessThanEqualAndDataFimGreaterThanEqual(
                        data,
                        data
                )
                .orElseGet(() -> {

                    CicloFinanceiro novoCiclo =
                            calcularCiclo(data);

                    return criarCiclo(
                            novoCiclo.getDataInicio(),
                            novoCiclo.getDataFim()
                    );
                });
    }
}