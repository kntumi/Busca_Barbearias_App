package kev.app.timeless.viewmodel;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import kev.app.timeless.api.Service;

public class MapViewModel extends ViewModel {
    private Map<String, Map<String, Object>> estabelecimentos;
    private Map<String, Map<String, Map<String, Object>>> horarios, servicos, tiposServicos, subServicos, contactos;
    private MutableLiveData<Location> location;
    private Service service;

    @Inject
    public MapViewModel(Service service) {
        super();
        this.service = service;
    }

    public Map<String, Map<String, Object>> getEstabelecimentos() {
        if (estabelecimentos == null) {
            estabelecimentos = new HashMap<>();
        }

        return estabelecimentos;
    }

    public Map<String, Map<String, Map<String, Object>>> getServiços() {
        if (servicos == null) {
            servicos = new HashMap<>();
        }

        return servicos;
    }

    public Map<String, Map<String, Map<String, Object>>> getHorários() {
        if (horarios == null) {
            horarios = new HashMap<>();
        }

        return horarios;
    }

    public Map<String, Map<String, Map<String, Object>>> getTiposServiços() {
        if (tiposServicos == null) {
            tiposServicos = new HashMap<>();
        }

        return tiposServicos;
    }

    public Map<String, Map<String, Map<String, Object>>> getSubServiços() {
        if (subServicos == null) {
            subServicos = new HashMap<>();
        }

        return subServicos;
    }

    public Map<String, Map<String, Map<String, Object>>> getContactos() {
        if (contactos == null) {
            contactos = new HashMap<>();
        }

        return contactos;
    }

    public MutableLiveData<Location> getLocation() {
        if (location == null) {
            location = new MutableLiveData<>();
        }

        return location;
    }

    public Service getService() {
        return service;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        tiposServicos = null;
        subServicos = null;
        contactos = null;
        servicos = null;
        service = null;
        horarios = null;
        location = null;
        estabelecimentos = null;
    }
}