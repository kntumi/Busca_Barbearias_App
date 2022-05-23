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
import androidx.lifecycle.Observer;
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
import kev.app.timeless.databinding.FragmentScheduleBinding;
import kev.app.timeless.model.Horário;
import kev.app.timeless.model.User;
import kev.app.timeless.util.FragmentUtil;
import kev.app.timeless.viewmodel.MapViewModel;

public class ScheduleFragment extends Fragment implements View.OnClickListener, EventListener<QuerySnapshot> {
    private FragmentScheduleBinding binding;
    private ConstraintSet constraintSet;
    private ListenerRegistration listenerRegistration;
    private Bundle bundle;
    private MutableLiveData<List<Horário>> horário;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private FragmentResultListener childResultListener, parentResultListener;
    private Observer<List<User>> observer;
    private String loggedInUserId;
    private Disposable disposable;
    private MapViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_schedule, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        bundle = new Bundle();
        observer = this::observarUser;
        parentResultListener = this::observarParent;
        childResultListener = this::observarChild;
        horário = new MutableLiveData<>(new ArrayList<>());
        constraintSet = new ConstraintSet();
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f instanceof ScheduleListFragment) {
                    horário.observeForever(((ScheduleListFragment) f).getObserver());
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f instanceof ScheduleListFragment) {
                    horário.removeObserver(((ScheduleListFragment) f).getObserver());
                }
            }
        };
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
        bundle.clear();
        loggedInUserId = null;
        disposable = null;
        observer = null;
        fragmentLifecycleCallbacks = null;
        childResultListener = null;
        parentResultListener = null;
        horário = null;
        bundle = null;
        listenerRegistration = null;
        constraintSet = null;
        viewModel = null;
        binding = null;
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarUser(List<User> users) {
        loggedInUserId = users.size() == 0 ? null : users.get(users.size() - 1).getId();

        if (bundle == null) {
            return;
        }

        if (TextUtils.equals(loggedInUserId, bundle.getString("id"))) {
            binding.barra.inflateMenu(R.menu.add);
            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = result;

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (TextUtils.isEmpty(bundle.getString("id"))) {
            return;
        }

        if (getChildFragmentManager().getFragments().size() == 0) {
            if (viewModel.getHorários().containsKey(bundle.getString("id"))) {
                if (viewModel.getHorários().get(bundle.getString("id")).size() == 0) {
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new TextFragment(), "currentFragment")
                            .runOnCommit(() -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("txt", "Sem horário disponível");
                                getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                            }).commit();
                } else {
                    ArrayList<Horário> hs = new ArrayList<>();
                    for (Map.Entry<String, Map<String, Double>> entry : viewModel.getHorários().get(bundle.getString("id")).entrySet()) {
                        Horário h = new Horário(Integer.parseInt(entry.getKey()));
                        for (Map.Entry<String, Double> entry1 : entry.getValue().entrySet()) {
                            switch (entry1.getKey()) {
                                case "horaAbertura":
                                    h.setHoraAbertura(entry1.getValue());
                                    break;
                                case "horaEncerramento":
                                    h.setHoraEncerramento(entry1.getValue());
                                    break;
                            }
                        }

                        hs.add(h);
                    }

                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(R.id.layoutFragment, new ScheduleListFragment())
                            .runOnCommit(() -> horário.setValue(hs))
                            .commit();
                }

                listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("horario").addSnapshotListener(this);
            } else {
                disposable = viewModel.getService().getBarbeariaService().obterHorário(bundle.getString("id"))
                        .doOnSubscribe(disposable -> {
                            FragmentUtil.observarFragment("LoadingFragment", getChildFragmentManager(), R.id.layoutFragment);
                            bundle.putInt("value", 0);
                            getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
                        })
                        .subscribe(stringMapMap -> {
                            viewModel.getHorários().put(bundle.getString("id"), stringMapMap);

                            ArrayList<Horário> hs = new ArrayList<>();
                            for (Map.Entry<String, Map<String, Double>> entry : stringMapMap.entrySet()) {
                                Horário h = new Horário(Integer.parseInt(entry.getKey()));
                                for (Map.Entry<String, Double> entry1 : entry.getValue().entrySet()) {
                                    switch (entry1.getKey()) {
                                        case "horaAbertura": h.setHoraAbertura(entry1.getValue());
                                            break;
                                        case "horaEncerramento": h.setHoraEncerramento(entry1.getValue());
                                            break;
                                    }
                                }

                                hs.add(h);
                            }

                            if (stringMapMap.size() == 0) {
                                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("txt", "Sem horário disponível");
                                    getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                                }).commit();
                            } else {
                                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new ScheduleListFragment()).runOnCommit(() -> horário.setValue(hs)).commit();
                            }

                            listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("horario").addSnapshotListener(this);
                        }, throwable -> {
                            bundle.putInt("value", 1);
                            getChildFragmentManager().setFragmentResult("LoadingFragment", bundle);
                        });
            }

            return;
        }

        switch (getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName()) {
            case "LoadingFragment":
                break;
            case "ScheduleListFragment":
                                            if (horário.getValue() == null || horário.getValue().size() == 0) {
                                                ArrayList<Horário> hs = new ArrayList<>();
                                                for (Map.Entry<String, Map<String, Double>> entry : viewModel.getHorários().get(bundle.getString("id")).entrySet()) {
                                                    Horário h = new Horário(Integer.parseInt(entry.getKey()));
                                                    for (Map.Entry<String, Double> entry1 : entry.getValue().entrySet()) {
                                                        switch (entry1.getKey()) {
                                                            case "horaAbertura": h.setHoraAbertura(entry1.getValue());
                                                                break;
                                                            case "horaEncerramento": h.setHoraEncerramento(entry1.getValue());
                                                                break;
                                                        }
                                                    }

                                                    hs.add(h);
                                                }

                                                horário.setValue(hs);

                                                Bundle b = new Bundle();
                                                if (result.containsKey(getClass().getSimpleName())) {
                                                    if (result.getBundle(getClass().getSimpleName()).containsKey("ScheduleListFragment")) {
                                                        if (result.getBundle(getClass().getSimpleName()).getBundle("ScheduleListFragment").containsKey("position")) {
                                                            b.putInt("position", result.getBundle(getClass().getSimpleName()).getBundle("ScheduleListFragment").getInt("position"));
                                                        }
                                                    }
                                                }

                                                requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                                            }
                break;
            case "TextFragment":  Bundle bundle = new Bundle();
                                  bundle.putString("txt", "Sem horário disponível");
                                  getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                break;
        }

        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("horario").addSnapshotListener(this);
    }

    private void observarChild(String requestKey, Bundle result) {
        if (result.containsKey("idToRemove")) {
            viewModel.getService().getBarbeariaService().removerHorario(bundle.getString("id"), result.getInt("idToRemove")).subscribe(new MaybeCallbackObserver<>(aBoolean -> {
                if (!aBoolean) {
                    Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                }
            }, Throwable::printStackTrace, null));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.inserir: new InsertScheduleFragment().show(getChildFragmentManager(), "currentFragment");
                break;
            case R.id.retryBtn:
                break;
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getHorários().containsKey(bundle.getString("id"))) {
                    viewModel.getHorários().get(bundle.getString("id")).clear();
                }

                if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals(getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName(), "TextFragment")) {
                    getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new TextFragment(), "currentFragment").runOnCommit(() -> {
                        Bundle bundle = new Bundle();
                        bundle.putString("txt", "Sem horário disponível");
                        getChildFragmentManager().setFragmentResult("TextFragment", bundle);
                    }).commit();
                }

                return;
            }

            Map<String, Map<String, Double>> map = new HashMap<>();

            ArrayList<Horário> list = new ArrayList<>();
            for (DocumentSnapshot documentSnapshot : value) {
                Horário horário = new Horário(Integer.parseInt(documentSnapshot.getId()));
                map.put(documentSnapshot.getId(), new HashMap<>());

                for (String key : documentSnapshot.getData().keySet()) {
                    map.get(documentSnapshot.getId()).put(key, documentSnapshot.getDouble(key));

                    switch (key) {
                        case "horaAbertura": horário.setHoraAbertura(documentSnapshot.getDouble(key));
                            break;
                        case "horaEncerramento": horário.setHoraEncerramento(documentSnapshot.getDouble(key));
                            break;
                    }
                }

                list.add(horário);
            }

            if (!viewModel.getHorários().containsKey(bundle.getString("id"))) {
                viewModel.getHorários().put(bundle.getString("id"), new HashMap<>());
            }

            if (viewModel.getHorários().get(bundle.getString("id")).equals(map)) {
                return;
            } else {
                viewModel.getHorários().get(bundle.getString("id")).putAll(map);
            }

            if (horário.getValue() != null) {
                requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), Bundle.EMPTY);
            }

            viewModel.getHorários().put(bundle.getString("id"), map);
            horário.setValue(list);

            if (getChildFragmentManager().getFragments().size() == 0 || !TextUtils.equals("ScheduleListFragment", getChildFragmentManager().getFragments().get(getChildFragmentManager().getFragments().size() - 1).getClass().getSimpleName())) {
                getChildFragmentManager().beginTransaction().replace(R.id.layoutFragment, new ScheduleListFragment()).commit();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}