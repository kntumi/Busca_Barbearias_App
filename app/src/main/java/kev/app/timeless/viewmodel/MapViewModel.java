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
    private Map<String, Map<String, Map<String, Double>>> horários;
    private Map<String, Map<String, Map<String, Object>>> serviços;
    private Map<String, Map<String, Map<String, Object>>> tiposServiços;
    private Map<String, Map<String, Map<String, Object>>> subServiços;
    private Map<String, Map<String, Map<String, Object>>> contactos;
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
        if (serviços == null) {
            serviços = new HashMap<>();
        }

        return serviços;
    }

    public Map<String, Map<String, Map<String, Double>>> getHorários() {
        if (horários == null) {
            horários = new HashMap<>();
        }

        return horários;
    }

    public Map<String, Map<String, Map<String, Object>>> getTiposServiços() {
        if (tiposServiços == null) {
            tiposServiços = new HashMap<>();
        }

        return tiposServiços;
    }

    public Map<String, Map<String, Map<String, Object>>> getSubServiços() {
        if (subServiços == null) {
            subServiços = new HashMap<>();
        }

        return subServiços;
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
        tiposServiços = null;
        subServiços = null;
        contactos = null;
        serviços = null;
        service = null;
        horários = null;
        location = null;
        estabelecimentos = null;
    }
}