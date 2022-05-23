package kev.app.timeless.di.modules;

import androidx.lifecycle.ViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;
import kev.app.timeless.di.ViewModelKey;
import kev.app.timeless.viewmodel.MapViewModel;

@Module
public abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(MapViewModel.class)
    public abstract ViewModel bindsMapViewModel(MapViewModel mapViewModel);
}