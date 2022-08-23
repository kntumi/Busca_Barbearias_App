package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutNonUserBinding;
import kev.app.timeless.util.FragmentUtil;

public class NonUserFragment extends Fragment {
    private LayoutNonUserBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_non_user, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.iniciarSessao.setOnClickListener(view -> FragmentUtil.observarFragment("LoginFragment", requireActivity().getSupportFragmentManager(), R.id.layoutPrincipal));
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.iniciarSessao.setOnClickListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}