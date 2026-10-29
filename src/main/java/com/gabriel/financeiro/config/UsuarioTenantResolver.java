package com.gabriel.financeiro.config;

import java.util.Map;
import java.util.UUID;

import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/*
 * Cada usuário enxerga só os próprios dados.
 *
 * As entidades com campo @TenantId (usuarioId) são gravadas com o id do usuário
 * logado e o Hibernate acrescenta "usuario_id = ?" em todas as consultas delas.
 * Fora de uma requisição logada (login, tarefas de inicialização) vale SEM_USUARIO,
 * que não é dono de nada: as consultas voltam vazias.
 */
@Component
public class UsuarioTenantResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

	public static final UUID SEM_USUARIO = new UUID(0L, 0L);

	@Override
	public UUID resolveCurrentTenantIdentifier() {

		RequestAttributes atributos = RequestContextHolder.getRequestAttributes();

		if (atributos == null) {
			return SEM_USUARIO;
		}

		Object usuarioId = atributos.getAttribute(SessaoUsuario.USUARIO_ID, RequestAttributes.SCOPE_SESSION);

		return usuarioId instanceof UUID id ? id : SEM_USUARIO;
	}

	// A sessão do Hibernate (open-in-view) é aberta antes do login trocar o usuário da sessão HTTP
	@Override
	public boolean validateExistingCurrentSessions() {
		return false;
	}

	@Override
	public void customize(Map<String, Object> hibernateProperties) {
		hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
	}
}
