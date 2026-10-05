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

import com.gabriel.financeiro.entities.Categoria;
import com.gabriel.financeiro.service.CategoriaService;

@Controller
@RequestMapping("/categoria")
public class CategoriaController {

	private final CategoriaService categoriaServ;

	public CategoriaController(CategoriaService categoriaServ) {
		this.categoriaServ = categoriaServ;
	}

	// Tela principal: lista + formulario de criacao
	@GetMapping
	public String pageCategoria(@RequestParam(defaultValue = "0") int page,
	                            @RequestParam(defaultValue = "50") int size,
	                            @RequestParam(required = false) String tipo,
	                            Model model) {

		// Filtro da listagem: DESPESA, RECEITA ou todas (qualquer outro valor)
		String filtro = "DESPESA".equalsIgnoreCase(tipo) || "RECEITA".equalsIgnoreCase(tipo) ? tipo.toUpperCase() : null;

		Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending());
		Page<Categoria> paginaCategorias = categoriaServ.listCategoria(filtro, pageable);

		Categoria nova = new Categoria();
		nova.setTipo(filtro != null ? filtro : "DESPESA");

		model.addAttribute("categoria", nova);
		model.addAttribute("categorias", paginaCategorias.getContent());
		model.addAttribute("filtroTipo", filtro);
		model.addAttribute("totalDespesa", categoriaServ.contarPorTipo("DESPESA"));
		model.addAttribute("totalReceita", categoriaServ.contarPorTipo("RECEITA"));

		return "categoria";
	}

	// Salva nova categoria
	@PostMapping
	public String insertCategoriaBotao(Categoria categoria, RedirectAttributes redirectAttributes) {
		try {
			categoriaServ.insertCategoria(categoria);
			redirectAttributes.addFlashAttribute("mensagem", "Categoria cadastrada com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar categoria: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/categoria";
	}

	// Atualiza categoria existente
	@PostMapping("/update/{id}")
	public String salvarEdicaoCategoria(@PathVariable UUID id, Categoria categoria, RedirectAttributes redirectAttributes) {
		try {
			categoriaServ.updateCategoria(id, categoria);
			redirectAttributes.addFlashAttribute("mensagem", "Categoria atualizada com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar categoria: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/categoria";
	}

	// Carrega a tela de edicao
	@GetMapping("/update/{id}")
	public String update(@PathVariable UUID id, Model model) {
		Categoria categoria = categoriaServ.getById(id);

		model.addAttribute("categoria", categoria);
		model.addAttribute("modo", "update");

		return "categoria-upd";
	}

	// Executa a exclusao da categoria
	@PostMapping("/delete/{id}")
	public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
		try {
			categoriaServ.deleteCategoria(id);
			redirectAttributes.addFlashAttribute("mensagem", "Categoria excluida com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir categoria: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/categoria";
	}
}
