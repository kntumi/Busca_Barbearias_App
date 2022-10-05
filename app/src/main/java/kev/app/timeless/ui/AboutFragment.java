package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.PrecomputedTextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentAboutBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.Result;
import kev.app.timeless.model.User;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class AboutFragment extends DaggerFragment implements View.OnClickListener, AsyncLayoutInflater.OnInflateFinishedListener {
    private FragmentAboutBinding binding;
    private Observer<List<User>> observer;
    private MapViewModel viewModel;
    private Bundle bundle;
    private Map<State, ViewGroup.LayoutParams> layoutParamsMap;
    private Map<State, View> map;
    private AsyncLayoutInflater asyncLayoutInflater;
    private ListenerRegistration listenerRegistration;
    private Future<Result<Map<String, Object>>> future;
    private String loggedInUserId;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            bundle = getArguments();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_about, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = users -> loggedInUserId = users.size() == 0 ? null : users.get(users.size() - 1).getId();
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        asyncLayoutInflater = new AsyncLayoutInflater(requireContext());
        layoutParamsMap = new HashMap<>();
        map = new HashMap<>();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(true);
        }

        if (map.containsKey(State.Error)) {
            ConstraintLayout layout = (ConstraintLayout) map.get(State.Error);

            MaterialButton btn = layout.findViewById(R.id.retryBtn);

            if (btn.hasOnClickListeners()) {
                btn.setOnClickListener(null);
            }
        }

        if (map.containsKey(State.Loaded)) {
            ConstraintLayout layout = (ConstraintLayout) map.get(State.Loaded);

            for (int i = 0 ; i < layout.getChildCount() ; i++) {
                View child = layout.getChildAt(i);

                if (!child.hasOnClickListeners()) {
                    continue;
                }

                child.setOnClickListener(null);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map.clear();
        layoutParamsMap.clear();
        loggedInUserId = null;
        asyncLayoutInflater = null;
        bundle = null;
        observer = null;
        viewModel = null;
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        observarBundle(savedInstanceState == null ? bundle : savedInstanceState.getBundle("bundle"));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    private void observarBundle(Bundle result) {
        bundle = bundle == null || bundle.size() == 0 ? result : bundle;

        if (viewModel.getEstabelecimentos().containsKey(bundle.getString("id"))) {
            onLoaded();
        } else {
            onLoading();
        }

        adicionarListener();
    }

    private void inicializarPerfil(View view) {
        ConstraintLayout layout = (ConstraintLayout) view;

        Map<String, Object> map = viewModel.getEstabelecimentos().get(bundle.getString("id"));

        for (int i = 0 ; i < layout.getChildCount() ; i++) {
            View child = layout.getChildAt(i);

            if (!child.hasOnClickListeners()) {
                child.setOnClickListener(this);
            }

            if (child.getId() == R.id.edit || child.getId() == R.id.info) {
                continue;
            }

            MaterialTextView materialTextView = (MaterialTextView) child;

            String s = null;

            switch (child.getId()) {
                case R.id.txtNome: s = String.valueOf(map.get("nome"));
                    break;
                case R.id.txtContacto: s = "Contactos";
                    break;
                case R.id.txtServicos: s = "Serviços";
                    break;
                case R.id.txtHorario: s = "Horário";
                    break;
                case R.id.nome: s = "Nome";
                    break;
            }

            if (!TextUtils.equals(s, materialTextView.getText())) {
                try {
                    materialTextView.setTextFuture(PrecomputedTextCompat.getTextFuture(TextUtils.isEmpty(s) ? "Sem nome" : s, materialTextView.getTextMetricsParamsCompat(), viewModel.getService().getExecutor()));
                } catch (Exception e) {
                    materialTextView.setText(TextUtils.isEmpty(s) ? "Sem nome" : s);
                }
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

    private void adicionarListener() {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).addSnapshotListener(this::observarDocument);
    }

    private void observarDocument(DocumentSnapshot documentSnapshot, FirebaseFirestoreException exception) {
        try {
            if (!documentSnapshot.exists()) {
                viewModel.getEstabelecimentos().remove(documentSnapshot.getId());
                return;
            }

            viewModel.getEstabelecimentos().put(documentSnapshot.getId(), documentSnapshot.getData());

            if (documentSnapshot.contains("nome")) {

            } else {

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void obterEstabelecimento(Bundle bundle) {
        Result<Map<String, Object>> result;

        future = viewModel.getService().getExecutor().submit(() -> viewModel.getService().getBarbeariaService().obterEstabelecimento(bundle.getString("id")));

        try {
            result = future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            result = new Result.Error<>(e);
        }

        if (result instanceof Result.Error) {
            return;
        }

        Map<String, Object> map = ((Result.Success<Map<String, Object>>) result).data;

        viewModel.getEstabelecimentos().put(bundle.getString("id"), map);

        if (map.size() == 0) {
            onEmpty();
        } else {
            onLoaded();
        }
    }

    private void removerViewsDoLayout() {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v == binding.view) {
                continue;
            }

            binding.layoutPrincipal.removeViewAt(i);
        }
    }

    private void onLoading() {
        if (!layoutParamsMap.containsKey(State.Loading)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 48;
            layoutParams.rightMargin = 24;
            layoutParams.leftMargin = 24;
            layoutParamsMap.put(State.Loading, layoutParams);
        }

        if (!map.containsKey(State.Loading)) {
            asyncLayoutInflater.inflate(R.layout.loading, null, this);
            return;
        }

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Loading), layoutParamsMap.get(State.Loading));
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

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Empty), layoutParamsMap.get(State.Empty));
    }

    private void onLoaded() {
        if (!layoutParamsMap.containsKey(State.Loaded)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.topMargin = 18;
            layoutParamsMap.put(State.Loaded, layoutParams);
        }

        if (!map.containsKey(State.Loaded)) {
            asyncLayoutInflater.inflate(R.layout.about, null, this);
            return;
        }

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Loaded), layoutParamsMap.get(State.Loaded));
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

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Error), layoutParamsMap.get(State.Error));
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    @Override
    public void onClick(View view) {
        String key;

        switch (view.getId()) {
            case R.id.edit: key = "InsertNameFragment";
                break;
            case R.id.txtContacto: key = "ContactsFragment";
                break;
            case R.id.txtHorario: key = "ScheduleFragment";
                break;
            case R.id.txtServicos: key = "ServicesFragment";
                break;
            case R.id.retryBtn: onLoading();
                return;
            default:
                return;
        }

        requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, obterFragment(key)).commit();
    }

    @Override
    public void onInflateFinished(@NonNull View view, int resid, @Nullable ViewGroup parent) {
        State state = obterStatePorID(resid);

        if (binding.layoutPrincipal.getChildCount() > 1) {
            removerViewsDoLayout();
        }

        switch (state) {
            case Error: inicializarRetryBtn(view);
                break;
            case Empty:
                break;
            case Loaded: inicializarPerfil(view);
                break;
            case Loading: obterEstabelecimento(bundle);
                break;
        }

        map.put(state, view);

        binding.layoutPrincipal.addView(view, layoutParamsMap.get(state));
    }

    private Fragment obterFragment (String key) {
        Fragment fragment = null;

        switch (key) {
            case "InsertNameFragment": fragment = new InsertNameFragment();
                break;
            case "ContactsFragment": fragment = new ContactsFragment();
                break;
            case "ScheduleFragment": fragment = new ScheduleFragment();
                break;
            case "ServicesFragment": fragment = new ServicesFragment();
                break;
        }

        return fragment;
    }

    private State obterStatePorID(int resid) {
        State state;

        switch (resid) {
            case R.layout.loading: state = State.Loading;
                break;
            case R.layout.error: state = State.Error;
                break;
            case R.layout.text: state = State.Empty;
                break;
            case R.layout.about: state = State.Loaded;
                break;
            default: throw new IllegalStateException("Unexpected value: " + resid);
        }

        return state;
    }
}