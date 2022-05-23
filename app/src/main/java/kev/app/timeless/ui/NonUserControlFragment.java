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

import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutControlNonUserBinding;

public class NonUserControlFragment extends Fragment {
    private LayoutControlNonUserBinding binding;
    private View.OnClickListener onClickListener;
    private Observer<Boolean> observer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_control_non_user, container, false);
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
        binding.btnOpcoes.setOnClickListener(onClickListener);
        binding.btnVoltar.setOnClickListener(onClickListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.btnEncontrar.setOnClickListener(null);
        binding.btnOpcoes.setOnClickListener(null);
        binding.btnVoltar.setOnClickListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        observer = null;
        onClickListener = null;
        binding = null;
    }

    public Observer<Boolean> getObserver() {
        return observer;
    }
}