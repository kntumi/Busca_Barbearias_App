package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
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
import kev.app.timeless.databinding.LayoutTypeServiceBinding;
import kev.app.timeless.model.TipoServiço;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class TypeServicesFragment extends Fragment implements EventListener<QuerySnapshot>, View.OnClickListener {
    private LayoutTypeServiceBinding binding;
    private MapViewModel viewModel;
    private String id, idServiço;
    private List<ListenerRegistration> listeners;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private FragmentResultListener fragmentResultListener, parentResultListener;
    private MaybeCallbackObserver<Map<String, Map<String, Object>>> callbackObserver;
    private MutableLiveData<List<TipoServiço>> tiposServiços;
    private EventListener<DocumentSnapshot> eventListener;
    private Bundle bundle;
    private Disposable disposable;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_type_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        bundle = new Bundle();
        listeners = new ArrayList<>();
        tiposServiços = new MutableLiveData<>();
        parentResultListener = this::observarParent;
        fragmentResultListener = this::observarResultado;
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof TypeServicesListFragment) {
                    tiposServiços.observeForever(((TypeServicesListFragment) f).getObserver());
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof TypeServicesListFragment) {
                    tiposServiços.removeObserver(((TypeServicesListFragment) f).getObserver());
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
        getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
        getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

        if (disposable != null) {
            disposable.dispose();
        }

        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }

        listeners.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listeners.clear();
        bundle.clear();
        disposable = null;
        eventListener = null;
        onMenuItemClickListener = null;
        bundle = null;
        tiposServiços = null;
        fragmentResultListener = null;
        callbackObserver = null;
        fragmentLifecycleCallbacks = null;
        id = null;
        idServiço = null;
        viewModel = null;
        binding = null;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = result;
        id = result.containsKey("id") ? result.getString("id") : null;
        idServiço = result.containsKey("idServiço") ? result.getString("idServiço") : null;

        for (ListenerRegistration listener : listeners) {
            listener.remove();
        }

        listeners.clear();

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(idServiço) || !viewModel.getServiços().containsKey(id) || !viewModel.getServiços().get(id).containsKey(idServiço) || (viewModel.getServiços().containsKey(id) && !viewModel.getServiços().get(id).containsKey(idServiço) && getChildFragmentManager().getFragments().size() != 0)) {
            requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
            return;
        }

        if (onMenuItemClickListener == null) {
            onMenuItemClickListener = this::observarOnMenuItemClick;
        }

        binding.barra.setTitle(viewModel.getServiços().containsKey(id) ? "Tipos de ".concat(String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome"))) : null);
        binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);

        if (getChildFragmentManager().getFragments().size() == 0) {
            if (viewModel.getTiposServiços().containsKey(idServiço)) {
                if (viewModel.getTiposServiços().get(idServiço).size() == 0) {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" disponível.");
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
                } else {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TypeServicesListFragment()).runOnCommit(() -> {
                        ArrayList<TipoServiço> tipoServiços = new ArrayList<>();
                        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getTiposServiços().get(idServiço).entrySet()) {
                            tipoServiços.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                        }

                        tiposServiços.setValue(tipoServiços);
                    }).commit();
                }

                if (eventListener == null) {
                    eventListener = this::observarOnEventListener;
                }

                listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).addSnapshotListener(eventListener));
                listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").addSnapshotListener(this));
            } else {
                if (callbackObserver == null) {
                    callbackObserver = new MaybeCallbackObserver<>(this::observarMap, this::observarErro, null);
                }

                viewModel.getService().getBarbeariaService().obterTiposServiços(id, idServiço).doOnSubscribe(this::observarDisposable).subscribe(callbackObserver);
            }

            return;
        }

        switch (getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName()) {
            case "LoadingFragment":
                break;
            case "TypeServicesListFragment": if (tiposServiços.getValue() == null  || tiposServiços.getValue().size() == 0) {
                                                 ArrayList<TipoServiço> tipoServiços = new ArrayList<>();
                                                 for (Map.Entry<String, Map<String, Object>> entry : viewModel.getTiposServiços().get(idServiço).entrySet()) {
                                                     tipoServiços.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                                                 }

                                                 tiposServiços.setValue(tipoServiços);
                                             }
                break;
            case "TextFragment":    Bundle bundle = new Bundle();
                                    bundle.putString("txt", "Nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" disponível.");
                                    getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                break;
        }

        if (eventListener == null) {
            eventListener = this::observarOnEventListener;
        }

        listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).addSnapshotListener(eventListener));
        listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").addSnapshotListener(this));
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        new InsertTypeServiceFragment().show(requireParentFragment().getChildFragmentManager(), null);
        return true;
    }

    private void observarResultado(String requestKey, Bundle result) {
        if (result.containsKey("idTipoServiçoEscolhido")) {
            Bundle b = new Bundle();
            b.putString("idTipoServiço", result.getString("idTipoServiçoEscolhido"));
            requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
            return;
        }

        if (result.containsKey("idToRemove")) {
            if (disposable != null) {
                disposable.dispose();
            }

            disposable = viewModel.getService().getBarbeariaService().removerTipoServiço(id, idServiço, result.getString("idToRemove")).subscribe(aBoolean -> {
                if (!aBoolean) {

                }
            }, Throwable::printStackTrace);
        }
    }

    private void observarDisposable(Disposable disposable) {
        FragmentUtil.observarFragment("LoadingFragment", getChildFragmentManager(), R.id.layoutFragment);
        Bundle bundle = new Bundle();
        bundle.putInt("value", 0);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    private void observarMap(Map<String, Map<String, Object>> map) {
        if (!viewModel.getTiposServiços().containsKey(idServiço)) {
            viewModel.getTiposServiços().put(idServiço, new HashMap<>());
        }

        viewModel.getTiposServiços().get(idServiço).putAll(map);

        if (map.size() == 0) {
            getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                Bundle bundle = new Bundle();
                bundle.putString("txt", "Nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" disponível.");
                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
            }).commit();
        } else {
            getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TypeServicesListFragment(), "currentFragment").runOnCommit(() -> {
                ArrayList<TipoServiço> tipoServiços = new ArrayList<>();
                for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                    tipoServiços.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
                }

                tiposServiços.setValue(tipoServiços);
            }).commit();
        }

        if (eventListener == null) {
            eventListener = this::observarOnEventListener;
        }

        listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).addSnapshotListener(eventListener));
        listeners.add(viewModel.getService().getFirestore().collection("Barbearia").document(id).collection("servicos").document(idServiço).collection("tipos").addSnapshotListener(this));
    }

    private void observarOnEventListener(DocumentSnapshot value, FirebaseFirestoreException error) {
        try {
            if (!value.exists()) {
                if (viewModel.getServiços().get(id).containsKey(idServiço)) {
                    viewModel.getServiços().get(id).remove(idServiço);

                    if (viewModel.getTiposServiços().containsKey(idServiço)) {
                        viewModel.getTiposServiços().remove(idServiço);
                    }
                }

                requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new MyServicesFragment()).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void observarErro(Throwable throwable) {
        Bundle bundle = new Bundle();
        bundle.putInt("value", 1);
        getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getTiposServiços().containsKey(idServiço)) {
                    viewModel.getTiposServiços().remove(idServiço);
                }

                if (bundle.containsKey("idTipoServiço")) {
                    requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), Bundle.EMPTY);
                }

                if (!TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "TextFragment")) {
                    getChildFragmentManager().beginTransaction().remove(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1)).runOnCommit(() -> getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment())
                            .runOnCommit(() -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("txt", "Nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" disponível.");
                                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                            }).commit()).commit();
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putString("txt", "Nenhum tipo de "+String.valueOf(viewModel.getServiços().get(id).get(idServiço).get("nome")).toLowerCase()+" disponível.");
                    getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value.getDocuments()) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (!viewModel.getTiposServiços().containsKey(idServiço)) {
                viewModel.getTiposServiços().put(idServiço, new HashMap<>());
            }

            if (viewModel.getTiposServiços().get(idServiço).equals(map)) {
                return;
            } else {
                viewModel.getTiposServiços().get(idServiço).putAll(map);
            }

            ArrayList<TipoServiço> tiposServiçosDb = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
                tiposServiçosDb.add(new TipoServiço(entry.getKey(), String.valueOf(entry.getValue().get("nome"))));
            }

            if (bundle.containsKey("idTipoServiço")) {
                requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), Bundle.EMPTY);
            }

            tiposServiços.setValue(tiposServiçosDb);

            if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "TypeServicesListFragment")) {
                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TypeServicesListFragment()).commit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn:
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new ServicesFragment()).commit();
                break;
        }
    }
}