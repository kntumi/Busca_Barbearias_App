package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import kev.app.timeless.model.SubServiço;
import kev.app.timeless.model.User;
import kev.app.timeless.util.State;
import kev.app.timeless.util.SubServiceAdapter;
import kev.app.timeless.util.SubServiceListAdapter;
import kev.app.timeless.viewmodel.MapViewModel;

public class SubServiceFragment extends DaggerFragment implements View.OnClickListener, EventListener<QuerySnapshot> {
    private LayoutDefaultBinding binding;
    private MapViewModel viewModel;
    private ItemTouchHelper itemTouchHelper;
    private Disposable disposable;
    private LinearLayoutManager linearLayoutManager;
    private ListenerRegistration listenerRegistration;
    private FragmentResultListener fragmentResultListener;
    private Bundle bundle;
    private String loggedInUserId;
    private SubServiceAdapter subServiceAdapter;
    private Observer<List<User>> observer;
    private SubServiceListAdapter subServiceListAdapter;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_default, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = this::observarUser;
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        fragmentResultListener = this::observarResult;
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        subServiceAdapter = new SubServiceAdapter(new DiffUtil.ItemCallback<State>() {
            @Override
            public boolean areItemsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }
        }, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }
        }

        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle = null;
        observer = null;
        linearLayoutManager = null;
        viewModel = null;
        subServiceListAdapter = null;
        fragmentResultListener = null;
        listenerRegistration = null;
        subServiceAdapter = null;
        itemTouchHelper = null;
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

    private void observarUser(List<User> users) {
        loggedInUserId = users.size() == 0 ? null : users.get(0).getId();

        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            binding.barra.setOnMenuItemClickListener(item -> {
                requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new InsertSubServiceFragment()).commit();
                return true;
            });

            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    private void observarResult(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
            Map<String, Map<String, Object>> map = viewModel.getServiços().get(bundle.getString("id"));

            if (map.containsKey(bundle.getString("selectedService"))) {
                binding.barra.setTitle("Tipos de "+map.get(result.getString("selectedService")).get("nome"));
            }
        }

        if (viewModel.getTiposServiços().containsKey(bundle.getString("selectedService"))) {
            Map<String, Map<String, Object>> map = viewModel.getTiposServiços().get(bundle.getString("selectedService"));

            if (map.containsKey(bundle.getString("selectedTypeService"))) {
                binding.barra.setTitle(binding.barra.getTitle()+" de "+map.get(bundle.getString("selectedTypeService")).get("nome"));
            }
        }

        State state;

        if (viewModel.getSubServiços().containsKey(bundle.getString("selectedTypeService"))) {
            state = viewModel.getSubServiços().get(bundle.getString("selectedTypeService")).size() == 0 ? State.Empty : State.Loaded;
        } else {
            state = State.Loading;
        }

        if (state != State.Loaded) {
            subServiceAdapter.submitList(Collections.singletonList(state), () -> {
                List<State> states = subServiceAdapter.getCurrentList();

                for (int  i = 0 ; i < states.size(); i++) {
                    if (states.get(i) == State.Loading) {
                        obterSubServicos(bundle);
                    }
                }
            });

            return;
        }
        
        onLoad();
    }

    private void obterSubServicos(Bundle bundle) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = viewModel.getService().getBarbeariaService().obterSubServiços(bundle.getString("id"), bundle.getString("selectedService"), bundle.getString("selectedTypeService")).subscribe(stringMapMap -> {
            viewModel.getSubServiços().put(bundle.getString("selectedTypeService"), stringMapMap);

            if (stringMapMap.size() != 0) {
                onLoad();
                return;
            }

            subServiceAdapter.submitList(Collections.singletonList(State.Empty), this::adicionarListener);
        }, throwable -> subServiceAdapter.submitList(Collections.singletonList(State.Error)));
    }

    private void onLoad() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (subServiceListAdapter == null) {
            subServiceListAdapter = new SubServiceListAdapter(new DiffUtil.ItemCallback<SubServiço>() {
                @Override
                public boolean areItemsTheSame(@NonNull SubServiço oldItem, @NonNull SubServiço newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull SubServiço oldItem, @NonNull SubServiço newItem) {
                    return false;
                }
            });
        }

        adicionarListener();

        if (!TextUtils.isEmpty(loggedInUserId) && itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    switch (direction) {
                        case ItemTouchHelper.LEFT:
                            break;
                        case ItemTouchHelper.RIGHT:
                            break;
                    }
                }
            });
        }

        ArrayList<SubServiço> subServicos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry: viewModel.getSubServiços().get(bundle.getString("selectedTypeService")).entrySet()) {
            subServicos.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
        }

        subServiceListAdapter.submitList(subServicos);
    }

    private void adicionarListener () {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").document(bundle.getString("selectedService")).collection("tipos").document(bundle.getString("selectedTypeService")).collection("subservicos").addSnapshotListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn: obterSubServicos(bundle);
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new TypeServicesFragment()).commit();
                break;
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getSubServiços().containsKey(bundle.getString("selectedTypeService"))) {
                    viewModel.getSubServiços().get(bundle.getString("selectedTypeService")).clear();
                }

                subServiceAdapter.submitList(Collections.singletonList(State.Empty));

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (viewModel.getSubServiços().containsKey(bundle.getString("selectedTypeService"))) {
                if (map.equals(viewModel.getSubServiços().get(bundle.getString("selectedTypeService")))) {
                    return;
                }
            }

            viewModel.getSubServiços().put(bundle.getString("selectedTypeService"), map);

            ArrayList<SubServiço> subServicos = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry: map.entrySet()) {
                subServicos.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
            }

            subServiceListAdapter.submitList(subServicos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}