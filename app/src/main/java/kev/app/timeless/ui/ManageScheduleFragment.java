package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
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
    private Map<String, Double> horario;
    private Bundle bundle;
    private FragmentResultListener parentResultListener;
    private Disposable disposable;
    private Toolbar.OnMenuItemClickListener onMenuItemClickListener;
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
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

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
        horario.clear();
        bundle = null;
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

            if (map.size() == 0) {

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Menu menu = binding.barra.getMenu();

        for (int i = 0 ; i < menu.size() ; i++) {
            MenuItem menuItem = menu.getItem(i);

            if (bundle.containsKey("isClosed") && bundle.getBoolean("isClosed")) {
                if (menuItem.getItemId() != R.id.encerrar) {
                    continue;
                }

                menuItem.setVisible(true);
                break;
            }

            if (menuItem.getItemId() != R.id.nao_encerrar) {
                continue;
            }

            menuItem.setVisible(true);
            break;
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
            case R.id.nao_encerrar:
                break;
            case R.id.encerrar:
                break;
            case R.id.inserir:
                break;
        }

        return true;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == View.NO_ID) {
            requireParentFragment().getChildFragmentManager().beginTransaction().replace(R.id.layoutPrincipal, new ScheduleFragment()).commit();
        }
    }
}
