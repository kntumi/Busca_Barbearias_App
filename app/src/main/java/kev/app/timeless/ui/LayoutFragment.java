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

import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutFragmentBinding;
import kev.app.timeless.util.FragmentUtil;

public class LayoutFragment extends BottomSheetDialogFragment {
    private LayoutFragmentBinding binding;
    private FragmentResultListener childResultListener, parentResultListener;
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
        childResultListener = this::observarChild;
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                getChildFragmentManager().setFragmentResult(f.getClass().getSimpleName(), new Bundle(bundle));
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, childResultListener);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        childResultListener = null;
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
            FragmentUtil.observarFragment(result.getString("fragmentToLoad"), getChildFragmentManager(), binding.layoutPrincipal.getId());
            return;
        }

        if (getChildFragmentManager().getFragments().size() == 0) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.layoutPrincipal.getId(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? new MyServicesFragment() : new ServicesFragment())
                    .commit();
        }
    }

    private void observarChild(String requestKey, Bundle result) {
        if (result.containsKey("idTipoServiço")) {
            if (TextUtils.isEmpty(result.getString("idTipoServiço"))) {
                for (Fragment f : getChildFragmentManager().getFragments()) {
                    if (f.isResumed()) {
                        if (TextUtils.equals(f.getClass().getSimpleName(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? "MySubServiceFragment" : "SubServiceFragment")) {
                            getChildFragmentManager().beginTransaction().remove(f).commit();
                            break;
                        }
                    }
                }

                return;
            }

            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (f.isResumed()) {
                    if (TextUtils.equals(f.getClass().getSimpleName(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? "MySubServiceFragment" : "SubServiceFragment")) {
                        return;
                    }
                }
            }

            bundle.putString("idTipoServiço", result.getString("idTipoServiço"));

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.layoutPrincipal.getId(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? new MySubServiceFragment() : new SubServiceFragment())
                    .commit();

            return;
        }

        if (result.containsKey("idServiço")) {
            if (TextUtils.isEmpty(result.getString("idServiço"))) {
                for (Fragment f : getChildFragmentManager().getFragments()) {
                    if (f.isResumed()) {
                        if (TextUtils.equals(f.getClass().getSimpleName(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? "MyTypeServicesFragment" : "TypeServicesFragment")) {
                            getChildFragmentManager().beginTransaction().remove(f).commit();
                            break;
                        }
                    }
                }

                return;
            }

            for (Fragment f : getChildFragmentManager().getFragments()) {
                if (f.isResumed()) {
                    if (TextUtils.equals(f.getClass().getSimpleName(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? "MyTypeServicesFragment" : "TypeServicesFragment")) {
                        return;
                    }
                }
            }

            bundle.putString("idServiço", result.getString("idServiço"));

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(binding.layoutPrincipal.getId(), TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineEstablishmentFragment") ? new MyTypeServicesFragment() : new TypeServicesFragment())
                    .commit();

            return;
        }

        Fragment parent = null;

        for (Fragment currentFragment : getChildFragmentManager().getFragments()) {
            if (currentFragment.isResumed()) {
                parent = currentFragment;
                break;
            }
        }

        if (parent == null) {
            return;
        }

        if (!bundle.containsKey(parent.getClass().getSimpleName())) {
            bundle.putBundle(parent.getClass().getSimpleName(), new Bundle());
        }

        for (Fragment child : parent.getChildFragmentManager().getFragments()) {
            if (child.isResumed()) {
                if (!bundle.getBundle(parent.getClass().getSimpleName()).containsKey(child.getClass().getSimpleName())) {
                    bundle.getBundle(parent.getClass().getSimpleName()).putBundle(child.getClass().getSimpleName(), new Bundle());
                }

                if (result.containsKey("position")) {
                    bundle.getBundle(parent.getClass().getSimpleName()).getBundle(child.getClass().getSimpleName()).putInt("position", result.getInt("position"));
                } else {
                    if (bundle.getBundle(parent.getClass().getSimpleName()).getBundle(child.getClass().getSimpleName()).containsKey("position")) {
                        bundle.getBundle(parent.getClass().getSimpleName()).getBundle(child.getClass().getSimpleName()).remove("position");
                    }
                }

                parent.getChildFragmentManager().setFragmentResult(child.getClass().getSimpleName(), bundle);

                break;
            }
        }
    }
}