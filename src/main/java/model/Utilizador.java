package model;

import java.time.LocalDateTime;
import java.util.List;

public class Utilizador {
    private Integer idUtilizador;
    private String email;
    private String nome;
    private String password;
    private String tipoPerfil;  //  user ou admin
    private LocalDateTime dataCriacao;

    // Relacionamentos “1-N”:
    private CarteiraEuro carteiraEuro;       // 1-1 para o saldo em EUR
    private List<Portfolio> portfolio;       // 1-N para ativos
    private List<Ordem> ordens;              // 1-N para Ordens de compra/venda
    private List<Transacao> transacoes; // 1-N para Transacoes de cripto
    private List<Historico_movimentos> historicoEur; // 1-N para depósitos/levantamentos

    // ====== Construtores, getters e setters ======
    public Utilizador() { }

    public Integer getIdUtilizador() {
        return idUtilizador;
    }
    public void setIdUtilizador(Integer idUtilizador) {
        this.idUtilizador = idUtilizador;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getNome() {
        return nome;
    }
    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String passwordHash) {
        this.password = passwordHash;
    }

    public String getTipoPerfil() {
        return tipoPerfil;
    }
    public void setTipoPerfil(String tipoPerfil) {
        this.tipoPerfil = tipoPerfil;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public CarteiraEuro getCarteiraEuro() {
        return carteiraEuro;
    }
    public void setCarteiraEuro(CarteiraEuro carteiraEuro) {
        this.carteiraEuro = carteiraEuro;
    }

    public List<Portfolio> getPortfolio() {
        return portfolio;
    }
    public void setPortfolio(List<Portfolio> portfolio) {
        this.portfolio = portfolio;
    }

    public List<Ordem> getOrdens() {
        return ordens;
    }
    public void setOrdens(List<Ordem> ordens) {
        this.ordens = ordens;
    }

    public List<Transacao> getTransacoes() {
        return transacoes;
    }
    public void setTransacoes(List<Transacao> transacoes) {
        this.transacoes = transacoes;
    }

    public List<Historico_movimentos> getHistoricoEur() {
        return historicoEur;
    }
    public void setHistoricoEur(List<Historico_movimentos> historicoEur) {
        this.historicoEur = historicoEur;
    }
}
