package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Ordem {
    private Integer idOrdem;

    // Relacionamento
    private Utilizador utilizador;
    private Moeda moeda;

    // FKs para tabelas de domínio
    private Integer idTipoOrdem;
    private Integer idStatus;
    private Integer idModo;

    // Texto vindo das tabelas de domínio
    private String tipoOrdem;   // "compra" ou "venda"
    private String status;      // "ativa", "executada", "expirada"
    private String modo;        // "market" ou "limit"

    // Valores da ordem
    private BigDecimal quantidade;
    private BigDecimal precoUnitarioEur;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;

    // ====== Construtores ======
    public Ordem() { }

    // ====== Getters e Setters ======

    public Integer getIdOrdem() {
        return idOrdem;
    }
    public void setIdOrdem(Integer idOrdem) {
        this.idOrdem = idOrdem;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }
    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public Moeda getMoeda() {
        return moeda;
    }
    public void setMoeda(Moeda moeda) {
        this.moeda = moeda;
    }

    public Integer getIdTipoOrdem() {
        return idTipoOrdem;
    }
    public void setIdTipoOrdem(Integer idTipoOrdem) {
        this.idTipoOrdem = idTipoOrdem;
    }

    public String getTipoOrdem() {
        return tipoOrdem;
    }
    public void setTipoOrdem(String tipoOrdem) {
        this.tipoOrdem = tipoOrdem;
    }

    public Integer getIdStatus() {
        return idStatus;
    }
    public void setIdStatus(Integer idStatus) {
        this.idStatus = idStatus;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIdModo() {
        return idModo;
    }
    public void setIdModo(Integer idModo) {
        this.idModo = idModo;
    }

    public String getModo() {
        return modo;
    }
    public void setModo(String modo) {
        this.modo = modo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }
    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitarioEur() {
        return precoUnitarioEur;
    }
    public void setPrecoUnitarioEur(BigDecimal precoUnitarioEur) {
        this.precoUnitarioEur = precoUnitarioEur;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
}
