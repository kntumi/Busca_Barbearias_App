package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutDefaultBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.Contacto;
import kev.app.timeless.model.User;
import kev.app.timeless.util.ContactsAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class ContactsFragment extends DaggerFragment implements View.OnClickListener, AsyncLayoutInflater.OnInflateFinishedListener {
    private Observer<List<User>> observer;
    private LayoutDefaultBinding binding;
    private Bundle bundle;
    private ContactsAdapter contactsAdapter;
    private Map<State, LinearLayout.LayoutParams> layoutParamsMap;
    private AsyncLayoutInflater asyncLayoutInflater;
    private FragmentResultListener parentResultListener;
    private ListenerRegistration listenerRegistration;
    private Map<State, View> map;
    private MapViewModel viewModel;
    private LinearLayoutManager linearLayoutManager;
    private List<Contacto> contactos, contactosPrincipais, contactosSecundarios;
    private String loggedInUserId;
    private Disposable disposable;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_default, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        map = new HashMap<>();
        layoutParamsMap = new HashMap<>();
        asyncLayoutInflater = new AsyncLayoutInflater(requireContext());
        observer = this::observarUser;
        binding.barra.setTitle("Contactos");

        if (savedInstanceState == null) {
            parentResultListener = this::observarParent;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (parentResultListener != null) {
            requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        }

        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.layoutPrincipal.removeAllViews();
        observer = null;
        viewModel = null;
        asyncLayoutInflater = null;
        bundle = null;
        parentResultListener = null;
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        bundle = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle("bundle");

        if (savedInstanceState != null) {
            observarParent(getClass().getSimpleName(), bundle);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarView(View v) {
        switch (v.getId()) {
            case R.id.layoutBarraProgresso: obterContactos(bundle);
                break;
            case R.id.layoutText: inicializarText(v);
                break;
            case R.id.recyclerView: inicializarRecyclerView(v);
                break;
            case R.id.layoutError: inicializarRetryBtn(v);
                break;
        }
    }

    private void inicializarRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v;

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (contactsAdapter == null) {
            contactsAdapter = new ContactsAdapter(new DiffUtil.ItemCallback<Contacto>() {
                @Override
                public boolean areItemsTheSame(@NonNull Contacto oldItem, @NonNull Contacto newItem) {
                    return newItem.equals(oldItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Contacto oldItem, @NonNull Contacto newItem) {
                    return (oldItem.getNrTelefone() == newItem.getNrTelefone()) & (oldItem.isContactoPrincipal() && newItem.isContactoPrincipal());
                }
            }, this, viewModel.getService().getExecutor());
        }

        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        }

        if (linearLayoutManager != recyclerView.getLayoutManager()) {
            recyclerView.setLayoutManager(linearLayoutManager);
        }

        adicionarListener();

        if (contactsAdapter != recyclerView.getAdapter()) {
            recyclerView.setAdapter(contactsAdapter);
        }

        if (contactos != null) {
            contactos.clear();
        }

        contactos = (contactos == null) ? new ArrayList<>() : null;

        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getContactos().get(bundle.getString("id")).entrySet()) {
            contactos.add(new Contacto(Integer.parseInt(entry.getKey()), Boolean.parseBoolean(entry.getValue().get("contactoPrincipal").toString())));
        }

        atualizarContactos(contactos);
    }

    private void atualizarContactos(List<Contacto> contactos) {
        contactosPrincipais = (contactosPrincipais == null) ? new ArrayList<>() : contactosPrincipais;
        contactosSecundarios = (contactosSecundarios == null) ? new ArrayList<>() : contactosSecundarios;

        if (!contactosPrincipais.isEmpty()) {
            contactosPrincipais.clear();
        }

        if (!contactosSecundarios.isEmpty()) {
            contactosSecundarios.clear();
        }

        for (Contacto contacto : contactos) {
            if (contacto.isContactoPrincipal()) {
                contactosPrincipais.add(contacto);
            } else {
                contactosSecundarios.add(contacto);
            }
        }

        Contacto[] contactos1 = new Contacto[(contactosPrincipais.size() + contactosSecundarios.size()) + (!contactosPrincipais.isEmpty() ? 1 : 0) + (!contactosSecundarios.isEmpty() ? 1 : 0)];

        int i = 0, k;

        if (!contactosPrincipais.isEmpty()) {
            contactos1[++i] = null;

            for (Contacto contacto : contactosPrincipais) {
                contactos1[i] = contacto;
                i++;
            }
        }

        k = contactosPrincipais.isEmpty() ? 0 : i;

        if (!contactosSecundarios.isEmpty()) {
            contactos1[++k] = null;

            for (Contacto contacto : contactosSecundarios) {
                contactos1[k] = contacto;
                k++;
            }
        }

        contactsAdapter.submitList(Arrays.asList(contactos1), () -> contactsAdapter.notifyDataSetChanged());
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

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        State state;

        if (viewModel.getContactos().containsKey(result.getString("id"))) {
            state = viewModel.getContactos().get(result.getString("id")).size() == 0 ? State.Empty : State.Loaded;
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
            layoutParams.bottomMargin = 24;
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

    private void removerViews () {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v.getId() == binding.barra.getId()) {
                continue;
            }

            binding.layoutPrincipal.removeView(v);
        }
    }

    private void obterContactos(Bundle result) {
        disposable = viewModel.getService().getBarbeariaService().obterContactos(result.getString("id"))
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(maps -> {
                    viewModel.getContactos().put(result.getString("id"), maps);

                    if (maps.size() == 0) {
                        onEmpty();
                    } else {
                        onLoaded();
                    }
                }, throwable -> onError());
    }

    private void observarUser(List<User> users) {
        loggedInUserId = users.size() != 0 ? users.get(users.size() - 1).getId() : null;

        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            binding.barra.setOnMenuItemClickListener(item -> requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new NewContactFragment()).commit() != -1);

            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new AboutFragment()).commit();
                break;
            case R.id.retryBtn: obterContactos(bundle);
                break;
            case R.id.more:
                break;
        }
    }

    public void adicionarListener () {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("contactos").addSnapshotListener(this::observarCollection);
    }

    private void observarCollection(QuerySnapshot value, FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getContactos().containsKey(bundle.getString("id"))) {
                    viewModel.getContactos().get(bundle.getString("id")).clear();
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot snapshot : value) {
                map.put(snapshot.getId(), snapshot.getData());
            }

            if (viewModel.getContactos().containsKey(bundle.getString("id"))) {
                if (map.equals(viewModel.getContactos().get(bundle.getString("id")))) {
                    return;
                }
            }

            viewModel.getContactos().put(bundle.getString("id"), map);

            if (contactos != null) {
                contactos.clear();
            }

            contactos = (contactos == null) ? new ArrayList<>() : null;

            for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                contactos.add(new Contacto(Integer.parseInt(entry.getKey()), Boolean.parseBoolean(entry.getValue().get("contactoPrincipal").toString())));
            }

            atualizarContactos(contactos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInflateFinished(@NonNull View view, int resid, @Nullable ViewGroup parent) {
        State state = obterStatePorID(resid);

        if (binding.layoutPrincipal.getChildCount() > 1) {
            removerViews();
        }

        map.put(state, view);

        observarView(map.get(state));

        binding.layoutPrincipal.addView(view, layoutParamsMap.get(state));
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