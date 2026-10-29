package com.gabriel.financeiro.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/*
 * O ciclo financeiro era único só pela data de início. Dias de fechamento diferentes
 * (ex.: 30 e 31) geram ciclos com o mesmo início e fins diferentes, e criar o segundo
 * violava essa restrição em todas as telas. A entidade agora é única por início + fim,
 * mas o ddl-auto=update não remove a restrição antiga: ela é removida aqui.
 */
@Component
public class RestricaoCicloConfig implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(RestricaoCicloConfig.class);

	private final JdbcTemplate jdbcTemplate;

	public RestricaoCicloConfig(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {

		try {
			List<String> antigas = jdbcTemplate.queryForList("""
					SELECT tc.constraint_name
					FROM information_schema.table_constraints tc
					JOIN information_schema.key_column_usage kcu
					  ON kcu.constraint_name = tc.constraint_name
					 AND kcu.table_schema = tc.table_schema
					WHERE tc.table_schema = current_schema()
					  AND tc.table_name = 'ciclo_financeiro'
					  AND tc.constraint_type = 'UNIQUE'
					GROUP BY tc.constraint_name
					HAVING COUNT(*) = 1 AND MAX(kcu.column_name) = 'data_inicio'
					""", String.class);

			for (String constraint : antigas) {
				jdbcTemplate.execute("ALTER TABLE ciclo_financeiro DROP CONSTRAINT \"" + constraint + "\"");
				log.info("Restrição única antiga {} de ciclo_financeiro removida", constraint);
			}

		} catch (Exception e) {
			// Não impede a aplicação de subir; só registra o problema
			log.warn("Não foi possível ajustar a restrição única de ciclo_financeiro: {}", e.getMessage());
		}
	}
}
