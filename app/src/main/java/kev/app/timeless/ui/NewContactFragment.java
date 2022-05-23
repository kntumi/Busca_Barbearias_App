package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentNewContactBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.util.ContactsTypeAdapter;
import kev.app.timeless.viewmodel.MapViewModel;

public class NewContactFragment extends DaggerFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private FragmentNewContactBinding binding;
    private List<Disposable> disposables;
    private ContactsTypeAdapter contactsTypeAdapter;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private Map<String, Object> map;
    private TextWatcher textWatcher;
    private MapViewModel viewModel;
    private Runnable runnable;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Inject
    Service service;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_new_contact, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contactsTypeAdapter = new ContactsTypeAdapter(new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                return false;
            }
        }, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(contactsTypeAdapter);
        parentResultListener = this::observarParent;
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                binding.barra.setOnMenuItemClickListener(null);

                if (TextUtils.isEmpty(editable)) {
                    return;
                }

                if (onMenuItemClickListener == null) {
                    onMenuItemClickListener = NewContactFragment.this::observarOnMenuItemClick;
                }

                if (disposables == null) {
                    disposables = new ArrayList<>();
                }

                for (Disposable disposable : disposables) {
                    if (!disposable.isDisposed()) {
                        disposable.dispose();
                    }

                    disposables.remove(disposable);
                }

                if (TextUtils.equals("Editar contacto", binding.barra.getTitle())) {
                    binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);
                    return;
                }

                disposables.add(service.getBarbeariaService().obterContacto(bundle.getString("id"), editable.toString()).doOnSuccess(aBoolean -> {
                    if (!aBoolean) {
                        binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);
                    }
                }).subscribe(aBoolean -> binding.numero.setError(aBoolean ? "Um contacto seu com este número foi encontrado" : null), throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show()));
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.nrTelefone.addTextChangedListener(textWatcher);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.nrTelefone.removeTextChangedListener(textWatcher);
        binding.barra.setNavigationOnClickListener(null);

        if (onMenuItemClickListener != null) {
            binding.barra.setOnMenuItemClickListener(null);
        }

        if (disposables != null) {
            for (Disposable disposable : disposables) {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            disposables.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        binding.recyclerView.setLayoutManager(null);
        onMenuItemClickListener = null;
        runnable = null;
        viewModel = null;
        map = null;
        contactsTypeAdapter = null;
        textWatcher = null;
        disposables = null;
        parentResultListener = null;
        bundle = null;
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
            if (viewModel == null) {
                viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
            }

            List<Map<String, Object>> contactos = viewModel.getContactos().get(bundle.getString("id"));
            String id = String.valueOf(result.getInt("idToUpdate"));

            for (int i = 0; i < Objects.requireNonNull(contactos).size() ; i++) {
                Map<String, Object> map = contactos.get(i);

                if (!TextUtils.equals(String.valueOf(map.get("nrTelefone")), id)) {
                    continue;
                }

                String contactoPrincipal = Boolean.parseBoolean(String.valueOf(map.get("contactoPrincipal"))) ? "Sim" : "Não";

                if (runnable == null) {
                    runnable = () -> {
                        System.out.println("crazy bone "+binding.recyclerView.getChildCount());
                        for (int n = 0 ; n < binding.recyclerView.getChildCount() ; n++) {
                            CompoundButton button = (CompoundButton) binding.recyclerView.getChildAt(n);

                            if (!TextUtils.equals(contactoPrincipal, button.getText())) {
                                continue;
                            }

                            button.setChecked(true);
                            break;
                        }
                    };
                }

                binding.barra.setTitle("Editar contacto");
                binding.nrTelefone.setText(String.valueOf(map.get("nrTelefone")));

                break;
            }
        }

        if (TextUtils.isEmpty(binding.barra.getTitle())) {
            binding.barra.setTitle("Novo contacto");
        }

        if (contactsTypeAdapter.getCurrentList().size() == 0) {
            contactsTypeAdapter.submitList(Arrays.asList("Sim", "Não"), new Runnable() {
                @Override
                public void run() {
                    binding.recyclerView.setAdapter(contactsTypeAdapter);
                }
            });
        }
    }

    private void observarResposta(Boolean aBoolean) {
        if (aBoolean) {
            onClick(binding.recyclerView);
        } else {
            Toast.makeText(requireContext(), "", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        Bundle b = new Bundle();
        b.putString("fragmentToLoad", "ContactsFragment");
        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult("LayoutFragment", b);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        for (int i = 0 ; i < binding.recyclerView.getChildCount() ; i++) {
            if (compoundButton == binding.recyclerView.getChildAt(i)) {
                if (b) {
                    bundle.putInt("selectedPosition", i);
                }

                break;
            }
        }

        for (int i = 0 ; i < binding.recyclerView.getChildCount() ; i++) {
            if (i != bundle.getInt("selectedPosition")) {
                CompoundButton button = (CompoundButton) binding.recyclerView.getChildAt(i);
                button.setChecked(false);
                break;
            }
        }
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        if (map == null) {
            map = new HashMap<>();
        }

        map.put("contactoPrincipal", bundle.getInt("selectedPosition") == 0);

        return disposables.add(service.getBarbeariaService().inserirContacto(bundle.getString("id"), binding.nrTelefone.getText().toString(), map).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
            }
        }).doOnSubscribe(disposable -> item.setEnabled(false)).doFinally(() -> item.setEnabled(true)).subscribe(this::observarResposta, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show()));
    }
}