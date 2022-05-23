package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import kev.app.timeless.databinding.SubServiceBinding;
import kev.app.timeless.model.SubServiço;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class SubServiceFragment extends Fragment implements View.OnClickListener, EventListener<QuerySnapshot> {
    private SubServiceBinding binding;
    private MapViewModel viewModel;
    private String id, idTipoServiço, idServiço;
    private ListenerRegistration listenerRegistration;
    private FragmentResultListener fragmentResultListener;
    private ConstraintSet constraintSet;
    private MaybeCallbackObserver<Map<String, Map<String, Object>>> callbackObserver;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private Bundle bundle;
    private MutableLiveData<List<SubServiço>> subServiços;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.sub_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bundle = new Bundle();
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        fragmentResultListener = this::observarResult;
        subServiços = new MutableLiveData<>();
        constraintSet = new ConstraintSet();
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof SubServiceListFragment) {
                    subServiços.observeForever(((SubServiceListFragment) f).getObserver());
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof SubServiceListFragment) {
                    subServiços.removeObserver(((SubServiceListFragment) f).getObserver());
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        binding.barra.setNavigationOnClickListener(this);

        if (constraintSet.getKnownIds().length != 0) {
            constraintSet.applyTo(binding.layoutPrincipal);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);
        constraintSet.clone(binding.layoutPrincipal);

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle.clear();
        fragmentLifecycleCallbacks = null;
        bundle = null;
        constraintSet = null;
        viewModel = null;
        fragmentResultListener = null;
        listenerRegistration = null;
        callbackObserver = null;
        subServiços = null;
        id = null;
        idServiço = null;
        idTipoServiço = null;
        binding = null;
    }

    private void observarResult(String requestKey, Bundle result) {
        id = result.containsKey("id") ? result.getString("id") : null;
        idServiço = result.containsKey("idServiço") ? result.getString("idServiço") : null;
        idTipoServiço = result.containsKey("idTipoServiço") ? result.getString("idTipoServiço") : null;

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(idServiço)) {
            return;
        }

        if (!viewModel.getTiposServiços().containsKey(idServiço)) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layoutFragment, new TextFragment())
                    .runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" para escolher");
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();

            return;
        }

        if (TextUtils.isEmpty(idTipoServiço)) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layoutFragment, new TextFragment(), "currentFragment")
                    .runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Não foi escolhido nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase());
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    })
                    .commit();

            return;
        }

        binding.barra.setTitle(viewModel.getServiços().containsKey(id) ? "Tipos de ".concat(String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).concat(" de ").concat(String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")))) : null);

        if (getChildFragmentManager().getFragments().size() == 0) {
            if (viewModel.getSubServiços().containsKey(idTipoServiço)) {
                if (viewModel.getSubServiços().get(idTipoServiço).size() == 0) {
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new TextFragment(), "currentFragment")
                            .runOnCommit(() -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("txt", "Sem tipos de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" de "+String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")).toLowerCase());
                                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                            }).commit();
                } else {
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new SubServiceListFragment(), "currentFragment")
                            .runOnCommit(() -> {
                                ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

                                for (Map.Entry<String, Map<String, Object>> entry: viewModel.getSubServiços().get(idTipoServiço).entrySet()) {
                                    subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
                                }

                                subServiços.setValue(subServiçosDaDb);
                                listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this);
                            }).commit();
                }
            } else {
                if (callbackObserver == null) {
                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                }

                viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);
            }

            return;
        }


        String actual = getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName();

        if (TextUtils.equals(actual, "TextFragment")) {
            if (viewModel.getSubServiços().containsKey(idTipoServiço)) {
                if (viewModel.getSubServiços().get(idTipoServiço).size() == 0) {
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new TextFragment(), "currentFragment")
                            .runOnCommit(() -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("txt", "Sem tipos de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" de "+String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")).toLowerCase());
                                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                            }).commit();
                } else {
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new SubServiceListFragment(), "currentFragment")
                            .runOnCommit(() -> {
                                ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

                                for (Map.Entry<String, Map<String, Object>> entry: viewModel.getSubServiços().get(idTipoServiço).entrySet()) {
                                    subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
                                }

                                subServiços.setValue(subServiçosDaDb);
                            }).commit();
                }
            } else {
                if (callbackObserver == null) {
                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                }

                viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);

                return;
            }
        } else {
            switch (actual) {
                case "LoadingFragment":
                    break;
                case "SubServiceListFragment":
                    if (subServiços.getValue() == null || subServiços.getValue().size() == 0) {
                        ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

                        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getSubServiços().get(idTipoServiço).entrySet()) {
                            subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
                        }

                        subServiços.setValue(subServiçosDaDb);
                    }
                    break;
            }
        }

        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this);
    }

    private void observarErro(Throwable throwable) {
        bundle.putInt("value", 1);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    private void observarMap(Map<String, Map<String, Object>> stringMapMap) {
        viewModel.getSubServiços().put(idTipoServiço, stringMapMap);

        if (stringMapMap.size() == 0) {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layoutFragment, new TextFragment(), "currentFragment")
                    .runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem tipos de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" de "+String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")).toLowerCase());
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
        } else {
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.layoutFragment, new SubServiceListFragment(), "currentFragment")
                    .runOnCommit(() -> {
                        ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

                        for (Map.Entry<String, Map<String, Object>> entry: stringMapMap.entrySet()) {
                            subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
                        }

                        subServiços.setValue(subServiçosDaDb);
                    }).commit();
        }

        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this);
    }

    private void observarDisposable(Disposable disposable) {
        FragmentUtil.observarFragment("LoadingFragment", getChildFragmentManager(), R.id.layoutFragment);
        bundle.putInt("value", 0);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn: if (callbackObserver == null) {
                                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                                }

                                viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new TypeServicesFragment()).commit();
                break;
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getSubServiços().containsKey(idTipoServiço)) {
                    viewModel.getSubServiços().get(idTipoServiço).clear();
                }

                if (subServiços.getValue().size() == 0) {
                    subServiços.getValue().clear();
                }

                if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "TextFragment")) {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment()).runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem tipos de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" de "+String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")).toLowerCase());
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (!viewModel.getSubServiços().containsKey(idTipoServiço)) {
                viewModel.getSubServiços().put(idTipoServiço, new HashMap<>());
            }

            if (viewModel.getSubServiços().get(idTipoServiço).equals(map)) {
                return;
            } else {
                viewModel.getSubServiços().get(idTipoServiço).putAll(map);
            }

            ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry: map.entrySet()) {
                subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
            }

            subServiços.setValue(subServiçosDaDb);

            if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "SubServiceListFragment")) {
                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new SubServiceListFragment()).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}