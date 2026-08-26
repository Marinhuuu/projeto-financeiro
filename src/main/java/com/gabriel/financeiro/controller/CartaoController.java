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

	    model.addAttribute("cartaoCredito", new CartaoCredito());      // objeto do formulário
	    model.addAttribute("cartao", paginaCartao); // lista paginada

	    return "cartaodecredito";
	}
	
	@PostMapping("/create")
	public String insertCartaoBotao(CartaoCredito cartao) {
		 cartaoServ.insertCartao(cartao);
		 return "redirect:/cartao";
	}
	
	@GetMapping("/update/{id}")
	public String update(@PathVariable UUID id, Model model) {
		CartaoCredito Cartao = cartaoServ.getById(id);
		
		model.addAttribute("cartao", Cartao);
		model.addAttribute("modo", "update");
		
		return "cartaodecredito-upd";
	}
	 
	@PostMapping("/delete/{id}")
	public String delete(@PathVariable UUID id) {
		cartaoServ.deleteCartao(id);
		return "redirect:/cartao";
	}
	
	
	
}
