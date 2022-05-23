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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Collections;
import java.util.List;
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
import kev.app.timeless.util.AboutAdapter;
import kev.app.timeless.util.Info;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class AboutFragment extends DaggerFragment implements View.OnClickListener, State<AboutFragment.State>, Info {
    private FragmentAboutBinding binding;
    private FragmentResultListener parentResultListener;
    private Observer<List<User>> observer;
    private MapViewModel viewModel;
    private Disposable disposable;
    private Bundle bundle;
    private State state;
    private ListenerRegistration listenerRegistration;
    private AboutAdapter aboutAdapter;
    private String loggedInUserId;
    private Bundle b;

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
        parentResultListener = this::observarParent;
        observer = this::observarUser;
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        aboutAdapter = new AboutAdapter(new DiffUtil.ItemCallback<State>() {
            @Override
            public boolean areItemsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }
        }, this, this, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(aboutAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (b != null) {
            b.clear();
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setLayoutManager(null);
        binding.recyclerView.setAdapter(null);
        aboutAdapter = null;
        loggedInUserId = null;
        b = null;
        state = null;
        bundle = null;
        observer = null;
        parentResultListener = null;
        viewModel = null;
        binding = null;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle == null || bundle.size() == 0 ? result : bundle;

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (TextUtils.isEmpty(result.getString("id"))) {
            return;
        }

        if (state == null) {
            state = viewModel.getEstabelecimentos().containsKey(result.getString("id")) ? State.Loaded : State.Loading;
        }

        aboutAdapter.submitList(Collections.singletonList(state));

        switch (state) {
            case Loading: obterEstabelecimento(result);
                break;
            case Loaded: adicionarListener();
                break;
        }
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

            aboutAdapter.atualizarNome(documentSnapshot.contains("nome") ? documentSnapshot.getString("nome") : "Sem nome para mostrar");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void obterEstabelecimento(Bundle result) {
        disposable = service.getBarbeariaService().obterEstabelecimento(result.getString("id"))
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(stringObjectMap -> viewModel.getEstabelecimentos().put(result.getString("id"), stringObjectMap))
                .doAfterSuccess(stringObjectMap -> adicionarListener())
                .doFinally(() -> aboutAdapter.submitList(Collections.singletonList(state)))
                .subscribe(stringObjectMap -> state = State.Loaded, throwable -> state = State.Error);
    }

    private void observarUser(List<User> users) {
        if (users.size() == 0) {
            requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
        }

        loggedInUserId = users.size() == 0 ? null : users.get(0).getId();
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    @Override
    public void onClick(View view) {
        if (b == null) {
            b = new Bundle();
        }

        switch (view.getId()) {
            case R.id.edit: b.putString("fragmentToLoad", "InsertNameFragment");
                break;
            case R.id.txtContacto: b.putString("fragmentToLoad", "ContactsFragment");
                break;
            case R.id.txtHorario: b.putString("fragmentToLoad", "ScheduleFragment");
                break;
            case R.id.txtLoc: System.out.println("parentFragmentClass: ".concat(requireParentFragment().getClass().getSimpleName()));
                break;
            case R.id.txtServicos: b.putString("fragmentToLoad", "ServicesFragment");
                break;
            case R.id.retryBtn: System.out.println("yep gringo!!");
                break;
        }

        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult("LayoutFragment", b);
    }

    @Override
    public State value() {
        return state;
    }

    @Override
    public Boolean isUserLoggedIn() {
        return bundle == null ? null : TextUtils.equals(bundle.getString("id"), loggedInUserId);
    }

    public enum State {
        Loading,
        Error,
        Loaded
    }
}