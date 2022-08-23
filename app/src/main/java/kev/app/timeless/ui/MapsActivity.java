package kev.app.timeless.ui;

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
import java.util.Objects;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
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

    private Disposable disposable;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ActivityMapsBinding binding;
    private LiveData<List<User>> users;
    private ActivityResultLauncher<String[]> activityResultLauncher;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private FragmentManager.OnBackStackChangedListener onBackStackChangedListener;
    private Observer<List<User>> observer;
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        users.removeObserver(observer);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getSupportFragmentManager().removeOnBackStackChangedListener(onBackStackChangedListener);

        if (authStateListener != null) {
            service.getAuth().removeAuthStateListener(authStateListener);
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        activityResultLauncher.unregister();
        onBackStackChangedListener = null;
        user = null;
        users = null;
        observer = null;
        activityResultLauncher = null;
        fragmentLifecycleCallbacks = null;
        authStateListener = null;
        disposable = null;
        binding = null;
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
        if (disposable != null) {
            disposable.dispose();
        }

        if (firebaseAuth.getCurrentUser() == null) {
            if (user != null) {
                disposable = service.userDao().apagarUsuario().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {}, Throwable::printStackTrace);
            }
        } else {
            if (user == null) {
                disposable = service.userDao().inserirUsuario(new User(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid(), firebaseAuth.getCurrentUser().getEmail())).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(() -> {}, Throwable::printStackTrace);
            }
        }
    }

    private void observarResult(Map<String, Boolean> result) {
        Bundle bundle = new Bundle();

        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            bundle.putBoolean(entry.getKey(), entry.getValue());
        }

        getSupportFragmentManager().setFragmentResult("MapsFragment", bundle);
    }
}