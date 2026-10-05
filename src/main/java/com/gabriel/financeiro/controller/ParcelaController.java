package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.service.ParcelaService;

@Controller
@RequestMapping("/parcela")
public class ParcelaController {

    private final ParcelaService parcelaServ;

    public ParcelaController(ParcelaService parcelaServ) {
        this.parcelaServ = parcelaServ;
    }

    @GetMapping
    public String pageParcela(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        parcelaServ.atualizarParcelasAtrasadas();

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("dataVencimento").ascending().and(Sort.by("qtdParcela")));

        Page<Parcela> paginaParcelas =
                parcelaServ.ListParcela(pageable);

        model.addAttribute("paginaParcelas", paginaParcelas);
        model.addAttribute("totalPendente", parcelaServ.calcularTotalPendente());
        model.addAttribute("totalPago", parcelaServ.calcularTotalPago());
        model.addAttribute("percentualPago", parcelaServ.calcularPercentualPago());

        return "parcelas";
    }

    @PostMapping("/{id}/pagar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pagarParcela(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(construirRespostaJson(parcelaServ.pagarParcela(id)));
        } catch (Exception e) {
            return respostaErro(e);
        }
    }

    @PostMapping("/{id}/desfazer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> desfazerPagamento(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(construirRespostaJson(parcelaServ.desfazerPagamento(id)));
        } catch (Exception e) {
            return respostaErro(e);
        }
    }

    private ResponseEntity<Map<String, Object>> respostaErro(Exception e) {
        Map<String, Object> response = new HashMap<>();
        response.put("sucesso", false);
        response.put("mensagem", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    private Map<String, Object> construirRespostaJson(Parcela parcela) {
        BigDecimal totalPendente = parcelaServ.calcularTotalPendente();
        BigDecimal totalPago = parcelaServ.calcularTotalPago();
        BigDecimal percentualPago = parcelaServ.calcularPercentualPago();

        Map<String, Object> response = new HashMap<>();
        response.put("sucesso", true);
        response.put("id", parcela.getId().toString());
        response.put("status", parcela.getStatusParcela().name());
        response.put("statusDescricao", parcela.getStatusParcela().getDescricao());
        response.put("totalPendente", totalPendente);
        response.put("totalPago", totalPago);
        response.put("percentualPago", percentualPago);
        return response;
    }
}
