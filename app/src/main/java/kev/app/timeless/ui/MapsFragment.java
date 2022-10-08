package kev.app.timeless.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.databinding.DataBindingUtil;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.api.Barbearia.Barbearia;
import kev.app.timeless.databinding.FragmentMapsBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class MapsFragment extends DaggerFragment implements View.OnClickListener, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback {
    @Inject
    ViewModelProvidersFactory providerFactory;

    private FragmentMapsBinding binding;
    private Observer<kev.app.timeless.model.User> userObserver;
    private FragmentResultListener parentResultListener;
    private Handler handler;
    private ExecutorService executorService;
    private MapsActivity mapsActivity;
    private GoogleMap mMap;
    private SupportMapFragment supportMapFragment;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private MapViewModel viewModel;
    private String id;

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_maps, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        onMenuItemClickListener = new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return false;
            }
        };
        executorService = viewModel.getService().getExecutor();
        mapsActivity = (MapsActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
        parentResultListener = this::observeParent;
        userObserver = this::observeUser;
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapsActivity.getSupportFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.toolbar.setNavigationOnClickListener(this);
        binding.toolbar.setOnMenuItemClickListener(onMenuItemClickListener);

        if (mMap != null) {
            mMap.setOnMapLoadedCallback(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapsActivity.getSupportFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.toolbar.setNavigationOnClickListener(null);
        binding.toolbar.setOnMenuItemClickListener(null);

        if (mMap != null) {
            mMap.setOnMapLoadedCallback(null);
            binding.localizacao.setOnClickListener(null);
            binding.perto.setOnClickListener(null);
        }
    }

    public Observer<User> getUserObserver() {
        return userObserver;
    }

    private void observeUser(User user) {
        id = (user == null) ? null : user.getId();
    }

    private void observeLocation(Location locationReceived) {
        if (locationReceived != null) {
            try {
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(new LatLng(locationReceived.getLatitude(), locationReceived.getLongitude())).zoom(15).build()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return;
        }

        Snackbar.make(binding.layoutPrincipal, "", Snackbar.LENGTH_LONG).setAction("", this).show();
    }

    private void searchForPlaces(LatLng latLng) {
        binding.perto.setEnabled(false);

        executorService.execute(() -> {
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(latLng.latitude, latLng.longitude), 0.5 * 1000);

            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (GeoQueryBounds b : bounds) {
                tasks.add(Barbearia.getPlaceQuery(viewModel.getService().getFirestore(), b.startHash, b.endHash).get());
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener(executorService, taskList -> {
                handler.post(() -> binding.perto.setEnabled(true));

                if (!taskList.isSuccessful() || taskList.getException() != null) {
                    handler.post(() -> Snackbar.make(binding.layoutPrincipal, "", Snackbar.LENGTH_LONG).show());
                    return;
                }

                for (Task<QuerySnapshot> task : tasks) {
                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        String hash = documentSnapshot.getString("hash");

                        GeoLocation location = (hash == null || hash.isEmpty()) ? null : GeoHash.locationFromHash(hash);

                        if (location == null || GeoFireUtils.getDistanceBetween(location, new GeoLocation(latLng.latitude, latLng.longitude)) >= 5000) {
                            continue;
                        }

                        viewModel.getEstabelecimentos().put(documentSnapshot.getId(), documentSnapshot.getData());

                        handler.post(() -> mMap.addMarker(new MarkerOptions().position(new LatLng(location.latitude, location.longitude))));
                    }
                }
            });
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchView:
                requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment("SearchFragment", null)).commit();
                break;
            case R.id.localizacao:
                verifyPermission();
                break;
            case R.id.perto:
                searchForPlaces(mMap.getCameraPosition().target);
                break;
            case View.NO_ID:
                requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).add(FragmentUtil.obterFragment("ProfileFragment", null), null).commitNow();
                break;
        }
    }

    private void verifyPermission() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtainCurrentLocation();
        } else {
            requestLocationPermissions();
        }
    }

    private void observeParent(String requestKey, Bundle result) {
        if (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            int granted = 0;

            for (String key : result.keySet()) {
                if (TextUtils.equals(key, Manifest.permission.ACCESS_FINE_LOCATION) || TextUtils.equals(key, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    granted = granted + (result.getBoolean(key) ? +1 : -1);
                }
            }

            if (granted <= 0) {
                return;
            }

            obtainCurrentLocation();
        }
    }

    private void initialize() {
        String text = "Pesquise por nome";

        try {
            binding.searchView.setPrecomputedText(PrecomputedTextCompat.getTextFuture(text, binding.searchView.getTextMetricsParamsCompat(), executorService).get());
        } catch (Exception e) {
            binding.searchView.setText(text);
        }

        supportMapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    private void obtainCurrentLocation() {
        if (!mMap.isMyLocationEnabled()) {
            mMap.setMyLocationEnabled(true);
        }

        if (viewModel.getLocation() != null) {
            System.out.println("damn ");
            return;
        }

        Location location = null;

        for (String provider : viewModel.getService().getLocationManager().getAllProviders()) {
            Location previousLocation = (location == null) ? (location = viewModel.getService().getLocationManager().getLastKnownLocation(provider)) : viewModel.getService().getLocationManager().getLastKnownLocation(provider);

            if (previousLocation == null || previousLocation == location) {
                continue;
            }

            location = previousLocation.getAccuracy() > location.getAccuracy() ? previousLocation : location;
        }

        if (location == null && !viewModel.getService().getLocationManager().getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireActivity(), "O GPS está desactivado!", Toast.LENGTH_LONG).show();
            return;
        }

        if (location != null) {
            viewModel.setLocation(location);
            observeLocation(location);
            return;
        }

        binding.localizacao.setEnabled(false);

        Tasks.withTimeout(viewModel.getService().getLocationService().getLocation(new CancellationTokenSource().getToken()), 5, TimeUnit.SECONDS).addOnCompleteListener(executorService, task -> {
            handler.post(() -> binding.localizacao.setEnabled(true));

            if (task.getException() != null || !task.isSuccessful()) {
                Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                return;
            }

            Location currentLocation = task.getResult();

            viewModel.setLocation(currentLocation);

            try {
                handler.post(() -> observeLocation(currentLocation));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(executorService, e -> {
            try {
                handler.post(() -> Snackbar.make(binding.layoutPrincipal, "", Snackbar.LENGTH_LONG).setAction("Tentar", this).show());
            } catch (Exception x) {
                x.printStackTrace();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), (requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES ? R.raw.map_night_styling : R.raw.map_day_styling));

        verifyPermission();

        mMap.setOnMapLoadedCallback(this);

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    public void requestLocationPermissions() {
        mapsActivity.getActivityResultLauncher().launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    public void onMapLoaded() {
        binding.localizacao.setOnClickListener(this);
        binding.perto.setOnClickListener(this);
    }
}