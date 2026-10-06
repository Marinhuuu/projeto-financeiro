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

        return getOuCriarCiclo(hoje);
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

    /*
     * O ciclo termina no dia de fechamento (ajustado ao tamanho do mês)
     * e começa no dia seguinte ao fechamento do mês anterior.
     * O início é calculado a partir do fechamento do mês anterior, e não
     * com fim.minusMonths(1), para não sobrepor ciclos com fechamento 29-31.
     */
    public CicloFinanceiro calcularCiclo(LocalDate data) {

        int diaFechamento =
                configService.getConfiguracao().getDiaFechamento();

        YearMonth mesFim =
                data.getDayOfMonth() <= ajustarDia(YearMonth.from(data), diaFechamento).getDayOfMonth()
                        ? YearMonth.from(data)
                        : YearMonth.from(data).plusMonths(1);

        LocalDate fim =
                ajustarDia(mesFim, diaFechamento);

        LocalDate inicio =
                ajustarDia(mesFim.minusMonths(1), diaFechamento)
                        .plusDays(1);

        return new CicloFinanceiro(
                null,
                inicio,
                fim,
                false
        );
    }

    /*
     * Ciclo cuja referência (mês em que termina) é o mês informado.
     * Usa o ciclo já gravado quando existe; senão devolve o calculado sem gravar,
     * para que navegar por meses futuros/passados não crie ciclos no banco.
     */
    public CicloFinanceiro getCicloPorReferencia(YearMonth referencia) {

        int diaFechamento =
                configService.getConfiguracao().getDiaFechamento();

        LocalDate fechamento =
                ajustarDia(referencia, diaFechamento);

        return cicloRepo
                .findFirstByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
                        fechamento,
                        fechamento
                )
                .orElseGet(() -> calcularCiclo(fechamento));
    }

    @Transactional
    public CicloFinanceiro getOuCriarCiclo(LocalDate data) {

        return cicloRepo
                .findFirstByDataInicioLessThanEqualAndDataFimGreaterThanEqualOrderByDataInicioDesc(
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
