package model;

import java.time.LocalDateTime;

public class Ordem {
    private int id_ordem;
    private int id_carteira;
    private int id_moeda;
    private String tipo; // "compra" ou "venda"
    private double quantidade_total;
    private double quantidade_restante;
    private double preco_no_momento; // valor da moeda no momento da ordem
    private LocalDateTime timestamp_criacao;
    private String status; // "PENDENTE", "EXECUTADA", "PARCIAL", "CANCELADA"

    //Getters e Setters

    public int getId_ordem() {
        return id_ordem;
    }
    public int getId_carteira() {
        return id_carteira;
    }
    public int getId_moeda() {
        return id_moeda;
    }
    public String getTipo() {
        return tipo;
    }
    public double getQuantidade_total() {
        return quantidade_total;
    }
    public double getQuantidade_restante() {return quantidade_restante;}
    public double getPreco_no_momento() {
        return preco_no_momento;
    }
    public LocalDateTime getTimestamp_criacao() {
        return timestamp_criacao;
    }
    public String getStatus() {
        return status;
    }


    public void setId_ordem(int id_ordem) {
        this.id_ordem = id_ordem;
    }
    public void setId_carteira(int id_carteira) {
        this.id_carteira = id_carteira;
    }
    public void setId_moeda(int id_moeda) {
        this.id_moeda = id_moeda;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public void setQuantidade_total(double quantidade_total) {this.quantidade_total = quantidade_total;}

    public void setQuantidade_restante(double quantidade) {
        this.quantidade_restante = quantidade;
    }
    public void setPreco_no_momento(double valor_por_unidade) {this.preco_no_momento = valor_por_unidade;}
    public void setTimestamp_criacao(LocalDateTime timestamp_criacao) {this.timestamp_criacao = timestamp_criacao;}
    public void setStatus(String status) {this.status = status;}

}
