package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentLoadingBinding;

public class LoadingFragment extends Fragment {
    private FragmentLoadingBinding binding;
    private View.OnClickListener onClickListener;
    private FragmentResultListener fragmentResultListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_loading, container, false);
        return binding.layoutLoading;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onClickListener = (View.OnClickListener) requireParentFragment();
        fragmentResultListener = (requestKey, result) -> {
            for (String key : result.keySet()) {
                if (TextUtils.equals(key, "value")) {
                    observarLoad(result.getInt(key));
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.retryBtn.setOnClickListener(onClickListener);
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.retryBtn.setOnClickListener(null);
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentResultListener = null;
        binding.layoutLoading.removeAllViews();
        onClickListener = null;
        fragmentResultListener = null;
        binding = null;
    }

    private void observarLoad(Integer integer) {
        switch (integer){
            case 0: mostrarProgresso();
                break;
            case 1: mostrarBtn();
                break;
        }
    }

    private void mostrarProgresso() {
        if (binding.retryBtn.getVisibility() == View.VISIBLE){
            binding.retryBtn.setVisibility(View.GONE);
        }

        binding.barraProgresso.setVisibility(View.VISIBLE);
    }

    private void mostrarBtn() {
        if (binding.barraProgresso.getVisibility() == View.VISIBLE) {
            binding.barraProgresso.setVisibility(View.GONE);
        }

        binding.retryBtn.setVisibility(View.VISIBLE);
    }
}