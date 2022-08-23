package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import java.util.Map;

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
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private String nome;
    private Menu menu;
    private MapViewModel viewModel;
    private TextWatcher textWatcher;

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
        menu = binding.barra.getMenu();
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                MenuItem item = menu.getItem(0);

                if (TextUtils.isEmpty(editable) || TextUtils.equals(editable, nome)) {
                    if (item.isEnabled()) {
                        item.setEnabled(false);
                    }

                    return;
                }

                if (!item.isEnabled()) {
                    item.setEnabled(true);
                }
            }
        };
        onMenuItemClickListener = this::observarMenuItemClick;

        if (savedInstanceState == null) {
            fragmentResultListener = this::observarParent;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fragmentResultListener != null) {
            requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        }

        binding.barra.setNavigationOnClickListener(view -> requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new AboutFragment()).commit());
        binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);
        binding.txtNome.addTextChangedListener(textWatcher);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.txtNome.removeTextChangedListener(textWatcher);
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
        onMenuItemClickListener = null;
        textWatcher = null;
        viewModel = null;
        nome = null;
        fragmentResultListener = null;
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        bundle = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle("bundle");

        if (savedInstanceState != null) {
            observarParent(null, bundle);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        if (viewModel.getEstabelecimentos().containsKey(bundle.getString("id"))) {
            Map<String, Object> map = viewModel.getEstabelecimentos().get(bundle.getString("id"));

            if (map.containsKey("nome")) {
                nome = String.valueOf(map.get("nome"));
            }
        }

        binding.barra.setSubtitle(TextUtils.isEmpty(nome) ? "Desconhecido" : nome);
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
}