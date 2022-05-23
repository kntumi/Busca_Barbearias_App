package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentProfileBinding;
import kev.app.timeless.model.User;
import kev.app.timeless.util.FragmentUtil;

public class ProfileFragment extends BottomSheetDialogFragment {
    private Observer<List<User>> observer;
    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onStart() {
        super.onStart();
        observer = this::observarUser;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        observer = null;
        binding = null;
    }

    private void observarUser(List<User> users) {
        if (users.size() == 0) {
            FragmentUtil.observarFragment("NonUserFragment", getChildFragmentManager(), R.id.layoutPrincipal);
        } else {
            FragmentUtil.observarFragment("UserFragment", getChildFragmentManager(), R.id.layoutPrincipal);
        }
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }
}