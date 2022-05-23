package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
import kev.app.timeless.R;
import kev.app.timeless.databinding.MySubServiceBinding;
import kev.app.timeless.model.SubServiço;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class MySubServiceFragment extends androidx.fragment.app.Fragment implements View.OnClickListener, EventListener<QuerySnapshot> {
    private MySubServiceBinding binding;
    private MapViewModel viewModel;
    private String id, idTipoServiço, idServiço;
    private List<ListenerRegistration> listenerRegistrations;
    private FragmentResultListener childResultListener, parentResultListener;
    private ConstraintSet constraintSet;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private Bundle bundle;
    private List<Disposable> disposables;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private MutableLiveData<List<SubServiço>> subServiços;
    private EventListener<DocumentSnapshot> eventListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.my_sub_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        parentResultListener = this::observarParent;
        subServiços = new MutableLiveData<>();
        constraintSet = new ConstraintSet();
        disposables = new ArrayList<>();
        listenerRegistrations = new ArrayList<>();
        childResultListener = this::observarChild;
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof SubServiceListFragment) {
                    subServiços.observeForever(((SubServiceListFragment) f).getObserver());
                }

                if (f instanceof InsertSubServiceFragment) {
                    getChildFragmentManager().setFragmentResult(f.getClass().getSimpleName(), bundle);
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof SubServiceListFragment) {
                    subServiços.removeObserver(((SubServiceListFragment) f).getObserver());
                }
            }

            @Override
            public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentDetached(fm, f);
                if (f instanceof ManageSubServiceFragment) {
                    if (bundle.containsKey("idTipoServiço")) {
                        ArrayList<SubServiço> subServiçosDaDb = new ArrayList<>();

                        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getSubServiços().get(bundle.getString("idTipoServiço")).entrySet()) {
                            subServiçosDaDb.add(new SubServiço(String.valueOf(entry.getValue().get("nome")), Double.parseDouble(String.valueOf(entry.getValue().get("preco"))), entry.getKey()));
                        }

                        subServiços.setValue(subServiçosDaDb);
                    }
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, childResultListener);
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.barra.setNavigationOnClickListener(this);

        if (constraintSet.getKnownIds().length != 0) {
            constraintSet.applyTo(binding.layoutPrincipal);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        constraintSet.clone(binding.layoutPrincipal);
        binding.barra.setOnMenuItemClickListener(null);
        binding.barra.setNavigationOnClickListener(null);

        for (ListenerRegistration listenerRegistration : listenerRegistrations) {
            listenerRegistration.remove();
        }

        listenerRegistrations.clear();

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        disposables.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle.clear();
        disposables = null;
        fragmentLifecycleCallbacks = null;
        bundle = null;
        listenerRegistrations = null;
        constraintSet = null;
        onMenuItemClickListener = null;
        viewModel = null;
        parentResultListener = null;
        childResultListener = null;
        subServiços = null;
        id = null;
        idServiço = null;
        eventListener = null;
        idTipoServiço = null;
        binding = null;
    }

    private void observarChild(String requestKey, Bundle result) {
        if (result.containsKey("idToUpdate")) {
            Bundle b = new Bundle();
            b.putString("id", bundle.getString("id"));
            b.putString("idServiço", bundle.getString("idServiço"));
            b.putString("idTipoServiço", bundle.getString("idTipoServiço"));
            b.putString("idSubServiço", result.getString("idToUpdate"));

            new ManageSubServiceFragment().show(getChildFragmentManager(), null);
            getChildFragmentManager().setFragmentResult("ManageSubServiceFragment", b);

            return;
        }

        if (result.containsKey("idToRemove")) {
            for (Disposable disposable : disposables) {
                disposable.dispose();
            }

            disposables.clear();

            disposables.add(viewModel.getService().getBarbeariaService().removerSubServiço(bundle.getString("id"), bundle.getString("idServiço"), bundle.getString("idTipoServiço"), result.getString("idToRemove")).subscribe(aBoolean -> {
                if (!aBoolean) {
                    Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                }
            }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show()));
        }
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = result;
        id = result.containsKey("id") ? result.getString("id") : null;
        idServiço = result.containsKey("idServiço") ? result.getString("idServiço") : null;
        idTipoServiço = result.containsKey("idTipoServiço") ? result.getString("idTipoServiço") : null;

        for (ListenerRegistration listenerRegistration : listenerRegistrations) {
            listenerRegistration.remove();
        }

        listenerRegistrations.clear();

        binding.barra.setOnMenuItemClickListener(null);

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

        if (onMenuItemClickListener == null) {
            onMenuItemClickListener = this::observarOnMenuItemClick;
        }

        binding.barra.setTitle(viewModel.getServiços().containsKey(id) ? "Tipos de ".concat(String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).concat(" de ").concat(String.valueOf(viewModel.getTiposServiços().get(idServiço).get(idTipoServiço).get("nome")))) : null);
        binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);

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
                            }).commit();
                }

                if (eventListener == null) {
                    eventListener = this::observarOnEventListener;
                }

                listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).addSnapshotListener(eventListener));
                listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this));

            } else {
                disposables.add(viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(this::observarMap, this::observarErro));
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
                disposables.add(viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(this::observarMap, this::observarErro));
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

        if (eventListener == null) {
            eventListener = this::observarOnEventListener;
        }

        listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).addSnapshotListener(eventListener));
        listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this));
    }

    private void observarOnEventListener(DocumentSnapshot value, FirebaseFirestoreException error) {
        try {
            if (!value.exists()) {
                if (viewModel.getTiposServiços().containsKey(idServiço)) {
                    if (viewModel.getTiposServiços().get(idServiço).containsKey(idTipoServiço)) {
                        viewModel.getTiposServiços().get(idServiço).remove(idTipoServiço);
                    }

                    requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new MyTypeServicesFragment()).commit();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        new InsertSubServiceFragment().show(getChildFragmentManager(), null);
        return true;
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

        if (eventListener == null) {
            eventListener = this::observarOnEventListener;
        }

        listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).addSnapshotListener(eventListener));
        listenerRegistrations.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").document(idTipoServiço).collection("subservicos").addSnapshotListener(this));
    }

    private void observarDisposable(Disposable disposable) {
        FragmentUtil.observarFragment("LoadingFragment", getChildFragmentManager(), R.id.layoutFragment);
        bundle.putInt("value", 0);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn: disposables.add(viewModel.getService().getBarbeariaService().obterSubServiços(id, idServiço, idTipoServiço).doOnSubscribe(this::observarDisposable).subscribe(this::observarMap, this::observarErro));
                break;
            case R.id.inserir: new InsertSubServiceFragment().show(getChildFragmentManager(), null);
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new MyTypeServicesFragment()).commit();
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