package com.gabriel.financeiro.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.service.CartaoService;

@Controller
@RequestMapping("/cartao")
public class CartaoController {

	private final CartaoService cartaoServ;

	public CartaoController(CartaoService cartaoServ) {
		this.cartaoServ = cartaoServ;
	}

	@GetMapping
	public String pageCartao(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
		Page<CartaoCredito> paginaCartao = cartaoServ.ListCartao(pageable);

		model.addAttribute("cartaoCredito", new CartaoCredito());
		model.addAttribute("cartao", paginaCartao);

		return "cartaodecredito";
	}

	@PostMapping("/create")
	public String insertCartaoBotao(CartaoCredito cartao, RedirectAttributes redirectAttributes) {
		try {
			cartaoServ.insertCartao(cartao);
			redirectAttributes.addFlashAttribute("mensagem", "Cartao cadastrado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar cartao: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/cartao";
	}

	@GetMapping("/update/{id}")
	public String update(@PathVariable UUID id, Model model) {
		CartaoCredito cartao = cartaoServ.getById(id);

		model.addAttribute("cartao", cartao);
		model.addAttribute("modo", "update");

		return "cartaodecredito-upd";
	}

	@PostMapping("/update/{id}")
	public String salvarEdicao(@PathVariable UUID id, CartaoCredito cartao, RedirectAttributes redirectAttributes) {
		try {
			cartaoServ.updateCartao(id, cartao);
			redirectAttributes.addFlashAttribute("mensagem", "Cartao atualizado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar cartao: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/cartao";
	}

	@PostMapping("/delete/{id}")
	public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
		try {
			cartaoServ.deleteCartao(id);
			redirectAttributes.addFlashAttribute("mensagem", "Cartao excluido com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir cartao: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/cartao";
	}
}
