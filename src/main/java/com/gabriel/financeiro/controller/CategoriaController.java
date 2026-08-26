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

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.service.CategoriaService;

@Controller
@RequestMapping("/categoria")
public class CategoriaController {
	
	private final CategoriaService categoriaServ;

	public CategoriaController(CategoriaService categoriaServ) {
		this.categoriaServ = categoriaServ;
	}

	// Tela principal: lista + formulário de criação
	@GetMapping
	public String pageCategoria(@RequestParam(defaultValue = "0") int page, 
	                            @RequestParam(defaultValue = "10") int size,
	                            Model model) {

		Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
		Page<Categoria> paginaCategorias = categoriaServ.listCategoria(pageable);

		model.addAttribute("categoria", new Categoria()); // Objeto do formulário
		model.addAttribute("categorias", paginaCategorias.getContent()); // Lista paginada

		return "categoria";
	}

//	@GetMapping("/inserir")
//	public String insert(Model model) {
//		model.addAttribute("categoria", new Categoria());
//		model.addAttribute("modo", "insert");
//
//		return "categoria";
//	}

	// Salva nova categoria (recebe do form em categoria.html)
	@PostMapping
	public String insertCategoriaBotao(Categoria categoria) {
		categoriaServ.insertCategoria(categoria);
		return "redirect:/categoria";
	}

	// Atualiza categoria existente (recebe do form em categoria-upd.html)
	@PostMapping("/create")
	public String salvarEdicaoCategoria(Categoria categoria) {
		categoriaServ.insertCategoria(categoria); // Ou o método equivalente de update na sua Service
		return "redirect:/categoria";
	}

	// Carrega a tela de edição (categoria-upd.html)
	@GetMapping("/update/{id}")
	public String update(@PathVariable UUID id, Model model) {
		Categoria categoria = categoriaServ.getById(id);

		model.addAttribute("categoria", categoria);
		model.addAttribute("modo", "update");

		return "categoria-upd";
	}

	// Executa a exclusão da categoria
	@PostMapping("/delete/{id}")
	public String delete(@PathVariable UUID id) {
		categoriaServ.deleteCategoria(id); // Certifique-se de que este método existe na sua Service
		return "redirect:/categoria";
	}
}