// Transacao.java
package model;

import com.jfoenix.controls.datamodels.treetable.RecursiveTreeObject;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transacao extends RecursiveTreeObject<Transacao>
        implements Identifiable<Integer> {
    private Integer idTransacao;
    private Utilizador utilizador;
    private Moeda moeda;
    private BigDecimal quantidade;
    private BigDecimal precoUnitarioEur;
    private BigDecimal totalEur;
    private LocalDateTime dataHora;
    private String tipo;

    public Transacao() {}

    @Override
    public Integer getId() {
        return idTransacao;
    }

    @Override
    public void setId(Integer id) {
        this.idTransacao = id;
    }

    // Getters & Setters
    public Utilizador getUtilizador() { return utilizador; }
    public void setUtilizador(Utilizador utilizador) { this.utilizador = utilizador; }

    public Moeda getMoeda() { return moeda; }
    public void setMoeda(Moeda moeda) { this.moeda = moeda; }

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }

    public BigDecimal getPrecoUnitarioEur() { return precoUnitarioEur; }
    public void setPrecoUnitarioEur(BigDecimal precoUnitarioEur) { this.precoUnitarioEur = precoUnitarioEur; }

    public BigDecimal getTotalEur() { return totalEur; }
    public void setTotalEur(BigDecimal totalEur) { this.totalEur = totalEur; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
