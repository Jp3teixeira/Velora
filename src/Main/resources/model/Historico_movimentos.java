package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Historico_movimentos {
    private Integer idMovimento;
    private Utilizador utilizador;
    private String tipo;
    private BigDecimal valorEur;
    private LocalDateTime dataHora;

    // ====== Construtores, getters e setters ======
    public Historico_movimentos() { }

    public Integer getIdMovimento() {
        return idMovimento;
    }
    public void setIdMovimento(Integer idMovimento) {
        this.idMovimento = idMovimento;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }
    public void setUtilizador(Utilizador utilizador) {
        this.utilizador = utilizador;
    }

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValorEur() {
        return valorEur;
    }
    public void setValorEur(BigDecimal valorEur) {
        this.valorEur = valorEur;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}
