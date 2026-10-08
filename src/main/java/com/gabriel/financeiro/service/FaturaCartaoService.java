package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.enums.StatusParcela;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;
import com.gabriel.financeiro.repository.ParcelaRepository;

@Service
public class FaturaCartaoService {

    private final FaturaCartaoRepository faturaRepo;
    private final ParcelaRepository parcelaRepo;
    private final DespesaRepository despesaRepo;
    private final CicloFinanceiroService cicloService;

    public FaturaCartaoService(
            FaturaCartaoRepository faturaRepo,
            ParcelaRepository parcelaRepo,
            DespesaRepository despesaRepo,
            CicloFinanceiroService cicloService) {

        this.faturaRepo = faturaRepo;
        this.parcelaRepo = parcelaRepo;
        this.despesaRepo = despesaRepo;
        this.cicloService = cicloService;
    }

    // =========================================================
    // CONSULTAS
    // =========================================================

    /*
     * Faturas que fecham dentro do ciclo (ver FaturasDoCiclo); ciclo null = todas.
     */
    @Transactional
    public List<FaturaCartao> listarFaturas(CicloFinanceiro ciclo) {

        List<FaturaCartao> faturas;

        if (ciclo == null) {
            faturas = faturaRepo.findAllByOrderByAnoReferenciaDescMesReferenciaDesc();
        } else {
            faturas = faturaRepo.listarDoCiclo(cicloService.faturasDoCiclo(ciclo));
        }

        atualizarStatus(faturas);

        return faturas;
    }

    @Transactional
    public FaturaCartao buscarPorId(UUID id) {

        FaturaCartao fatura = faturaRepo.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Fatura não encontrada"));

        atualizarStatus(List.of(fatura));

        return fatura;
    }

    // =========================================================
    // REFERÊNCIA DA FATURA
    // =========================================================

    /*
     * Mês/ano da fatura em que uma compra feita em "data" entra.
     * Compra depois do fechamento do mês vai para a fatura seguinte.
     */
    public YearMonth calcularReferencia(
            CartaoCredito cartao,
            LocalDate data) {

        YearMonth mesDaCompra = YearMonth.from(data);

        LocalDate fechamentoDoMes =
                ajustarDia(mesDaCompra, cartao.getDiaFechamento());

        if (data.isAfter(fechamentoDoMes)) {
            return mesDaCompra.plusMonths(1);
        }

        return mesDaCompra;
    }

    @Transactional
    public FaturaCartao getOuCriarFatura(
            CartaoCredito cartao,
            YearMonth referencia) {

        return faturaRepo
                .findByCartaoAndMesReferenciaAndAnoReferencia(
                        cartao,
                        referencia.getMonthValue(),
                        referencia.getYear()
                )
                .orElseGet(() -> criarFatura(cartao, referencia));
    }

    private FaturaCartao criarFatura(
            CartaoCredito cartao,
            YearMonth referencia) {

        FaturaCartao fatura = new FaturaCartao();

        fatura.setMesReferencia(referencia.getMonthValue());
        fatura.setAnoReferencia(referencia.getYear());
        fatura.setValorTotal(java.math.BigDecimal.ZERO);
        fatura.setCartao(cartao);

        aplicarDatas(fatura, cartao, referencia);

        fatura.setStatusFatura(calcularStatus(fatura));

        return faturaRepo.save(fatura);
    }

    /*
     * Fechamento no mês de referência. Normalmente o vencimento ocorre
     * depois do fechamento; se o dia de vencimento for menor ou igual
     * ao de fechamento, o vencimento cai no mês seguinte.
     */
    private void aplicarDatas(
            FaturaCartao fatura,
            CartaoCredito cartao,
            YearMonth referencia) {

        fatura.setDataFechamento(calcularDataFechamento(cartao, referencia));
        fatura.setDataVencimento(calcularDataVencimento(cartao, referencia));
    }

    public LocalDate calcularDataFechamento(CartaoCredito cartao, YearMonth referencia) {
        return ajustarDia(referencia, cartao.getDiaFechamento());
    }

    public LocalDate calcularDataVencimento(CartaoCredito cartao, YearMonth referencia) {

        YearMonth mesVencimento =
                cartao.getDiaVencimento() <= cartao.getDiaFechamento()
                        ? referencia.plusMonths(1)
                        : referencia;

        return ajustarDia(mesVencimento, cartao.getDiaVencimento());
    }

    private LocalDate ajustarDia(YearMonth mes, int dia) {
        return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
    }

    // =========================================================
    // TOTAL E STATUS
    // =========================================================

    /*
     * Recalcula valorTotal e status a partir das parcelas.
     * Fatura que ficou sem parcelas é removida.
     */
    @Transactional
    public void sincronizar(FaturaCartao fatura) {

        if (parcelaRepo.countByFatura(fatura) == 0) {
            faturaRepo.delete(fatura);
            return;
        }

        fatura.setValorTotal(parcelaRepo.somarPorFatura(fatura));
        aplicarStatus(fatura, calcularStatus(fatura));

        faturaRepo.save(fatura);
    }

    @Transactional
    public void atualizarStatus(List<FaturaCartao> faturas) {

        for (FaturaCartao fatura : faturas) {

            StatusFatura status = calcularStatus(fatura);

            if (status != fatura.getStatusFatura() || !pagamentoConsistente(fatura)) {
                aplicarStatus(fatura, status);
                faturaRepo.save(fatura);
            }
        }
    }

