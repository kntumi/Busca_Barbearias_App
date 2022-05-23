package kev.app.timeless.model;

public class SubServiço {
    private String nome;
    private Double preço;
    private String id;

    public SubServiço(String nome, Double preço, String id) {
        this.nome = nome;
        this.preço = preço;
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Double getPreço() {
        return preço;
    }

    public void setPreço(Double preço) {
        this.preço = preço;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "SubServiço{" +
                "nome='" + nome + '\'' +
                ", preço=" + preço +
                ", id='" + id + '\'' +
                '}';
    }
}
