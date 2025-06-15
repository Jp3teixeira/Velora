package model;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transacao extends RecursiveTreeObject<Transacao> {
    private Integer idTransacao;
    private Utilizador utilizador;
    private Moeda moeda;
    private BigDecimal quantidade;
    private BigDecimal precoUnitarioEur;
    private BigDecimal totalEur;
    private LocalDateTime dataHora;
    private String tipo;

    public Transacao() {}
    // getters & setters...
    public Integer getIdTransacao() { return idTransacao; }
    public void setIdTransacao(Integer i) { this.idTransacao = i; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador u) { this.utilizador = u; }
    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda m) { this.moeda = m; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal q) { this.quantidade = q; }
    public BigDecimal getPrecoUnitarioEur() { return precoUnitarioEur; }
    public void setPrecoUnitarioEur(BigDecimal p) { this.precoUnitarioEur = p; }
    public BigDecimal getTotalEur() { return totalEur; }
    public void setTotalEur(BigDecimal t) { this.totalEur = t; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime d) { this.dataHora = d; }
    public String getTipo() {return tipo;}
    public void setTipo(String tipo) {this.tipo = tipo;}


}
