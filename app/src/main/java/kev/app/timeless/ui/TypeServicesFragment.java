package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutDefaultBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.TipoServiço;
import kev.app.timeless.model.User;
import kev.app.timeless.util.State;
import kev.app.timeless.util.TypeServiceAdapter;
import kev.app.timeless.util.TypeServicesAdapter;
import kev.app.timeless.viewmodel.MapViewModel;

public class TypeServicesFragment extends DaggerFragment implements EventListener<QuerySnapshot>, View.OnClickListener {
    @Inject
    ViewModelProvidersFactory providerFactory;

    private LayoutDefaultBinding binding;
    private MapViewModel viewModel;
    private ListenerRegistration listenerRegistration;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private FragmentResultListener parentResultListener;
    private Bundle bundle;
    private Disposable disposable;
    private Observer<List<User>> observer;
    private TypeServiceAdapter typeServiceAdapter;
    private TypeServicesAdapter typeServicesAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_default, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        parentResultListener = this::observarParent;
        observer = this::observarUser;
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        typeServiceAdapter = new TypeServiceAdapter(new DiffUtil.ItemCallback<State>() {
            @Override
            public boolean areItemsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }
        }, this);
        binding.recyclerView.setAdapter(typeServiceAdapter);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

        if (disposable != null) {
            disposable.dispose();
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        binding.recyclerView.setLayoutManager(null);
        bundle.clear();
        listenerRegistration = null;
        disposable = null;
        observer = null;
        typeServiceAdapter = null;
        onMenuItemClickListener = null;
        bundle = null;
        viewModel = null;
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

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        binding.barra.setTitle(viewModel.getServiços().containsKey(bundle.getString("id")) ? "Tipos de ".concat(String.valueOf(viewModel.getServiços().get(bundle.getString("id")).get(bundle.getString("selectedService")).get("nome"))) : null);

        State state;

        if (viewModel.getTiposServiços().containsKey(bundle.getString("id"))) {
            state = viewModel.getTiposServiços().get(bundle.getString("id")).size() == 0 ? State.Empty : State.Loaded;
        } else {
            state = State.Loading;
        }

        if (state != State.Loaded) {
            typeServiceAdapter.submitList(Collections.singletonList(state), () -> {
                List<State> states = typeServiceAdapter.getCurrentList();

                for (int  i = 0 ; i < states.size(); i++) {
                    if (states.get(i) == State.Loading) {
                        obterTiposServicos(result);
                    }
                }
            });

            return;
        }

        onLoad();
    }

    private void onLoad() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (typeServicesAdapter == null) {
            typeServicesAdapter = new TypeServicesAdapter(new DiffUtil.ItemCallback<TipoServiço>() {
                @Override
                public boolean areItemsTheSame(@NonNull TipoServiço oldItem, @NonNull TipoServiço newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull TipoServiço oldItem, @NonNull TipoServiço newItem) {
                    return false;
                }
            }, this);
        }

        adicionarListener();

        if (binding.recyclerView.getAdapter() == typeServicesAdapter) {
            return;
        }

        binding.recyclerView.setAdapter(typeServicesAdapter);

        ArrayList<TipoServiço> tiposServicos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getTiposServiços().get(bundle.getString("selectedService")).entrySet()) {
            tiposServicos.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
        }

        typeServicesAdapter.submitList(tiposServicos);
    }

    private void obterTiposServicos(Bundle result) {
        disposable = viewModel.getService().getBarbeariaService().obterTiposServiços(result.getString("id"), result.getString("selectedService")).subscribe(stringMapMap -> {
            viewModel.getTiposServiços().put(result.getString("selectedService"), stringMapMap);

            if (stringMapMap.size() != 0) {
                onLoad();
                return;
            }

            typeServiceAdapter.submitList(Collections.singletonList(State.Empty), this::adicionarListener);
        }, throwable -> typeServiceAdapter.submitList(Collections.singletonList(State.Error)));
    }

    private void adicionarListener() {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").document(bundle.getString("selectedService")).collection("tipos").addSnapshotListener(this);
    }

    private void observarUser(List<User> users) {
        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            if (onMenuItemClickListener == null) {
                onMenuItemClickListener = this::observarOnMenuItemClick;
            }

            binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);

            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        new InsertTypeServiceFragment().show(requireParentFragment().getChildFragmentManager(), null);
        return true;
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (binding.recyclerView.getAdapter() != typeServiceAdapter) {
                    binding.recyclerView.setAdapter(typeServiceAdapter);
                }

                if (viewModel.getTiposServiços().containsKey(bundle.getString("selectedService"))) {
                    viewModel.getTiposServiços().get(bundle.getString("selectedService")).clear();
                }

                State state = typeServiceAdapter.getCurrentList().get(0);

                if (state != State.Empty) {
                    typeServiceAdapter.submitList(Collections.singletonList(State.Empty));
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (viewModel.getTiposServiços().containsKey(bundle.getString("selectedService"))) {
                if (map.equals(viewModel.getTiposServiços().get(bundle.getString("selectedService")))) {
                    return;
                }
            }

            if (binding.recyclerView.getAdapter() != typeServicesAdapter) {
                binding.recyclerView.setAdapter(typeServicesAdapter);
            }

            viewModel.getTiposServiços().put(bundle.getString("selectedService"), map);

            ArrayList<TipoServiço> tiposServicos = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                tiposServicos.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
            }

            typeServicesAdapter.submitList(tiposServicos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn: obterTiposServicos(bundle);
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new ServicesFragment()).commit();
                break;
            case R.id.txt: try {

                              Bundle b = new Bundle();
                              b.putString("fragmentToLoad", "SubServiceFragment");
                              b.putString("selectedService", bundle.getString("selectedService"));
                              b.putString("selectedTypeService", typeServicesAdapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getId());
                              requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);

                           } catch (Exception e) {
                              e.printStackTrace();
                           }
                break;
        }
    }
}