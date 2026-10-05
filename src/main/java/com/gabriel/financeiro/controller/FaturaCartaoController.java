package com.gabriel.financeiro.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.service.FaturaCartaoService;

@Controller
@RequestMapping("/faturas")
public class FaturaCartaoController {

    private final FaturaCartaoService faturaService;
    private final ParcelaRepository parcelaRepo;

    public FaturaCartaoController(FaturaCartaoService faturaService, ParcelaRepository parcelaRepo) {
        this.faturaService = faturaService;
        this.parcelaRepo = parcelaRepo;
    }

    @GetMapping
    public String listarFaturas(Model model) {
        List<FaturaCartao> faturas = faturaService.listarFaturas();
        model.addAttribute("faturas", faturas);
        return "faturas";
    }

    @GetMapping("/{id}")
    public String detalhesFatura(@PathVariable UUID id, Model model) {

        FaturaCartao fatura = faturaService.buscarPorId(id);

        // Carrega parcelas separadamente (evita LazyInitializationException)
        List<Parcela> todasParcelas = parcelaRepo.findByFatura(fatura);

        // Separa: credito a vista vs parcelado
        List<Parcela> parcelasVista = todasParcelas.stream()
                .filter(p -> p.getDespesa() != null
                        && p.getDespesa().getFormaPagamento() != null
                        && "CARTAO_VISTA".equalsIgnoreCase(
                                p.getDespesa().getFormaPagamento().getCodigo()))
                .collect(Collectors.toList());

        List<Parcela> parcelasParceladas = todasParcelas.stream()
                .filter(p -> !parcelasVista.contains(p))
                .collect(Collectors.toList());

        model.addAttribute("fatura", fatura);
        model.addAttribute("parcelasVista", parcelasVista);
        model.addAttribute("parcelasParceladas", parcelasParceladas);

        return "fatura-detalhes";
    }
}
