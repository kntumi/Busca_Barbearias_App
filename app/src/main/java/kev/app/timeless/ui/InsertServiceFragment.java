package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.InsertServiceBinding;
import kev.app.timeless.model.User;
import kev.app.timeless.viewmodel.MapViewModel;

public class InsertServiceFragment extends BottomSheetDialogFragment {
    private InsertServiceBinding binding;
    private MapViewModel viewModel;
    private Observer<List<User>> observer;
    private Disposable disposable;
    private Map<String, String> map;
    private String id;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.insert_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsActivity) requireActivity()).providerFactory).get(MapViewModel.class);
        observer = this::observarUser;
        map = new HashMap<>();
        binding.txtContexto.setText("Novo Tipo de Serviço");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable = null;
        map = null;
        id = null;
        observer = null;
        viewModel = null;
        binding = null;
    }

    private void observarUser(List<User> users) {
        id = users.size() != 0 ? users.get(0).getId() : null;

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (TextUtils.isEmpty(binding.txtNome.getText())) {
            binding.txtNome.setText("Desconhecido");
        }

        if (TextUtils.isEmpty(id)) {
            return;
        }

        binding.inserir.setOnClickListener(view -> {
            map.put("nome", binding.txtNome.getText().toString());
            disposable = viewModel.getService().getBarbeariaService().inserirServiço(id, map).doFinally(() -> binding.inserir.setEnabled(true)).doOnSubscribe(disposable -> binding.inserir.setEnabled(false)).subscribe(aBoolean -> {
                if (aBoolean) {
                    requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
                } else {
                    Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show();
                }
            }, throwable -> Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show());
        });
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }
}