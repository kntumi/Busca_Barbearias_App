package kev.app.timeless.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.maybe.MaybeCallbackObserver;
import io.reactivex.schedulers.Schedulers;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentMapsBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class MapsFragment extends DaggerFragment implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLongClickListener, View.OnClickListener, GoogleMap.OnInfoWindowClickListener {
    @Inject
    Service service;

    @Inject
    ViewModelProvidersFactory providerFactory;

    private FragmentMapsBinding binding;
    private GoogleMap mMap;
    private Observer<List<kev.app.timeless.model.User>> userObserver;
    private Map<String, Marker> markers;
    private FragmentResultListener parentResultListener, childResultListener;
    private Disposable disposable;
    private SupportMapFragment mapFragment;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private Bundle bundle;
    private ExecutorService executorService;
    private MapsActivity mapsActivity;
    private MapViewModel viewModel;
    public String id;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_maps, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapsActivity = (MapsActivity) requireActivity();
        markers = new HashMap<>();
        bundle = new Bundle();
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        executorService = (ExecutorService) viewModel.getService().getExecutor();
        userObserver = this::observarUsers;
        parentResultListener = this::observarParent;
        childResultListener = this::observarChild;
        onMenuItemClickListener = item -> getChildFragmentManager().beginTransaction().add(new ProfileFragment(), null).commit() == 0;
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (savedInstanceState == null) {
            verificarPermissao();
        }

        binding.textView.setTextFuture(PrecomputedTextCompat.getTextFuture("Pesquise por nome", binding.textView.getTextMetricsParamsCompat(), executorService));

        if (viewModel.getLocation().getValue() != null) {
            observarLocation(viewModel.getLocation().getValue());
        }

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapsActivity.getSupportFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, childResultListener);
        binding.materialToolbar.setOnMenuItemClickListener(onMenuItemClickListener);
        binding.localizacao.setOnClickListener(this);
        binding.textView.setOnClickListener(this);
        binding.ir.setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapsActivity.getSupportFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.materialToolbar.setOnMenuItemClickListener(null);
        binding.localizacao.setOnClickListener(null);
        binding.textView.setOnClickListener(null);
        binding.ir.setOnClickListener(null);

        try {
            mMap.setOnMapLongClickListener(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMap.setOnInfoWindowClickListener(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMap.setOnCameraMoveListener(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle.clear();
        bundle = null;
        mapsActivity = null;
        mapFragment = null;
        onMenuItemClickListener = null;
        parentResultListener = null;
        childResultListener = null;
        markers.clear();
        markers = null;
        executorService = null;
        viewModel = null;
        disposable = null;
        id = null;
        userObserver = null;
        mMap = null;
        binding = null;
    }

    public Observer<List<User>> getUserObserver() {
        return userObserver;
    }

    private void observarUsers(List<User> users) {
        id = users.size() == 0 ? null : users.get(0).getId();
        bundle.putString("id", id);

        if (mMap != null) {
            mMap.setOnMapLongClickListener(TextUtils.isEmpty(id) ? null : this);
        }
    }

    @SuppressLint("MissingPermission")
    private void observarLocation(Location locationReceived) {
        if (locationReceived != null) {
            if (viewModel.getLocation().getValue() == null) {
                viewModel.getLocation().setValue(locationReceived);
            }

            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(viewModel.getLocation().getValue().getLatitude(), viewModel.getLocation().getValue().getLongitude())).zoom(19).bearing(0).tilt(0).build()));
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private void buscarEstabelecimentos(Map<String, Map<String, Object>> estabelecimentos) {
        viewModel.getEstabelecimentos().putAll(estabelecimentos);

        for (String key : estabelecimentos.keySet()) {
            Map<String, Object> map = estabelecimentos.get(key);

            if (map.containsKey("hash")) {
                GeoLocation location = GeoHash.locationFromHash(String.valueOf(map.get("hash")));
                double distanceInM = GeoFireUtils.getDistanceBetween(location, new GeoLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude));

                if (distanceInM <= 5000) {
                    markers.put(key, mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude)).title(String.valueOf(map.get("nome")))));
                    markers.get(key).setTag(key);
                }
            }
        }
    }

    @Override
    public void onCameraIdle() {
        if (mMap.getCameraPosition().zoom < 17) {
            return;
        }

        requireActivity().runOnUiThread(() -> {
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(mMap.getCameraPosition().target.latitude, mMap.getCameraPosition().target.longitude), 0.5 * 1000);

            executorService.submit(() -> {
                try {
                    Map<String, Map<String, Object>> estabelecimentos = new HashMap<>();
                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                    for (GeoQueryBounds b : bounds) {
                        tasks.add(service.getFirestore().collection("Barbearia").orderBy("hash").startAt(b.startHash).endAt(b.endHash).get());
                    }

                    Tasks.whenAllComplete(tasks)
                            .addOnSuccessListener(tasks1 -> {
                                for (Task<?> task : tasks1) {
                                    Task<QuerySnapshot> task1 = (Task<QuerySnapshot>) task;

                                    for (DocumentSnapshot documentSnapshot : task1.getResult()) {
                                        estabelecimentos.put(documentSnapshot.getId(), documentSnapshot.getData());
                                    }
                                }

                                buscarEstabelecimentos(estabelecimentos);
                            })
                            .addOnFailureListener(Throwable::printStackTrace);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setOnInfoWindowClickListener(this);

        if (viewModel.getLocation().getValue() != null) {
            observarLocation(viewModel.getLocation().getValue());
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.localizacao: obterLocalizacaoActual();
                break;
            case R.id.ir:
                break;
            case R.id.textView: requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment("SearchFragment", bundle)).commit();
                break;
        }
    }

    private void obterLocalizacaoActual() {
        if (viewModel.getLocation().getValue() != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(viewModel.getLocation().getValue().getLatitude(), viewModel.getLocation().getValue().getLongitude())).zoom(19).bearing(0).tilt(0).build()));
        } else {
            verificarPermissao();
        }
    }

    private void verificarPermissao() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (viewModel.getLocation().getValue() != null) {
                return;
            }

            Location location = null;

            for (String provider : service.getLocationManager().getAllProviders()) {
                Location previousLocation = (location == null) ? (location = service.getLocationManager().getLastKnownLocation(provider)) : service.getLocationManager().getLastKnownLocation(provider);

                if (previousLocation == null || previousLocation == location) {
                    continue;
                }

                if (previousLocation.hasAccuracy() && location.hasAccuracy()) {
                    location = previousLocation.getAccuracy() > location.getAccuracy() ? previousLocation : location;
                }
            }

            if (location != null) {
                observarLocation(location);
                return;
            }

            if (!service.getLocationManager().getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
                Snackbar.make(binding.layoutPrincipal, "O GPS está desactivado!", Snackbar.LENGTH_LONG).setAction("Ativar", this).show();
                return;
            }

            service.getLocationService().getLocation(new CancellationTokenSource().getToken()).subscribe(new MaybeCallbackObserver<>(this::observarLocation, Throwable::printStackTrace, null));

        } else {
            mapsActivity.getActivityResultLauncher().launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void observarChild(String s, Bundle bundle) {
        if (bundle.containsKey("update")) {
            Map<String, Object> map = viewModel.getEstabelecimentos().get(bundle.getString("update"));

            if (map.containsKey("nome")) {
                if (TextUtils.isEmpty(String.valueOf(map.get("nome")))) {
                    if (mMap != null) {
                        mMap.clear();
                    }

                    return;
                }

                if (markers.containsKey(bundle.getString("update"))) {
                    if (!TextUtils.equals(String.valueOf(map.get("nome")), markers.get(bundle.getString("update")).getTitle())) {
                        if (markers.get(bundle.getString("update")).isInfoWindowShown()) {
                            markers.get(bundle.getString("update")).hideInfoWindow();
                        }

                        markers.get(bundle.getString("update")).setTitle(String.valueOf(viewModel.getEstabelecimentos().get(bundle.getString("update")).get("nome")));
                        markers.get(bundle.getString("update")).showInfoWindow();
                    }
                }
            } else {
                if (mMap != null) {
                    mMap.clear();
                }

                return;
            }

            if (map.containsKey("hash")) {
                GeoLocation newLocation = GeoHash.locationFromHash(String.valueOf(map.get("hash")));
                LatLng oldLocation = markers.containsKey(bundle.getString("update")) ? markers.get(bundle.getString("update")).getPosition() : null;

                if (oldLocation != null && oldLocation.equals(new LatLng(newLocation.latitude, newLocation.longitude))) {
                    return;
                }

                if (mMap != null) {
                    if (markers.containsKey(bundle.getString("update"))) {
                        markers.get(bundle.getString("update")).remove();
                    }

                    markers.remove(bundle.getString("update"));
                    markers.put(bundle.getString("update"), mMap.addMarker(new MarkerOptions().position(new LatLng(newLocation.latitude, newLocation.longitude)).title(String.valueOf(viewModel.getEstabelecimentos().get(bundle.getString("update")).get("nome")))));
                    markers.get(bundle.getString("update")).setTag(bundle.getString("update"));
                }
            } else {
                if (mMap != null) {
                    if (markers.containsKey(bundle.getString("update"))) {
                        markers.get(bundle.getString("update")).remove();
                    }

                    markers.remove(bundle.getString("update"));
                    mMap.clear();
                }
            }

            return;
        }

        if (bundle.containsKey("remove")) {
            if (markers.containsKey(bundle.getString("remove"))) {
                markers.get(bundle.getString("remove")).remove();
            }

            markers.remove(bundle.getString("remove"));

            if (mMap != null) {
                mMap.clear();
            }

            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (TextUtils.equals(f.getClass().getSimpleName(), "MineEstablishmentFragment") || TextUtils.equals(f.getClass().getSimpleName(), "NonEstablishmentFragment")) {
                    getChildFragmentManager().beginTransaction().remove(f).commit();
                    break;
                }
            }
        }
    }

    private void observarParent(String requestKey, Bundle result) {
        if (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            int granted = 0;

            for (String key : result.keySet()) {
                if (TextUtils.equals(key, Manifest.permission.ACCESS_FINE_LOCATION) || TextUtils.equals(key, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    granted = result.getBoolean(key) ? granted + 1 : granted - 1;
                }
            }

            if (granted <= 0) {
                Toast.makeText(requireActivity(), "Não foi possivel buscar a sua localização", Toast.LENGTH_LONG).show();
                return;
            }

            if (mMap == null) {
                mapFragment.getMapAsync(this);
            }

            verificarPermissao();
        }
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        if (disposable != null) {
            disposable.dispose();
        }

        if (markers.get(id) == null) {
            if (viewModel.getEstabelecimentos().containsKey(id)) {
                if (viewModel.getEstabelecimentos().get(id).containsKey("nome")) {
                    String nome = String.valueOf(viewModel.getEstabelecimentos().get(id).get("nome"));

                    if (!TextUtils.isEmpty(nome)) {
                        disposable = service.getLocationService().insertLocation(id, GeoFireUtils.getGeoHashForLocation(new GeoLocation(latLng.latitude, latLng.longitude))).timeout(10, TimeUnit.SECONDS)
                                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                .subscribe(task -> {
                                    if (task.isSuccessful()) {
                                        markers.put(id, mMap.addMarker(new MarkerOptions().position(new LatLng(latLng.latitude, latLng.longitude)).title(nome)));
                                        markers.get(id).setTag(id);
                                    } else {
                                        Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                                    }
                                }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());
                    }
                }
            }
        } else {
            disposable = service.getLocationService().removeLocation(id).timeout(10, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(task -> {
                        if (task.isSuccessful()) {
                            markers.get(id).remove();
                            markers.remove(id);
                            mMap.clear();
                        } else {
                            Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                        }
                    }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        getChildFragmentManager().beginTransaction().add(new LayoutFragment(), null).runOnCommit(() -> {
            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (!TextUtils.equals(f.getClass().getSimpleName(), "LayoutFragment")) {
                    continue;
                }

                f.getChildFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment("AboutFragment", bundle)).commit();
            }
        }).commit();
    }
}