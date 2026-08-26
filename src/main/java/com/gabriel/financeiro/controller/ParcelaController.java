package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gabriel.financeiro.entities.Parcela;
import com.gabriel.financeiro.service.ParcelaService;

@Controller
@RequestMapping("/parcela")
public class ParcelaController {

    private ParcelaService parcelaServ;

    public ParcelaController(ParcelaService parcelaServ) {
        this.parcelaServ = parcelaServ;
    }

    @GetMapping
    public String pageParcela(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100000") int size,
            Model model) {

        parcelaServ.atualizarParcelasAtrasadas();

        Pageable pageable = PageRequest.of(page, size);

        Page<Parcela> paginaParcelas =
                parcelaServ.ListParcela(pageable);

        BigDecimal totalPendente =
                parcelaServ.calcularTotalPendente();

        BigDecimal totalPago =
                parcelaServ.calcularTotalPago();

        BigDecimal percentualPago =
                parcelaServ.calcularPercentualPago();

        model.addAttribute("paginaParcelas", paginaParcelas);
        model.addAttribute("totalPendente", totalPendente);
        model.addAttribute("totalPago", totalPago);
        model.addAttribute("percentualPago", percentualPago);

        return "parcelas";
    }
    
    @PostMapping("/{id}/pagar")
    public String pagarParcela(@PathVariable UUID id) {

        parcelaServ.pagarParcela(id);

        return "redirect:/parcela";
    }
    
    
}
