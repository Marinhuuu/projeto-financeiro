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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.CicloSelecionado;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.repository.ParcelaRepository;
import com.gabriel.financeiro.service.FaturaCartaoService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/faturas")
public class FaturaCartaoController {

    private final FaturaCartaoService faturaService;
    private final ParcelaRepository parcelaRepo;
    private final CicloSelecionado cicloSelecionado;

    public FaturaCartaoController(FaturaCartaoService faturaService, ParcelaRepository parcelaRepo,
            CicloSelecionado cicloSelecionado) {
        this.faturaService = faturaService;
        this.parcelaRepo = parcelaRepo;
        this.cicloSelecionado = cicloSelecionado;
    }

    // Status que o sistema calcula para as faturas, na ordem dos filtros da tela
    private static final List<StatusFatura> STATUS_FILTRO = List.of(
            StatusFatura.EM_ABERTO, StatusFatura.FECHADA, StatusFatura.VENCIDA, StatusFatura.PAGA);

    @GetMapping
    public String listarFaturas(@RequestParam(required = false) String status,
            @RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
            HttpServletRequest request, Model model) {

        CicloFinanceiro ciclo = cicloSelecionado.resolver(cicloParam, request, model);

        // O status é recalculado em memória (atualizarStatus), por isso o filtro é feito após listar
        List<FaturaCartao> todas = faturaService.listarFaturas(ciclo);

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

    // =========================================================
    // PAGAMENTO DA FATURA
    // origem=detalhes volta para a tela da fatura; senão volta para a lista (mantendo o filtro)
    // =========================================================

    @PostMapping("/{id}/pagar")
    public String pagarFatura(@PathVariable UUID id,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {

        try {
            faturaService.pagarFatura(id);
            redirectAttributes.addFlashAttribute("mensagem", "Fatura marcada como paga.");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensagem", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
        }

        return redirecionar(id, origem, status, redirectAttributes);
    }

    @PostMapping("/{id}/desfazer-pagamento")
    public String desfazerPagamento(@PathVariable UUID id,
            @RequestParam(required = false) String origem,
            @RequestParam(required = false) String status,
            RedirectAttributes redirectAttributes) {

        try {
            faturaService.desfazerPagamento(id);
            redirectAttributes.addFlashAttribute("mensagem", "Pagamento da fatura desfeito.");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("mensagem", e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
        }

        return redirecionar(id, origem, status, redirectAttributes);
    }

    private String redirecionar(UUID id, String origem, String status, RedirectAttributes redirectAttributes) {

        if ("detalhes".equals(origem)) {
            return "redirect:/faturas/" + id;
        }

        if (status != null && !status.isBlank()) {
            redirectAttributes.addAttribute("status", status);
        }

        return "redirect:/faturas";
    }
}
