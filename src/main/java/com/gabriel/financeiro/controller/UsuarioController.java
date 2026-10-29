package com.gabriel.financeiro.controller;

import java.util.UUID;

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

import com.gabriel.financeiro.config.MensagemErro;
import com.gabriel.financeiro.config.Paginacao;
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
			HttpServletRequest request, Model model) {

		carregarListagem(page, size, request, model);
		model.addAttribute("usuario", new Usuario());
		model.addAttribute("modo", "create");

		return "usuario";
	}

	@PostMapping
	public String insertUsuario(Usuario usuario, HttpServletRequest request, RedirectAttributes redirectAttributes) {
		try {
			usuarioServ.insertUsuario(usuario, SessaoUsuario.usuarioId(request));
			redirectAttributes.addFlashAttribute("mensagem", "Usuário cadastrado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao cadastrar usuário: " + MensagemErro.de(e));
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
			HttpServletRequest request, Model model) {

		carregarListagem(page, size, request, model);
		model.addAttribute("usuario", usuarioServ.getGerenciavel(id, SessaoUsuario.usuarioId(request)));
		model.addAttribute("modo", "update");

		return "usuario";
	}

	@PostMapping("/update/{id}")
	public String salvarEdicao(@PathVariable UUID id, Usuario usuario, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {
		try {
			usuarioServ.updateUsuario(id, usuario, SessaoUsuario.usuarioId(request));
			redirectAttributes.addFlashAttribute("mensagem", "Usuário atualizado com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao atualizar usuário: " + MensagemErro.de(e));
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

			usuarioServ.deleteUsuario(id, SessaoUsuario.usuarioId(request));
			redirectAttributes.addFlashAttribute("mensagem", "Usuário excluído com sucesso!");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", "Erro ao excluir usuário: " + MensagemErro.de(e));
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		}
		return "redirect:/usuario";
	}

	private void carregarListagem(int page, int size, HttpServletRequest request, Model model) {
		Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending().and(Sort.by("sobrenome")));
		model.addAttribute("paginaUsuario", usuarioServ.listUsuario(SessaoUsuario.usuarioId(request), pageable));
		model.addAttribute("usuarioLogadoId", SessaoUsuario.usuarioId(request));
	}
}
