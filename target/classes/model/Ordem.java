package model;

import java.time.LocalDateTime;

public class Ordem {
    private int id_ordem;
    private int id_carteira;
    private int id_moeda;
    private String tipo;
    private double quantidade;
    private double valor_por_unidade; // mudar para ValorAtual da moeda
    private LocalDateTime timestamp_criacao;
    private String status;

    public int getId_ordem() {
        return id_ordem;
    }

    public void setId_ordem(int id_ordem) {
        this.id_ordem = id_ordem;
    }

    public int getId_carteira() {
        return id_carteira;
    }

    public void setId_carteira(int id_carteira) {
        this.id_carteira = id_carteira;
    }

    public int getId_moeda() {
        return id_moeda;
    }

    public void setId_moeda(int id_moeda) {
        this.id_moeda = id_moeda;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(double quantidade) {
        this.quantidade = quantidade;
    }

    public double getValor_por_unidade() {
        return valor_por_unidade;
    }

    public void setValor_por_unidade(double valor_por_unidade) {
        this.valor_por_unidade = valor_por_unidade;
    }

    public LocalDateTime getTimestamp_criacao() {
        return timestamp_criacao;
    }

    public void setTimestamp_criacao(LocalDateTime timestamp_criacao) {
        this.timestamp_criacao = timestamp_criacao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
