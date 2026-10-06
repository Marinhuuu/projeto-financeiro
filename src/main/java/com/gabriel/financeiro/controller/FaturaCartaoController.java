package com.gabriel.financeiro.controller;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusFatura;
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

    // Status que o sistema calcula para as faturas, na ordem dos filtros da tela
    private static final List<StatusFatura> STATUS_FILTRO = List.of(
            StatusFatura.EM_ABERTO, StatusFatura.FECHADA, StatusFatura.VENCIDA, StatusFatura.PAGA);

    @GetMapping
    public String listarFaturas(@RequestParam(required = false) String status, Model model) {

        // O status é recalculado em memória (atualizarStatus), por isso o filtro é feito após listar
        List<FaturaCartao> todas = faturaService.listarFaturas();

        StatusFatura filtro = STATUS_FILTRO.stream()
                .filter(s -> s.name().equalsIgnoreCase(status))
                .findFirst()
                .orElse(null);

        List<FaturaCartao> faturas = filtro == null
                ? todas
                : todas.stream().filter(f -> f.getStatusFatura() == filtro).collect(Collectors.toList());

        Map<StatusFatura, Long> totalPorStatus = new EnumMap<>(StatusFatura.class);
        for (StatusFatura s : STATUS_FILTRO) {
            totalPorStatus.put(s, todas.stream().filter(f -> f.getStatusFatura() == s).count());
        }

        model.addAttribute("faturas", faturas);
        model.addAttribute("filtroStatus", filtro);
        model.addAttribute("totalPorStatus", totalPorStatus);
        model.addAttribute("totalFaturas", todas.size());
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
