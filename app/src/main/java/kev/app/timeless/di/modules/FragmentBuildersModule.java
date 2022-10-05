package kev.app.timeless.di.modules;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import kev.app.timeless.ui.AboutFragment;
import kev.app.timeless.ui.ContactsFragment;
import kev.app.timeless.ui.InsertNameFragment;
import kev.app.timeless.ui.LoginFragment;
import kev.app.timeless.ui.ManageScheduleFragment;
import kev.app.timeless.ui.MapsFragment;
import kev.app.timeless.ui.NewContactFragment;
import kev.app.timeless.ui.RegisterFragment;
import kev.app.timeless.ui.RemoveAccountFragment;
import kev.app.timeless.ui.ScheduleFragment;
import kev.app.timeless.ui.SearchFragment;
import kev.app.timeless.ui.ServicesFragment;
import kev.app.timeless.ui.SubServiceFragment;
import kev.app.timeless.ui.TypeServicesFragment;
import kev.app.timeless.ui.UserFragment;

@Module
public abstract class FragmentBuildersModule {
    @ContributesAndroidInjector abstract LoginFragment contributeLoginFragment();
    @ContributesAndroidInjector abstract RegisterFragment contributeRegisterFragment();
    @ContributesAndroidInjector abstract MapsFragment contributeMapsFragment();
    @ContributesAndroidInjector abstract UserFragment contributeUserFragment();
    @ContributesAndroidInjector abstract RemoveAccountFragment contributeRemoveAccountFragment();
    @ContributesAndroidInjector abstract AboutFragment contributeAboutFragment();
    @ContributesAndroidInjector abstract InsertNameFragment contributeInsertNameFragment();
    @ContributesAndroidInjector abstract ContactsFragment contributeContactsFragment();
    @ContributesAndroidInjector abstract NewContactFragment contributeNewContactFragment();
    @ContributesAndroidInjector abstract ServicesFragment contributeServicesFragment();
    @ContributesAndroidInjector abstract TypeServicesFragment contributeTypeServicesFragment();
    @ContributesAndroidInjector abstract SubServiceFragment contributeSubServicesFragment();
    @ContributesAndroidInjector abstract ScheduleFragment contributeScheduleFragment();
    @ContributesAndroidInjector abstract ManageScheduleFragment contributeManageScheduleFragment();
    @ContributesAndroidInjector abstract SearchFragment contributeSearchFragment();
}