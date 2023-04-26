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
import kev.app.timeless.databinding.LocationWhereaboutsBinding;

public class LocationWhereAbout extends BottomSheetDialogFragment {
    private LocationWhereaboutsBinding whereaboutsBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        whereaboutsBinding = DataBindingUtil.inflate(inflater, R.layout.location_whereabouts, container, false);
        return whereaboutsBinding.parent;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
