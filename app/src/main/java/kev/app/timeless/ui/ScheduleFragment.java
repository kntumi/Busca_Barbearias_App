package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.RangeSlider;
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
import kev.app.timeless.databinding.FragmentScheduleBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.Horário;
import kev.app.timeless.model.User;
import kev.app.timeless.util.ScheduleAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class ScheduleFragment extends DaggerFragment implements View.OnClickListener, View.OnLongClickListener, CompoundButton.OnCheckedChangeListener, EventListener<QuerySnapshot> {
    private FragmentScheduleBinding binding;
    private ListenerRegistration listenerRegistration;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Observer<List<User>> observer;
    private Map<State, View> map;
    private LinearLayoutManager linearLayoutManager;
    private String loggedInUserId;
    private Disposable disposable;
    private ScheduleAdapter scheduleAdapter;
    private MapViewModel viewModel;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private List<Float> values;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_schedule, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        observer = users -> loggedInUserId = users.size() == 0 ? null : users.get(users.size() - 1).getId();
        parentResultListener = this::observarParent;
        map = new HashMap<>();
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
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (map.containsKey(State.Error)) {
            ConstraintLayout layout = (ConstraintLayout) map.get(State.Error);

            for (int i = 0 ; i < layout.getChildCount() ; i++) {
                View v = layout.getChildAt(i);

                if (!v.hasOnClickListeners()) {
                    continue;
                }

                v.setOnClickListener(null);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map.clear();
        bundle.clear();
        linearLayoutManager = null;
        loggedInUserId = null;
        disposable = null;
        onGlobalLayoutListener = null;
        map = null;
        observer = null;
        scheduleAdapter = null;
        parentResultListener = null;
        bundle = null;
        values = null;
        listenerRegistration = null;
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

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        State state;

        if (viewModel.getHorários().containsKey(bundle.getString("id"))) {
            state = viewModel.getHorários().get(bundle.getString("id")).size() == 0 ? State.Empty : State.Loaded;
        } else {
            state = State.Loading;
        }

        switch (state) {
            case Loaded: onLoaded();
                break;
            case Empty: onEmpty();
                break;
            case Loading: onLoading();
                break;
        }
    }

    private void observarLayout() {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v == binding.barra) {
                continue;
            }

            switch (v.getId()) {
                case R.id.layoutBarraProgresso:
                    obterHorario(bundle);
                    break;
                case R.id.recyclerView:
                    inicializarRecyclerView(v);
                    break;
                case R.id.layoutText:
                    break;
                case R.id.layoutError:
                    break;
                case R.id.slider:
                    inicializarSlider(v);
                    break;
            }
        }
    }

    private void inicializarSlider(View v) {
        RangeSlider slider = (RangeSlider) v;

        if (values == null) {
            values = new ArrayList<>();

            for (int i = 7; i < 22; i++) {
                for (int j = 0; j <= 55; j++) {
                    if (j % 5 == 0) {
                        if (String.valueOf(j).length() == 2) {
                            values.add(Float.valueOf(i + "." + j));
                        } else {
                            values.add(Float.valueOf(i + ".0" + j));
                        }
                    }
                }
            }
        }

        slider.clearOnChangeListeners();

        if (slider.getValues().isEmpty() || slider.getValues().size() == 1) {
            slider.setValues(7f, 21.55f);
        }

        slider.addOnChangeListener(this::observarSlider);
    }

    private void observarSlider(RangeSlider slider, float v, boolean b) {
        if (slider.getValues().get(0) >= 7) {
            if (!values.contains(slider.getValues().get(0))) {
                Float valor = null;
                for (Float aFloat : values) {
                    if (valor != null) {
                        if (slider.getValues().get(0) >= aFloat) {
                            valor = aFloat;
                        }
                    } else {
                        valor = aFloat;
                    }
                }

                slider.setValues(valor, slider.getValues().get(1));
                return;
            }
        } else {
            slider.setValues(7f, slider.getValues().get(1));
            return;
        }

        if (slider.getValues().get(1) > 21.55f) {
            slider.setValues(slider.getValues().get(0), 21.55f);
            return;
        } else {
            if (!values.contains(slider.getValues().get(1))) {
                Float valor = null;
                for (Float aFloat : values) {
                    if (valor != null) {
                        if (slider.getValues().get(1) >= aFloat) {
                            valor = aFloat;
                        }
                    } else {
                        valor = aFloat;
                    }
                }

                slider.setValues(slider.getValues().get(0), valor);
                return;
            }
        }

        String horaAbertura = "", horaEncerramento = "";

        for (Float hora : slider.getValues()) {
            String s = String.valueOf(hora);

            if (s.length() - s.indexOf(".") == 2) {
                switch (slider.getValues().indexOf(hora)) {
                    case 0:
                        horaAbertura = s.concat("0");
                        break;
                    case 1:
                        horaEncerramento = s.concat("0");
                        break;
                }
            } else {
                switch (slider.getValues().indexOf(hora)) {
                    case 0:
                        horaAbertura = s;
                        break;
                    case 1:
                        horaEncerramento = s;
                        break;
                }
            }
        }

        binding.barra.setSubtitle("Aberto das ".concat(horaAbertura.replace(".", ":").concat(" até ").concat(horaEncerramento.replace(".", ":"))));
    }

    private void inicializarRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v;

        if (scheduleAdapter == null) {
            scheduleAdapter = new ScheduleAdapter(new DiffUtil.ItemCallback<Horário>() {
                @Override
                public boolean areItemsTheSame(@NonNull Horário oldItem, @NonNull Horário newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Horário oldItem, @NonNull Horário newItem) {
                    return false;
                }
            }, this);
        }

        try {
            Map<String, Object> map = viewModel.getHorários().get(bundle.getString("id")).get(String.valueOf(bundle.getInt("selectedDay")));

            if (map == null) {
                throw new NullPointerException();
            }

        } catch (Exception e) {
            binding.barra.setSubtitle("Selecione um dia da semana");

            for (int i = 0; i < binding.barra.getMenu().size(); i++) {
                binding.barra.getMenu().removeItem(i);
            }
        }

        for (int j = 0; j < binding.layoutPrincipal.getChildCount(); j++) {
            View view = binding.layoutPrincipal.getChildAt(j);

            if (view.getId() != R.id.slider) {
                continue;
            }

            view.setEnabled(!TextUtils.equals(binding.barra.getSubtitle(), "Selecione um dia da semana"));
        }

        if (scheduleAdapter.equals(recyclerView.getAdapter())) {
            return;
        }

        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        }

        if (!TextUtils.isEmpty(loggedInUserId)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.leftMargin = 16;
            layoutParams.rightMargin = 16;
            layoutParams.topMargin = 16;
            layoutParams.bottomMargin = 36;

            binding.layoutPrincipal.addView(View.inflate(requireActivity(), R.layout.slider, null), layoutParams);
        }

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(scheduleAdapter);

        ArrayList<Horário> hs = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getHorários().get(bundle.getString("id")).entrySet()) {
            Horário h = new Horário(Integer.parseInt(entry.getKey()));

            for (Map.Entry<String, Object> entry1 : entry.getValue().entrySet()) {
                switch (entry1.getKey()) {
                    case "horaAbertura": h.setHoraAbertura(Double.parseDouble(String.valueOf(entry1.getValue())));
                        break;
                    case "horaEncerramento": h.setHoraEncerramento(Double.parseDouble(String.valueOf(entry1.getValue())));
                        break;
                }
            }

            hs.add(h);
        }

        scheduleAdapter.submitList(hs, this::adicionarListener);
    }

    private void onEmpty() {
        if (!map.containsKey(State.Empty)) {
            map.put(State.Empty, View.inflate(requireActivity(), R.layout.text, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.topMargin = 24;

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Empty), layoutParams);
    }

    private void onLoading() {
        if (!map.containsKey(State.Loading)) {
            map.put(State.Loading, View.inflate(requireActivity(), R.layout.loading, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.topMargin = 24;

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Loading), layoutParams);
    }

    private void removerViewsDoLayout() {
        for (int i = 0 ; i < binding.layoutPrincipal.getChildCount() ; i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v == binding.barra) {
                continue;
            }

            binding.layoutPrincipal.removeViewAt(i);
        }
    }

    private void onError() {
        if (!map.containsKey(State.Error)) {
            map.put(State.Error, View.inflate(requireActivity(), R.layout.error, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.topMargin = 24;

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Error), layoutParams);
    }

    private void obterHorario(Bundle bundle) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = viewModel.getService().getBarbeariaService().obterHorário(bundle.getString("id")).subscribe(stringMapMap -> {
            viewModel.getHorários().put(bundle.getString("id"), stringMapMap);

            if (stringMapMap.size() == 0) {
                onEmpty();
            } else {
                onLoaded();
            }
        }, throwable -> onError());
    }

    private void onLoaded() {
        if (!map.containsKey(State.Loaded)) {
            map.put(State.Loaded, View.inflate(requireActivity(), R.layout.list, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = 24;
        layoutParams.rightMargin = 24;
        layoutParams.topMargin = 24;
        layoutParams.bottomMargin = TextUtils.isEmpty(loggedInUserId) ? 36 : 0;

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Loaded), layoutParams);
    }

    private void adicionarListener() {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("horario").addSnapshotListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.inserir: new InsertScheduleFragment().show(getChildFragmentManager(), "currentFragment");
                break;
            case R.id.retryBtn:
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new AboutFragment()).commit();
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

                onEmpty();

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot documentSnapshot : value) {
                map.put(documentSnapshot.getId(), documentSnapshot.getData());
            }

            if (viewModel.getHorários().containsKey(bundle.getString("id"))) {
                if (map.equals(viewModel.getHorários().get(bundle.getString("id")))) {
                    return;
                }
            }

            viewModel.getHorários().put(bundle.getString("id"), map);

            ArrayList<Horário> hs = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : viewModel.getHorários().get(bundle.getString("id")).entrySet()) {
                Horário h = new Horário(Integer.parseInt(entry.getKey()));

                for (Map.Entry<String, Object> entry1 : entry.getValue().entrySet()) {
                    switch (entry1.getKey()) {
                        case "horaAbertura": h.setHoraAbertura(Double.parseDouble(String.valueOf(entry1.getValue())));
                            break;
                        case "horaEncerramento": h.setHoraEncerramento(Double.parseDouble(String.valueOf(entry1.getValue())));
                            break;
                    }
                }

                hs.add(h);
            }

            scheduleAdapter.submitList(hs);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        for (int i = 0 ; i < linearLayoutManager.getChildCount() ; i++) {
            ((CompoundButton) linearLayoutManager.getChildAt(i)).setChecked(false);
        }

        compoundButton.setChecked(b);

        if (b) {
            bundle.putInt("selectedDay", scheduleAdapter.getCurrentList().get(linearLayoutManager.getPosition(compoundButton)).getDia());
        } else {
            bundle.remove("selectedDay");
        }

        linearLayoutManager.scrollToPosition(linearLayoutManager.getPosition(compoundButton));

        Map<String, Object> map = viewModel.getHorários().get(bundle.getString("id")).get(String.valueOf(bundle.getInt("selectedDay")));

        if (b) {
            try {
                binding.barra.setSubtitle("Aberto das ".concat(String.valueOf(map.get("horaAbertura")).replace(".", ":").concat(" até ").concat(String.valueOf(map.get("horaEncerramento")).replace(".", ":"))));
            } catch (Exception e) {
                binding.barra.setSubtitle("Encerrado");
            }
        } else {
            binding.barra.setSubtitle("Selecione um dia da semana");
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = viewModel.getService().getBarbeariaService().removerHorario(bundle.getString("id"), scheduleAdapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getDia()).doOnEvent((aBoolean, throwable) -> Toast.makeText(requireContext(), aBoolean ? "" : null, Toast.LENGTH_LONG).show()).subscribe();

        return true;
    }
}