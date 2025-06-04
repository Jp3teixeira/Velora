package model;

import java.math.BigDecimal;
import java.util.List;

public class Moeda {
    private Integer idMoeda;
    private String nome;
    private String simbolo;
    private String tipo;


    private BigDecimal valorAtual;     // último valor em EUR vindo de PrecoMoeda
    private BigDecimal variacao24h;    // variação percentual calculada
    private BigDecimal volumeMercado;  // soma de VolumeMercado das últimas 24 h


    // ===== Construtores =====

    public Moeda() {

    }


    public Moeda(Integer idMoeda,
                 String nome,
                 String simbolo,
                 BigDecimal valorAtual,
                 BigDecimal variacao24h,
                 BigDecimal volumeMercado) {
        this.idMoeda = idMoeda;
        this.nome = nome;
        this.simbolo = simbolo;
        this.valorAtual = valorAtual;
        this.variacao24h = variacao24h;
        this.volumeMercado = volumeMercado;
    }

    // ===== Getters e Setters =====

    public Integer getIdMoeda() {
        return idMoeda;
    }
    public void setIdMoeda(Integer idMoeda) {
        this.idMoeda = idMoeda;
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

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValorAtual() {
        return valorAtual;
    }
    public void setValorAtual(BigDecimal valorAtual) {
        this.valorAtual = valorAtual;
    }

    public BigDecimal getVariacao24h() {
        return variacao24h;
    }
    public void setVariacao24h(BigDecimal variacao24h) {
        this.variacao24h = variacao24h;
    }

    public BigDecimal getVolumeMercado() {
        return volumeMercado;
    }
    public void setVolumeMercado(BigDecimal volumeMercado) {
        this.volumeMercado = volumeMercado;
    }

}
