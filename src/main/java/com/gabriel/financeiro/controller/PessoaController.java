package com.gabriel.financeiro.controller;

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
import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.service.PessoaService;

@Controller
@RequestMapping("/pessoa")
public class PessoaController {

    private final PessoaService pessoaServ;

    public PessoaController(PessoaService pessoaServ) {
        this.pessoaServ = pessoaServ;
    }

    @GetMapping
    public String pagePessoa(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable =
                Paginacao.de(
                        page,
                        size,
                        Sort.by("nome").ascending()
                );

        Page<Pessoa> paginaPessoa =
                pessoaServ.listPessoa(pageable);

        model.addAttribute("paginaPessoa", paginaPessoa);
        model.addAttribute("pessoa", new Pessoa());

        return "pessoa";
    }

    @GetMapping("/update/{id}")
    public String update(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending());

        model.addAttribute("paginaPessoa", pessoaServ.listPessoa(pageable));
        model.addAttribute("pessoa", pessoaServ.getById(id));
        model.addAttribute("modo", "update");

        return "pessoa";
    }

    @PostMapping("/update/{id}")
    public String salvarEdicao(@PathVariable UUID id, Pessoa pessoa, RedirectAttributes redirectAttributes) {
        try {
            pessoaServ.updatePessoa(id, pessoa);
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa atualizada com sucesso!");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar pessoa: " + MensagemErro.de(e));
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
            return "redirect:/pessoa/update/" + id;
        }
        return "redirect:/pessoa";
    }

    @PostMapping
    public String insertPessoa(Pessoa pessoa, RedirectAttributes redirectAttributes) {
        try {
            pessoaServ.insertPessoa(pessoa);
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa cadastrada com sucesso!");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar pessoa: " + MensagemErro.de(e));
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
        }
        return "redirect:/pessoa";
    }

    @PostMapping("/excluir")
    public String excluirPessoa(
            @RequestParam UUID id,
            RedirectAttributes redirectAttributes) {
        try {
            pessoaServ.deletePessoa(id);
            redirectAttributes.addFlashAttribute("mensagem", "Pessoa excluida com sucesso!");
            redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir pessoa: " + MensagemErro.de(e));
            redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
        }
        return "redirect:/pessoa";
    }
}
