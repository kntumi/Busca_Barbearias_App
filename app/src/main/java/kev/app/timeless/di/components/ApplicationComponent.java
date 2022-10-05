
package kev.app.timeless.di.components;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;
import kev.app.timeless.MyApplication;

import kev.app.timeless.api.Service;
import kev.app.timeless.di.modules.ActivityBuildersModule;
import kev.app.timeless.di.modules.FragmentBuildersModule;
import kev.app.timeless.di.modules.NetworkModule;
import kev.app.timeless.di.modules.ViewModelFactoryModule;
import kev.app.timeless.di.modules.ViewModelModule;

@Singleton
@Component(modules = {NetworkModule.class, ViewModelModule.class, ViewModelFactoryModule.class, AndroidInjectionModule.class, ActivityBuildersModule.class, FragmentBuildersModule.class})
public interface ApplicationComponent extends AndroidInjector<MyApplication> {
    Service service();
}