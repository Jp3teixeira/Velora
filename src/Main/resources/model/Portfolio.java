package model;

import java.math.BigDecimal;

public class Portfolio {
    private Integer idPortfolio;
    private Utilizador utilizador;
    private Moeda moeda;             // (apenas cryptos serão usadas aqui)
    private BigDecimal quantidade;

    // ====== Construtores, getters e setters ======
    public Portfolio() { }

    public Integer getIdPortfolio() {
        return idPortfolio;
    }
    public void setIdPortfolio(Integer idPortfolio) {
        this.idPortfolio = idPortfolio;
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

    public BigDecimal getQuantidade() {
        return quantidade;
    }
    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
