package com.exemplo.fornecedoresservice.service;

import com.exemplo.fornecedoresservice.client.ProdutoClient;
import com.exemplo.fornecedoresservice.dto.ProdutoDTO;
import com.exemplo.fornecedoresservice.model.Fornecedor;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FornecedorService {
    private final FornecedorRepository repository;
    private final ProdutoClient produtoClient;

    public FornecedorService(FornecedorRepository repository, ProdutoClient produtoClient) {
        this.repository = repository;
        this.produtoClient = produtoClient;
    }

    public List<Fornecedor> listarTodos() {
        return repository.findAll();
    }

    public Optional<Fornecedor> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public Fornecedor salvar(Fornecedor fornecedor) {
        fornecedor.setId(null);
        return repository.save(fornecedor);
    }

    public List<ProdutoDTO> listarProdutos() {
        return produtoClient.listarTodos();
    }
}
