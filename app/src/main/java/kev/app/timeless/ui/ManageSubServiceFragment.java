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
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.databinding.ManageSubServiceBinding;
import kev.app.timeless.viewmodel.MapViewModel;

public class ManageSubServiceFragment extends BottomSheetDialogFragment {
    private ManageSubServiceBinding binding;
    private MapViewModel viewModel;
    private FragmentResultListener fragmentResultListener;
    private Disposable disposable;
    private Map<String, Object> map;
    private Bundle bundle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.manage_sub_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsActivity) requireActivity()).providerFactory).get(MapViewModel.class);
        map = new HashMap<>();
        fragmentResultListener = this::observarResult;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

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
        fragmentResultListener = null;
        disposable = null;
        map = null;
        bundle = null;
        viewModel = null;
        binding = null;
    }

    private void observarResult(String requestKey, Bundle result) {
        bundle = result;

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (TextUtils.isEmpty(bundle.getString("id")) || TextUtils.isEmpty(bundle.getString("idServiço")) || TextUtils.isEmpty(bundle.getString("idTipoServiço")) || TextUtils.isEmpty(bundle.getString("idSubServiço"))) {
            return;
        }

        if (viewModel.getSubServiços().containsKey(bundle.getString("idTipoServiço"))) {
            if (viewModel.getSubServiços().get(bundle.getString("idTipoServiço")).containsKey(bundle.getString("idSubServiço"))) {
                for (Map.Entry<String, Object> entry : viewModel.getSubServiços().get(bundle.getString("idTipoServiço")).get(bundle.getString("idSubServiço")).entrySet()) {
                    switch (entry.getKey()) {
                        case "nome": binding.txtNome.setText(String.valueOf(entry.getValue()));
                            break;
                        case "preco": binding.txtPreco.setText(String.valueOf(entry.getValue()));
                            break;
                    }
                }
            }
        }

        binding.txtContexto.setText(viewModel.getServiços().containsKey(bundle.getString("id")) ? "Tipo de "+ viewModel.getServiços().get(bundle.getString("id")).get(bundle.getString("idServiço")).get("nome")+ " de "+viewModel.getTiposServiços().get(bundle.getString("idServiço")).get(bundle.getString("idTipoServiço")).get("nome") : "Desconhecido");
        binding.inserir.setOnClickListener(view -> {
            if (viewModel.getSubServiços().containsKey(bundle.getString("idTipoServiço"))) {
                if (viewModel.getSubServiços().get(bundle.getString("idTipoServiço")).containsKey(bundle.getString("idSubServiço"))) {
                    String nome = "", preco = "";

                    for (Map.Entry<String, Object> entry : viewModel.getSubServiços().get(bundle.getString("idTipoServiço")).get(bundle.getString("idSubServiço")).entrySet()) {
                        switch (entry.getKey()) {
                            case "nome": nome = String.valueOf(entry.getValue());
                                break;
                            case "preco": preco = String.valueOf(entry.getValue());
                                break;
                        }
                    }

                    if (TextUtils.equals(binding.txtNome.getText().toString(), nome) && TextUtils.equals(binding.txtPreco.getText(), preco)) {
                        Toast.makeText(requireActivity(), "Altere os campos para editar este tipo de "+ viewModel.getServiços().get(bundle.getString("id")).get(bundle.getString("idServiço")).get("nome").toString().toLowerCase()+ " de "+viewModel.getTiposServiços().get(bundle.getString("idServiço")).get(bundle.getString("idTipoServiço")).get("nome").toString().toLowerCase(), Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }

            map.put("nome", binding.txtNome.getText().toString());
            map.put("preco", binding.txtPreco.getText().toString());
            disposable = viewModel.getService().getBarbeariaService().editarSubServiço(bundle.getString("id"), bundle.getString("idServiço"), bundle.getString("idTipoServiço"), bundle.getString("idSubServiço") , map).doFinally(() -> binding.inserir.setEnabled(true)).doOnSubscribe(disposable -> binding.inserir.setEnabled(false))
                    .subscribe(aBoolean -> {
                        if (aBoolean) {
                            requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
                        } else {
                            Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show();
                        }
                    }, throwable -> Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show());
        });
    }
}