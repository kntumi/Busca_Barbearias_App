package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutFragmentBinding;

public class LayoutFragment extends BottomSheetDialogFragment {
    private LayoutFragmentBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_fragment, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.layoutPrincipal.removeAllViews();
        binding = null;
    }
}