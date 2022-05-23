package kev.app.timeless.di.modules;

import android.content.Context;

import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.ui.MapsActivity;
import kev.app.timeless.viewmodel.MapViewModel;

@Module
public class NetworkModule{
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    public NetworkModule(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Provides
    @Singleton
    public Context context(){
        return context;
    }

    @Provides
    @Singleton
    public FirebaseFirestore getFirebaseFirestore() {
        return firestore;
    }

    @Provides
    @Singleton
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }
}
