package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.maybe.MaybeCallbackObserver;
import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutBaseBinding;
import kev.app.timeless.model.Serviço;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class MyServicesFragment extends Fragment implements View.OnClickListener, EventListener<QuerySnapshot> {
    private LayoutBaseBinding binding;
    private MapViewModel viewModel;
    private Bundle bundle;
    private ConstraintSet constraintSet;
    private MaybeCallbackObserver<Map<String, Map<String, Object>>> callbackObserver;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private ListenerRegistration listenerRegistration;
    private Disposable disposable;
    private MutableLiveData<List<Serviço>> serviços;
    private FragmentResultListener childResultListener, parentResultListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_base, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        constraintSet = new ConstraintSet();
        childResultListener = this::observarChild;
        parentResultListener = this::observarParent;
        serviços = new MutableLiveData<>();
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof ServiceListFragment) {
                    serviços.observeForever(((ServiceListFragment) f).getObserver());
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof ServiceListFragment) {
                    serviços.removeObserver(((ServiceListFragment) f).getObserver());
                }
            }
        };
        binding.txt.setText("Serviços");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (constraintSet.getKnownIds().length != 0) {
            constraintSet.applyTo(binding.layoutPrincipal);
        }

        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, childResultListener);
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        constraintSet.clone(binding.layoutPrincipal);

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        serviços = null;
        disposable = null;
        listenerRegistration = null;
        fragmentLifecycleCallbacks = null;
        childResultListener = null;
        parentResultListener = null;
        bundle = null;
        callbackObserver = null;
        constraintSet = null;
        viewModel = null;
        binding = null;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = result;

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (TextUtils.isEmpty(bundle.getString("id"))) {
            return;
        }

        binding.inserir.setOnClickListener(this);

        if (getChildFragmentManager().getFragments().size() == 0) {
            if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
                if (viewModel.getServiços().get(bundle.getString("id")).size() == 0) {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem serviços disponíveis.");
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
                } else {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new ServiceListFragment(), "currentFragment").runOnCommit(() -> {
                        ArrayList<Serviço> serviçosDaViewModel = new ArrayList<>();
                        for (Map.Entry<String, Map<String, Object>> entry: viewModel.getServiços().get(bundle.getString("id")).entrySet()) {
                            serviçosDaViewModel.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                        }

                        serviços.setValue(serviçosDaViewModel);
                    }).commit();
                }

                listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").addSnapshotListener(this);
            } else {
                if (callbackObserver == null) {
                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                }

                viewModel.getService().getBarbeariaService().obterServiços(bundle.getString("id")).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);
            }

            return;
        }

        switch (getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName()) {
            case "LoadingFragment":
                break;
            case "ServiceListFragment": if (serviços.getValue() == null || serviços.getValue().size() == 0) {
                                            ArrayList<Serviço> serviçosDaViewModel = new ArrayList<>();
                                            for (Map.Entry<String, Map<String, Object>> entry: viewModel.getServiços().get(bundle.getString("id")).entrySet()) {
                                                serviçosDaViewModel.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                                            }

                                            serviços.setValue(serviçosDaViewModel);
                                        }
                break;
            case "TextFragment":  Bundle bundle = new Bundle();
                                  bundle.putString("txt", "Sem serviços disponíveis.");
                                  getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                break;
        }

        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").addSnapshotListener(this);
    }

    private void observarChild(String requestKey, Bundle result) {
        if (result.containsKey("idServiçoEscolhido")) {
            Bundle b = new Bundle();
            b.putString("idServiço", result.getString("idServiçoEscolhido"));
            requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
        }

        if (result.containsKey("idToRemove")) {
            disposable = viewModel.getService().getBarbeariaService().removerServiço(bundle.getString("id"), result.getString("idToRemove"))
                    .subscribe(aBoolean -> {
                        if (!aBoolean) {
                           Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                        }
                    }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());
        }
    }

    private void observarErro(Throwable throwable) {
        bundle.putInt("value", 1);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    private void observarMap(Map<String, Map<String, Object>> map) {
        if (!viewModel.getServiços().containsKey(bundle.getString("id"))) {
            viewModel.getServiços().put(bundle.getString("id"), new HashMap<>());
        }

        viewModel.getServiços().get(bundle.getString("id")).putAll(map);

        if (map.size() == 0) {
            getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                Bundle bundle = new Bundle();
                bundle.putString("txt", "Sem serviços disponíveis.");
                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
            }).commit();
        } else {
            getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new ServiceListFragment(), "currentFragment").runOnCommit(() -> {
                ArrayList<Serviço> serviçosDaDb = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> entry: viewModel.getServiços().get(bundle.getString("id")).entrySet()) {
                    serviçosDaDb.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                }

                serviços.setValue(serviçosDaDb);
            }).commit();
        }

        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("servicos").addSnapshotListener(this);
    }

    private void observarDisposable(Disposable disposable) {
        FragmentUtil.observarFragment("LoadingFragment", getChildFragmentManager(), R.id.layoutFragment);
        bundle.putInt("value", 0);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.inserir: new InsertServiceFragment().show(getChildFragmentManager(), "currentFragment");
                break;
            case R.id.retryBtn: if (callbackObserver == null) {
                                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                                }

                                viewModel.getService().getBarbeariaService().obterServiços(bundle.getString("id")).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);
                break;
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (serviços.getValue() != null) {
                    int position = -1;
                    if (bundle.containsKey(getClass().getSimpleName())) {
                        if (bundle.getBundle(getClass().getSimpleName()).containsKey("ServiceListFragment")) {
                            if (bundle.getBundle(getClass().getSimpleName()).getBundle("ServiceListFragment").containsKey("position")) {
                                position = bundle.getBundle(getClass().getSimpleName()).getBundle("ServiceListFragment").getInt("position");
                            }
                        }
                    }

                    if (position != -1) {
                        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), Bundle.EMPTY);
                    }
                }

                if (viewModel.getServiços().containsKey(bundle.getString("id"))) {
                    viewModel.getServiços().get(bundle.getString("id")).clear();
                }

                if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "TextFragment")) {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment()).runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem serviços disponíveis.");
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("txt", "Sem serviços disponíveis.");
                    getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (!viewModel.getServiços().containsKey(bundle.getString("id"))) {
                viewModel.getServiços().put(bundle.getString("id"), new HashMap<>());
            }

            if (viewModel.getServiços().get(bundle.getString("id")).equals(map)) {
                return;
            } else {
                viewModel.getServiços().get(bundle.getString("id")).putAll(map);
            }

            ArrayList<Serviço> serviçosDaDb = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry: map.entrySet()) {
                serviçosDaDb.add(new Serviço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
            }

            if (serviços.getValue() != null) {
                int position = -1;
                if (bundle.containsKey(getClass().getSimpleName())) {
                    if (bundle.getBundle(getClass().getSimpleName()).containsKey("ServiceListFragment")) {
                        if (bundle.getBundle(getClass().getSimpleName()).getBundle("ServiceListFragment").containsKey("position")) {
                            position = bundle.getBundle(getClass().getSimpleName()).getBundle("ServiceListFragment").getInt("position");
                        }
                    }
                }

                if (position != -1) {
                    Serviço serviço = serviços.getValue().get(position);

                    if (!map.containsKey(serviço.getId())) {
                        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), Bundle.EMPTY);
                    }
                }
            }

            serviços.setValue(serviçosDaDb);

            if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "ServiceListFragment")) {
                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new ServiceListFragment()).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}