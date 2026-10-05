package com.gabriel.financeiro.config;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.gabriel.financeiro.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Bloqueia as rotas para quem não está logado.
 * As rotas públicas (login, primeiro acesso, estáticos) são excluídas em SegurancaConfig.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

	private final UsuarioService usuarioServ;

	public AutenticacaoInterceptor(UsuarioService usuarioServ) {
		this.usuarioServ = usuarioServ;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		UUID usuarioId = SessaoUsuario.usuarioId(request);

		if (usuarioId != null && usuarioServ.existe(usuarioId)) {
			return true;
		}

		// Usuário excluído enquanto logado: descarta a sessão
		SessaoUsuario.encerrar(request);

		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"sucesso\":false,\"mensagem\":\"Sessão expirada. Faça login novamente.\"}");
			return false;
		}

		response.sendRedirect(request.getContextPath() + "/login");
		return false;
	}
}
