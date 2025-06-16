package model;

import java.math.BigDecimal;


public class Moeda {
    private Integer idMoeda;
    private String nome, simbolo, foto;
    private Integer idTipo;
    private String tipo;       // “fiat”|“crypto”
    private BigDecimal valorAtual;
    private BigDecimal variacao24h;
    private BigDecimal volume24h;


    public Moeda() {}

    public Moeda(Integer idMoeda,
                 String nome,
                 String simbolo,
                 BigDecimal valorAtual,
                 BigDecimal variacao24h,
                 BigDecimal volumeMercado) {
        this.idMoeda       = idMoeda;
        this.nome          = nome;
        this.simbolo       = simbolo;
        this.valorAtual    = valorAtual;
        this.variacao24h   = variacao24h;

    }

    // getters & setters...
    public Integer getIdMoeda() { return idMoeda; }
    public void setIdMoeda(Integer id) { this.idMoeda = id; }
    public String getNome() { return nome; }
    public void setNome(String n) { this.nome = n; }
    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String s) { this.simbolo = s; }
    public String getFoto() { return foto; }
    public void setFoto(String f) { this.foto = f; }
    public Integer getIdTipo() { return idTipo; }
    public void setIdTipo(Integer t) { this.idTipo = t; }
    public String getTipo() { return tipo; }
    public void setTipo(String t) { this.tipo = t; }
    public BigDecimal getValorAtual() { return valorAtual; }
    public void setValorAtual(BigDecimal v) { this.valorAtual = v; }
    public BigDecimal getVariacao24h() { return variacao24h; }
    public void setVariacao24h(BigDecimal v) { this.variacao24h = v; }
    public BigDecimal getVolume24h() { return volume24h; }
    public void setVolume24h(BigDecimal v) { this.volume24h = v; }
}
