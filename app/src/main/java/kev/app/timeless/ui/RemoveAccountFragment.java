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

import com.google.firebase.auth.EmailAuthProvider;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.RemoveAccountBinding;

public class RemoveAccountFragment extends DaggerFragment {
    private RemoveAccountBinding binding;
    private View.OnClickListener onClickListener;
    private Bundle bundle;
    private List<Disposable> disposables;
    private FragmentResultListener parentResultListener;

    @Inject
    Service service;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.remove_account, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        parentResultListener = this::observarParent;
        disposables = new ArrayList<>();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        if (binding.btnAceitar.hasOnClickListeners()) {
            binding.btnAceitar.setOnClickListener(null);
        }

        if (binding.btnRejeitar.hasOnClickListeners()) {
            binding.btnRejeitar.setOnClickListener(null);
        }

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        disposables.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bundle = null;
        parentResultListener = null;
        onClickListener = null;
        binding.layoutPrincipal.removeAllViews();
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        bundle = savedInstanceState == null ? new Bundle() : new Bundle(savedInstanceState.getBundle("bundle"));

        if (bundle.size() != 0) {
            observarParent(getClass().getSimpleName(), bundle);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    private void observarParent(String requestKey, Bundle result) {
        if (bundle.size() == 0) {
            bundle = result;
        }

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        disposables.clear();

        if (binding.btnRejeitar.hasOnClickListeners()) {
            binding.btnRejeitar.setOnClickListener(null);
        }

        if (binding.btnAceitar.hasOnClickListeners()) {
            binding.btnAceitar.setOnClickListener(null);
        }

        if (TextUtils.isEmpty(bundle.getString("email"))) {
            return;
        }

        if (onClickListener == null) {
            onClickListener = this::observarClique;
        }

        binding.btnAceitar.setOnClickListener(onClickListener);
        binding.btnRejeitar.setOnClickListener(onClickListener);
    }

    private void observarClique(View view) {
        if (view.getId() == R.id.btnRejeitar) {
            requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
            return;
        }

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }

        disposables.clear();

        if (TextUtils.isEmpty(binding.txtSenha.getText())) {
            Toast.makeText(requireActivity(), "Por favor preencha o campo acima!", Toast.LENGTH_LONG).show();
            return;
        }

        disposables.add(service.getAuthService().reautenticar(EmailAuthProvider.getCredential(bundle.getString("email"), binding.txtSenha.getText().toString())).doOnSubscribe(disposable -> mudarEstadoBtns(false)).doFinally(() -> mudarEstadoBtns(true)).subscribe(this::observarResposta, this::observarErro));
    }

    private void mudarEstadoBtns(boolean estado) {
        binding.btnAceitar.setEnabled(estado);
    }

    private void observarResposta(Boolean aBoolean) {
        if (!aBoolean) {
            Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show();
            return;
        }

        disposables.add(service.getAuthService().apagarConta(service.getAuth().getCurrentUser()).subscribe(() -> requireParentFragment().getChildFragmentManager().beginTransaction().remove(this).commit()));
    }

    private void observarErro(Throwable throwable) {
        throwable.printStackTrace();
    }
}