package kev.app.timeless.model;

public class Contacto {
    private int nrTelefone;
    private boolean contactoPrincipal;

    public Contacto() {
    }

    public Contacto(int nrTelefone, boolean contactoPrincipal) {
        this.nrTelefone = nrTelefone;
        this.contactoPrincipal = contactoPrincipal;
    }

    public int getNrTelefone() {
        return nrTelefone;
    }

    public void setNrTelefone(int nrTelefone) {
        this.nrTelefone = nrTelefone;
    }

    public boolean isContactoPrincipal() {
        return contactoPrincipal;
    }

    public void setContactoPrincipal(boolean contactoPrincipal) {
        this.contactoPrincipal = contactoPrincipal;
    }

    @Override
    public String toString() {
        return "Contacto{" +
                "nrTelefone=" + nrTelefone +
                ", contactoPrincipal=" + contactoPrincipal +
                '}';
    }
}
