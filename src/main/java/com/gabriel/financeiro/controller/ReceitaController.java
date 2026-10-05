package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
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

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.Receita;
import com.gabriel.financeiro.service.CategoriaService;
import com.gabriel.financeiro.service.ReceitaService;

@Controller
@RequestMapping("/receita")
public class ReceitaController {

    private final ReceitaService receitaServ;
    private final CategoriaService categoriaServ;

    public ReceitaController(
            ReceitaService receitaServ,
            CategoriaService categoriaServ) {

        this.receitaServ = receitaServ;
        this.categoriaServ = categoriaServ;
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
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            Model model) {

        LocalDate hoje = LocalDate.now();

        // Mes/ano ausentes ou invalidos voltam para o mes atual
        if (mes == null || mes < 1 || mes > 12) {
            mes = hoje.getMonthValue();
        }

        if (ano == null || ano < 1900 || ano > 9999) {
            ano = hoje.getYear();
        }

        List<Receita> receitas =
                receitaServ.buscarPorMesEAno(mes, ano);

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
