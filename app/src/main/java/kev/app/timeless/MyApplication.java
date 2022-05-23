package kev.app.timeless;

import dagger.android.AndroidInjector;
import dagger.android.DaggerApplication;
import kev.app.timeless.di.components.DaggerApplicationComponent;
import kev.app.timeless.di.modules.NetworkModule;


public class MyApplication extends DaggerApplication {
    public MyApplication() {
    }

    protected AndroidInjector<? extends DaggerApplication> applicationInjector() {
        return DaggerApplicationComponent.builder().networkModule(new NetworkModule(this)).build();
    }
}