package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.FragmentInsertScheduleBinding;
import kev.app.timeless.model.User;
import kev.app.timeless.viewmodel.MapViewModel;

public class InsertScheduleFragment extends BottomSheetDialogFragment implements View.OnClickListener{
    private FragmentInsertScheduleBinding binding;
    private Observer<List<User>> observer;
    private MapViewModel viewModel;
    private List<Float> values;
    private Map<String, Double> horario;
    private PopupMenu popupMenu;
    private Disposable disposable;
    private String id;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_insert_schedule, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = this::observarUser;
        horario = new HashMap<>();
        popupMenu = new PopupMenu(requireActivity(), binding.txtDiaSemana);
        popupMenu.inflate(R.menu.dia_semana);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsFragment) requireParentFragment().requireParentFragment().requireParentFragment()).providerFactory).get(MapViewModel.class);
        values = new ArrayList<>();
        binding.txtDiaSemana.setText(TextUtils.isEmpty(binding.txtDiaSemana.getText()) ? diaSemana() : binding.txtDiaSemana.getText());
        inicializarValores();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.txtDiaSemana.setOnClickListener(this);
        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
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
                slider.setValues(binding.slider.getValues().get(0), 21.55f);
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
                        case 0: horaAbertura = s.concat("0");
                            break;
                        case 1: horaEncerramento = s.concat("0");
                            break;
                    }
                } else {
                    switch (slider.getValues().indexOf(hora)) {
                        case 0: horaAbertura = s;
                            break;
                        case 1: horaEncerramento = s;
                            break;
                    }
                }
            }

            horario.put("horaAbertura", Double.parseDouble(String.valueOf(slider.getValues().get(0))));
            horario.put("horaEncerramento", Double.parseDouble(String.valueOf(slider.getValues().get(1))));
            binding.txtHorario.setText("Aberto das ".concat(horaAbertura.replace(".", ":").concat(" até ").concat(horaEncerramento.replace(".", ":"))));
        });
        popupMenu.setOnMenuItemClickListener(item -> {
            binding.txtDiaSemana.setText(item.getTitle());
            int dia = 0;

            switch (item.getTitle().toString()) {
                case "Domingo": dia = Calendar.SUNDAY;
                    break;
                case "Segunda-Feira": dia = Calendar.MONDAY;
                    break;
                case "Terça-Feira": dia = Calendar.TUESDAY;
                    break;
                case "Quarta-Feira": dia = Calendar.WEDNESDAY;
                    break;
                case "Quinta-Feira": dia = Calendar.THURSDAY;
                    break;
                case "Sexta-Feira": dia = Calendar.FRIDAY;
                    break;
                case "Sábado": dia = Calendar.SATURDAY;
                    break;
            }

            if (viewModel.getHorários().containsKey(id)) {
                if (viewModel.getHorários().get(id).containsKey(String.valueOf(dia))) {
                    String horaAbertura = "", horaEncerramento = "";



                    horario.put("horaAbertura", Double.parseDouble(horaAbertura));
                    horario.put("horaEncerramento", Double.parseDouble(horaEncerramento));
                    binding.txtHorario.setText("Aberto das ".concat(horaAbertura.replace(".", ":").concat(" até ").concat(horaEncerramento.replace(".", ":"))));
                    return true;
                }
            }

            inicializarValoresDefault();

            return true;
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.txtDiaSemana.setOnClickListener(null);
        binding.slider.clearOnChangeListeners();
        popupMenu.setOnMenuItemClickListener(null);

        if (disposable != null) {
            disposable.dispose();
        }

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable = null;
        horario.clear();
        popupMenu.dismiss();
        horario.clear();
        horario = null;
        popupMenu = null;
        viewModel = null;
        id = null;
        observer = null;
        binding = null;
    }

    private void observarUser(List<User> users) {
        id = users.size() == 0 ? null : users.get(0).getId();

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (TextUtils.isEmpty(id)) {
            return;
        }

        binding.inserir.setOnClickListener(this);

        if (viewModel.getHorários().containsKey(id)) {
            String diaSemana = String.valueOf(obterDiaSemanaNaString());

            if (viewModel.getHorários().get(id).containsKey(diaSemana)) {

               List<Float> floats = new ArrayList<>();



               String horaAbertura = "", horaEncerramento = "";

               for (Float hora : floats) {
                   String s = String.valueOf(hora);

                   if (s.length() - s.indexOf(".") == 2) {
                       switch (floats.indexOf(hora)) {
                           case 0:
                               horaAbertura = s.concat("0");
                               break;
                           case 1:
                               horaEncerramento = s.concat("0");
                               break;
                       }
                   } else {
                       switch (floats.indexOf(hora)) {
                           case 0:
                               horaAbertura = s;
                               break;
                           case 1:
                               horaEncerramento = s;
                               break;
                       }
                   }
               }

               horario.put("horaAbertura", Double.parseDouble(horaAbertura));
               horario.put("horaEncerramento", Double.parseDouble(horaEncerramento));
               binding.txtHorario.setText("Aberto das ".concat(horaAbertura.replace(".", ":").concat(" até ").concat(horaEncerramento.replace(".", ":"))));
               binding.slider.setValues(floats);
               return;
            }
        }

        inicializarValoresDefault();
    }

    private void inicializarValoresDefault() {
        binding.txtHorario.setText("Desconhecido");
        horario.put("horaAbertura", Double.parseDouble(String.valueOf(7.0f)));
        horario.put("horaEncerramento", Double.parseDouble(Float.toString(21.55f)));
        binding.slider.setValues(7f, 21.55f);
    }

    private void inicializarValores() {
        for (int i = 7 ; i < 22 ; i++) {
            for (int j = 0 ; j <= 55 ; j++) {
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

    private String diaSemana() {
        switch (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
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

    @Override
    public void onClick(View view) {
        System.out.println("Horarss "+horario);
        switch (view.getId()) {
            case R.id.inserir: if (disposable != null) {
                                   disposable.dispose();
                               }

                               disposable = viewModel.getService().getBarbeariaService().inserirHorario(id, obterDiaSemanaNaString(), horario)
                                        .doOnSubscribe(disposable -> binding.inserir.setEnabled(false))
                                       .doOnError(throwable -> binding.inserir.setEnabled(true))
                                        .subscribe(aBoolean -> {
                                            if (aBoolean) {
                                                requireParentFragment().getChildFragmentManager().beginTransaction().remove(InsertScheduleFragment.this).commit();
                                            } else {
                                                Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
                                                binding.inserir.setEnabled(true);
                                            }
                                        }, Throwable::printStackTrace);
                break;
            case R.id.txtDiaSemana: popupMenu.show();
                break;
        }
    }

    private Integer obterDiaSemanaNaString () {
        switch (binding.txtDiaSemana.getText().toString()) {
            case "Domingo": return Calendar.SUNDAY;
            case "Segunda-Feira": return Calendar.MONDAY;
            case "Terça-Feira": return Calendar.TUESDAY;
            case "Quarta-Feira": return Calendar.WEDNESDAY;
            case "Quinta-Feira": return Calendar.THURSDAY;
            case "Sexta-Feira": return Calendar.FRIDAY;
            case "Sábado": return Calendar.SATURDAY;
        }

        return 0;
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }
}