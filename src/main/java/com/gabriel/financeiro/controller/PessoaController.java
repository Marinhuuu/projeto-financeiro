package com.gabriel.financeiro.controller;

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
                PageRequest.of(
                        page,
                        size,
                        Sort.by("nome").ascending()
                );

        Page<Pessoa> paginaPessoa =
                pessoaServ.listPessoa(pageable);

        model.addAttribute(
                "paginaPessoa",
                paginaPessoa
        );

        model.addAttribute(
                "pessoa",
                new Pessoa()
        );

        return "pessoa";
    }

    @PostMapping
    public String insertPessoa(Pessoa pessoa) {

        pessoaServ.insertPessoa(pessoa);

        return "redirect:/pessoa";
    }

    @PostMapping("/excluir")
    public String excluirPessoa(
            @RequestParam UUID id) {

        pessoaServ.deletePessoa(id);

        return "redirect:/pessoa";
    }
}