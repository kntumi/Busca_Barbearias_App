package kev.app.timeless.model;

public class Horário {
    private int dia;
    private double horaAbertura;
    private double horaEncerramento;

    public Horário(int dia) {
        this.dia = dia;
    }

    public int getDia() {
        return dia;
    }

    public void setDia(int dia) {
        this.dia = dia;
    }

    public double getHoraAbertura() {
        return horaAbertura;
    }

    public void setHoraAbertura(double horaAbertura) {
        this.horaAbertura = horaAbertura;
    }

    public double getHoraEncerramento() {
        return horaEncerramento;
    }

    public void setHoraEncerramento(double horaEncerramento) {
        this.horaEncerramento = horaEncerramento;
    }

    @Override
    public String toString() {
        return "Horário{" +
                "dia=" + dia +
                ", horaAbertura=" + horaAbertura +
                ", horaEncerramento=" + horaEncerramento +
                '}';
    }
}