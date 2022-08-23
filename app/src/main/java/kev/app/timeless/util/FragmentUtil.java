package kev.app.timeless.util;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

import kev.app.timeless.ui.AboutFragment;
import kev.app.timeless.ui.ContactsFragment;
import kev.app.timeless.ui.InsertNameFragment;
import kev.app.timeless.ui.LayoutFragment;
import kev.app.timeless.ui.LoadingFragment;
import kev.app.timeless.ui.LoginFragment;
import kev.app.timeless.ui.ManageScheduleFragment;
import kev.app.timeless.ui.MapsFragment;
import kev.app.timeless.ui.NewContactFragment;
import kev.app.timeless.ui.NonUserControlFragment;
import kev.app.timeless.ui.NonUserFragment;
import kev.app.timeless.ui.ProfileFragment;
import kev.app.timeless.ui.RegisterFragment;
import kev.app.timeless.ui.ScheduleFragment;
import kev.app.timeless.ui.SearchFragment;
import kev.app.timeless.ui.ServicesFragment;
import kev.app.timeless.ui.SubServiceFragment;
import kev.app.timeless.ui.TypeServicesFragment;
import kev.app.timeless.ui.UserControlFragment;
import kev.app.timeless.ui.UserFragment;

public class FragmentUtil {
    public static boolean existeNaBackstack(String key, FragmentManager fragmentManager){
        for (int i = 0; i < fragmentManager.getBackStackEntryCount() ; i++){
            if (TextUtils.equals(fragmentManager.getBackStackEntryAt(i).getName(), key)){
                return true;
            }
        }
        return false;
    }

    public static String ultimoNaBackstack(FragmentManager fragmentManager){
        if (fragmentManager.getBackStackEntryCount() > 0){
            return fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
        }
            return null;
    }

    public static Integer numBackStack(FragmentManager fragmentManager){
        return fragmentManager.getBackStackEntryCount();
    }

    public static Fragment obterFragment (String key, Bundle args) {
        Fragment fragment = null;

        switch (key){
            case "SearchFragment": fragment = new SearchFragment();
                break;
            case "AboutFragment": fragment = new AboutFragment();
                break;
            case "ContactsFragment": fragment = new ContactsFragment();
                break;
            case "TypeServicesFragment": fragment = new TypeServicesFragment();
                break;
            case "NewContactFragment": fragment = new NewContactFragment();
                break;
            case "ScheduleFragment": fragment = new ScheduleFragment();
                break;
            case "InsertNameFragment": fragment = new InsertNameFragment();
                break;
            case "LoginFragment": fragment = new LoginFragment();
                break;
            case "ProfileFragment": fragment = new ProfileFragment();
                break;
            case "LoadingFragment": fragment = new LoadingFragment();
                break;
            case "MapsFragment": fragment = new MapsFragment();
                break;
            case "RegisterFragment": fragment = new RegisterFragment();
                break;
            case "NonUserFragment": fragment = new NonUserFragment();
                break;
            case "UserFragment":  fragment = new UserFragment();
                break;
            case "NonUserControlFragment": fragment = new NonUserControlFragment();
                break;
            case "UserControlFragment": fragment = new UserControlFragment();
                break;
            case "LayoutFragment": fragment = new LayoutFragment();
                break;
            case "SubServiceFragment": fragment = new SubServiceFragment();
                break;
            case "ServicesFragment": fragment = new ServicesFragment();
                break;
            case "ManageScheduleFragment": fragment = new ManageScheduleFragment();
                break;
        }

        fragment.setArguments(args);

        return fragment;
    }

    public static Fragment obterFragment (String key) {
        Fragment fragment = null;

        switch (key){
            case "AboutFragment": fragment = new AboutFragment();
                break;
            case "ContactsFragment": fragment = new ContactsFragment();
                break;
            case "TypeServicesFragment": fragment = new TypeServicesFragment();
                break;
            case "NewContactFragment": fragment = new NewContactFragment();
                break;
            case "ScheduleFragment": fragment = new ScheduleFragment();
                break;
            case "InsertNameFragment": fragment = new InsertNameFragment();
                break;
            case "LoginFragment": fragment = new LoginFragment();
                break;
            case "ProfileFragment": fragment = new ProfileFragment();
                break;
            case "LoadingFragment": fragment = new LoadingFragment();
                break;
            case "MapsFragment": fragment = new MapsFragment();
                break;
            case "RegisterFragment": fragment = new RegisterFragment();
                break;
            case "NonUserFragment": fragment = new NonUserFragment();
                break;
            case "UserFragment":  fragment = new UserFragment();
                break;
            case "NonUserControlFragment": fragment = new NonUserControlFragment();
                break;
            case "UserControlFragment": fragment = new UserControlFragment();
                break;
            case "LayoutFragment": fragment = new LayoutFragment();
                break;
            case "SubServiceFragment": fragment = new SubServiceFragment();
                break;
            case "ServicesFragment": fragment = new ServicesFragment();
                break;
            case "ManageScheduleFragment": fragment = new ManageScheduleFragment();
                break;
        }

        return fragment;
    }

    public static void criarFragment(String key, FragmentManager fragmentManager, int id){
        Fragment fragment;

        switch (key){
            case "AboutFragment": fragment = new AboutFragment();
                break;
            case "ContactsFragment": fragment = new ContactsFragment();
                break;
            case "NewContactFragment": fragment = new NewContactFragment();
                break;
            case "ScheduleFragment": fragment = new ScheduleFragment();
                break;
            case "InsertNameFragment": fragment = new InsertNameFragment();
                break;
            case "LoginFragment": fragment = new LoginFragment();
                break;
            case "ProfileFragment": fragment = new ProfileFragment();
                break;
            case "LoadingFragment": fragment = new LoadingFragment();
                break;
            case "MapsFragment": fragment = new MapsFragment();
                break;
            case "RegisterFragment": fragment = new RegisterFragment();
                break;
            case "NonUserFragment": fragment = new NonUserFragment();
                break;
            case "UserFragment":  fragment = new UserFragment();
                break;
            case "NonUserControlFragment": fragment = new NonUserControlFragment();
                break;
            case "UserControlFragment": fragment = new UserControlFragment();
                break;
            case "LayoutFragment": fragment = new LayoutFragment();
                break;
            case "ServicesFragment": fragment = new ServicesFragment();
                break;
            default: return;
        }

        if (numBackStack(fragmentManager) == 0){
            fragmentManager
                    .beginTransaction()
                    .add(id, Objects.requireNonNull(fragment), key)
                    .addToBackStack(key)
                    .commit();
        }else {
            fragmentManager
                    .beginTransaction()
                    .replace(id, Objects.requireNonNull(fragment), key)
                    .addToBackStack(key)
                    .commit();
        }
    }

    public static void selecionarFragment(String key, FragmentManager fragmentManager){
        if (!TextUtils.equals(ultimoNaBackstack(fragmentManager), key)){
            fragmentManager.popBackStack(key, 0);
        }
    }

    public static void observarFragment(String key, FragmentManager fragmentManager, int id) {
        if (existeNaBackstack(key, fragmentManager)){
            selecionarFragment(key, fragmentManager);
        }else {
            criarFragment(key, fragmentManager, id);
        }
    }
}