package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.firebase.firestore.ListenerRegistration;

import kev.app.timeless.R;
import kev.app.timeless.databinding.ControlUserBinding;

public class UserControlFragment extends Fragment {
    private ControlUserBinding binding;
    private View.OnClickListener onClickListener;
    private Observer<Boolean> observer;
    private ListenerRegistration listenerRegistration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.control_user, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onClickListener = (View.OnClickListener) requireParentFragment();
        observer = aBoolean -> binding.btnEncontrar.setEnabled(aBoolean);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.btnEncontrar.setOnClickListener(onClickListener);
        binding.btnEncontrarBarberia.setOnClickListener(onClickListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.btnEncontrar.setOnClickListener(null);
        binding.btnEncontrarBarberia.setOnClickListener(null);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listenerRegistration = null;
        observer = null;
        onClickListener = null;
        binding = null;
    }

    public Observer<Boolean> getObserver() {
        return observer;
    }
}