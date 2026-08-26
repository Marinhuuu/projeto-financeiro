package com.gabriel.financeiro.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.repository.FaturaCartaoRepository;

@Service
public class FaturaCartaoService {

    private final FaturaCartaoRepository faturaRepo;

    public FaturaCartaoService(FaturaCartaoRepository faturaRepo) {
        this.faturaRepo = faturaRepo;
      }
    public List<FaturaCartao> listarFaturas() {
        return faturaRepo.findAll();
    }

    public FaturaCartao buscarPorId(UUID id) {

        return faturaRepo.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Fatura não encontrada"));
    }
    
    public FaturaCartao getOuCriarFatura(
            CartaoCredito cartao,
            LocalDate dataCompra) {

        int diaFechamento = cartao.getDiaFechamento();

        /*
         * Descobre o fechamento do mês da compra.
         */
        YearMonth mesDaCompra =
                YearMonth.from(dataCompra);

        int ultimoDiaDoMes =
                mesDaCompra.lengthOfMonth();

        int diaFechamentoReal =
                Math.min(diaFechamento, ultimoDiaDoMes);

        LocalDate fechamentoDoMes =
                mesDaCompra.atDay(diaFechamentoReal);

        /*
         * Se a compra aconteceu depois do fechamento
         * deste mês, ela pertence à próxima fatura.
         *
         * Se aconteceu antes ou no fechamento,
         * pertence à fatura deste mês.
         */
        YearMonth referencia;

        if (dataCompra.isAfter(fechamentoDoMes)) {

            referencia = mesDaCompra.plusMonths(1);

        } else {

            referencia = mesDaCompra;
        }

        Integer mesReferencia =
                referencia.getMonthValue();

        Integer anoReferencia =
                referencia.getYear();

        /*
         * Procura a fatura correspondente.
         */
        return faturaRepo
                .findByCartaoAndMesReferenciaAndAnoReferencia(
                        cartao,
                        mesReferencia,
                        anoReferencia
                )
                .orElseGet(() ->
                        criarFatura(
                                cartao,
                                mesReferencia,
                                anoReferencia
                        )
                );
    }

    private FaturaCartao criarFatura(
            CartaoCredito cartao,
            Integer mesReferencia,
            Integer anoReferencia) {

        YearMonth referencia =
                YearMonth.of(anoReferencia, mesReferencia);

        /*
         * Data de fechamento da fatura.
         */
        int diaFechamento = Math.min(
                cartao.getDiaFechamento(),
                referencia.lengthOfMonth()
        );

        LocalDate dataFechamento =
                referencia.atDay(diaFechamento);

        /*
         * Normalmente o vencimento ocorre depois
         * do fechamento.
         *
         * Se o dia de vencimento for menor ou igual
         * ao dia de fechamento, usamos o mês seguinte.
         */
        YearMonth mesVencimento;

        if (cartao.getDiaVencimento() <= cartao.getDiaFechamento()) {
            mesVencimento = referencia.plusMonths(1);
        } else {
            mesVencimento = referencia;
        }

        int diaVencimento = Math.min(
                cartao.getDiaVencimento(),
                mesVencimento.lengthOfMonth()
        );

        LocalDate dataVencimento =
                mesVencimento.atDay(diaVencimento);

        FaturaCartao fatura = new FaturaCartao();

        fatura.setMesReferencia(mesReferencia);
        fatura.setAnoReferencia(anoReferencia);
        fatura.setDataFechamento(dataFechamento);
        fatura.setDataVencimento(dataVencimento);
        fatura.setValorTotal(java.math.BigDecimal.ZERO);
        fatura.setStatusFatura(StatusFatura.PENDENTE);
        fatura.setCartao(cartao);

        return faturaRepo.save(fatura);
    }
    
    public FaturaCartao salvar(FaturaCartao fatura) {
        return faturaRepo.save(fatura);
    }
}