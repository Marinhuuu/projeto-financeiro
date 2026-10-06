package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.CicloSelecionado;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.CicloFinanceiro;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.service.CategoriaService;
import com.gabriel.financeiro.service.ReceitaService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/receita")
public class ReceitaController {

    private final ReceitaService receitaServ;
    private final CategoriaService categoriaServ;
    private final CicloSelecionado cicloSelecionado;

    public ReceitaController(
            ReceitaService receitaServ,
            CategoriaService categoriaServ,
            CicloSelecionado cicloSelecionado) {

        this.receitaServ = receitaServ;
        this.categoriaServ = categoriaServ;
        this.cicloSelecionado = cicloSelecionado;
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

        // Somente categorias do tipo RECEITA
        model.addAttribute(
                "categoria",
                categoriaServ.listByTipo("RECEITA")
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
    public String insertReceita(Receita receita, RedirectAttributes redirectAttributes) {
        try {
            receitaServ.insertReceita(receita);
            redirectAttributes.addFlashAttribute("mensagem", "Receita cadastrada com sucesso!");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar receita: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
        }
        return "redirect:/receita";
    }

    // ==========================================
    // GERENCIAR RECEITAS
    // ==========================================

    @GetMapping("/receitas")
    public String listarReceitas(
            @RequestParam(name = CicloSelecionado.PARAMETRO, required = false) String cicloParam,
            HttpServletRequest request,
            Model model) {

        CicloFinanceiro ciclo =
                cicloSelecionado.resolver(cicloParam, request, model);

        List<Receita> receitas =
                receitaServ.listarReceitasDoCiclo(ciclo);

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

        return "receitas";
    }

 // ==========================================
 // EXCLUIR RECEITA
 // ==========================================

 @PostMapping("/excluir")
 public String excluirReceita(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
     try {
         receitaServ.excluirReceita(id);
         redirectAttributes.addFlashAttribute("mensagem", "Receita excluida com sucesso!");
         redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
     } catch (Exception e) {
         redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir receita: " + e.getMessage());
         redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
     }
     return "redirect:/receita/receitas";
 }
}
