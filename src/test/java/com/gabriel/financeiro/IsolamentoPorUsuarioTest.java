package com.gabriel.financeiro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.gabriel.financeiro.config.SessaoUsuario;
import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.repository.PessoaRepository;

/*
 * Os dados de um usuário não aparecem para outro (@TenantId + UsuarioTenantResolver).
 */
@SpringBootTest
class IsolamentoPorUsuarioTest {

	@Autowired
	private PessoaRepository pessoaRepo;

	@Autowired
	private TransactionTemplate tx;

	// Executa a ação numa transação própria, como se o usuário estivesse logado
	private <T> T comoUsuario(UUID usuarioId, Supplier<T> acao) {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession(true).setAttribute(SessaoUsuario.USUARIO_ID, usuarioId);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		try {
			return tx.execute(status -> acao.get());
		} finally {
			RequestContextHolder.resetRequestAttributes();
		}
	}

	@Test
	void usuarioNaoVeDadosDeOutroUsuario() {

		UUID dono = UUID.randomUUID();
		UUID outro = UUID.randomUUID();

		Pessoa salva = comoUsuario(dono, () -> pessoaRepo.save(new Pessoa(null, "Teste de isolamento")));

		try {
			assertEquals(dono, salva.getUsuarioId());

			assertTrue(comoUsuario(dono, () -> pessoaRepo.findById(salva.getId())).isPresent());

			assertTrue(comoUsuario(outro, () -> pessoaRepo.findById(salva.getId())).isEmpty());
			assertTrue(comoUsuario(outro, () -> pessoaRepo.findAll()).isEmpty());
			assertTrue(comoUsuario(outro, () -> pessoaRepo.existsById(salva.getId())) == Boolean.FALSE);

		} finally {
			comoUsuario(dono, () -> {
				pessoaRepo.deleteById(salva.getId());
				return null;
			});
		}
	}
}
