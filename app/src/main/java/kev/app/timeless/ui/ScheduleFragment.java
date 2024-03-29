package kev.app.timeless.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.text.PrecomputedTextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentScheduleBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.Horário;
import kev.app.timeless.model.User;
import kev.app.timeless.util.LoggedInListener;
import kev.app.timeless.util.ScheduleAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class ScheduleFragment extends DaggerFragment implements View.OnClickListener, LoggedInListener, EventListener<QuerySnapshot> {
    private FragmentScheduleBinding binding;
    private ListenerRegistration listenerRegistration;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Observer<List<User>> observer;
    private Map<State, View> map;
    private LinearLayoutManager linearLayoutManager;
    private String loggedInUserId;
    private List<Task<?>> tasks;
    private ScheduleAdapter scheduleAdapter;
    private MapViewModel viewModel;
    private ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener;
    private Handler handler;

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
        onGlobalLayoutListener = this::observarLayout;
        handler = new Handler(Looper.getMainLooper());
        map = new HashMap<>();

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

        binding.layoutPrincipal.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.layoutPrincipal.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
        binding.barra.setNavigationOnClickListener(null);

        if (tasks != null) {
            tasks.clear();
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }

        if (map.containsKey(State.Error)) {
            ConstraintLayout layout = (ConstraintLayout) map.get(State.Error);

            for (int i = 0; i < layout.getChildCount(); i++) {
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
        handler = null;
        tasks = null;
        onGlobalLayoutListener = null;
        map = null;
        observer = null;
        scheduleAdapter = null;
        parentResultListener = null;
        bundle = null;
        listenerRegistration = null;
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

    public List<Task<?>> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        return tasks;
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
        for (int i = 0; i < binding.layoutPrincipal.getChildCount(); i++) {
            View v = binding.layoutPrincipal.getChildAt(i);

            if (v == binding.barra) {
                continue;
            }

            switch (v.getId()) {
                case R.id.layoutBarraProgresso: obterHorario(bundle);
                    break;
                case R.id.recyclerView: inicializarRecyclerView(v);
                    break;
                case R.id.layoutText: inicializarText(v);
                    break;
                case R.id.layoutError: inicializarRetryBtn(v);
                    break;
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

    private void inicializarText(View v) {
        AppCompatTextView appCompatTextView = v.findViewById(R.id.txt);

        String txtAMostrar = TextUtils.equals(bundle.getString("id"), loggedInUserId) ? "Adicione um tipo de serviço para os outros usuarios o verem " : "Sem tipo de disponiveis";

        if (TextUtils.equals(appCompatTextView.getText(), txtAMostrar)) {
            return;
        }

        appCompatTextView.setPrecomputedText(PrecomputedTextCompat.create(txtAMostrar, appCompatTextView.getTextMetricsParamsCompat()));
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
            }, this, this);
        }

        if (linearLayoutManager == null) {
            linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        }

        if (!linearLayoutManager.equals(recyclerView.getLayoutManager())) {
            recyclerView.setLayoutManager(linearLayoutManager);
        }

        if (scheduleAdapter.equals(recyclerView.getAdapter())) {
            return;
        }

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
        getTasks().clear();

        getTasks().add(
                Tasks.withTimeout(viewModel.getService().getBarbeariaService().obterHorario(bundle.getString("id")), 5, TimeUnit.SECONDS)
                        .addOnSuccessListener(viewModel.getService().getExecutor(), queryDocumentSnapshots -> {
                            Map<String, Map<String, Object>> horario = new HashMap<>();

                            for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                                horario.put(documentSnapshot.getId(), new HashMap<>());

                                for (String key : documentSnapshot.getData().keySet()) {
                                    horario.get(documentSnapshot.getId()).put(key, documentSnapshot.get(key));
                                }
                            }

                            viewModel.getHorários().put(bundle.getString("id"), horario);

                            try {
                                handler.post(() -> {
                                    if (horario.size() == 0) {
                                        onEmpty();
                                    } else {
                                        onLoaded();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).addOnFailureListener(viewModel.getService().getExecutor(), e -> {
                            try {
                                handler.post(this::onError);
                            } catch (Exception x) {
                                x.printStackTrace();
                            }
                        })
        );
    }

    private void onLoaded() {
        if (!map.containsKey(State.Loaded)) {
            map.put(State.Loaded, View.inflate(requireActivity(), R.layout.list, null));
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.topMargin = 24;
        layoutParams.bottomMargin = 36;

        removerViewsDoLayout();

        binding.layoutPrincipal.addView(map.get(State.Loaded), layoutParams);
    }

    private void adicionarListener() {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("horario").addSnapshotListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.retryBtn: obterHorario(bundle);
                break;
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new AboutFragment()).commit();
                break;
            case R.id.show: observarBtnShow(view);
                break;
            case R.id.edit: abrirManageScheduleFragment(view);
                break;
        }
    }

    private void abrirManageScheduleFragment(View view) {
        Bundle b = new Bundle();

        b.putString("fragmentToLoad", "ManageScheduleFragment");

        for (int i = 0 ; i < linearLayoutManager.getChildCount() ; i ++) {
            ConstraintLayout layout = (ConstraintLayout) linearLayoutManager.getChildAt(i);

            if (!view.equals(Objects.requireNonNull(layout).findViewById(R.id.edit))) {
                continue;
            }

            b.putInt("chosenDay", obterDiaSemana(layout.findViewById(R.id.txtDiaSemana)));
        }

        if (!b.containsKey("chosenDay")) {
            return;
        }

        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
    }

    private int obterDiaSemana(TextView txtDiaSemana) {
        int diaSemana;

        switch (txtDiaSemana.getText().toString()) {
            case "Domingo": diaSemana = Calendar.SUNDAY;
                break;
            case "Segunda-Feira": diaSemana = Calendar.MONDAY;
                break;
            case "Terça-Feira": diaSemana = Calendar.TUESDAY;
                break;
            case "Quarta-Feira": diaSemana = Calendar.WEDNESDAY;
                break;
            case "Quinta-Feira": diaSemana = Calendar.THURSDAY;
                break;
            case "Sexta-Feira": diaSemana = Calendar.FRIDAY;
                break;
            case "Sábado": diaSemana = Calendar.SATURDAY;
                break;
            default: diaSemana = 10;
        }

        return diaSemana;
    }

    private void observarBtnShow(View view) {
        try {
            for (int i = 0 ; i < linearLayoutManager.getChildCount() ; i++) {
                ConstraintLayout v = (ConstraintLayout) linearLayoutManager.getChildAt(i);

                TextView textView = v.findViewById(R.id.txtHorario);

                if (v.findViewById(R.id.show) != view) {
                    if (textView.getVisibility() == View.VISIBLE) {
                        textView.setVisibility(View.GONE);
                    }

                    if (v.findViewById(R.id.show).getRotation() != 0) {
                        v.findViewById(R.id.show).setRotation(0);
                    }

                    continue;
                }

                textView.setVisibility(textView.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        view.setRotation(view.getRotation() == 180 ? 0 : 180);
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (viewModel.getHorários().containsKey(bundle.getString("id"))) {
                    Map<String, Map<String, Object>> map = viewModel.getHorários().get(bundle.getString("id"));

                    if (!map.isEmpty()) {
                        map.clear();
                    }
                }

                if (bundle.containsKey("selectedDay")) {
                    bundle.remove("selectedDay");
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

            if (bundle.containsKey("selectedDay")) {
                bundle.remove("selectedDay");
            }

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
    public String loggedInUserId() {
        return loggedInUserId;
    }
}