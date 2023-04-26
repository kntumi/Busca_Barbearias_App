package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.LayoutUserBinding;
import kev.app.timeless.model.User;

public class UserFragment extends DaggerFragment {
    private LayoutUserBinding binding;
    private View.OnClickListener onClickListener;
    private Observer<User> observer;
    private Disposable disposable;
    private AlertDialog alertDialog;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;

    @Inject
    Service service;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_user, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onStart() {
        super.onStart();
        onClickListener = this::observarClick;
        observer = this::observarUser;
        alertDialog = new MaterialAlertDialogBuilder(requireActivity()).setCancelable(false).create();
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof RemoveAccountFragment) {
                    for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
                        binding.layoutPrincipal.getChildAt(i).setVisibility(binding.layoutPrincipal.getChildAt(i).getId() == f.getView().getId() ? View.VISIBLE : View.GONE);
                    }
                }
                
                alertDialog.setView(f.getView());
                alertDialog.show();
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof RemoveAccountFragment) {
                    for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
                        binding.layoutPrincipal.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                }

                alertDialog.setView(null);
                alertDialog.hide();
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.apagarConta.setOnClickListener(onClickListener);
        binding.terminarSessao.setOnClickListener(onClickListener);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.apagarConta.setOnClickListener(null);
        binding.terminarSessao.setOnClickListener(null);
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentLifecycleCallbacks = null;
        alertDialog = null;
        observer = null;
        onClickListener = null;
        binding.layoutPrincipal.removeAllViews();
        binding = null;
    }

    private void observarClick(View view) {
        switch (view.getId()) {
            case R.id.terminarSessao: service.getAuthService().fazerLogOut();
                break;
            case R.id.apagarConta: getChildFragmentManager().beginTransaction().add(new RemoveAccountFragment(), null).runOnCommit(() -> getChildFragmentManager().setFragmentResult("RemoveAccountFragment", newBundle())).commit();
                break;
        }
    }

    private Bundle newBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("email", service.getAuth().getCurrentUser().getEmail());
        return bundle;
    }

    private void observarUser(User user) {
        if (user != null) {
            binding.txtEmail.setText(user.getEmail());
        }
    }

    public Observer<User> getObserver() {
        return observer;
    }
}