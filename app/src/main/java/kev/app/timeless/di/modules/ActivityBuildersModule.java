package kev.app.timeless.di.modules;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import kev.app.timeless.ui.MapsActivity;

@Module
public abstract class ActivityBuildersModule {
    @ContributesAndroidInjector abstract MapsActivity contributeMapsActivity();
}
