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
import kev.app.timeless.model.Result;
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
    private Bundle previousState;
    private ExecutorService executorService;
    private MapsActivity mapsActivity;
    private GoogleMap mMap;
    private SupportMapFragment supportMapFragment;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private Observer<Result<Location>> locationObserver;
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
        previousState = savedInstanceState;
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
        locationObserver = this::observeLocationResult;
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapsActivity.getSupportFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.bottomAppBar.setOnMenuItemClickListener(onMenuItemClickListener);
        binding.searchNearby.setOnClickListener(this);
        viewModel.getLocation().observeForever(locationObserver);

        if (mMap != null) {
            mMap.setOnMapLoadedCallback(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapsActivity.getSupportFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.bottomAppBar.setOnMenuItemClickListener(null);
        binding.searchNearby.setOnClickListener(null);
        viewModel.getLocation().removeObserver(locationObserver);

        if (mMap != null) {
            mMap.setOnMapLoadedCallback(null);
            binding.localizacao.setOnClickListener(null);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState == null) {
            return;
        }

        for (String key : savedInstanceState.keySet()) {
            System.out.println(key+": "+savedInstanceState.get(key));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
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

    private void observeLocationResult(Result<Location> result) {
        if (result instanceof Result.Error) {

        }
    }

    private void searchForPlaces(LatLng latLng) {
        executorService.execute(() -> {
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(latLng.latitude, latLng.longitude), 0.5 * 1000);

            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (GeoQueryBounds b : bounds) {
                tasks.add(Barbearia.getPlaceQuery(viewModel.getService().getFirestore(), b.startHash, b.endHash).get());
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener(executorService, taskList -> {
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
            case R.id.searchView: requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment("SearchFragment", null)).commit();
                break;
            case R.id.localizacao: verifyPermission();
                break;
            case View.NO_ID: requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).add(FragmentUtil.obterFragment("ProfileFragment", null), null).commitNow();
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
        int granted = 0;

        for (String key : result.keySet()) {
            if (TextUtils.equals(key, Manifest.permission.ACCESS_FINE_LOCATION) || TextUtils.equals(key, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                granted = granted + (result.getBoolean(key) ? 1 : 0);
            }
        }

        if (granted != 0) {
            obtainCurrentLocation();
        }
    }

    private void initialize() {
        CharSequence text = "Começar a busca";

        try {
            binding.searchNearby.setPrecomputedText(PrecomputedTextCompat.getTextFuture(text, binding.searchNearby.getTextMetricsParamsCompat(), executorService).get());
        } catch (Exception e) {
            binding.searchNearby.setText(text);
        }

        supportMapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    private void obtainCurrentLocation() {
        try {
            if (!mMap.isMyLocationEnabled()) {
                mMap.setMyLocationEnabled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (viewModel.getLocation() != null) {
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
            Snackbar.make(binding.localizacao, "Para continuar ative o GPS", Snackbar.LENGTH_LONG).setAction("Sim", this).show();
            return;
        }

        if (location != null) {
            //viewModel.setLocation(location);
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

            //viewModel.setLocation(currentLocation);

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

        if (mMap.getUiSettings().isMyLocationButtonEnabled()) {
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        if (previousState == null) {
            verifyPermission();
        }

        mMap.setOnMapLoadedCallback(this);
    }

    public void requestLocationPermissions() {
        mapsActivity.getActivityResultLauncher().launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    public void onMapLoaded() {
        binding.localizacao.setOnClickListener(this);
    }
}