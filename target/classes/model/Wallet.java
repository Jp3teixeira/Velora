package model;

public class Wallet {
    private int id_carteira;
    private int id_utilizador;
    private double saldo;

    public int getId_carteira() {
        return id_carteira;
    }

    public void setId_carteira(int id_carteira) {
        this.id_carteira = id_carteira;
    }

    public int getId_utilizador() {
        return id_utilizador;
    }

    public void setId_utilizador(int id_utilizador) {
        this.id_utilizador = id_utilizador;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }
}