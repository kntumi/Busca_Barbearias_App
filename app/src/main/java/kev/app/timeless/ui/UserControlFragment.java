package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.ControlUserBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;

public class UserControlFragment extends DaggerFragment {
    private ControlUserBinding binding;
    private View.OnClickListener onClickListener;
    private Observer<Boolean> observer;
    private ListenerRegistration listenerRegistration;
    private LiveData<List<User>> users;
    private Observer<List<User>> userObserver;

    @Inject
    Service service;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.control_user, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userObserver = this::observarUsers;
        users = service.userDao().buscarUsuarioActual();
        onClickListener = (View.OnClickListener) requireParentFragment();
        observer = aBoolean -> binding.btnEncontrar.setEnabled(aBoolean);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.btnEncontrar.setOnClickListener(onClickListener);
        binding.btnOpcoes.setOnClickListener(onClickListener);
        binding.btnVoltar.setOnClickListener(onClickListener);
        binding.btnEncontrarBarberia.setOnClickListener(onClickListener);
        users.observeForever(userObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.btnEncontrar.setOnClickListener(null);
        binding.btnOpcoes.setOnClickListener(null);
        binding.btnVoltar.setOnClickListener(null);
        binding.btnEncontrarBarberia.setOnClickListener(null);
        users.removeObserver(userObserver);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        users = null;
        userObserver = null;
        service = null;
        listenerRegistration = null;
        observer = null;
        onClickListener = null;
        binding = null;
    }

    public Observer<Boolean> getObserver() {
        return observer;
    }

    private void observarUsers(List<User> users) {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

    }

    private void observarDocument(DocumentSnapshot value, FirebaseFirestoreException error) {
        try {
            if (value.exists()) {

            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}