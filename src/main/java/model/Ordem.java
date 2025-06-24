// src/model/Ordem.java
package model;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Ordem extends RecursiveTreeObject<Ordem> implements Identifiable<Integer> {
    private Integer        idOrdem;
    private Utilizador     utilizador;
    private Moeda          moeda;
    private Integer        idTipoOrdem;
    private Integer        idStatus;
    private Integer        idModo;
    private OrdemTipo      tipoOrdem;
    private OrdemStatus    status;
    private OrdemModo      modo;
    private BigDecimal     quantidade;
    private BigDecimal     precoUnitarioEur;
    private LocalDateTime  dataCriacao;
    private LocalDateTime  dataExpiracao;

    public Ordem() {}

    @Override
    public Integer getId() {
        return idOrdem;
    }

    @Override
    public void setId(Integer id) {
        this.idOrdem = id;
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

    public Integer getIdStatus() {
        return idStatus;
    }

    public void setIdStatus(Integer idStatus) {
        this.idStatus = idStatus;
    }

    public Integer getIdModo() {
        return idModo;
    }

    public void setIdModo(Integer idModo) {
        this.idModo = idModo;
    }

    public OrdemTipo getTipoOrdem() {
        return tipoOrdem;
    }

    public void setTipoOrdem(OrdemTipo tipoOrdem) {
        this.tipoOrdem = tipoOrdem;
    }

    public OrdemStatus getStatus() {
        return status;
    }

    public void setStatus(OrdemStatus status) {
        this.status = status;
    }

    public OrdemModo getModo() {
        return modo;
    }

    public void setModo(OrdemModo modo) {
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
