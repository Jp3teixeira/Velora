package model;

import java.math.BigDecimal;
import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;

public class Portfolio extends RecursiveTreeObject<Portfolio> {
    private Integer idPortfolio;
    private Utilizador utilizador;
    private Moeda moeda;
    private BigDecimal quantidade;
    private BigDecimal precoMedioCompra;

    public Portfolio() {}
    // getters & setters...
    public Integer getIdPortfolio() { return idPortfolio; }
    public void setIdPortfolio(Integer i) { this.idPortfolio = i; }
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador u) { this.utilizador = u; }
    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda m) { this.moeda = m; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal q) { this.quantidade = q; }
    public BigDecimal getPrecoMedioCompra() { return precoMedioCompra; }
    public void setPrecoMedioCompra(BigDecimal p) { this.precoMedioCompra = p; }
}
