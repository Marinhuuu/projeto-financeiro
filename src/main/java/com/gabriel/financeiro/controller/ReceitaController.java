package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.repository.ReceitaRepository;
import com.gabriel.financeiro.service.CategoriaService;
import com.gabriel.financeiro.service.ReceitaService;

@Controller
@RequestMapping("/receita")
public class ReceitaController {

    private final ReceitaService receitaServ;
    private final CategoriaService categoriaServ;
    private final ReceitaRepository receitaRepo;

    public ReceitaController(
            ReceitaService receitaServ,
            CategoriaService categoriaServ,
            ReceitaRepository receitaRepo) {

        this.receitaServ = receitaServ;
        this.categoriaServ = categoriaServ;
        this.receitaRepo = receitaRepo;
    }

    // ==========================================
    // TELA DE INSERIR RECEITA
    // ==========================================

    @GetMapping
    public String pageReceita(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageableReceita =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("dataEntrada").descending()
                );

        Page<Receita> paginaReceitas =
                receitaServ.listReceita(pageableReceita);

        Receita receita = new Receita();
        receita.setCategoria(new Categoria());

        model.addAttribute("receita", receita);

        model.addAttribute(
                "receitas",
                paginaReceitas.getContent()
        );

        Pageable pageableCategoria =
                PageRequest.of(
                        0,
                        100,
                        Sort.by("nome").ascending()
                );

        model.addAttribute(
                "categoria",
                categoriaServ.listCategoria(pageableCategoria)
        );

        model.addAttribute(
                "paginaReceitas",
                paginaReceitas
        );

        return "receita";
    }

    // ==========================================
    // INSERIR RECEITA
    // ==========================================

    @PostMapping("/create")
    public String insertReceita(Receita receita) {

        receitaServ.insertReceita(receita);

        return "redirect:/receita";
    }

    // ==========================================
    // GERENCIAR RECEITAS
    // ==========================================

    @GetMapping("/receitas")
    public String listarReceitas(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            Model model) {

        LocalDate hoje = LocalDate.now();

        if (mes == null) {
            mes = hoje.getMonthValue();
        }

        if (ano == null) {
            ano = hoje.getYear();
        }

        // Primeiro dia do mês
        LocalDate inicio =
                LocalDate.of(ano, mes, 1);

        // Último dia do mês
        LocalDate fim =
                inicio.withDayOfMonth(
                        inicio.lengthOfMonth()
                );

        // BUSCA DIRETAMENTE NO REPOSITORY
        List<Receita> receitas =
                receitaRepo.findByDataEntradaBetween(
                        inicio,
                        fim
                );

        // Soma
        BigDecimal total =
                receitas.stream()
                        .map(Receita::getValor)
                        .filter(valor -> valor != null)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute("receitas", receitas);
        model.addAttribute("total", total);
        model.addAttribute("mesSelecionado", mes);
        model.addAttribute("anoSelecionado", ano);

        return "receitas";
    }
    
 // ==========================================
 // EXCLUIR RECEITA
 // ==========================================

 @PostMapping("/excluir")
 public String excluirReceita(@RequestParam("id") java.util.UUID id) {

     receitaServ.excluirReceita(id);

     return "redirect:/receita/receitas";
 }
}