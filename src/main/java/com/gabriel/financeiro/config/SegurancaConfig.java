package com.gabriel.financeiro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SegurancaConfig implements WebMvcConfigurer {

	private final AutenticacaoInterceptor autenticacaoInterceptor;

	public SegurancaConfig(AutenticacaoInterceptor autenticacaoInterceptor) {
		this.autenticacaoInterceptor = autenticacaoInterceptor;
	}

	// Hash BCrypt com salt aleatório; só o hash é gravado no banco.
	// static: o UsuarioService (usado pelo interceptor) depende deste bean.
	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(autenticacaoInterceptor)
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/login",
						"/primeiro-acesso",
						"/error",
						"/js/**",
						"/css/**",
						"/images/**",
						"/favicon.ico");
	}
}
