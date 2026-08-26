package com.gabriel.financeiro.controller;


import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.gabriel.financeiro.entities.FaturaCartao;
import com.gabriel.financeiro.service.FaturaCartaoService;

@Controller
@RequestMapping("/faturas")
public class FaturaCartaoController {

    private final FaturaCartaoService faturaService;

    public FaturaCartaoController(FaturaCartaoService faturaService) {
        this.faturaService = faturaService;
    }

    @GetMapping
    public String listarFaturas(Model model) {

        List<FaturaCartao> faturas =
                faturaService.listarFaturas();

        model.addAttribute("faturas", faturas);

        return "faturas";
    }

    @GetMapping("/{id}")
    public String detalhesFatura(
            @PathVariable UUID id,
            Model model) {

        FaturaCartao fatura =
                faturaService.buscarPorId(id);

        model.addAttribute("fatura", fatura);

        return "fatura-detalhes";
    }
}