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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Objects;

import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutFragmentBinding;
import kev.app.timeless.util.FragmentUtil;

public class LayoutFragment extends BottomSheetDialogFragment {
    private LayoutFragmentBinding binding;
    private FragmentResultListener parentResultListener;
    private Bundle bundle;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_fragment, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentResultListener = this::observarParent;
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (!f.isStateSaved()) {
                    getChildFragmentManager().setFragmentResult(f.getClass().getSimpleName(), new Bundle(bundle.containsKey(f.getClass().getSimpleName()) ? bundle.getBundle(f.getClass().getSimpleName()) : bundle));
                }

                System.out.println("isStateSaved: "+f.isStateSaved());
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        parentResultListener = null;
        bundle = null;
        fragmentLifecycleCallbacks = null;
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        bundle = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle("bundle");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        if (result.containsKey("idToUpdate")) {
            bundle.putInt("idToUpdate", result.getInt("idToUpdate"));
        } else {
            bundle.remove("idToUpdate");
        }

        if (result.containsKey("fragmentToLoad")) {
            String key = result.getString("fragmentToLoad");

            if (!result.containsKey("id")) {
                result.putString("id", bundle.getString("id"));
            }

            result.remove("fragmentToLoad");

            bundle.putBundle(key, result);

            getChildFragmentManager().beginTransaction().replace(binding.layoutPrincipal.getId(), FragmentUtil.obterFragment(Objects.requireNonNull(key))).commit();
        }
    }
}