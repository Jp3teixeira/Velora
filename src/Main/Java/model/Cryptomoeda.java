package model;

public class Cryptomoeda {
    private int id_moeda;
    private String nome;
    private String simbolo;
    private double valor_atual;
    private double variacao_24h;
    private double volume_mercado;

    public int getId_moeda() {
        return id_moeda;
    }

    public void setId_moeda(int id_moeda) {
        this.id_moeda = id_moeda;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSimbolo() {
        return simbolo;
    }

    public void setSimbolo(String simbolo) {
        this.simbolo = simbolo;
    }

    public double getValor_atual() {
        return valor_atual;
    }

    public void setValor_atual(double valor_atual) {
        this.valor_atual = valor_atual;
    }

    public double getVariacao_24h() {
        return variacao_24h;
    }

    public void setVariacao_24h(double variacao_24h) {
        this.variacao_24h = variacao_24h;
    }

    public double getVolume_mercado() {
        return volume_mercado;
    }

    public void setVolume_mercado(double volume_mercado) {
        this.volume_mercado = volume_mercado;
    }
}