    /*
     * Mantém status e dataPagamento coerentes: PAGA sempre tem data,
     * os demais status nunca têm. Fatura que ficou PAGA sem passar por
     * pagarFatura (ex.: todas as parcelas pagas uma a uma) recebe a data de hoje.
     */
    private void aplicarStatus(FaturaCartao fatura, StatusFatura status) {

        fatura.setStatusFatura(status);

        if (status != StatusFatura.PAGA) {
            fatura.setDataPagamento(null);
        } else if (fatura.getDataPagamento() == null) {
            fatura.setDataPagamento(LocalDate.now());
        }
    }

    private boolean pagamentoConsistente(FaturaCartao fatura) {
        return (fatura.getStatusFatura() == StatusFatura.PAGA) == (fatura.getDataPagamento() != null);
    }

    private StatusFatura calcularStatus(FaturaCartao fatura) {

        if (fatura.getId() != null
                && parcelaRepo.countByFatura(fatura) > 0
                && parcelaRepo.countByFaturaAndStatusParcelaNot(fatura, StatusParcela.PAGA) == 0) {

            return StatusFatura.PAGA;
        }

        LocalDate hoje = LocalDate.now();

        if (fatura.getDataVencimento() != null && hoje.isAfter(fatura.getDataVencimento())) {
            return StatusFatura.VENCIDA;
        }

        if (fatura.getDataFechamento() != null && hoje.isAfter(fatura.getDataFechamento())) {
            return StatusFatura.FECHADA;
        }

        return StatusFatura.EM_ABERTO;
    }

    // =========================================================
    // PAGAMENTO DA FATURA
    // A fatura é PAGA quando todas as suas parcelas estão pagas (calcularStatus),
    // então pagar a fatura = dar baixa em todas as parcelas dela. Nenhum
    // lançamento é criado e o valor não muda: o saldo já conta as parcelas
    // pelo mês de referência, independentemente do status.
    // =========================================================

    @Transactional
    public FaturaCartao pagarFatura(UUID id) {

        FaturaCartao fatura = buscarPorId(id);

        if (fatura.getStatusFatura() == StatusFatura.PAGA) {
            throw new RuntimeException("Esta fatura já está paga.");
        }

        List<Parcela> parcelas = parcelaRepo.findByFatura(fatura);

        if (parcelas.isEmpty()) {
            throw new RuntimeException("Esta fatura não tem lançamentos para pagar.");
        }

        for (Parcela parcela : parcelas) {

            if (parcela.getStatusParcela() != StatusParcela.PAGA) {
                parcela.setStatusParcela(StatusParcela.PAGA);
                parcelaRepo.save(parcela);
            }
        }

        fatura.setDataPagamento(LocalDate.now());
        sincronizar(fatura);

        return fatura;
    }

    @Transactional
    public FaturaCartao desfazerPagamento(UUID id) {

        FaturaCartao fatura = buscarPorId(id);

        if (fatura.getStatusFatura() != StatusFatura.PAGA) {
            throw new RuntimeException("Esta fatura não está paga.");
        }

        LocalDate hoje = LocalDate.now();

        for (Parcela parcela : parcelaRepo.findByFatura(fatura)) {

            parcela.setStatusParcela(
                    parcela.getDataVencimento() != null && parcela.getDataVencimento().isBefore(hoje)
                            ? StatusParcela.ATRASADA
                            : StatusParcela.PENDENTE
            );

            parcelaRepo.save(parcela);
        }

        // Volta para EM_ABERTO, FECHADA ou VENCIDA conforme as datas; a data é limpa em aplicarStatus
        sincronizar(fatura);

        return fatura;
    }

    // =========================================================
    // RECALCULAR FATURAS APÓS ALTERAR O CARTÃO
    // =========================================================

    /*
     * Usado quando o dia de fechamento/vencimento do cartão muda:
     * corrige as datas das faturas existentes, redistribui as parcelas
     * pela nova regra e recalcula os totais.
     */
    @Transactional
    public void recalcularFaturasDoCartao(CartaoCredito cartao) {

        for (FaturaCartao fatura : faturaRepo.findByCartao(cartao)) {

            aplicarDatas(
                    fatura,
                    cartao,
                    YearMonth.of(fatura.getAnoReferencia(), fatura.getMesReferencia())
            );

            faturaRepo.save(fatura);
        }

        LocalDate hoje = LocalDate.now();

        for (Despesa despesa : despesaRepo.findByCartao(cartao)) {

            YearMonth primeiraReferencia =
                    calcularReferencia(cartao, despesa.getDataCompra());

            for (Parcela parcela : parcelaRepo.findByDespesaOrderByQtdParcelaAsc(despesa)) {

                FaturaCartao fatura = getOuCriarFatura(
                        cartao,
                        primeiraReferencia.plusMonths(parcela.getQtdParcela() - 1)
                );

                parcela.setFatura(fatura);
                parcela.setDataVencimento(fatura.getDataVencimento());

                if (parcela.getStatusParcela() != StatusParcela.PAGA) {
                    parcela.setStatusParcela(
                            fatura.getDataVencimento().isBefore(hoje)
                                    ? StatusParcela.ATRASADA
                                    : StatusParcela.PENDENTE
                    );
                }

                parcelaRepo.save(parcela);
            }
        }

        for (FaturaCartao fatura : faturaRepo.findByCartao(cartao)) {
            sincronizar(fatura);
        }
    }
}
