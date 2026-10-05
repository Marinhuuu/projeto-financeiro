package com.gabriel.financeiro.controller;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.SessaoUsuario;
import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/usuario")
public class UsuarioController {

	private final UsuarioService usuarioServ;

	public UsuarioController(UsuarioService usuarioServ) {
		this.usuarioServ = usuarioServ;
	}

	// O hash da senha só é definido no primeiro acesso, nunca por este formulário
	@InitBinder("usuario")
	public void configurarBinder(WebDataBinder binder) {
		binder.setDisallowedFields("senha");
	}

	// =========================================================
	// TELA: CADASTRO + LISTAGEM
	// =========================================================

	@GetMapping
	public String pageUsuario(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		carregarListagem(page, size, model);
		model.addAttribute("usuario", new Usuario());
		model.addAttribute("modo", "create");

		return "usuario";
	}

	@PostMapping
	public String insertUsuario(Usuario usuario, RedirectAttributes redirectAttributes) {
		try {
			usuarioServ.insertUsuario(usuario);
			redirectAttributes.addFlashAttribute("mensagem", "Usuário cadastrado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar usuário: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/usuario";
	}

	// =========================================================
	// EDIÇÃO
	// =========================================================

	@GetMapping("/update/{id}")
	public String update(@PathVariable UUID id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		carregarListagem(page, size, model);
		model.addAttribute("usuario", usuarioServ.getById(id));
		model.addAttribute("modo", "update");

		return "usuario";
	}

	@PostMapping("/update/{id}")
	public String salvarEdicao(@PathVariable UUID id, Usuario usuario, RedirectAttributes redirectAttributes) {
		try {
			usuarioServ.updateUsuario(id, usuario);
			redirectAttributes.addFlashAttribute("mensagem", "Usuário atualizado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar usuário: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
			return "redirect:/usuario/update/" + id;
		}
		return "redirect:/usuario";
	}

	// =========================================================
	// EXCLUSÃO
	// =========================================================

	@PostMapping("/excluir")
	public String excluirUsuario(@RequestParam UUID id, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		try {
			if (id.equals(SessaoUsuario.usuarioId(request))) {
				throw new RuntimeException("você não pode excluir o próprio usuário");
			}

			usuarioServ.deleteUsuario(id);
			redirectAttributes.addFlashAttribute("mensagem", "Usuário excluído com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir usuário: " + e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/usuario";
	}

	private void carregarListagem(int page, int size, Model model) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("nome").ascending().and(Sort.by("sobrenome")));
		model.addAttribute("paginaUsuario", usuarioServ.listUsuario(pageable));
	}
}
