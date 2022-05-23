package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.InsertNameBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.viewmodel.MapViewModel;

public class InsertNameFragment extends DaggerFragment {
    private InsertNameBinding binding;
    private Bundle bundle;
    private FragmentResultListener fragmentResultListener;
    private Disposable disposable;
    private MapViewModel viewModel;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.insert_name, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        fragmentResultListener = this::observarParent;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        binding.barra.setNavigationOnClickListener(this::observarClick);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);
        binding.barra.setOnMenuItemClickListener(null);

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle = null;
        viewModel = null;
        fragmentResultListener = null;
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

        binding.barra.setOnMenuItemClickListener(null);

        if (TextUtils.isEmpty(bundle.getString("id"))) {
            requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
            return;
        }

        binding.nome.setHint(viewModel.getEstabelecimentos().containsKey(bundle.getString("id")) ? viewModel.getEstabelecimentos().get(bundle.getString("id")).containsKey("nome") ? TextUtils.isEmpty(String.valueOf(viewModel.getEstabelecimentos().get(bundle.getString("id")).get("nome"))) ? "Desconhecido" : String.valueOf(viewModel.getEstabelecimentos().get(bundle.getString("id")).get("nome")) : "Desconhecido" : "Desconhecido");
        binding.barra.setOnMenuItemClickListener(this::observarMenuItemClick);
    }

    private boolean observarMenuItemClick(MenuItem item) {
        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        disposable = viewModel.getService().getBarbeariaService().editarNome(binding.txtNome.getText().toString(), bundle.getString("id")).doOnSubscribe(disposable -> item.setEnabled(false)).doFinally(() -> item.setEnabled(true)).subscribe(aBoolean -> {
            if (aBoolean){
                requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
            } else {
                Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
            }
        }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());

        return true;
    }


    private void observarClick(View view) {
        Bundle b = new Bundle();
        b.putString("fragmentToLoad", "AboutFragment");
        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
    }
}