package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentManageScheduleBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.viewmodel.MapViewModel;

public class ManageScheduleFragment extends DaggerFragment implements View.OnClickListener{
    private FragmentManageScheduleBinding binding;
    private Map<String, String> horario;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Disposable disposable;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
    private FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks;
    private MaterialTimePicker materialTimePicker;
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
        fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentResumed(fm, f);
                if (f.getClass().getSimpleName().equals(materialTimePicker.getClass().getSimpleName())) {
                    ConstraintLayout layout = (ConstraintLayout) f.getView();

                    for (int i = 0 ; i < layout.getChildCount() ; i++) {
                        View v = layout.getChildAt(i);

                        if (v.getId() != R.id.header_title) {
                            if (v.getId() == R.id.material_timepicker_mode_button && v.getVisibility() == View.VISIBLE) {
                                v.setVisibility(View.GONE);
                            }

                            if (v.getId() == R.id.material_timepicker_cancel_button) {
                                MaterialButton materialButton = (MaterialButton) v;

                                if (!TextUtils.equals(materialButton.getText(), "Cancelar")) {
                                    materialButton.setText("Cancelar");
                                }
                            }

                            continue;
                        }

                        MaterialTextView materialTextView = (MaterialTextView) v;

                        String txt = bundle.getInt("selectedEndIconParentId") == R.id.horaAbertura ? "Selecione a hora de abertura" : "Selecione a hora de encerramento";

                        if (!TextUtils.equals(materialTextView.getText(), txt)) {
                            materialTextView.setText(txt);
                        }
                    }
                }
            }
        };

        horario = new HashMap<>();

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

        binding.barra.setNavigationOnClickListener(this);
        binding.horaAbertura.setEndIconOnClickListener(this);
        binding.horaEncerramento.setEndIconOnClickListener(this);
        materialTimePicker.addOnPositiveButtonClickListener(this);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);
        binding.horaAbertura.setEndIconOnClickListener(null);
        binding.horaEncerramento.setEndIconOnClickListener(null);
        materialTimePicker.removeOnPositiveButtonClickListener(this);
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
        bundle.clear();
        binding.manageSchedule.removeAllViews();
        materialTimePicker.dismiss();
        horario.clear();
        bundle = null;
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
            binding.barra.setSubtitle(getDiaSemana(bundle.getInt("chosenDay")));
        }

        if (TextUtils.isEmpty(binding.barra.getTitle())) {
            binding.barra.setTitle("Editar horário");
        }

        try {
            Map<String, Object> map = viewModel.getHorários().get(bundle.getString("id")).get(String.valueOf(bundle.getInt("chosenDay")));

            for (String key : map.keySet()) {
                String s = String.valueOf(map.get(key));

                if (s.contains(".")) {
                    if (s.length() != 5) {
                        String[] chars = s.split("\\.");

                        for (int i = 0 ; i < chars.length ; i++) {
                            switch (i) {
                                case 0: s = chars[i].length() == 1 ? "0".concat(chars[i]) : chars[i];
                                    break;
                                case 1: s = s.concat(":".concat(chars[i].length() == 1 ? chars[i].concat("0") : chars[i]));
                                    break;
                            }
                        }
                    }

                } else {
                    s =  s.length() == 1 ? "0".concat(s.concat(":00")) : s.concat(":00");
                }

                switch (key) {
                    case "horaAbertura": binding.txtHoraAbertura.setText(s);
                        break;
                    case "horaEncerramento": binding.txtHoraEncerramento.setText(s);
                        break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Menu menu = binding.barra.getMenu();

        for (int i = 0 ; i < menu.size() ; i++) {
            MenuItem menuItem = menu.getItem(i);
            menuItem.setVisible(bundle.containsKey("isClosed") && bundle.getBoolean("isClosed") ? menuItem.getItemId() == R.id.encerrar || menuItem.getItemId() == R.id.inserir : menuItem.getItemId() == R.id.nao_encerrar || menuItem.getItemId() == R.id.inserir);
        }
    }

    private String getDiaSemana(int diaSemana) {
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

    private boolean observarOnMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nao_encerrar: encerrar();
                break;
            case R.id.encerrar: reabrir();
                break;
            case R.id.inserir: if (disposable != null) {
                                   disposable.dispose();
                               }

                               if (TextUtils.isEmpty(horario.get("horaAbertura")) || TextUtils.isEmpty(horario.get("horaEncerramento"))) {
                                   return false;
                               }

                               disposable = viewModel.getService().getBarbeariaService().inserirServiço(bundle.getString("id"), horario).subscribe(aBoolean -> Toast.makeText(requireActivity(), aBoolean ? "" : "", Toast.LENGTH_LONG).show(), throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());
                break;
        }

        return true;
    }

    private void reabrir() {
        for (int i = 0 ; i < binding.manageSchedule.getChildCount() ; i++) {
            View v = binding.manageSchedule.getChildAt(i);

            if (v.getId() == R.id.barra) {
                continue;
            }

            if (v.getId() == R.id.horaAbertura || v.getId() == R.id.horaEncerramento) {
                TextInputLayout textInputLayout = (TextInputLayout) v;

                for (int j = 0 ; j < textInputLayout.getChildCount() ; j++) {
                    textInputLayout.getChildAt(j).setEnabled(true);
                }
            }

            v.setEnabled(true);
        }

        Menu menu = binding.barra.getMenu();

        for (int i = 0 ; i < menu.size() ; i++) {
            MenuItem menuItem = menu.getItem(i);
            menuItem.setVisible(menuItem.getItemId() == R.id.nao_encerrar || menuItem.getItemId() == R.id.inserir);
        }

        bundle.putBoolean("isClosed", false);
    }

    private void encerrar() {
        for (int i = 0 ; i < binding.manageSchedule.getChildCount() ; i++) {
            View v = binding.manageSchedule.getChildAt(i);

            if (v.getId() == R.id.barra) {
                continue;
            }

            if (v.getId() == R.id.horaAbertura || v.getId() == R.id.horaEncerramento) {
                TextInputLayout textInputLayout = (TextInputLayout) v;

                for (int j = 0 ; j < textInputLayout.getChildCount() ; j++) {
                    textInputLayout.getChildAt(j).setEnabled(false);
                }
            }

            v.setEnabled(false);
        }

        Menu menu = binding.barra.getMenu();

        for (int i = 0 ; i < menu.size() ; i++) {
            MenuItem menuItem = menu.getItem(i);
            menuItem.setVisible(menuItem.getItemId() == R.id.encerrar || menuItem.getItemId() == R.id.inserir);
        }

        bundle.putBoolean("isClosed", true);
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
        }
    }

    private void verQualEndIcon(View endIconView) {
        for (int i = 0 ; i < binding.manageSchedule.getChildCount() ; i++) {
            View v = binding.manageSchedule.getChildAt(i);

            if (v.getId() == R.id.horaAbertura || v.getId() == R.id.horaEncerramento) {
                TextInputLayout textInputLayout = (TextInputLayout) v;

                for (int j = 0 ; j < textInputLayout.getChildCount() ; j++) {
                    ViewGroup viewGroup = (ViewGroup) textInputLayout.getChildAt(j);

                    for (int k = 0 ; k < viewGroup.getChildCount() ; k++) {
                        View vChild = viewGroup.getChildAt(k);

                        if (vChild instanceof ViewGroup) {
                            ViewGroup viewGroup1 = (ViewGroup) vChild;

                            for (int l = 0 ; l < viewGroup1.getChildCount() ; l++) {
                                View vChild1 = viewGroup1.getChildAt(l);

                                if (vChild1 instanceof ViewGroup) {
                                    ViewGroup viewGroup2 = (ViewGroup) vChild1;

                                    for (int m = 0 ; m < viewGroup2.getChildCount() ; m++) {
                                        View vChild2 = viewGroup2.getChildAt(m);

                                        if (!endIconView.equals(vChild2)) {
                                            continue;
                                        }

                                        bundle.putInt("selectedEndIconParentId", textInputLayout.getId());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        materialTimePicker.show(getChildFragmentManager(), null);
    }

    private void mudarHoras() {
        horario.put(bundle.getInt("selectedEndIconParentId") == binding.horaAbertura.getId() ? "horaAbertura" : "horaEncerramento", String.valueOf(materialTimePicker.getHour()).concat(":".concat(String.valueOf(materialTimePicker.getMinute()))));

        for (String key : horario.keySet()) {
            String s = horario.get(key);

            if (s.length() != 5) {
                String[] chars = s.split("\\.");

                for (int i = 0 ; i < chars.length ; i++) {
                    switch (i) {
                        case 0: s = chars[i].length() == 1 ? "0".concat(chars[i]) : chars[i];
                            break;
                        case 1: s = s.concat(":".concat(chars[i].length() == 1 ? chars[i].concat("0") : chars[i]));
                            break;
                    }
                }
            }

            if (!TextUtils.equals(horario.get(key), s)) {
                horario.put(key, s);
            }
        }

        System.out.println(horario);

        bundle.remove("selectedEndIconParentId");
    }
}