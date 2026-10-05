package com.gabriel.financeiro.controller;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gabriel.financeiro.config.SessaoUsuario;
import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

	private final UsuarioService usuarioServ;

	public LoginController(UsuarioService usuarioServ) {
		this.usuarioServ = usuarioServ;
	}

	// O formulário de primeiro acesso nunca define id, CPF ou hash diretamente
	@InitBinder("usuario")
	public void configurarBinder(WebDataBinder binder) {
		binder.setDisallowedFields("id", "cpf", "senha");
	}

	// =========================================================
	// LOGIN
	// =========================================================

	@GetMapping("/login")
	public String pageLogin(HttpServletRequest request) {

		if (SessaoUsuario.usuarioId(request) != null) {
			return "redirect:/home";
		}

		return "login";
	}

	@PostMapping("/login")
	public String login(@RequestParam String cpf,
			@RequestParam(required = false) String senha,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		String cpfNormalizado = UsuarioService.normalizarCpf(cpf);

		if (!UsuarioService.cpfValido(cpfNormalizado)) {
			return erroLogin(redirectAttributes, cpf, "CPF inválido");
		}

		Optional<Usuario> usuario = usuarioServ.buscarPorCpf(cpfNormalizado);

		// Primeiro acesso: CPF sem cadastro ou cadastrado sem senha
		if (usuario.isEmpty() || usuario.get().isPrimeiroAcessoPendente()) {
			request.getSession(true).setAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO, cpfNormalizado);
			return "redirect:/primeiro-acesso";
		}

		Optional<Usuario> autenticado = usuarioServ.autenticar(cpfNormalizado, senha);

		if (autenticado.isEmpty()) {
			return erroLogin(redirectAttributes, cpf, "CPF ou senha incorretos");
		}

		SessaoUsuario.iniciar(request, autenticado.get());

		return "redirect:/home";
	}

	private String erroLogin(RedirectAttributes redirectAttributes, String cpf, String mensagem) {
		redirectAttributes.addFlashAttribute("mensagem", mensagem);
		redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
		redirectAttributes.addFlashAttribute("cpfInformado", cpf);
		return "redirect:/login";
	}

	@PostMapping("/logout")
	public String logout(HttpServletRequest request, RedirectAttributes redirectAttributes) {

		SessaoUsuario.encerrar(request);

		redirectAttributes.addFlashAttribute("mensagem", "Você saiu do sistema.");
		redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");

		return "redirect:/login";
	}

	// =========================================================
	// PRIMEIRO ACESSO (somente uma vez por CPF)
	// =========================================================

	@GetMapping("/primeiro-acesso")
	public String pagePrimeiroAcesso(HttpServletRequest request, Model model) {

		String cpf = cpfPrimeiroAcesso(request);

		if (cpf == null) {
			return "redirect:/login";
		}

		Optional<Usuario> existente = usuarioServ.buscarPorCpf(cpf);

		if (existente.isPresent() && !existente.get().isPrimeiroAcessoPendente()) {
			request.getSession().removeAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO);
			return "redirect:/login";
		}

		// Após um erro, o formulário volta com o que foi digitado (flash "usuario")
		if (!model.containsAttribute("usuario")) {
			model.addAttribute("usuario", existente.orElseGet(Usuario::new));
		}

		Usuario exibicao = new Usuario();
		exibicao.setCpf(cpf);

		model.addAttribute("cpfFormatado", exibicao.getCpfFormatado());
		model.addAttribute("cadastroExistente", existente.isPresent());

		return "primeiro-acesso";
	}

	@PostMapping("/primeiro-acesso")
	public String concluirPrimeiroAcesso(@ModelAttribute("usuario") Usuario dados,
			@RequestParam(required = false) String senha,
			@RequestParam(required = false) String confirmacaoSenha,
			HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		String cpf = cpfPrimeiroAcesso(request);

		if (cpf == null) {
			return "redirect:/login";
		}

		try {
			Usuario usuario = usuarioServ.concluirPrimeiroAcesso(cpf, dados, senha, confirmacaoSenha);

			SessaoUsuario.iniciar(request, usuario);

			redirectAttributes.addFlashAttribute("mensagem", "Cadastro concluído! Bem-vindo, " + usuario.getNome() + ".");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");

			return "redirect:/home";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
			redirectAttributes.addFlashAttribute("usuario", dados);

			return "redirect:/primeiro-acesso";
		}
	}

	private String cpfPrimeiroAcesso(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		return sessao == null ? null : (String) sessao.getAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO);
	}
}
