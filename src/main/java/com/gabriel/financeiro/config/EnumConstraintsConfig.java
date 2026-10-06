package com.gabriel.financeiro.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.gabriel.financeiro.enums.StatusFatura;
import com.gabriel.financeiro.enums.StatusParcela;

/*
 * O Hibernate cria um CHECK com os valores do enum para colunas @Enumerated(STRING),
 * mas o ddl-auto=update nunca atualiza esse CHECK. Quando um valor novo entra no enum
 * (ex.: StatusFatura.PAGA), gravar esse valor falha com "could not execute statement".
 * Na subida da aplicação, recria os CHECKs com os valores atuais dos enums.
 */
@Component
public class EnumConstraintsConfig implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(EnumConstraintsConfig.class);

	private final JdbcTemplate jdbcTemplate;

	public EnumConstraintsConfig(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		recriarCheck("fatura_cartao", "status_fatura", StatusFatura.values());
		recriarCheck("parcela", "status_parcela", StatusParcela.values());
	}

	private void recriarCheck(String tabela, String coluna, Enum<?>[] valores) {

		String constraint = tabela + "_" + coluna + "_check";

		String permitidos = Arrays.stream(valores)
				.map(v -> "'" + v.name() + "'")
				.collect(Collectors.joining(", "));

		try {
			jdbcTemplate.execute("ALTER TABLE " + tabela + " DROP CONSTRAINT IF EXISTS " + constraint);
			jdbcTemplate.execute("ALTER TABLE " + tabela + " ADD CONSTRAINT " + constraint
					+ " CHECK (" + coluna + " IN (" + permitidos + "))");
		} catch (Exception e) {
			// Não impede a aplicação de subir; só registra o problema
			log.warn("Não foi possível atualizar o CHECK {}: {}", constraint, e.getMessage());
		}
	}
}
