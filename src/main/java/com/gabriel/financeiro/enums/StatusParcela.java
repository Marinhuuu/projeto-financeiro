package com.gabriel.financeiro.enums;

public enum StatusParcela {
	
	EM_ABERTO("Em aberto"),
	ATRASADA("Atrasada"),
	PENDENTE("Pendente"),
	PAGA("Paga");
	
	
	  private final String descricao;

    StatusParcela(String descricao) {
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
