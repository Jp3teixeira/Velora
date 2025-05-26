package model;

import java.math.BigDecimal;

public class Moeda {
    private int id_moeda;
    private String nome;
    private String simbolo;
    private BigDecimal valorAtual;
    private BigDecimal variacao24h;
    private BigDecimal volumeMercado;

    // Construtor
    public Moeda(int id_moeda, String nome, String simbolo, BigDecimal valorAtual, BigDecimal variacao24h, BigDecimal volumeMercado) {
        this.id_moeda = id_moeda;
        this.nome = nome;
        this.simbolo = simbolo;
        this.valorAtual = valorAtual;
        this.variacao24h = variacao24h;
        this.volumeMercado = volumeMercado;
    }

    // Getters
    public int getid_moeda() {return id_moeda;}
    public String getNome() { return nome; }
    public String getSimbolo() { return simbolo; }
    public BigDecimal getValorAtual() { return valorAtual; }
    public BigDecimal getVariacao24h() { return variacao24h; }
    public BigDecimal getVolumeMercado() { return volumeMercado; }

    @Override
    public String toString() {
        return nome + " (" + simbolo + ") - $" + valorAtual;
    }
}
