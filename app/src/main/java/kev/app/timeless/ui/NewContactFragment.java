package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
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
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private TextWatcher textWatcher;
    private MapViewModel viewModel;

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
                System.out.println("areItemsTheSame ");
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
                System.out.println("areContentsTheSame ");
                return false;
            }
        }, this);
        contactsTypeAdapter.submitList(Arrays.asList("Sim", "Não"));
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

                if (bundle.containsKey("idToUpdate")) {
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

        if (onGlobalLayoutListener != null) {
            binding.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
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
        viewModel = null;
        onGlobalLayoutListener = null;
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

            bundle.putString("idToUpdate", String.valueOf(result.getInt("idToUpdate")));
            binding.nrTelefone.setText(bundle.getString("idToUpdate"));
            binding.barra.setTitle("Editar contacto");

            if (onGlobalLayoutListener == null) {
                onGlobalLayoutListener = () -> {
                    try {
                        Map<String, Object> map = viewModel.getContactos().get(bundle.getString("id")).get(bundle.getString("idToUpdate"));
                        boolean isChecked = Boolean.parseBoolean(String.valueOf(map.get("contactoPrincipal")));

                        if (bundle.get("selectedPosition") != null) {
                            CompoundButton button = (CompoundButton) binding.recyclerView.getChildAt(bundle.getInt("selectedPosition"));

                            if (button.isChecked() == isChecked) {
                                return;
                            }
                        }

                        String txtBtn = isChecked ? "Sim" : "Não";

                        for (int n = 0 ; n < binding.recyclerView.getChildCount() ; n++) {
                            CompoundButton button = (CompoundButton) binding.recyclerView.getChildAt(n);

                            if (!TextUtils.equals(txtBtn, button.getText())) {
                                continue;
                            }

                            button.setChecked(true);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                };
            }

            binding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        }

        if (TextUtils.isEmpty(binding.barra.getTitle())) {
            for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
                binding.barra.getMenu().getItem(i).setEnabled(bundle.containsKey("selectedPosition"));
            }

            binding.barra.setTitle("Novo contacto");
        }
    }

    private void observarResposta(Boolean aBoolean) {
        if (aBoolean) {
            onClick(null);
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
            if (compoundButton != binding.recyclerView.getChildAt(i)) {
                continue;
            }

            if (b) {
                bundle.putInt("selectedPosition", i);
            } else {
                bundle.remove("selectedPosition");
            }

            break;
        }

        for (int i = 0 ; i < binding.recyclerView.getChildCount() ; i++) {
            if (i == bundle.getInt("selectedPosition")) {
                continue;
            }

            CompoundButton button = (CompoundButton) binding.recyclerView.getChildAt(i);
            button.setChecked(false);
            break;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().getItem(i).setEnabled(bundle.containsKey("selectedPosition"));
        }
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        if (map == null) {
            map = new HashMap<>();
        }

        map.put("contactoPrincipal", bundle.getInt("selectedPosition") == 0);

        return disposables.add(service.getBarbeariaService().inserirContacto(bundle.getString("id"), binding.nrTelefone.getText().toString(), map).doOnSubscribe(disposable -> {
            if (onGlobalLayoutListener != null) {
                binding.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
            }
        })
                .doFinally(() -> {
                    if (onGlobalLayoutListener != null) {
                        binding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
                    }
                }).doOnEvent((aBoolean, throwable) -> bundle.putString("status", throwable == null ? String.valueOf(aBoolean) : "between")).doOnSubscribe(disposable -> item.setEnabled(false)).doFinally(() -> item.setEnabled(true)).subscribe(this::observarResposta, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show()));
    }
}