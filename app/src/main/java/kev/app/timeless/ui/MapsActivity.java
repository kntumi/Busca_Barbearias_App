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
import androidx.lifecycle.Observer;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
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
    private LiveData<List<User>> users;
    private ActivityResultLauncher<String[]> activityResultLauncher;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private FragmentManager.OnBackStackChangedListener onBackStackChangedListener;
    private Observer<List<User>> observer;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener;
    private User user;

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
        observer = this::observarUsers;
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        onSharedPreferenceChangeListener = this::observarSharedPreferences;
        onBackStackChangedListener = this::observarBackstack;
        users = service.userDao().buscarUsuarioActual();
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                switch (f.getClass().getSimpleName()) {
                    case "MapsFragment": users.observeForever(((MapsFragment) f).getUserObserver());
                        break;
                    case "ProfileFragment": users.observeForever(((ProfileFragment) f).getObserver());
                        break;
                    case "UserFragment": users.observeForever(((UserFragment) f).getObserver());
                        break;
                    case "InsertScheduleFragment": users.observeForever(((InsertScheduleFragment) f).getObserver());
                        break;
                    case "InsertServiceFragment": users.observeForever(((InsertServiceFragment) f).getObserver());
                        break;
                    case "AboutFragment": users.observeForever(((AboutFragment) f).getObserver());
                        break;
                    case "ServicesFragment": users.observeForever(((ServicesFragment) f).getObserver());
                        break;
                    case "ContactsFragment": users.observeForever(((ContactsFragment) f).getObserver());
                        break;
                    case "TypeServicesFragment": users.observeForever(((TypeServicesFragment) f).getObserver());
                        break;
                    case "ScheduleFragment": users.observeForever(((ScheduleFragment) f).getObserver());
                        break;
                    case "SubServiceFragment": users.observeForever(((SubServiceFragment) f).getObserver());
                        break;
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                switch (f.getClass().getSimpleName()) {
                    case "MapsFragment": users.removeObserver(((MapsFragment) f).getUserObserver());
                        break;
                    case "ProfileFragment": users.removeObserver(((ProfileFragment) f).getObserver());
                        break;
                    case "UserFragment": users.removeObserver(((UserFragment) f).getObserver());
                        break;
                    case "InsertScheduleFragment": users.removeObserver(((InsertScheduleFragment) f).getObserver());
                        break;
                    case "InsertServiceFragment": users.removeObserver(((InsertServiceFragment) f).getObserver());
                        break;
                    case "AboutFragment": users.removeObserver(((AboutFragment) f).getObserver());
                        break;
                    case "ServicesFragment": users.removeObserver(((ServicesFragment) f).getObserver());
                        break;
                    case "ContactsFragment": users.removeObserver(((ContactsFragment) f).getObserver());
                        break;
                    case "TypeServicesFragment": users.removeObserver(((TypeServicesFragment) f).getObserver());
                        break;
                    case "ScheduleFragment": users.removeObserver(((ScheduleFragment) f).getObserver());
                        break;
                    case "SubServiceFragment": users.removeObserver(((SubServiceFragment) f).getObserver());
                        break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        users.observeForever(observer);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, true);
        getSupportFragmentManager().addOnBackStackChangedListener(onBackStackChangedListener);
        sharedPref.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        users.removeObserver(observer);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);
        sharedPref.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);

        if (authStateListener != null) {
            service.getAuth().removeAuthStateListener(authStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityResultLauncher.unregister();
    }

    public ActivityResultLauncher<String[]> getActivityResultLauncher() {
        return activityResultLauncher;
    }

    private void observarUsers(List<User> users) {
        user = users.size() != 0 ? users.get(0) : null;

        if (authStateListener != null) {
            service.getAuth().removeAuthStateListener(authStateListener);
        }

        if (authStateListener == null) {
            authStateListener = this::observarAuth;
        }

        service.getAuth().addAuthStateListener(authStateListener);
    }

    private void observarBackstack() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        }
    }

    private void observarAuth(FirebaseAuth firebaseAuth) {
        boolean isUserNull = firebaseAuth.getCurrentUser() == null;

        if ((isUserNull & user == null) || (!isUserNull & user != null)) {
            return;
        }

        SharedPreferences.Editor mEditor = isUserNull ? editor.putString("id", firebaseAuth.getCurrentUser().getUid()).putString("email", firebaseAuth.getCurrentUser().getEmail()) : editor.remove("id").remove("email");
        mEditor.apply();
    }

    private void observarSharedPreferences(SharedPreferences sharedPreferences, String s) {

    }

    private void observarResult(Map<String, Boolean> result) {
        Bundle bundle = new Bundle();

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            bundle.putBoolean(entry.getKey(), entry.getValue());
        }

        getSupportFragmentManager().setFragmentResult("MapsFragment", bundle);
    }
}