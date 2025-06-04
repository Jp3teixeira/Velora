package model;

import java.math.BigDecimal;

public class Portfolio {

    private int idPortfolio;
    private Utilizador utilizador;
    private Moeda moeda;
    private BigDecimal quantidade;
    private BigDecimal precoMedioCompra;  // novo campo

    // getters e setters
    public int getIdPortfolio() { return idPortfolio; }
    public void setIdPortfolio(int idPortfolio) { this.idPortfolio = idPortfolio; }

    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }

    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda moeda) { this.moeda = moeda; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoMedioCompra() { return precoMedioCompra; }
    public void setPrecoMedioCompra(BigDecimal precoMedioCompra) { this.precoMedioCompra = precoMedioCompra; }
}
