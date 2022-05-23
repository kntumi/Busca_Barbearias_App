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
import kev.app.timeless.databinding.InsertServiceBinding;
import kev.app.timeless.viewmodel.MapViewModel;

public class InsertTypeServiceFragment extends BottomSheetDialogFragment {
    private InsertServiceBinding binding;
    private MapViewModel viewModel;
    private FragmentResultListener fragmentResultListener;
    private Disposable disposable;
    private Map<String, String> mapToAdd;
    private String id, idServiço;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.insert_service, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), ((MapsActivity) requireActivity()).providerFactory).get(MapViewModel.class);
        mapToAdd = new HashMap<>();
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
        mapToAdd = null;
        id = null;
        idServiço = null;
        viewModel = null;
        binding = null;
    }

    private void observarResult(String requestKey, Bundle result) {
        id = result.containsKey("id") ? result.getString("id") : null;
        idServiço = result.containsKey("idServiço") ? result.getString("idServiço") : null;

        if (binding.inserir.hasOnClickListeners()) {
            binding.inserir.setOnClickListener(null);
        }

        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(idServiço)) {
            return;
        }

        binding.txtContexto.setText(viewModel.getServiços().containsKey(id) ? "Novo Tipo de "+ viewModel.getServiços().get(id).get(idServiço).get("nome") : "Desconhecido");
        binding.inserir.setOnClickListener(view -> {
            mapToAdd.put("nome", binding.txtNome.getText().toString());
            disposable = viewModel.getService().getBarbeariaService().inserirTipoServiço(id, idServiço, mapToAdd).doFinally(() -> binding.inserir.setEnabled(true)).doOnSubscribe(disposable -> binding.inserir.setEnabled(false)).subscribe(aBoolean -> {
                if (aBoolean) {
                    requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
                } else {
                    Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show();
                }
            }, throwable -> Toast.makeText(requireActivity(), "nn", Toast.LENGTH_LONG).show());
        });
    }
}