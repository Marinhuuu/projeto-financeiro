package com.gabriel.financeiro.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/*
 * Disponibiliza o nome do usuário logado para a navbar e o token CSRF em todas as telas.
 */
@ControllerAdvice
public class UsuarioLogadoAdvice {

	@ModelAttribute("usuarioLogadoNome")
	public String usuarioLogadoNome(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		return sessao == null ? null : (String) sessao.getAttribute(SessaoUsuario.USUARIO_NOME);
	}

	// Token CSRF para chamadas AJAX; criado aqui, antes da renderização (ver ProtecaoCsrf)
	@ModelAttribute("csrfToken")
	public String csrfToken(HttpServletRequest request) {
		return ProtecaoCsrf.token(request);
	}
}
