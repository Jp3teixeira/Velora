package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Ordem {
    private Integer idOrdem;
    private Utilizador utilizador;
    private Moeda moeda;
    private String tipo;
    private BigDecimal quantidade;
    private BigDecimal precoUnitarioEur;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private String Modo;
    private String status;

    // ====== Construtores, getters e setters ======
    public Ordem() { }

    public Integer getIdOrdem() {
        return idOrdem;
    }
    public void setIdOrdem(Integer idOrdem) {
        this.idOrdem = idOrdem;
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

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }
    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitarioEur() {
        return precoUnitarioEur;
    }
    public void setPrecoUnitarioEur(BigDecimal precoUnitarioEur) {
        this.precoUnitarioEur = precoUnitarioEur;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getModo(){ return Modo; }
    public void setModo(String modo){ Modo = modo; }
}
