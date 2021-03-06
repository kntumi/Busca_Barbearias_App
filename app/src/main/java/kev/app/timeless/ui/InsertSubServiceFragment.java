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
import kev.app.timeless.databinding.InsertSubServiceBinding;
import kev.app.timeless.viewmodel.MapViewModel;

public class InsertSubServiceFragment extends BottomSheetDialogFragment {
    private InsertSubServiceBinding binding;
    private MapViewModel viewModel;
    private FragmentResultListener fragmentResultListener;
    private Disposable disposable;
    private Map<String, Object> map;
    private Bundle bundle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.insert_sub_service, container, false);
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

        if (TextUtils.isEmpty(bundle.getString("id")) || TextUtils.isEmpty(bundle.getString("idServi??o")) || TextUtils.isEmpty(bundle.getString("idTipoServi??o"))) {
            return;
        }

        binding.txtContexto.setText(viewModel.getServi??os().containsKey(bundle.getString("id")) ? "Novo Tipo de "+ viewModel.getServi??os().get(bundle.getString("id")).get(bundle.getString("idServi??o")).get("nome")+ " de "+viewModel.getTiposServi??os().get(bundle.getString("idServi??o")).get(bundle.getString("idTipoServi??o")).get("nome") : "Desconhecido");
        binding.inserir.setOnClickListener(view -> {
            map.put("nome", binding.txtNome.getText().toString());
            map.put("preco", binding.txtPreco.getText().toString());
            disposable = viewModel.getService().getBarbeariaService().inserirSubServi??o(bundle.getString("id"), bundle.getString("idServi??o"), bundle.getString("idTipoServi??o"), map).doFinally(() -> binding.inserir.setEnabled(true)).doOnSubscribe(disposable -> binding.inserir.setEnabled(false))
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