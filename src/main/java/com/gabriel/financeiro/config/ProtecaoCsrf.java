package com.gabriel.financeiro.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/*
 * Proteção contra CSRF sem o Spring Security.
 *
 * Cada sessão tem um token aleatório. O Thymeleaf chama este bean (pelo nome
 * "requestDataValueProcessor") ao montar todo <form th:action> e inclui o token no
 * campo oculto "_csrf". Requisições AJAX enviam o token no cabeçalho X-CSRF-TOKEN
 * (disponível no model como "csrfToken", ver UsuarioLogadoAdvice).
 * Como interceptor, recusa com 403 qualquer POST/PUT/PATCH/DELETE sem o token da sessão.
 */
@Component("requestDataValueProcessor")
public class ProtecaoCsrf implements RequestDataValueProcessor, HandlerInterceptor {

	public static final String PARAMETRO = "_csrf";
	public static final String CABECALHO = "X-CSRF-TOKEN";

	private static final String CHAVE_SESSAO = "csrfToken";

	// Marca que o formulário em montagem é GET (não leva o token na URL)
	private static final String FORM_GET = ProtecaoCsrf.class.getName() + ".FORM_GET";

	private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

	private static final SecureRandom RANDOM = new SecureRandom();

	/*
	 * Token da sessão atual (cria a sessão e o token se ainda não existirem).
	 */
	public static String token(HttpServletRequest request) {

		HttpSession sessao = request.getSession(true);

		String token = (String) sessao.getAttribute(CHAVE_SESSAO);

		if (token == null) {
			byte[] bytes = new byte[32];
			RANDOM.nextBytes(bytes);
			token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
			sessao.setAttribute(CHAVE_SESSAO, token);
		}

		return token;
	}

	// =========================================================
	// VALIDAÇÃO (interceptor)
	// =========================================================

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		if (METODOS_SEGUROS.contains(request.getMethod()) || tokenValido(request)) {
			return true;
		}

		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("{\"sucesso\":false,\"mensagem\":\"Sessão expirada. Recarregue a página.\"}");
			return false;
		}

		response.sendError(HttpStatus.FORBIDDEN.value(), "Formulário expirado. Recarregue a página e tente novamente.");
		return false;
	}

	private boolean tokenValido(HttpServletRequest request) {

		HttpSession sessao = request.getSession(false);

		String esperado = sessao == null ? null : (String) sessao.getAttribute(CHAVE_SESSAO);

		String recebido = request.getHeader(CABECALHO);

		if (recebido == null) {
			recebido = request.getParameter(PARAMETRO);
		}

		return esperado != null && recebido != null && MessageDigest.isEqual(
				esperado.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
	}

	// =========================================================
	// CAMPO OCULTO NOS FORMULÁRIOS (RequestDataValueProcessor)
	// =========================================================

	@Override
	public String processAction(HttpServletRequest request, String action, String httpMethod) {

		if (httpMethod != null && "GET".equalsIgnoreCase(httpMethod)) {
			request.setAttribute(FORM_GET, Boolean.TRUE);
		} else {
			request.removeAttribute(FORM_GET);
		}

		return action;
	}

	@Override
	public String processFormFieldValue(HttpServletRequest request, String name, String value, String type) {
		return value;
	}

	@Override
	public Map<String, String> getExtraHiddenFields(HttpServletRequest request) {

		if (Boolean.TRUE.equals(request.getAttribute(FORM_GET))) {
			request.removeAttribute(FORM_GET);
			return Map.of();
		}

		return Map.of(PARAMETRO, token(request));
	}

	@Override
	public String processUrl(HttpServletRequest request, String url) {
		return url;
	}
}
