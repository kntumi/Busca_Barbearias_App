package kev.app.timeless.di.modules;

import androidx.lifecycle.ViewModelProvider;

import dagger.Binds;
import dagger.Module;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;

@Module
public abstract class ViewModelFactoryModule {
    @Binds
    public abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelProvidersFactory viewModelProviderFactory);
}
