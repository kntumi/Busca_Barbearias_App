package kev.app.timeless.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.Geoposition;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.OnMapCameraChangedListener;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.BuildConfig;
import kev.app.timeless.R;
import kev.app.timeless.api.Barbearia.Barbearia;
import kev.app.timeless.databinding.FragmentMapsBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class MapsFragment extends DaggerFragment implements View.OnClickListener, View.OnTouchListener {
    @Inject
    ViewModelProvidersFactory providerFactory;

    private FragmentMapsBinding binding;
    private Observer<List<kev.app.timeless.model.User>> userObserver;
    private FragmentResultListener parentResultListener;
    private Handler handler;
    private OnMapCameraChangedListener onMapCameraChangedListener;
    private ExecutorService executorService;
    private MapsActivity mapsActivity;
    private List<Float> xAndYs;
    private MapElementLayer mPinLayer;
    private MapViewModel viewModel;
    private String id;

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_maps, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        executorService = viewModel.getService().getExecutor();
        mapsActivity = (MapsActivity) requireActivity();
        handler = new Handler(Looper.getMainLooper());
        parentResultListener = this::observeParent;
        userObserver = this::observeUsers;
        mPinLayer = new MapElementLayer();
        onMapCameraChangedListener = mapCameraChangedEventArgs -> {


            return true;
        };

        if (savedInstanceState == null) {
            verifyPermission();
        }

        if (viewModel.getLocation() != null) {
            observeLocation(viewModel.getLocation());
        }

        initializeMap();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onResume() {
        super.onResume();
        mapsActivity.getSupportFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.map.addOnMapCameraChangedListener(onMapCameraChangedListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapsActivity.getSupportFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.map.removeOnMapCameraChangedListener(onMapCameraChangedListener);
        mPinLayer.getElements().clear();
    }

    public Observer<List<User>> getUserObserver() {
        return userObserver;
    }

    private void observeUsers(List<User> users) {
        id = users.size() == 0 ? null : users.get(0).getId();
    }

    private void observeLocation(Location locationReceived) {
        if (locationReceived != null && !locationReceived.equals(viewModel.getLocation())) {
            Geopoint location = new Geopoint(locationReceived.getLatitude(), locationReceived.getLongitude());
            MapIcon pushpin = new MapIcon();
            pushpin.setLocation(location);

            executorService.execute(() -> {
                Bitmap bitmap = drawableToBitmap(Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), R.drawable.my_location)));

                handler.post(() -> binding.map.beginSetScene(MapScene.createFromLocationAndZoomLevel(location, 17), MapAnimationKind.NONE, b -> {
                    if (!b) {
                        return;
                    }

                    pushpin.setImage(new MapImage(bitmap));

                    mPinLayer.getElements().add(pushpin);
                }));
            });

        } else {
            Snackbar.make(binding.layoutPrincipal, "", Snackbar.LENGTH_LONG).setAction("", this).show();
        }
    }

    private void searchForPlaces(Geoposition geoposition) {
        //

        executorService.execute(() -> {
            List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(geoposition.getLatitude(), geoposition.getLongitude()), 0.5 * 1000);

            List<Task<QuerySnapshot>> tasks = new ArrayList<>();

            for (GeoQueryBounds b : bounds) {
                tasks.add(Barbearia.getPlaceQuery(viewModel.getService().getFirestore(), b.startHash, b.endHash).get());
            }

            Tasks.whenAllComplete(tasks).addOnCompleteListener(executorService, taskList -> {
             //   handler.post(() -> binding.perto.setEnabled(true));

                for (Task<QuerySnapshot> task : tasks) {
                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        String hash = documentSnapshot.getString("hash");

                        GeoLocation location = (hash == null || hash.isEmpty()) ? null : GeoHash.locationFromHash(hash);

                        if (location == null || GeoFireUtils.getDistanceBetween(location, new GeoLocation(geoposition.getLatitude(), geoposition.getLongitude())) >= 5000) {
                            continue;
                        }

                        viewModel.getEstabelecimentos().put(documentSnapshot.getId(), documentSnapshot.getData());

                        MapIcon pushpin = new MapIcon();
                        pushpin.setLocation(new Geopoint(location.latitude, location.longitude));

                        Bitmap bitmap = drawableToBitmap(Objects.requireNonNull(AppCompatResources.getDrawable(requireContext(), R.drawable.push_pin)));
                        pushpin.setImage(new MapImage(bitmap));

                        handler.post(() -> mPinLayer.getElements().add(pushpin));
                    }
                }
            });
        });
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.textView: requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment("SearchFragment", null)).commit();
                break;
          //  case R.id.localizacao: verifyPermission();
          //      break;
