package com.gabriel.financeiro.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/*
 * Erros não tratados nos controllers (ex.: id inexistente em /faturas/{id},
 * UUID inválido na URL): em vez da página de erro 500, volta para a home com a
 * mensagem (ver MensagemErro). Requisições AJAX recebem JSON.
 */
@ControllerAdvice
public class TratamentoErrosAdvice {

	private static final String HOME = "/home";

	@ExceptionHandler(RuntimeException.class)
	public Object tratar(RuntimeException e, HttpServletRequest request, HttpServletResponse response) {

		String mensagem = MensagemErro.de(e);

		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			return ResponseEntity.badRequest().body(Map.of("sucesso", false, "mensagem", mensagem));
		}

		// A própria home falhou: redirecionar para ela entraria em loop
		if (HOME.equals(request.getRequestURI())) {
			ModelAndView erro = new ModelAndView("error");
			erro.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
			erro.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
			return erro;
		}

		FlashMap flash = RequestContextUtils.getOutputFlashMap(request);
		flash.put("mensagem", mensagem);
		flash.put("tipoMensagem", "erro");
		RequestContextUtils.saveOutputFlashMap(HOME, request, response);

		return "redirect:" + HOME;
	}
}
