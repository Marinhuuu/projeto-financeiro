package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.dto.FaturasDoCiclo;
import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.repository.CartaoRepository;
import com.gabriel.financeiro.repository.CicloFinanceiroRepository;

@Service
public class CicloFinanceiroService {

    private final CicloFinanceiroRepository cicloRepo;
    private final ConfiguracaoFinanceiraService configService;
    private final CartaoRepository cartaoRepo;

    public CicloFinanceiroService(
            CicloFinanceiroRepository cicloRepo,
            ConfiguracaoFinanceiraService configService,
            CartaoRepository cartaoRepo) {

        this.cicloRepo = cicloRepo;
        this.configService = configService;
        this.cartaoRepo = cartaoRepo;
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
                getDiaFechamento();

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
     * Faturas de cartão que pertencem ao ciclo (ver FaturasDoCiclo).
     */
    public FaturasDoCiclo faturasDoCiclo(CicloFinanceiro ciclo) {

        return FaturasDoCiclo.de(
                ciclo,
                getDiaFechamento()
        );
    }

    /*
     * Dia de fechamento do ciclo financeiro: segue o dia de fechamento dos
     * cartões, para que o ciclo coincida com o período da fatura.
     * Com cartões de dias diferentes, vale o dia mais comum (empate: o menor).
     * Sem cartões, usa a ConfiguracaoFinanceira.
     */
    public int getDiaFechamento(List<CartaoCredito> cartoes) {

        Map<Integer, Long> porDia = cartoes.stream()
                .map(CartaoCredito::getDiaFechamento)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(dia -> dia, TreeMap::new, Collectors.counting()));

        return porDia.entrySet().stream()
                .max(Comparator.comparing(Map.Entry<Integer, Long>::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElseGet(() -> configService.getConfiguracao().getDiaFechamento());
    }

    public int getDiaFechamento() {
        return getDiaFechamento(cartaoRepo.findAll());
    }

    /*
     * Ciclo cuja referência (mês em que termina) é o mês informado.
     * Usa o ciclo já gravado quando existe; senão devolve o calculado sem gravar,
     * para que navegar por meses futuros/passados não crie ciclos no banco.
     */
    public CicloFinanceiro getCicloPorReferencia(YearMonth referencia) {

        int diaFechamento =
                getDiaFechamento();

        CicloFinanceiro calculado =
                calcularCiclo(ajustarDia(referencia, diaFechamento));

        return cicloRepo
                .findFirstByDataInicioAndDataFim(
                        calculado.getDataInicio(),
                        calculado.getDataFim()
                )
                .orElse(calculado);
    }

    @Transactional
    public CicloFinanceiro getOuCriarCiclo(LocalDate data) {

        CicloFinanceiro calculado =
                calcularCiclo(data);

        return cicloRepo
                .findFirstByDataInicioAndDataFim(
                        calculado.getDataInicio(),
                        calculado.getDataFim()
                )
                .orElseGet(() -> criarCiclo(
                        calculado.getDataInicio(),
                        calculado.getDataFim()
                ));
    }
}
