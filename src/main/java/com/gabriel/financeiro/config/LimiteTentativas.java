package com.gabriel.financeiro.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/*
 * Limita tentativas erradas de login e de primeiro acesso (força bruta).
 *
 * As falhas são contadas por chave (ex.: "cpf:123..." e "ip:1.2.3.4"). Ao atingir
 * o máximo dentro da janela, a chave fica bloqueada até a janela acabar.
 * Fica em memória: reiniciar a aplicação zera os contadores.
 */
@Component
public class LimiteTentativas {

	public static final Duration JANELA = Duration.ofMinutes(15);

	private static final int LIMPAR_ACIMA_DE = 10_000;

	private final Map<String, Registro> falhas = new ConcurrentHashMap<>();

	private record Registro(int quantidade, Instant inicio) {

		boolean expirado(Instant agora) {
			return inicio.plus(JANELA).isBefore(agora);
		}
	}

	public boolean bloqueado(String chave, int maximo) {

		Registro registro = falhas.get(chave);

		if (registro == null) {
			return false;
		}

		if (registro.expirado(Instant.now())) {
			falhas.remove(chave, registro);
			return false;
		}

		return registro.quantidade() >= maximo;
	}

	public void registrarFalha(String chave) {

		Instant agora = Instant.now();

		if (falhas.size() > LIMPAR_ACIMA_DE) {
			falhas.values().removeIf(r -> r.expirado(agora));
		}

		falhas.compute(chave, (k, atual) -> atual == null || atual.expirado(agora)
				? new Registro(1, agora)
				: new Registro(atual.quantidade() + 1, atual.inicio()));
	}

	public void limpar(String chave) {
		falhas.remove(chave);
	}
}
