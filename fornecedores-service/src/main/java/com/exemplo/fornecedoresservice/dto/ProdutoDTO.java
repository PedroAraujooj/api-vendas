package com.exemplo.fornecedoresservice.dto;

import java.math.BigDecimal;

public record ProdutoDTO(Long id, String nome, BigDecimal preco) {
}
