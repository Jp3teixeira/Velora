package model;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import java.math.BigDecimal;

public class Portfolio extends RecursiveTreeObject<Portfolio>
        implements Identifiable<Integer> {
    private Integer idPortfolio;
    private Utilizador utilizador;
    private Moeda moeda;
    private BigDecimal quantidade;
    private BigDecimal precoMedioCompra;

    public Portfolio() {}

    @Override
    public Integer getId() {
        return idPortfolio;
    }

    @Override
    public void setId(Integer id) {
        this.idPortfolio = id;
    }

    // Getters & Setters...
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }

    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda moeda) { this.moeda = moeda; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoMedioCompra() { return precoMedioCompra; }
    public void setPrecoMedioCompra(BigDecimal precoMedioCompra) { this.precoMedioCompra = precoMedioCompra; }
}