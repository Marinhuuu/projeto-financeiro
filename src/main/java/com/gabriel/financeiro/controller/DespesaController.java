package com.gabriel.financeiro.controller;

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

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.entities.Despesa;
import com.gabriel.financeiro.service.CartaoService;
import com.gabriel.financeiro.service.CategoriaService;
import com.gabriel.financeiro.service.DespesaService;
import com.gabriel.financeiro.service.PessoaService;

@Controller
@RequestMapping("/despesa")
public class DespesaController {

	private final DespesaService despesaServ;
	private final CategoriaService categoriaServ;
	private final CartaoService cartaoServ;
	private final PessoaService pessoaServ;

	public DespesaController(DespesaService despesaServ, CategoriaService categoriaServ, CartaoService cartaoServ,
			PessoaService pessoaServ) {

		this.despesaServ = despesaServ;
		this.categoriaServ = categoriaServ;
		this.cartaoServ = cartaoServ;
		this.pessoaServ = pessoaServ;
	}

	// =========================================================
	// TELA DE CADASTRO DE DESPESA
	// =========================================================

	@GetMapping
	public String pageDespesa(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("descricao").ascending());

		Page<Despesa> paginaDespesa = despesaServ.ListDespesa(pageable);

		Despesa despesa = new Despesa();

		despesa.setCategoria(new Categoria());

		despesa.setCartao(new CartaoCredito());

		model.addAttribute("despesa", despesa);

		model.addAttribute("paginaDespesa", paginaDespesa);

		// =====================================================
		// CATEGORIAS
		// =====================================================

		Pageable pageableCategoria = PageRequest.of(0, 100, Sort.by("nome").ascending());

		model.addAttribute("categorias", categoriaServ.listCategoria(pageableCategoria));

		// =====================================================
		// CARTÕES
		// =====================================================

		Pageable pageableCartao = PageRequest.of(0, 100, Sort.by("nome").ascending());

		model.addAttribute("cartoes", cartaoServ.ListCartao(pageableCartao));

		// =====================================================
		// PESSOAS
		// =====================================================

		Pageable pageablePessoa = PageRequest.of(0, 100, Sort.by("nome").ascending());

		model.addAttribute("pessoas", pessoaServ.listPessoa(pageablePessoa));

		return "despesa";
	}

	// =========================================================
	// INSERIR DESPESA
	// =========================================================

	@PostMapping
	public String insertDespesa(Despesa despesa) {

		despesaServ.insertDespesa(despesa);

		return "redirect:/despesa";
	}

	// =========================================================
	// GERENCIAR DESPESAS
	// =========================================================

	@GetMapping("/despesas")
	public String gerenciarDespesas(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("dataCompra").descending());

		Page<Despesa> paginaDespesa = despesaServ.ListDespesa(pageable);

		model.addAttribute("paginaDespesa", paginaDespesa);

		return "despesas";
	}

	// =========================================================
	// EXCLUIR
	// =========================================================

	@PostMapping("/excluir")
	public String excluirDespesa(@RequestParam UUID id) {

		despesaServ.excluirDespesa(id);

		return "redirect:/despesa/despesas";
	}

	// =========================================================
	// DESPESAS DE TERCEIROS
	// =========================================================

	@GetMapping("/terceiros")
	public String gastosDeTerceiros(Model model) {

		List<Despesa> despesasTerceiros = despesaServ.listarDespesasDeTerceiros();

		model.addAttribute("despesasTerceiros", despesasTerceiros);

		return "despesas-terceiros";
	}

	// =========================================================
	// MARCAR COMO DEVOLVIDO
	// =========================================================

	@PostMapping("/terceiros/devolver")
	public String marcarComoDevolvido(@RequestParam UUID id) {

		despesaServ.marcarComoDevolvido(id);

		return "redirect:/despesa/terceiros";
	}

	// =========================================================
	// DESFAZER DEVOLUÇÃO
	// =========================================================

	@PostMapping("/terceiros/desfazer-devolucao")
	public String marcarComoNaoDevolvido(@RequestParam UUID id) {

		despesaServ.marcarComoNaoDevolvido(id);

		return "redirect:/despesa/terceiros";
	}
}