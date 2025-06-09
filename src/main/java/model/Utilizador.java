package model;

import java.time.LocalDateTime;
import java.util.List;

public class Utilizador {
    private Integer idUtilizador;
    private String email;
    private String nome;
    private String password;
    private Integer idPerfil;      // FK → Perfil
    private String perfil;         // texto de Perfil (user/admin)
    private LocalDateTime dataCriacao;
    private String foto;

    // relacionamentos
    private CarteiraEuro carteiraEuro;
    private List<Portfolio> portfolio;
    private List<Ordem> ordens;
    private List<Transacao> transacoes;

    public Utilizador() {}

    // getters & setters...
    public Integer getIdUtilizador() { return idUtilizador; }
    public void setIdUtilizador(Integer idUtilizador) { this.idUtilizador = idUtilizador; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Integer idPerfil) { this.idPerfil = idPerfil; }
    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public CarteiraEuro getCarteiraEuro() { return carteiraEuro; }
    public void setCarteiraEuro(CarteiraEuro carteiraEuro) { this.carteiraEuro = carteiraEuro; }
    public List<Portfolio> getPortfolio() { return portfolio; }
    public void setPortfolio(List<Portfolio> portfolio) { this.portfolio = portfolio; }
    public List<Ordem> getOrdens() { return ordens; }
    public void setOrdens(List<Ordem> ordens) { this.ordens = ordens; }
    public List<Transacao> getTransacoes() { return transacoes; }
    public void setTransacoes(List<Transacao> transacoes) { this.transacoes = transacoes; }
}
