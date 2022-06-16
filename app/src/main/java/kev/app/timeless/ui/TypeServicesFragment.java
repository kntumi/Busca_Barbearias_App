package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
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
import kev.app.timeless.model.TipoServiço;
import kev.app.timeless.model.User;
import kev.app.timeless.util.State;
import kev.app.timeless.util.TypeServicesAdapter;
import kev.app.timeless.viewmodel.MapViewModel;

public class TypeServicesFragment extends DaggerFragment implements EventListener<QuerySnapshot>, View.OnClickListener, AsyncLayoutInflater.OnInflateFinishedListener {
    @Inject
    ViewModelProvidersFactory providerFactory;

    private LayoutDefaultBinding binding;
    private AsyncLayoutInflater asyncLayoutInflater;
    private MapViewModel viewModel;
    private String loggedInUserId;
    private ListenerRegistration listenerRegistration;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private FragmentResultListener parentResultListener;
    private Bundle bundle;
    private Disposable disposable;
    private Observer<List<User>> observer;
    private Map<State, View> map;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private Map<State, LinearLayout.LayoutParams> layoutParamsMap;
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
        asyncLayoutInflater = new AsyncLayoutInflater(requireContext());
        map = new HashMap<>();
        layoutParamsMap = new HashMap<>();
        onGlobalLayoutListener = this::observarLayout;
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
        map.clear();
        layoutParamsMap.clear();
        bundle.clear();
        onGlobalLayoutListener = null;
        asyncLayoutInflater = null;
        layoutParamsMap = null;
        map = null;
        listenerRegistration = null;
        disposable = null;
        observer = null;
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

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getTiposServiços().containsKey(bundle.getString("selectedService"))) {
                    viewModel.getTiposServiços().get(bundle.getString("selectedService")).clear();
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

    @Override
    public void onInflateFinished(@NonNull View view, int resid, @Nullable ViewGroup parent) {
        State state = obterStatePorID(resid);

        if (binding.layoutPrincipal.getChildCount() > 1) {
            removerViews();
        }

        map.put(state, view);

        binding.layoutPrincipal.addView(view, layoutParamsMap.get(state));
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        if (TextUtils.isEmpty(binding.barra.getTitle())) {
            binding.barra.setTitle(viewModel.getServiços().containsKey(bundle.getString("id")) ? "Tipos de ".concat(String.valueOf(viewModel.getServiços().get(bundle.getString("id")).get(bundle.getString("selectedService")).get("nome"))) : null);
        }

        State state;

        if (viewModel.getTiposServiços().containsKey(bundle.getString("selectedService"))) {
            state = viewModel.getTiposServiços().get(bundle.getString("selectedService")).size() == 0 ? State.Empty : State.Loaded;
        } else {
            state = State.Loading;
        }

        switch (state) {
            case Loading: onLoading();
                break;
            case Loaded: onLoaded();
                break;
            case Empty: onEmpty();
                break;
        }
    }

    private void onEmpty() {
        if (!layoutParamsMap.containsKey(State.Empty)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 24;
            layoutParams.rightMargin = 24;
            layoutParams.leftMargin = 24;
            layoutParamsMap.put(State.Empty, layoutParams);
        }

        if (!map.containsKey(State.Empty)) {
            asyncLayoutInflater.inflate(R.layout.text, null, this);
            return;
        }

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Empty), layoutParamsMap.get(State.Empty));
    }

    private void onLoaded() {
        if (!layoutParamsMap.containsKey(State.Loaded)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 24;
            layoutParams.bottomMargin = 18;
            layoutParamsMap.put(State.Loaded, layoutParams);
        }

        if (!map.containsKey(State.Loaded)) {
            asyncLayoutInflater.inflate(R.layout.list, null, this);
            return;
        }

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Loaded), layoutParamsMap.get(State.Loaded));
    }

    private void onLoading() {
        if (!layoutParamsMap.containsKey(State.Loading)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 24;
            layoutParams.rightMargin = 24;
            layoutParams.leftMargin = 24;
            layoutParamsMap.put(State.Loading, layoutParams);
        }

        if (!map.containsKey(State.Loading)) {
            asyncLayoutInflater.inflate(R.layout.loading, null, this);
            return;
        }

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Loading), layoutParamsMap.get(State.Loading));
    }

    private void onError() {
        if (!layoutParamsMap.containsKey(State.Error)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 24;
            layoutParams.rightMargin = 24;
            layoutParams.leftMargin = 24;
            layoutParamsMap.put(State.Error, layoutParams);
        }

        if (!map.containsKey(State.Error)) {
            asyncLayoutInflater.inflate(R.layout.error, null, this);
            return;
        }

        removerViews();

        binding.layoutPrincipal.addView(map.get(State.Error), layoutParamsMap.get(State.Error));
    }

    private void obterTiposServicos(Bundle result) {
        disposable = viewModel.getService().getBarbeariaService().obterTiposServiços(result.getString("id"), result.getString("selectedService")).subscribe(stringMapMap -> {
            viewModel.getTiposServiços().put(result.getString("selectedService"), stringMapMap);

            if (stringMapMap.size() != 0) {
                onLoaded();
            } else {
                onEmpty();
            }
        }, throwable -> onError());
    }

    private void adicionarListener() {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").document(bundle.getString("selectedService")).collection("tipos").addSnapshotListener(this);
    }

    private void observarLayout() {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount(); i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v.getId() == binding.barra.getId()) {
                continue;
            }

            switch (v.getId()) {
                case R.id.layoutBarraProgresso: obterTiposServicos(bundle);
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

    private void inicializarRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v;

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

        if (typeServicesAdapter == recyclerView.getAdapter()) {
            return;
        }

        recyclerView.setAdapter(typeServicesAdapter);

        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        }

        if (linearLayoutManager != recyclerView.getLayoutManager()) {
            recyclerView.setLayoutManager(linearLayoutManager);
        }

        ArrayList<TipoServiço> tiposServicos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getTiposServiços().get(bundle.getString("selectedService")).entrySet()) {
            tiposServicos.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
        }

        typeServicesAdapter.submitList(tiposServicos);
    }

    private void inicializarRetryBtn(View v) {
        MaterialButton btn = v.findViewById(R.id.retryBtn);

        if (btn.hasOnClickListeners()) {
            return;
        }

        btn.setOnClickListener(this);
    }

    private void inicializarText(View v) {
        AppCompatTextView appCompatTextView = v.findViewById(R.id.txt);

        String txtAMostrar = TextUtils.equals(bundle.getString("id"), loggedInUserId) ? "Adicione um tipo de serviço para os outros usuarios o verem " : "Sem tipo de disponiveis";

        if (TextUtils.equals(appCompatTextView.getText(), txtAMostrar)) {
            return;
        }

        appCompatTextView.setText(txtAMostrar);
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

    private void observarUser(List<User> users) {
        loggedInUserId = users.size() != 0 ? users.get(users.size() - 1).getId() : null;

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

    private State obterStatePorID(int resid) {
        State state;

        switch (resid) {
            case R.layout.loading: state = State.Loading;
                break;
            case R.layout.list: state = State.Loaded;
                break;
            case R.layout.error: state = State.Error;
                break;
            case R.layout.text: state = State.Empty;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + resid);
        }

        return state;
    }
}