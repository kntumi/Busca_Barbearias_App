package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentSearchBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.viewmodel.MapViewModel;

public class SearchFragment extends DaggerFragment implements View.OnClickListener, SearchView.OnQueryTextListener {
    private FragmentSearchBinding binding;
    private Bundle bundle;
    private MapViewModel viewModel;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bundle = getArguments();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        observarBundle(savedInstanceState == null ? bundle : savedInstanceState.getBundle("bundle"));
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.materialToolbar.setNavigationOnClickListener(this);
        binding.searchView.setOnQueryTextListener(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.materialToolbar.setNavigationOnClickListener(null);
        binding.searchView.setOnQueryTextListener(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.layoutPrincipal.removeAllViews();
        viewModel = null;
        binding = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case View.NO_ID:  requireActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(binding.layoutPrincipal.getId(), new MapsFragment()).commit();
                break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    private void observarBundle(Bundle result) {
        bundle = bundle == null || bundle.size() == 0 ? result : bundle;
    }
}