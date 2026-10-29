package com.gabriel.financeiro.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/*
 * Mensagem de erro que pode ser mostrada ao usuário.
 *
 * Erros de negócio são lançados como RuntimeException com a mensagem em pt-BR e
 * são exibidos como estão. Qualquer outra exceção (banco, bugs) tem mensagem
 * técnica: é registrada no log e o usuário vê uma mensagem genérica.
 */
public final class MensagemErro {

	private static final Logger log = LoggerFactory.getLogger(MensagemErro.class);

	public static final String GENERICA = "Não foi possível concluir a operação. Tente novamente.";

	private MensagemErro() {
	}

	public static String de(Exception e) {

		if (e.getClass() == RuntimeException.class && e.getMessage() != null) {
			return e.getMessage();
		}

		if (e instanceof MethodArgumentTypeMismatchException) {
			return "Endereço inválido.";
		}

		log.error("Erro inesperado", e);

		return GENERICA;
	}
}
