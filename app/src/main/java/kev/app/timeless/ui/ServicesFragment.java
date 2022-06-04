package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import kev.app.timeless.model.Serviço;
import kev.app.timeless.model.User;
import kev.app.timeless.util.ServiceAdapter;
import kev.app.timeless.util.ServicesAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class ServicesFragment extends DaggerFragment implements View.OnClickListener, View.OnLongClickListener, EventListener<QuerySnapshot> {
    private LayoutDefaultBinding binding;
    private MapViewModel viewModel;
    private Bundle bundle, b;
    private ListenerRegistration listenerRegistration;
    private Disposable disposable;
    private Observer<List<User>> observer;
    private FragmentResultListener parentResultListener;
    private ServiceAdapter serviceAdapter;
    private LinearLayoutManager linearLayoutManager;
    private ServicesAdapter servicesAdapter;

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
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        parentResultListener = this::observarParent;
        observer = this::observarUser;
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        serviceAdapter = new ServiceAdapter(new DiffUtil.ItemCallback<State>() {
            @Override
            public boolean areItemsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }
        }, this);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        binding.recyclerView.setAdapter(serviceAdapter);
        binding.barra.setTitle("Serviços");
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

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setAdapter(null);
        binding.recyclerView.setLayoutManager(null);
        observer = null;
        serviceAdapter = null;
        servicesAdapter = null;
        parentResultListener = null;
        linearLayoutManager = null;
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

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (binding.recyclerView.getAdapter() != serviceAdapter) {
                    binding.recyclerView.setAdapter(serviceAdapter);
                }

                if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
                    viewModel.getServiços().get(bundle.getString("id")).clear();
                }

                State currentState = serviceAdapter.getCurrentList().get(0);

                if (currentState != State.Empty) {
                    serviceAdapter.submitList(Collections.singletonList(State.Empty));
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
                if (map.equals(viewModel.getServiços().get(bundle.getString("id")))) {
                    return;
                }
            }

            viewModel.getServiços().put(bundle.getString("id"), map);

            ArrayList<Serviço> servicos = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry: map.entrySet()) {
                servicos.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
            }

            if (binding.recyclerView.getAdapter() != servicesAdapter) {
                binding.recyclerView.setAdapter(servicesAdapter);
            }

            servicesAdapter.submitList(servicos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case View.NO_ID: if (b == null) {
                                 b = new Bundle();
                             }

                             b.putString("fragmentToLoad", "AboutFragment");
                             requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                break;
            case R.id.retryBtn: obterServicos(bundle);
                break;
            case R.id.txt: try {

                               if (b == null) {
                                   b = new Bundle();
                               }

                               b.putString("fragmentToLoad", "TypeServicesFragment");
                               b.putString("selectedService", servicesAdapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getId());
                               requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);

                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        try {
            disposable = viewModel.getService().getBarbeariaService().removerServiço(bundle.getString("id"), servicesAdapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getId()).subscribe(aBoolean -> Toast.makeText(requireActivity(), aBoolean ? "" : "", Toast.LENGTH_LONG).show(), throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarUser(List<User> users) {
        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            binding.barra.setOnMenuItemClickListener(item -> {
                if (b == null) {
                    b = new Bundle();
                }

                b.putString("fragmentToLoad", "InsertServiceFragment");
                requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                return true;
            });

            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        State state;

        if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
            state = viewModel.getServiços().get(bundle.getString("id")).size() == 0 ? State.Empty : State.Loaded;
        } else {
            state = State.Loading;
        }

        if (state != State.Loaded) {
            serviceAdapter.submitList(Collections.singletonList(state), () -> {
                List<State> states = serviceAdapter.getCurrentList();

                for (int  i = 0 ; i < states.size(); i++) {
                    if (states.get(i) == State.Loading) {
                        obterServicos(result);
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
            listenerRegistration = null;
        }

        if (servicesAdapter == null) {
            servicesAdapter = new ServicesAdapter(new DiffUtil.ItemCallback<Serviço>() {
                @Override
                public boolean areItemsTheSame(@NonNull Serviço oldItem, @NonNull Serviço newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Serviço oldItem, @NonNull Serviço newItem) {
                    return false;
                }
            },this, this);
        }

        adicionarListener();

        if (binding.recyclerView.getAdapter() == servicesAdapter) {
            return;
        }

        binding.recyclerView.setAdapter(servicesAdapter);

        ArrayList<Serviço> servicos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry: viewModel.getServiços().get(bundle.getString("id")).entrySet()) {
            servicos.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
        }

        servicesAdapter.submitList(servicos);
    }

    private void adicionarListener () {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").addSnapshotListener(this);
    }

    private void obterServicos(Bundle bundle) {
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }

        disposable = viewModel.getService().getBarbeariaService().obterServiços(bundle.getString("id")).subscribe(stringMapMap -> {
            viewModel.getServiços().put(bundle.getString("id"), stringMapMap);

            if (stringMapMap.size() != 0) {
                onLoad();
                return;
            }

            serviceAdapter.submitList(Collections.singletonList(State.Empty), this::adicionarListener);
        }, throwable -> serviceAdapter.submitList(Collections.singletonList(State.Error)));
    }
}