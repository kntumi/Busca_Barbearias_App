package kev.app.timeless.api;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import kev.app.timeless.api.Authentication.Auth;
import kev.app.timeless.api.Authentication.AuthService;
import kev.app.timeless.api.Barbearia.Barbearia;
import kev.app.timeless.api.Barbearia.BarbeariaService;
import kev.app.timeless.api.Location.LocationService;
import kev.app.timeless.room.UserDao;
import kev.app.timeless.room.UserDatabase;

@Singleton
public class Service {
    private FirebaseFirestore firestore;
    private Context context;
    private FirebaseAuth auth;
    private AuthService authService;
    private BarbeariaService barbeariaService;
    private UserDatabase userDatabase;
    private LocationService locationService;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Gson gson;
    private HttpRequestFactory requestFactory;

    @Inject
    public Service(FirebaseFirestore firestore, FirebaseAuth auth, Context context) {
        this.firestore = firestore;
        this.auth = auth;
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public BarbeariaService getBarbeariaService() {
        if (barbeariaService == null) {
            barbeariaService = new BarbeariaService() {
                @Override
                public Maybe<Map<String, Map<String, Object>>> buscarBarbearias(LatLng latLng) {
                    return Barbearia.buscarBarbearias(latLng, firestore);
                }

                @Override
                public Maybe<Boolean> inserirBarbearia(Map<String, ?> map) {
                    return Barbearia.inserirBarbearia(map, firestore);
                }

                @Override
                public Maybe<Boolean> editarNome(String nome, String id) {
                    return Barbearia.editarNome(nome, id, firestore);
                }

                @Override
                public Maybe<Boolean> editarEstado(Boolean aBoolean, String id) {
                    return Barbearia.editarEstado(aBoolean, id, firestore);
                }

                @Override
                public Maybe<Boolean> inserirHorarioPorDia(Integer diaSemana, Map<String, Map<String, Map<String, Float>>> map, String id) {
                    return Barbearia.inserirHorarioPorDia(diaSemana, map, id, firestore);
                }

                @Override
                public Maybe<Map<String, Map<String, Object>>> obterServi??os(String id) {
                    return Barbearia.obterServi??os(id, firestore);
                }

                @Override
                public Maybe<Boolean> removerContacto(String id, String nrContacto) {
                    return Barbearia.removerContacto(id, nrContacto, firestore);
                }

                @Override
                public Maybe<Map<String, Map<String, Object>>> obterTiposServi??os(String id, String idServi??o) {
                    return Barbearia.obterTiposServi??os(id, idServi??o, firestore);
                }

                @Override
                public Maybe<Map<String, Map<String, Object>>> obterSubServi??os(String id, String idServi??o, String idTipoServi??o) {
                    return Barbearia.obterSubServi??os(id, idServi??o, idTipoServi??o, firestore);
                }

                @Override
                public Maybe<Map<String, Object>> obterEstabelecimento(String id) {
                    return Barbearia.obterEstabelecimento(id, getRequestFactory(), getGson());
                }

                @Override
                public Maybe<Map<String, Map<String, Object>>> obterContactos(String id) {
                    return Barbearia.obterContactos(id, firestore);
                }

                @Override
                public Maybe<Map<String, Map<String, Object>>> obterHor??rio(String id) {
                    return Barbearia.obterHor??rio(id, firestore);
                }

                @Override
                public Maybe<Boolean> obterContacto(String id, String nrContacto) {
                    return Barbearia.obterContacto(id, nrContacto, firestore);
                }

                @Override
                public Maybe<Boolean> removerHorario(String id, int dia) {
                    return Barbearia.removerHorario(id, dia, firestore);
                }

                @Override
                public Maybe<Boolean> inserirHorario(String id, int dia ,Map<String, Double> horario) {
                    return Barbearia.inserirHorario(id, dia, horario, firestore);
                }

                @Override
                public Maybe<Boolean> inserirServi??o(String id, Map<String, String> map) {
                    return Barbearia.inserirServi??o(id, map, firestore);
                }

                @Override
                public Maybe<Boolean> inserirTipoServi??o(String id, String idServi??o, Map<String, String> map) {
                    return Barbearia.inserirTipoServi??o(id, idServi??o, map, firestore);
                }

                @Override
                public Maybe<Boolean> removerServi??o(String id, String idServi??o) {
                    return Barbearia.removerServi??o(id, idServi??o, firestore);
                }

                @Override
                public Maybe<Boolean> removerTipoServi??o(String id, String idServi??o, String idTipoServi??o) {
                    return Barbearia.removerTipoServi??o(id, idServi??o, idTipoServi??o, firestore);
                }

                @Override
                public Maybe<Boolean> inserirSubServi??o(String id, String idServi??o, String idTipoServi??o, Map<String, Object> map) {
                    return Barbearia.inserirSubServi??o(id, idServi??o, idTipoServi??o, map, firestore);
                }

                @Override
                public Maybe<Boolean> inserirEstado(String id, boolean estado) {
                    return Barbearia.inserirEstado(id, estado, firestore);
                }

                @Override
                public Maybe<Boolean> inserirContacto(String id, String nrContacto, Map<String, Object> map) {
                    return Barbearia.inserirContacto(id, nrContacto, map, firestore);
                }

                @Override
                public Maybe<Boolean> editarSubServi??o(String id, String idServi??o, String idTipoServi??o, String idSubServico, Map<String, Object> map) {
                    return Barbearia.editarSubServi??o(id, idServi??o, idTipoServi??o, idSubServico, map, firestore);
                }

                @Override
                public Maybe<Boolean> removerSubServi??o(String id, String idServi??o, String idTipoServi??o, String idSubServico) {
                    return Barbearia.removerSubServi??o(id, idServi??o, idTipoServi??o, idSubServico, firestore);
                }
            };
        }

        return barbeariaService;
    }

    public AuthService getAuthService() {
        if (authService == null){
            authService = new AuthService() {
                @Override
                public Completable fazerLogIn(String email, String password) {
                    return Auth.fazerLogIn(auth, email, password);
                }

                @Override
                public Completable criarConta(String email, String password) {
                    return Auth.criarConta(auth, email, password);
                }

                @Override
                public Completable apagarConta(FirebaseUser user) {
                    return Auth.apagarConta(user);
                }

                @Override
                public Maybe<Boolean> reautenticar(AuthCredential authCredential) {
                    return Auth.reautenticar(auth, authCredential);
                }

                @Override
                public void fazerLogOut() {
                    Auth.fazerLogOut(auth);
                }
            };
        }

        return authService;
    }

    public LocationService getLocationService() {
        if (locationService == null) {
            locationService = new LocationService() {
                @Override
                public Maybe<Location> getLocation(CancellationToken cancellationToken) {
                    return kev.app.timeless.api.Location.Location.getLocation(fusedLocationProviderClient, cancellationToken);
                }

                @Override
                public Maybe<Task<Void>> insertLocation(String id, String hash) {
                    return kev.app.timeless.api.Location.Location.insertLocation(id, hash, firestore);
                }

                @Override
                public Maybe<Task<Void>> removeLocation(String id) {
                    return kev.app.timeless.api.Location.Location.removeLocation(id, firestore);
                }
            } ;
        }
        return locationService;
    }

    public FirebaseFirestore getFirestore() {
        return firestore;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }

        return gson;
    }

    public HttpRequestFactory getRequestFactory() {
        if (requestFactory == null) {
            requestFactory = new NetHttpTransport().createRequestFactory();
        }

        return requestFactory;
    }

    public UserDao userDao() {
        if (userDatabase == null) {
            userDatabase = Room.databaseBuilder(context, UserDatabase.class, "UserDatabase").build();
        }

        return userDatabase.userDao();
    }
}