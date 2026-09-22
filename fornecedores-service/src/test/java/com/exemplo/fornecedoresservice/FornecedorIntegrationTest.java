package com.exemplo.fornecedoresservice;

import com.exemplo.fornecedoresservice.client.ProdutoClient;
import com.exemplo.fornecedoresservice.dto.ProdutoDTO;
import com.exemplo.fornecedoresservice.repository.FornecedorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FornecedorIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired FornecedorRepository repository;
    @MockBean ProdutoClient produtoClient;

    @Test
    void iniciaComCincoFornecedores() throws Exception {
        mvc.perform(get("/fornecedores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        assertThat(repository.findAll()).allSatisfy(f -> assertThat(f.getId()).isPositive());
    }

    @Test
    void buscaExistenteERetorna404ParaAusente() throws Exception {
        mvc.perform(get("/fornecedores/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Alfa Tecnologia"));
        mvc.perform(get("/fornecedores/999999"))
                .andExpect(status().isNotFound()).andExpect(content().string(""));
    }

    @Test
    void criaCom201EGeraIdMesmoSeClienteEnviarId() throws Exception {
        mvc.perform(post("/fornecedores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"nome\":\"Novo Fornecedor\",\"cnpj\":\"99888777000166\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(6));
        mvc.perform(get("/fornecedores/6"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Novo Fornecedor"));
        assertThat(repository.findById(1L).orElseThrow().getNome()).isEqualTo("Alfa Tecnologia");
    }

    @Test
    void rejeitaCamposAusentesOuEmBranco() throws Exception {
        for (String body : List.of("{}", "{\"nome\":\" \",\"cnpj\":\"123\"}",
                "{\"nome\":\"Novo\",\"cnpj\":\" \"}")) {
            mvc.perform(post("/fornecedores").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    void rejeitaCnpjDuplicadoSemCriarRegistro() throws Exception {
        mvc.perform(post("/fornecedores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Duplicado\",\"cnpj\":\"11222333000181\"}"))
                .andExpect(status().isConflict());
        assertThat(repository.count()).isEqualTo(5);
    }

    @Test
    void retornaProdutosDoClienteFeign() throws Exception {
        when(produtoClient.listarTodos()).thenReturn(List.of(new ProdutoDTO(10L, "Teclado", new BigDecimal("99.90"))));
        mvc.perform(get("/fornecedores/produtos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Teclado"))
                .andExpect(jsonPath("$[0].preco").value(99.90));
    }
}
