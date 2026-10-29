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

import com.gabriel.financeiro.config.LimiteTentativas;
import com.gabriel.financeiro.config.SessaoUsuario;
import com.gabriel.financeiro.entities.Usuario;
import com.gabriel.financeiro.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

	// Tentativas erradas permitidas por janela (LimiteTentativas.JANELA)
	private static final int MAX_FALHAS_POR_CPF = 5;
	private static final int MAX_FALHAS_POR_IP = 20;

	private static final String MENSAGEM_BLOQUEIO = "Muitas tentativas erradas. Tente novamente em 15 minutos.";

	private final UsuarioService usuarioServ;
	private final LimiteTentativas limiteTentativas;

	public LoginController(UsuarioService usuarioServ, LimiteTentativas limiteTentativas) {
		this.usuarioServ = usuarioServ;
		this.limiteTentativas = limiteTentativas;
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

		String chaveCpf = chaveCpf(cpfNormalizado);
		String chaveIp = "ip:" + request.getRemoteAddr();

		if (limiteTentativas.bloqueado(chaveCpf, MAX_FALHAS_POR_CPF)
				|| limiteTentativas.bloqueado(chaveIp, MAX_FALHAS_POR_IP)) {
			return erroLogin(redirectAttributes, cpf, MENSAGEM_BLOQUEIO);
		}

		Optional<Usuario> usuario = usuarioServ.buscarPorCpf(cpfNormalizado);

		// Primeiro acesso: convite (cadastrado sem senha) ou instalação sem nenhum usuário
		if (usuario.map(Usuario::isPrimeiroAcessoPendente).orElseGet(usuarioServ::permiteCadastroSemConvite)) {
			request.getSession(true).setAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO, cpfNormalizado);
			return "redirect:/primeiro-acesso";
		}

		Optional<Usuario> autenticado = usuarioServ.autenticar(cpfNormalizado, senha);

		if (autenticado.isEmpty()) {
			limiteTentativas.registrarFalha(chaveCpf);
			limiteTentativas.registrarFalha(chaveIp);
			return erroLogin(redirectAttributes, cpf, "CPF ou senha incorretos");
		}

		limiteTentativas.limpar(chaveCpf);

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

		boolean liberado = existente.map(Usuario::isPrimeiroAcessoPendente)
				.orElseGet(usuarioServ::permiteCadastroSemConvite);

		if (!liberado) {
			request.getSession().removeAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO);
			return "redirect:/login";
		}

		// Após um erro, o formulário volta com o que foi digitado (flash "usuario").
		// Os dados do convite não são exibidos: quem chegou aqui só informou o CPF.
		if (!model.containsAttribute("usuario")) {
			model.addAttribute("usuario", new Usuario());
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

		if (limiteTentativas.bloqueado(chaveCpf(cpf), MAX_FALHAS_POR_CPF)) {
			redirectAttributes.addFlashAttribute("mensagem", MENSAGEM_BLOQUEIO);
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
			return "redirect:/primeiro-acesso";
		}

		try {
			Usuario usuario = usuarioServ.concluirPrimeiroAcesso(cpf, dados, senha, confirmacaoSenha);

			SessaoUsuario.iniciar(request, usuario);

			redirectAttributes.addFlashAttribute("mensagem", "Cadastro concluído! Bem-vindo, " + usuario.getNome() + ".");
			redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso");

			return "redirect:/home";

		} catch (UsuarioService.IdentidadeNaoConfirmada e) {
			limiteTentativas.registrarFalha(chaveCpf(cpf));
			redirectAttributes.addFlashAttribute("mensagem", e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");

			return "redirect:/primeiro-acesso";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("mensagem", e.getMessage());
			redirectAttributes.addFlashAttribute("tipoMensagem", "erro");
			redirectAttributes.addFlashAttribute("usuario", dados);

			return "redirect:/primeiro-acesso";
		}
	}

	private static String chaveCpf(String cpf) {
		return "cpf:" + cpf;
	}

	private String cpfPrimeiroAcesso(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		return sessao == null ? null : (String) sessao.getAttribute(SessaoUsuario.CPF_PRIMEIRO_ACESSO);
	}
}
