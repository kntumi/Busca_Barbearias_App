package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

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
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentAboutBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.User;
import kev.app.timeless.util.ScheduleAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class AboutFragment extends DaggerFragment implements View.OnClickListener, AsyncLayoutInflater.OnInflateFinishedListener {
    private FragmentAboutBinding binding;
    private FragmentResultListener parentResultListener;
    private Observer<List<User>> observer;
    private MapViewModel viewModel;
    private Disposable disposable;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private Bundle bundle;
    private Map<State, ViewGroup.LayoutParams> layoutParamsMap;
    private Map<State, View> map;
    private AsyncLayoutInflater asyncLayoutInflater;
    private ListenerRegistration listenerRegistration;
    private String loggedInUserId;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Inject
    Service service;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_about, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = users -> loggedInUserId = users.size() == 0 ? null : users.get(0).getId();
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        onGlobalLayoutListener = this::observarLayout;
        asyncLayoutInflater = new AsyncLayoutInflater(requireContext());
        layoutParamsMap = new HashMap<>();
        map = new HashMap<>();

        if (savedInstanceState == null) {
            parentResultListener = this::observarParent;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.layoutPrincipal.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);

        if (parentResultListener != null) {
            requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.layoutPrincipal.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);

        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        for (State state : map.keySet()) {
            switch (state) {
                case Loading:
                    break;
                case Loaded:
                    break;
                case Empty:
                    break;
                case Error:
                    break;
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
        onGlobalLayoutListener = null;
        bundle = null;
        observer = null;
        parentResultListener = null;
        viewModel = null;
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
        bundle = bundle == null || bundle.size() == 0 ? result : bundle;

        if (viewModel.getEstabelecimentos().containsKey(bundle.getString("id"))) {
            onLoaded();
        } else {
            onLoading();
        }
    }

    private void observarLayout() {
        for (int i = 0; i < binding.layoutPrincipal.getChildCount(); i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v == binding.view) {
                continue;
            }

            switch (v.getId()) {
                case R.id.layoutBarraProgresso: obterEstabelecimento(bundle);
                    break;
                case R.id.layoutError: inicializarRetryBtn(v);
                    break;
                case R.id.about: inicializarNome(v);
                    break;
            }
        }
    }

    private void inicializarNome(View v) {
        ConstraintLayout layout = (ConstraintLayout) v;

        for (int i = 0 ; i < layout.getChildCount() ; i++) {
            View child = layout.getChildAt(i);

            if (child.hasOnClickListeners()) {
                child.setOnClickListener(null);
            }

            child.setOnClickListener(this);
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
        listenerRegistration = service.getFirestore().collection("Barbearia").document(bundle.getString("id")).addSnapshotListener(this::observarDocument);
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
        disposable = service.getBarbeariaService().obterEstabelecimento(bundle.getString("id"))
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(stringObjectMap -> {
                    viewModel.getEstabelecimentos().put(bundle.getString("id"), stringObjectMap);

                    if (stringObjectMap.size() == 0) {
                        onEmpty();
                    } else {
                        onLoaded();
                    }
                }, throwable -> onError());
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
            asyncLayoutInflater.inflate(TextUtils.isEmpty(loggedInUserId) ? R.layout.not_about : R.layout.about, null, this);
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
        if (view.getId() == R.id.txtLoc || view.getId()  == R.id.retryBtn) {
            switch (view.getId()) {
                case R.id.txtLoc:
                    break;
                case R.id.retryBtn:
                    break;
            }

            return;
        }

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
            default: throw new IllegalStateException("Unexpected value: " + view.getId());
        }

        requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, obterFragment(key)).commit();
    }

    @Override
    public void onInflateFinished(@NonNull View view, int resid, @Nullable ViewGroup parent) {
        State state = obterStatePorID(resid);

        if (binding.layoutPrincipal.getChildCount() > 1) {
            removerViewsDoLayout();
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
            case R.layout.about:
            case R.layout.not_about: state = State.Loaded;
                break;
            default: throw new IllegalStateException("Unexpected value: " + resid);
        }

        return state;
    }
}