package com.gabriel.financeiro.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.CartaoCredito;
import com.gabriel.financeiro.repository.CartaoRepository;

@Service
public class CartaoService {

	private final CartaoRepository cartaoRepo;

	public CartaoService(CartaoRepository cartaoRepo) {
		this.cartaoRepo = cartaoRepo;
	}
	
	public Page<CartaoCredito> ListCartao(Pageable pageable){
		
		return cartaoRepo.findAll(pageable);
		
	}
	
	public CartaoCredito getById(UUID id) {
		
		return cartaoRepo.findById(id).orElseThrow(() -> new RuntimeException("Cartao não encontrado"));
		
	}
	
	public CartaoCredito insertCartao(CartaoCredito cartao) {
		return cartaoRepo.save(cartao);
	}
	
	public CartaoCredito updateCartao(UUID id, CartaoCredito cartao) { 
		
		CartaoCredito Cartao = cartaoRepo.findById(id).orElseThrow(() -> new RuntimeException("Cartao não encontrado"));
		
		Cartao.setNome(cartao.getNome());
		Cartao.setLimite(cartao.getLimite());
		Cartao.setDiaVencimento(cartao.getDiaVencimento());
		Cartao.setDiaFechamento(cartao.getDiaFechamento());
		Cartao.setDespesas(cartao.getDespesas());
		
		return cartaoRepo.save(Cartao);
	
	}
	
	public void deleteCartao(UUID id) {
		cartaoRepo.deleteById(id);
	}
	
}
