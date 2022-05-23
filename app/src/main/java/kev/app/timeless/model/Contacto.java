package kev.app.timeless.model;

public class Contacto {
    private Integer nrTelefone;
    private Integer posicao;

    public Contacto() {
    }

    public Contacto(Integer nrTelefone, Integer posicao) {
        this.nrTelefone = nrTelefone;
        this.posicao = posicao;
    }

    public Integer getNrTelefone() {
        return nrTelefone;
    }

    public void setNrTelefone(Integer nrTelefone) {
        this.nrTelefone = nrTelefone;
    }

    public Integer getPosicao() {
        return posicao;
    }

    public void setPosicao(Integer posicao) {
        this.posicao = posicao;
    }

    @Override
    public String toString() {
        return "Contacto{" +
                "nrTelefone=" + nrTelefone +
                ", posicao=" + posicao +
                '}';
    }
}
