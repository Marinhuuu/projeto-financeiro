package com.gabriel.financeiro.enums;

public enum StatusFatura {

	VENCIDA("Vencida"),
	EM_ABERTO("Em Aberto"),
	PENDENTE("Pendente"),
	FECHADA("Fechada");
	   private final String descricao;

	    StatusFatura(String descricao) {
	        this.descricao = descricao;
	    } 

	    public String getDescricao() {
	        return descricao;
	    }
	    @Override
	    public String toString() { 
	        return descricao;
	    }
	
}

