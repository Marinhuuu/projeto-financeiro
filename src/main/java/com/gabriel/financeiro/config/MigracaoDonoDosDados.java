package com.gabriel.financeiro.config;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.gabriel.financeiro.service.UsuarioService;

/*
 * Os dados passaram a ter dono (coluna usuario_id, ver UsuarioTenantResolver).
 * O ddl-auto=update tenta criar a coluna como NOT NULL e falha em tabelas que já têm
 * linhas, por isso esta migração roda antes do Hibernate:
 * 1. cria usuario_id (aceitando nulo) nas tabelas que ainda não têm;
 * 2. atribui as linhas sem dono ao usuário do CPF em CPF_DONO_DADOS_ANTIGOS
 *    ou, sem essa variável, ao único usuário cadastrado;
 * 3. sem dono definido, as linhas ficam sem dono (invisíveis) e um aviso é registrado.
 */
@Configuration
public class MigracaoDonoDosDados implements InitializingBean {

	private static final Logger log = LoggerFactory.getLogger(MigracaoDonoDosDados.class);

	private static final List<String> TABELAS = List.of(
			"categoria", "pessoa", "cartao_credito", "receita", "despesa", "parcela", "fatura_cartao");

	private final JdbcTemplate jdbcTemplate;
	private final String cpfDono;

	public MigracaoDonoDosDados(JdbcTemplate jdbcTemplate, @Value("${CPF_DONO_DADOS_ANTIGOS:}") String cpfDono) {
		this.jdbcTemplate = jdbcTemplate;
		this.cpfDono = cpfDono;
	}

	// Garante que a migração rode antes de o Hibernate atualizar o schema
	@Bean
	public static EntityManagerFactoryDependsOnPostProcessor migracaoAntesDoHibernate() {
		return new EntityManagerFactoryDependsOnPostProcessor(MigracaoDonoDosDados.class);
	}

	@Override
	public void afterPropertiesSet() {

		try {
			List<String> semDono = TABELAS.stream()
					.filter(this::tabelaExiste)
					.peek(this::criarColuna)
					.filter(tabela -> contarSemDono(tabela) > 0)
					.toList();

			if (semDono.isEmpty()) {
				return;
			}

			UUID dono = buscarDono();

			if (dono == null) {
				log.warn("Há registros sem dono em {}. Defina CPF_DONO_DADOS_ANTIGOS com o CPF do usuário "
						+ "que deve recebê-los; até lá eles não aparecem para ninguém.", semDono);
				return;
			}

			for (String tabela : semDono) {
				int linhas = jdbcTemplate.update("UPDATE " + tabela + " SET usuario_id = ? WHERE usuario_id IS NULL", dono);
				log.info("{} registro(s) de {} atribuídos ao usuário {}", linhas, tabela, dono);
			}

		} catch (Exception e) {
			// Não impede a aplicação de subir; só registra o problema
			log.warn("Não foi possível migrar o dono dos dados: {}", e.getMessage());
		}
	}

	private boolean tabelaExiste(String tabela) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?)",
				Boolean.class, tabela));
	}

	private void criarColuna(String tabela) {
		jdbcTemplate.execute("ALTER TABLE " + tabela + " ADD COLUMN IF NOT EXISTS usuario_id uuid");
	}

	private long contarSemDono(String tabela) {
		Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE usuario_id IS NULL", Long.class);
		return total == null ? 0 : total;
	}

	private UUID buscarDono() {

		if (!tabelaExiste("usuario")) {
			return null;
		}

		String cpf = UsuarioService.normalizarCpf(cpfDono);

		List<UUID> usuarios = cpf.isEmpty()
				? jdbcTemplate.queryForList("SELECT id FROM usuario", UUID.class)
				: jdbcTemplate.queryForList("SELECT id FROM usuario WHERE cpf = ?", UUID.class, cpf);

		if (usuarios.size() == 1) {
			return usuarios.get(0);
		}

		if (!cpf.isEmpty()) {
			log.warn("CPF_DONO_DADOS_ANTIGOS não corresponde a nenhum usuário cadastrado");
		}

		return null;
	}
}
