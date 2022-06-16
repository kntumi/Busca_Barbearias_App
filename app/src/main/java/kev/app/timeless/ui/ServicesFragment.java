package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
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
    private String loggedInUserId;
    private Map<State, View> map;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
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
        onGlobalLayoutListener = this::observarLayout;
        map = new HashMap<>();
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        binding.barra.setTitle("Serviços");
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.barra.setNavigationOnClickListener(this);
        binding.layoutPrincipal.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);
        binding.layoutPrincipal.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        observer = null;
        onGlobalLayoutListener = null;
        map = null;
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
                if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
                    viewModel.getServiços().get(bundle.getString("id")).clear();
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

    private void observarLayout() {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount(); i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v.getId() == binding.barra.getId()) {
                continue;
            }

            switch (v.getId()) {
                case R.id.layoutBarraProgresso: obterServicos(bundle);
                    break;
                case R.id.layoutText: inicializarText(v);
                    break;
                case R.id.recyclerView: inicializarRecyclerView(v);
                    break;
                case R.id.layoutError: inicializarRetryBtn(v);
                    break;
            }
        }
    }

    private void inicializarRetryBtn(View v) {
        MaterialButton btn = v.findViewById(R.id.retryBtn);

        if (btn.hasOnClickListeners()) {
            return;
        }

        btn.setOnClickListener(this);
    }

    private void inicializarRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v;

        if (listenerRegistration != null) {
            listenerRegistration.remove();
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

        if (servicesAdapter.equals(recyclerView.getAdapter())) {
            return;
        }

        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        }

        if (!linearLayoutManager.equals(recyclerView.getLayoutManager())) {
            recyclerView.setLayoutManager(linearLayoutManager);
        }

        recyclerView.setAdapter(servicesAdapter);

        ArrayList<Serviço> servicos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry: viewModel.getServiços().get(bundle.getString("id")).entrySet()) {
            servicos.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
        }

        servicesAdapter.submitList(servicos);
    }

    private void inicializarText(View v) {
        AppCompatTextView appCompatTextView = v.findViewById(R.id.txt);

        String txtAMostrar = TextUtils.equals(bundle.getString("id"), loggedInUserId) ? "Adicione um serviço para os outros usuarios o verem " : "Sem serviços disponiveis";

        if (TextUtils.equals(appCompatTextView.getText(), txtAMostrar)) {
            return;
        }

        appCompatTextView.setText(txtAMostrar);
    }

    private void observarUser(List<User> users) {
        loggedInUserId = users.size() != 0 ? users.get(users.size() - 1).getId() : null;

        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            binding.barra.setOnMenuItemClickListener(item -> {
                requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new InsertServiceFragment()).commit();
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

        switch (state) {
            case Empty: onEmpty();
                break;
            case Loaded: onLoaded();
                break;
            case Loading: onLoading();
                break;
        }
    }

    private void removerViews () {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v.getId() == binding.barra.getId()) {
                continue;
            }

            binding.layoutPrincipal.removeView(v);
        }
    }

    private void onLoaded() {
        if (!map.containsKey(State.Loaded)) {
            map.put(State.Loaded, View.inflate(requireContext(), R.layout.list, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 24;
        layoutParams.bottomMargin = 18;

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Loaded), layoutParams);
    }

    private void onEmpty() {
        if (!map.containsKey(State.Empty)) {
            map.put(State.Empty, View.inflate(requireContext(), R.layout.text, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.leftMargin = 24;

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Empty), layoutParams);
    }

    private void onLoading() {
        if (!map.containsKey(State.Loading)) {
            map.put(State.Loading, View.inflate(requireContext(), R.layout.loading, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.leftMargin = 24;

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Loading), layoutParams);
    }

    private void adicionarListener () {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").addSnapshotListener(this);
    }

    private void obterServicos(Bundle bundle) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = viewModel.getService().getBarbeariaService().obterServiços(bundle.getString("id")).subscribe(stringMapMap -> {
            viewModel.getServiços().put(bundle.getString("id"), stringMapMap);

            if (stringMapMap.size() != 0) {
                onLoaded();
            } else {
                onEmpty();
            }
        }, throwable -> onError());
    }

    private void onError() {
        if (!map.containsKey(State.Error)) {
            map.put(State.Error, View.inflate(requireContext(), R.layout.error, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.leftMargin = 24;

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Error), layoutParams);
    }
}