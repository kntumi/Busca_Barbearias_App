package kev.app.timeless.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "Estabelecimentos")
public class Estabelecimento {
    @NotNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "nome")
    private String nome;

    @ColumnInfo(name = "estado")
    private Boolean estado;

    @ColumnInfo(name = "hash")
    private String hash;

    @Ignore
    public Estabelecimento() {
    }

    @Ignore
    public Estabelecimento(String id) {
        this.id = id;
    }

    public Estabelecimento(@NotNull String id, String nome, Boolean estado, String hash) {
        this.id = id;
        this.nome = nome;
        this.estado = estado;
        this.hash = hash;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    @NotNull
    public String getNome() {
        return nome;
    }

    public void setNome(@NotNull String nome) {
        this.nome = nome;
    }

    public Boolean getEstado() {
        return estado;
    }

    public void setEstado(Boolean estado) {
        this.estado = estado;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "Estabelecimento{" +
                "id='" + id + '\'' +
                ", nome='" + nome + '\'' +
                ", estado=" + estado +
                ", hash='" + hash + '\'' +
                '}';
    }
}