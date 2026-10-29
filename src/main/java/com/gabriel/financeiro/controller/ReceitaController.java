package com.gabriel.financeiro.controller;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.MensagemErro;
import com.gabriel.financeiro.config.Paginacao;
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
                Paginacao.de(
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
    // EDITAR RECEITA (mesma tela, modo "update")
    // ==========================================

    @GetMapping("/update/{id}")
    public String update(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        pageReceita(page, size, model);

        model.addAttribute("receita", receitaServ.buscarPorId(id));
        model.addAttribute("modo", "update");

        return "receita";
    }

    @PostMapping("/update/{id}")
    public String salvarEdicao(@PathVariable UUID id, Receita receita, RedirectAttributes redirectAttributes) {
        try {
            Receita salva = receitaServ.updateReceita(id, receita);
            redirectAttributes.addFlashAttribute("mensagem", "Receita atualizada " + descreverCiclo(salva));
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
            return redirecionarParaCiclo(salva, redirectAttributes);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar receita: " + MensagemErro.de(e));
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
            return "redirect:/receita/update/" + id;
        }
    }

    // ==========================================
    // INSERIR RECEITA
    // ==========================================

    @PostMapping("/create")
    public String insertReceita(Receita receita, RedirectAttributes redirectAttributes) {
        try {
            Receita salva = receitaServ.insertReceita(receita);
            redirectAttributes.addFlashAttribute("mensagem", (salva.isParteDeRecorrencia()
                    ? salva.getTotalOcorrencias() + " receitas cadastradas (uma por mês), a primeira "
                    : "Receita cadastrada ") + descreverCiclo(salva));
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
            return redirecionarParaCiclo(salva, redirectAttributes);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar receita: " + MensagemErro.de(e));
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
            return "redirect:/receita";
        }
    }

    // A lista de receitas mostra um ciclo por vez: depois de salvar, abre o ciclo
    // da receita (uma data futura cai num ciclo que não é o atual)
    private String redirecionarParaCiclo(Receita receita, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute(CicloSelecionado.PARAMETRO, YearMonth.from(receita.getCiclo().getDataFim()).toString());
        return "redirect:/receita/receitas";
    }

    private static String descreverCiclo(Receita receita) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "no ciclo de " + receita.getCiclo().getDataInicio().format(formato)
                + " a " + receita.getCiclo().getDataFim().format(formato) + ".";
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
         redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir receita: " + MensagemErro.de(e));
         redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
     }
     return "redirect:/receita/receitas";
 }

 @PostMapping("/excluir-recorrencia")
 public String excluirRecorrencia(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
     try {
         int excluidas = receitaServ.excluirRecorrenciaAPartirDe(id);
         redirectAttributes.addFlashAttribute("mensagem",
                 excluidas == 1 ? "Receita excluida com sucesso!" : excluidas + " receitas da recorrencia excluidas com sucesso!");
         redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
     } catch (Exception e) {
         redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir recorrencia: " + MensagemErro.de(e));
         redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
     }
     return "redirect:/receita/receitas";
 }
}
