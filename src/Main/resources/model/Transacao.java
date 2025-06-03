package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transacao {
    private Integer idTransacao;
    private Utilizador utilizador;
    private Moeda moeda;
    private String tipo;
    private BigDecimal quantidade;
    private BigDecimal precoUnitarioEur;
    private BigDecimal totalEur;
    private LocalDateTime dataHora;
    // ====== Construtores, getters e setters ======
    public Transacao() { }

    public Integer getIdTransacao() {
        return idTransacao;
    }
    public void setIdTransacao(Integer idTransacao) {
        this.idTransacao = idTransacao;
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

    public BigDecimal getTotalEur() {
        return totalEur;
    }
    public void setTotalEur(BigDecimal totalEur) {
        this.totalEur = totalEur;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
