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
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.internal.operators.maybe.MaybeCallbackObserver;
import io.reactivex.schedulers.Schedulers;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentMapsBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;
import kev.app.timeless.viewmodel.MapViewModel;

public class MapsFragment extends DaggerFragment implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener , GoogleMap.OnCameraIdleListener, GoogleMap.OnMapLongClickListener, View.OnClickListener, GoogleMap.OnInfoWindowClickListener {
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
    private Bundle bundle;
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
        binding.map.onCreate(savedInstanceState);
        markers = new HashMap<>();
        bundle = new Bundle();
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        userObserver = this::observarUsers;
        parentResultListener = this::observarParent;
        childResultListener = this::observarChild;

        if (savedInstanceState == null) {
            verificarPermissao();
        }

        if (viewModel.getLocation().getValue() != null) {
            observarLocation(viewModel.getLocation().getValue());
        }

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            binding.map.getMapAsync(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.map.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.map.onResume();
        mapsActivity.getSupportFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, childResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.map.onPause();
        mapsActivity.getSupportFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        try {
            mMap.setOnMapLongClickListener(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            mMap.setOnInfoWindowClickListener(null);
            mMap.setOnCameraIdleListener(null);
            mMap.setOnCameraMoveListener(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        binding.map.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.map.onDestroy();
        bundle.clear();
        bundle = null;
        mapsActivity = null;
        parentResultListener = null;
        childResultListener = null;
        markers.clear();
        markers = null;
        viewModel = null;
        disposable = null;
        id = null;
        userObserver = null;
        mMap = null;
        binding = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            binding.map.onSaveInstanceState(outState);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        binding.map.onLowMemory();
    }

    public Observer<List<User>> getUserObserver() {
        return userObserver;
    }

    private void observarUsers(List<User> users) {
        id = users.size() == 0 ? null : users.get(0).getId();
        bundle.putString("id", id);

        for (Fragment f : getChildFragmentManager().getFragments()) {
            getChildFragmentManager().beginTransaction().remove(f).commit();
        }

        getChildFragmentManager()
                .beginTransaction()
                .replace(binding.layoutFragment.getId(), users.size() == 0 ? new NonUserControlFragment(): new UserControlFragment(), null)
                .commit();

        try {
            mMap.setOnMapLongClickListener(TextUtils.isEmpty(id) ? null : this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void observarLocation(Location locationReceived) {
        if (locationReceived != null) {
            if (viewModel.getLocation().getValue() == null) {
                viewModel.getLocation().setValue(locationReceived);
            }

            if (mMap != null) {
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(viewModel.getLocation().getValue().getLatitude(), viewModel.getLocation().getValue().getLongitude())).zoom(19).bearing(90).tilt(30).build()));
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    private void buscarEstabelecimentos (Map<String, Map<String, Object>> estabelecimentos) {
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
        if (binding.layoutFragment.getVisibility() == View.GONE) {
            binding.layoutFragment.setVisibility(View.VISIBLE);
        }

        if (mMap.getCameraPosition().zoom >= 17) {
            service.getBarbeariaService().buscarBarbearias(mMap.getCameraPosition().target).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe(disposable ->  buscarEstabelecimentos(viewModel.getEstabelecimentos())).doOnSuccess(this::buscarEstabelecimentos).subscribe(new MaybeCallbackObserver<>(map -> viewModel.getEstabelecimentos().putAll(map), Throwable::printStackTrace, null));
        }
    }

    @Override
    public void onMapReady(@NotNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnCameraMoveListener(this);

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
            case R.id.btnOpcoes: new ProfileFragment().show(getChildFragmentManager(), "currentFragment");
                break;
            case R.id.btnVoltar: requireActivity().finish();
                break;
            case R.id.btnEncontrar: if (viewModel.getLocation().getValue() != null) {
                                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(viewModel.getLocation().getValue().getLatitude(), viewModel.getLocation().getValue().getLongitude())).zoom(19).bearing(90).tilt(30).build()));
                                    } else {
                                        verificarPermissao();
                                    }
                break;
            case R.id.btnEncontrarBarberia: new LayoutFragment().show(getChildFragmentManager(), "currentFragment");
                                            Bundle b = new Bundle();
                                            b.putString("fragmentToLoad", "AboutFragment");
                                            b.putString("id", id);
                                            getChildFragmentManager().setFragmentResult("LayoutFragment", b);
                break;
            case R.id.btn: if (viewModel.getEstabelecimentos().containsKey(id)) {
                                     Map<String, Object> map = viewModel.getEstabelecimentos().get(id);

                                     if (map.containsKey("hash")) {
                                         GeoLocation location = GeoHash.locationFromHash(String.valueOf(map.get("hash")));
                                         mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(new LatLng(location.latitude, location.longitude)).zoom(19).bearing(90).tilt(30).build()));
                                     } else {
                                         Toast.makeText(requireActivity(), "",Toast.LENGTH_LONG).show();
                                     }
                                 } else {
                                    Toast.makeText(requireActivity(), "",Toast.LENGTH_LONG).show();
                                 }
                break;
        }
    }

    private void verificarPermissao() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location cachedLocation = service.getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER) != null ? service.getLocationManager().getLastKnownLocation(LocationManager.GPS_PROVIDER) : service.getLocationManager().getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (cachedLocation == null) {
                if (service.getLocationManager().getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
                    if (viewModel.getLocation().getValue() == null) {
                        service.getLocationService().getLocation(new CancellationTokenSource().getToken()).doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) throws Exception {

                            }
                        }).doFinally(new Action() {
                            @Override
                            public void run() throws Exception {

                            }
                        }).subscribe(new MaybeCallbackObserver<>(this::observarLocation, Throwable::printStackTrace, null));
                    }
                } else {
                    Toast.makeText(requireActivity(), "O GPS está desactivado!", Toast.LENGTH_LONG).show();
                }
            } else {
                observarLocation(cachedLocation);
            }
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
                    if (result.getBoolean(key)) {
                        granted++;
                    }
                }
            }

            if (granted != 0) {
                if (mMap == null) {
                    binding.map.getMapAsync(this);
                }

                verificarPermissao();
            } else {
                Toast.makeText(requireActivity(), "Não foi possivel buscar a sua localização", Toast.LENGTH_LONG).show();
            }
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
        BottomSheetDialogFragment fragment;
        fragment = new BottomSheetDialogFragment();
        fragment.show(getChildFragmentManager(), "currentFragment");

        if (TextUtils.equals(fragment.getClass().getSimpleName(), "NonEstablishmentFragment")) {
            Bundle bundle = new Bundle();
            bundle.putString("id", String.valueOf(marker.getTag()));
            getChildFragmentManager().setFragmentResult(fragment.getClass().getSimpleName(), bundle);
        }
    }

    @Override
    public void onCameraMove() {
        binding.layoutFragment.setVisibility(View.GONE);
    }
}