//            case R.id.perto: searchForPlaces(binding.map.getMapCamera().getLocation().getPosition());
            //    break;
        }
    }

    private void verifyPermission() {
        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obtainCurrentLocation();
        } else {
            mapsActivity.getActivityResultLauncher().launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void observeParent(String requestKey, Bundle result) {
        if (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) || result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            int granted = 0;

            for (String key : result.keySet()) {
                if (TextUtils.equals(key, Manifest.permission.ACCESS_FINE_LOCATION) || TextUtils.equals(key, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    granted = granted + (result.getBoolean(key) ? + 1 : - 1);
                }
            }

            if (granted <= 0) {
                return;
            }

            obtainCurrentLocation();
        }
    }

    private void initializeMap() {
        binding.map.setMapRenderMode(MapRenderMode.RASTER);
        binding.map.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);
        binding.map.getLayers().add(mPinLayer);
        binding.map.setMapStyleSheet((requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES ? MapStyleSheets.roadDark() : MapStyleSheets.roadLight());

        String prefix = "set", suffix = "Visible";

        for (Method method : binding.map.getUserInterfaceOptions().getClass().getDeclaredMethods()) {
            String methodName = method.getName();

            if (methodName.contains(prefix) && methodName.contains(suffix)) {
                try {
                    method.invoke(binding.map.getUserInterfaceOptions(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void obtainCurrentLocation() {
        if (viewModel.getLocation() != null) {
            return;
        }

        Location location = null;

        for (String provider : viewModel.getService().getLocationManager().getAllProviders()) {
            @SuppressLint("MissingPermission") Location previousLocation = (location == null) ? (location = viewModel.getService().getLocationManager().getLastKnownLocation(provider)) : viewModel.getService().getLocationManager().getLastKnownLocation(provider);

            if (previousLocation == null || previousLocation == location) {
                continue;
            }

            if (previousLocation.hasAccuracy() && location.hasAccuracy()) {
                location = previousLocation.getAccuracy() > location.getAccuracy() ? previousLocation : location;
            }
        }

        if (location != null) {
            observeLocation(location);
            return;
        }

        if (!viewModel.getService().getLocationManager().getProviders(true).contains(LocationManager.GPS_PROVIDER)) {
            Snackbar.make(binding.layoutPrincipal, "O GPS está desactivado!", Snackbar.LENGTH_LONG).setAction("Ativar", this).show();
            return;
        }

        Tasks.withTimeout(viewModel.getService().getLocationService().getLocation(new CancellationTokenSource().getToken()), 5, TimeUnit.SECONDS).addOnCompleteListener(executorService, task -> {
            try {
                handler.post(() -> observeLocation(task.getResult()));
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
    public boolean onTouch(View view, MotionEvent motionEvent) {
        System.out.println(gestureDetectorCompat.onTouchEvent(motionEvent));

        switch(motionEvent.getAction()) {
            case (MotionEvent.ACTION_DOWN) : System.out.println("Action was DOWN");
                return true;
            case (MotionEvent.ACTION_MOVE) : System.out.println("Action was MOVE");
                return true;
            case (MotionEvent.ACTION_UP) : System.out.println("Action was UP");
                return true;
            case (MotionEvent.ACTION_CANCEL) : System.out.println("Action was CANCEL");
                return true;
            case (MotionEvent.ACTION_OUTSIDE) : System.out.println("Movement occurred outside bounds " + "of current screen element");
                return true;
            default : return view.onTouchEvent(motionEvent);
        }
    }
}