package com.gabriel.financeiro.config;

import java.util.UUID;

import com.gabriel.financeiro.entities.Usuario;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/*
 * Chaves e operações da sessão do usuário logado.
 */
public final class SessaoUsuario {

	public static final String USUARIO_ID = "usuarioLogadoId";
	public static final String USUARIO_NOME = "usuarioLogadoNome";

	// CPF validado na tela de login, aguardando o cadastro de primeiro acesso
	public static final String CPF_PRIMEIRO_ACESSO = "cpfPrimeiroAcesso";

	private SessaoUsuario() {
	}

	/*
	 * Descarta a sessão anterior e cria uma nova (evita fixação de sessão).
	 */
	public static void iniciar(HttpServletRequest request, Usuario usuario) {

		HttpSession anterior = request.getSession(false);

		if (anterior != null) {
			anterior.invalidate();
		}

		HttpSession sessao = request.getSession(true);

		sessao.setAttribute(USUARIO_ID, usuario.getId());
		sessao.setAttribute(USUARIO_NOME, usuario.getNome());
	}

	public static UUID usuarioId(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		return sessao == null ? null : (UUID) sessao.getAttribute(USUARIO_ID);
	}

	public static void encerrar(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		if (sessao != null) {
			sessao.invalidate();
		}
	}
}
