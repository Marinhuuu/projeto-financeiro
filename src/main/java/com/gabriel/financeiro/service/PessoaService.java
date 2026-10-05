package com.gabriel.financeiro.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gabriel.financeiro.entities.Pessoa;
import com.gabriel.financeiro.repository.DespesaRepository;
import com.gabriel.financeiro.repository.PessoaRepository;

@Service
public class PessoaService {

    private final PessoaRepository pessoaRepo;
    private final DespesaRepository despesaRepo;

    public PessoaService(PessoaRepository pessoaRepo, DespesaRepository despesaRepo) {
        this.pessoaRepo = pessoaRepo;
        this.despesaRepo = despesaRepo;
    }

    public Page<Pessoa> listPessoa(Pageable pageable) {

        return pessoaRepo.findAll(pageable);

    }

    public Pessoa getById(UUID id) {

        return pessoaRepo.findById(id)
                .orElseThrow(() ->
                    new RuntimeException("Pessoa não encontrada")
                );

    }

    public Pessoa insertPessoa(Pessoa pessoa) {

        if (pessoa.getNome() == null || pessoa.getNome().isBlank()) {
            throw new RuntimeException("Informe o nome da pessoa");
        }

        return pessoaRepo.save(pessoa);

    }

    public Pessoa updatePessoa(UUID id, Pessoa pessoa) {

        Pessoa pessoaExistente = getById(id);

        pessoaExistente.setNome(pessoa.getNome());

        return pessoaRepo.save(pessoaExistente);

    }

    public void deletePessoa(UUID id) {

        Pessoa pessoa = getById(id);

        if (despesaRepo.existsByPessoa(pessoa)) {
            throw new RuntimeException("Não é possível excluir: a pessoa possui despesas vinculadas");
        }

        pessoaRepo.delete(pessoa);

    }
}
