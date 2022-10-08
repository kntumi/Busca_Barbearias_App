package kev.app.timeless.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.ActivityMapsBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;

public class MapsActivity extends DaggerAppCompatActivity {
    @Inject
    Service service;

    @Inject
    ViewModelProvidersFactory providerFactory;

    private FirebaseAuth.AuthStateListener authStateListener;
    private ActivityMapsBinding binding;
    private LiveData<User> user;
    private ActivityResultLauncher<String[]> activityResultLauncher;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private FragmentManager.OnBackStackChangedListener onBackStackChangedListener;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_maps);
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::observarResult);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            getSupportFragmentManager().beginTransaction().replace(binding.layoutPrincipal.getId(), new MapsFragment()).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        mEditor = sharedPref.edit();
        onBackStackChangedListener = this::observarBackstack;
        authStateListener = this::observarAuth;
        user = new MutableLiveData<>(sharedPref.contains("id") && sharedPref.contains("email") ? new User(sharedPref.getString("id", ""), sharedPref.getString("email", "")) : null);
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                switch (f.getClass().getSimpleName()) {
                    case "MapsFragment": user.observeForever(((MapsFragment) f).getUserObserver());
                        break;
                    case "ProfileFragment":
                        break;
                    case "UserFragment":
                        break;
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                switch (f.getClass().getSimpleName()) {
                    case "MapsFragment": user.removeObserver(((MapsFragment) f).getUserObserver());
                        break;
                    case "ProfileFragment":
                        break;
                    case "UserFragment":
                        break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true);
        getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
        service.getAuth().addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
        service.getAuth().removeAuthStateListener(authStateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityResultLauncher.unregister();
    }

    public ActivityResultLauncher<String[]> getActivityResultLauncher() {
        return activityResultLauncher;
    }

    private void observarBackstack() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }

    private void observarAuth(FirebaseAuth firebaseAuth) {
        boolean isUserNull = (firebaseAuth.getCurrentUser() == null);

        if (isUserNull & user.getValue() == null || !isUserNull & user.getValue() != null) {
            return;
        }

        System.out.println("isUserNull: "+isUserNull);

        (isUserNull ? mEditor.remove("id").remove("email") : mEditor.putString("id", firebaseAuth.getCurrentUser().getUid()).putString("email", firebaseAuth.getCurrentUser().getEmail())).apply();
    }

    private void observarResult(Map<String, Boolean> result) {
        Bundle bundle = new Bundle();

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            bundle.putBoolean(entry.getKey(), entry.getValue());
        }

        getSupportFragmentManager().setFragmentResult("MapsFragment", bundle);
    }
}