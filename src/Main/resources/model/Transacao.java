package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transacao {
    private int id;
    private int id_ordem_compra;
    private int id_ordem_venda;
    private int id_moeda;
    private double quantidade_executada;
    private double preco_executado; // unitário
    private LocalDateTime timestamp;

    // Getters e Setters

    public int getId() {return id;}
    public int getId_ordem_compra() {return id_ordem_compra;}
    public int getId_ordem_venda() {return id_ordem_venda;}
    public int getId_moeda() {return id_moeda;}
    public double getQuantidade_executada() {return quantidade_executada;}
    public double getPreco_executado() {return preco_executado;}
    public LocalDateTime getTimestamp() {return timestamp;}


    public void setId(int id) {this.id = id;}
    public void setId_ordem_compra(int id_ordem_compra) {this.id_ordem_compra = id_ordem_compra;}
    public void setId_ordem_venda(int id_ordem_venda) {this.id_ordem_venda = id_ordem_venda;}
    public void setId_moeda(int id_moeda) {this.id_moeda = id_moeda;}
    public void setQuantidade_executada(double quantidadeExecutada) {this.quantidade_executada = quantidadeExecutada;}
    public void setPreco_executado(double precoExecutado) {this.preco_executado = precoExecutado;}
    public void setTimestamp(LocalDateTime timestamp) {this.timestamp = timestamp;}
}
