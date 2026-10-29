package com.gabriel.financeiro.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/*
 * Paginação a partir dos parâmetros "page"/"size" da URL.
 * Página negativa vira 0 e o tamanho fica entre 1 e TAMANHO_MAXIMO, para que um
 * valor inválido não gere erro 500 nem carregue a tabela inteira.
 */
public final class Paginacao {

	public static final int TAMANHO_MAXIMO = 100;

	private Paginacao() {
	}

	public static Pageable de(int page, int size, Sort sort) {
		return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), TAMANHO_MAXIMO), sort);
	}

	public static Pageable de(int page, int size) {
		return de(page, size, Sort.unsorted());
	}
}
