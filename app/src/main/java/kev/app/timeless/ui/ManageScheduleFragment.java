package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentManageScheduleBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.viewmodel.MapViewModel;

public class ManageScheduleFragment extends DaggerFragment implements View.OnClickListener {
    private FragmentManageScheduleBinding binding;
    private Map<String, String> horario;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Disposable disposable;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private MaterialTimePicker materialTimePicker;
    private TextWatcher textWatcher;
    private List<String> list;
    private List<Chip> chips;
    private MapViewModel viewModel;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_manage_schedule, container, false);
        return binding.manageSchedule;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        materialTimePicker = new MaterialTimePicker.Builder().setTimeFormat(android.text.format.DateFormat.is24HourFormat(requireContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H).setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK).setTheme(R.style.ThemeOverlay_MaterialComponents_TimePicker).build();
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String sufixo = bundle.getInt("selectedEndIconParentId") == binding.abertura.getId() ? "Abertura" : "Encerramento";

                for (Chip chip : chips) {
                    String key = "possivel".concat((chip.getId() == R.id.material_hour_tv ? "Hora" : "Minuto").concat(sufixo));

                    if (!horario.containsKey(key) && !chip.isChecked() || chip.isChecked()) {
                        horario.put(key, chip.getText().toString());
                    }
                }
            }
        };
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f.getClass().getSimpleName().equals(materialTimePicker.getClass().getSimpleName())) {
                    ConstraintLayout layout = (ConstraintLayout) f.getView();

                    try {
                        for (int i = 0; i < layout.getChildCount(); i++) {
                            View v = layout.getChildAt(i);

                            switch (v.getId()) {
                                case R.id.material_timepicker_cancel_button:
                                    adicionarTextAoCancelBtn(v);
                                    break;
                                case R.id.header_title:
                                    adicionarTextATituloHeader(v);
                                    break;
                                case R.id.material_timepicker_view:
                                    adicionarChips(v);
                                    break;
                                case R.id.material_timepicker_mode_button:
                                    v.setVisibility(v.getVisibility() == View.VISIBLE ? View.GONE : v.getVisibility());
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for (Chip chip : chips) {
                        chip.addTextChangedListener(textWatcher);
                    }
                }
            }

            @Override
            public void onFragmentPaused(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentPaused(fm, f);
                if (f.getClass().getSimpleName().equals(materialTimePicker.getClass().getSimpleName())) {
                    for (Chip chip : chips) {
                        chip.removeTextChangedListener(textWatcher);
                    }

                    chips.clear();
                }
            }
        };

        horario = new HashMap<>();
        list = new ArrayList<>();
        chips = new ArrayList<>();

        if (savedInstanceState == null) {
            parentResultListener = this::observarParent;
        }

        list.add("hora");
        list.add("minuto");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (parentResultListener != null) {
            requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        }

        binding.barra.setNavigationOnClickListener(this);
        binding.abertura.setEndIconOnClickListener(this);
        binding.encerramento.setEndIconOnClickListener(this);
        materialTimePicker.addOnPositiveButtonClickListener(this);
        materialTimePicker.addOnNegativeButtonClickListener(this);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);
        binding.abertura.setEndIconOnClickListener(null);
        binding.encerramento.setEndIconOnClickListener(null);
        materialTimePicker.removeOnPositiveButtonClickListener(this);
        materialTimePicker.removeOnNegativeButtonClickListener(this);
        getChildFragmentManager().unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);

        if (disposable != null) {
            disposable.dispose();
        }

        if (onMenuItemClickListener != null) {
            binding.barra.setOnMenuItemClickListener(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        list.clear();

        try {
            materialTimePicker.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }

        bundle.clear();
        binding.manageSchedule.removeAllViews();
        horario.clear();
        bundle = null;
        list = null;
        materialTimePicker = null;
        fragmentLifecycleCallbacks = null;
        onMenuItemClickListener = null;
        disposable = null;
        parentResultListener = null;
        horario = null;
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

        for (Map.Entry<String, String> entry : horario.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        outState.putBundle("bundle", new Bundle(bundle));
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        binding.barra.setOnMenuItemClickListener(null);

        if (TextUtils.isEmpty(bundle.getString("id"))) {
            return;
        }

        if (onMenuItemClickListener == null) {
            onMenuItemClickListener = this::observarOnMenuItemClick;
        }

        binding.barra.setOnMenuItemClickListener(onMenuItemClickListener);

        if (TextUtils.isEmpty(binding.barra.getSubtitle())) {
            binding.barra.setSubtitle(obterDiaSemana(bundle.getInt("chosenDay")));
        }

        Map<String, Object> map;

        if (TextUtils.isEmpty(binding.barra.getTitle())) {
            binding.barra.setTitle("Editar horário");
        }

        try {
            map = viewModel.getHorários().get(bundle.getString("id")).get(String.valueOf(bundle.getInt("chosenDay")));
        } catch (Exception e) {
            return;
        }

        binding.txtEncerramento.setText(obterTime(binding.encerramento.getId(), map));
        binding.txtAbertura.setText(obterTime(binding.abertura.getId(), map));
    }

    private String obterTime(int id, Map<String, ?> map) {
        String keyToLookAt = (id == binding.abertura.getId()) ? "Abertura" : "Encerramento", time = "";

        for (String s : list) {
            String key = s.concat(keyToLookAt);

            if (!map.containsKey(key)) {
                continue;
            }

            time = TextUtils.equals(s, "hora") ? time.concat(String.valueOf(map.get(key))) : time.concat(":"+map.get(key));
        }

        return TextUtils.isEmpty(time) ? "Encerrado" : time;
    }

    private String obterDiaSemana(int diaSemana) {
        switch (diaSemana) {
            case Calendar.SUNDAY: return "Domingo";
            case Calendar.MONDAY: return "Segunda-Feira";
            case Calendar.TUESDAY: return "Terça-Feira";
            case Calendar.WEDNESDAY: return "Quarta-Feira";
            case Calendar.THURSDAY: return "Quinta-Feira";
            case Calendar.FRIDAY: return "Sexta-Feira";
            case Calendar.SATURDAY: return "Sábado";
            default: return null;
        }
    }

    private void adicionarTextATituloHeader(View v) {
        MaterialTextView materialTextView = (MaterialTextView) v;

        String txt = bundle.getInt("selectedEndIconParentId") == binding.abertura.getId() ? "Selecione a hora de abertura" : "Selecione a hora de encerramento";

        if (!TextUtils.equals(materialTextView.getText(), txt)) {
            materialTextView.setText(txt);
        }
    }

    private void adicionarTextAoCancelBtn(View v) {
        MaterialButton materialButton = (MaterialButton) v;

        String txt = "Cancelar";

        if (!TextUtils.equals(materialButton.getText(), txt)) {
            materialButton.setText(txt);
        }
    }

    private void adicionarChips(View v) {
        ConstraintLayout layout1 = (ConstraintLayout) v;

        for (int j = 0 ; j < layout1.getChildCount() ; j++) {
            View view1 = layout1.getChildAt(j);

            if (view1.getId() != R.id.material_clock_display) {
                continue;
            }

            LinearLayout linearLayout = (LinearLayout) view1;

            Class<Chip> chipClass = Chip.class;

            for (int k = 0 ; k < linearLayout.getChildCount() ; k++) {
                View view2 = linearLayout.getChildAt(k);

                if (chipClass != view2.getClass()) {
                    continue;
                }

                chips.add((Chip) view2);
            }

            break;
        }
    }

    private boolean observarOnMenuItemClick(MenuItem item) {
        if (disposable != null) {
            disposable.dispose();
        }

        disposable = viewModel.getService().getBarbeariaService().inserirServiço(bundle.getString("id"), horario).subscribe(aBoolean -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show(), throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());

        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case View.NO_ID: requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new ScheduleFragment()).commit();
                break;
            case R.id.text_input_end_icon: verQualEndIcon(view);
                break;
            case R.id.material_timepicker_ok_button: mudarHoras();
                break;
            case R.id.material_timepicker_cancel_button: removerHoras();
                break;
        }
    }

    private void verQualEndIcon(View endIconView) {
        ViewParent viewParent = endIconView.getParent();

        Class<? extends LinearLayout> classe = TextInputLayout.class;

        while (viewParent.getClass() != classe) {
            viewParent = viewParent.getParent();
        }

        for (int i = 0 ; i < binding.manageSchedule.getChildCount() ; i++) {
            View v = binding.manageSchedule.getChildAt(i);

            if (!viewParent.equals(v)) {
                continue;
            }

            bundle.putInt("selectedEndIconParentId", v.getId());
            break;
        }

        materialTimePicker.show(getChildFragmentManager(), null);
    }

    private void removerHoras() {
        String prefixo = "possivel", sufixo = bundle.getInt("selectedEndIconParentId") == binding.abertura.getId() ? "Abertura" : "Encerramento";

        for (String s : list) {
            String key = prefixo.concat(s.substring(0, 1).toUpperCase().concat(s.substring(1)).concat(sufixo));

            if (horario.containsKey(key)) {
                horario.remove(key);
            }
        }

        if (bundle.containsKey("selectedEndIconParentId")) {
            bundle.remove("selectedEndIconParentId");
        }
    }

    private void mudarHoras() {
        String prefixo = "possivel", sufixo = bundle.getInt("selectedEndIconParentId") == binding.abertura.getId() ? "Abertura" : "Encerramento", time = "";

        for (String s : list) {
            String key = prefixo.concat(s.substring(0, 1).toUpperCase().concat(s.substring(1)).concat(sufixo));

            if (horario.containsKey(key)) {
                time = TextUtils.equals(s, "hora") ? time.concat(String.valueOf(horario.get(key))) : time.concat(":"+horario.get(key));
                horario.put(s.concat(sufixo), horario.get(key));
                horario.remove(key);
            }
        }

        MaterialAutoCompleteTextView materialAutoCompleteTextView = TextUtils.equals(sufixo, "Abertura") ? binding.txtAbertura : binding.txtEncerramento;
        materialAutoCompleteTextView.setText(time);

        if (bundle.containsKey("selectedEndIconParentId")) {
            bundle.remove("selectedEndIconParentId");
        }
    }
}