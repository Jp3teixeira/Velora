package model;

import java.math.BigDecimal;

public class Moeda implements Identifiable<Integer> {
    private Integer idMoeda;
    private String nome;
    private String simbolo;
    private String foto;
    private Integer idTipo;
    private String tipo;       // "fiat" | "crypto"
    private BigDecimal valorAtual;
    private BigDecimal variacao24h;
    private BigDecimal volume24h;

    public Moeda() {}

    public Moeda(Integer idMoeda, String nome, String simbolo,
                 BigDecimal valorAtual, BigDecimal variacao24h,
                 BigDecimal volume24h) {
        this.idMoeda = idMoeda;
        this.nome = nome;
        this.simbolo = simbolo;
        this.valorAtual = valorAtual;
        this.variacao24h = variacao24h;
        this.volume24h = volume24h;
    }

    @Override
    public Integer getId() {
        return idMoeda;
    }

    @Override
    public void setId(Integer id) {
        this.idMoeda = id;
    }

    // Getters & Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSimbolo() { return simbolo; }
    public void setSimbolo(String simbolo) { this.simbolo = simbolo; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public Integer getIdTipo() { return idTipo; }
    public void setIdTipo(Integer idTipo) { this.idTipo = idTipo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public BigDecimal getValorAtual() { return valorAtual; }
    public void setValorAtual(BigDecimal valorAtual) { this.valorAtual = valorAtual; }

    public BigDecimal getVariacao24h() { return variacao24h; }
    public void setVariacao24h(BigDecimal variacao24h) { this.variacao24h = variacao24h; }

    public BigDecimal getVolume24h() { return volume24h; }
    public void setVolume24h(BigDecimal volume24h) { this.volume24h = volume24h; }
}