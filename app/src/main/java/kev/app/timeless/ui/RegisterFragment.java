package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentRegisterBinding;
import kev.app.timeless.util.FragmentUtil;

public class RegisterFragment extends DaggerFragment {
    private FragmentRegisterBinding binding;
    private View.OnClickListener onClickListener;
    private Disposable disposable;
    private Map<String, Object> map;

    @Inject
    Service service;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false);
        return binding.layoutViews;
    }

    @Override
    public void onStart() {
        super.onStart();
        onClickListener = this::observarClique;
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.barra.setNavigationOnClickListener(onClickListener);
        binding.btn.setOnClickListener(onClickListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.barra.setNavigationOnClickListener(null);
        binding.btn.setOnClickListener(null);

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        map = null;
        onClickListener = null;
        binding = null;
    }

    private void observarClique(View view) {
        switch (view.getId()){
            case View.NO_ID: requireActivity().onBackPressed();
                break;
            case R.id.btn:  if (disposable != null) {
                                if (!disposable.isDisposed()) {
                                    disposable.dispose();
                                }

                                disposable = null;
                            }

                            disposable = service.getAuthService().criarConta(binding.email.getText().toString(), binding.senha.getText().toString()).timeout(10, TimeUnit.SECONDS).doOnSubscribe(disposable -> observarClique(0)).doFinally(() -> observarClique(1)).subscribe(() -> FragmentUtil.observarFragment("MapsFragment", requireActivity().getSupportFragmentManager(), R.id.layoutPrincipal), this::observarThrowable);
                break;
        }
    }

    private void observarClique(int valor) {
        switch (valor) {
            case 0:  binding.btn.setHovered(true);
                     binding.btn.setClickable(false);
                break;
            case 1:  binding.btn.setHovered(false);
                     binding.btn.setClickable(true);
                break;
        }
    }

    private void observarThrowable(Throwable throwable) {
        if (throwable instanceof FirebaseAuthException){
            System.out.println(((FirebaseAuthException) throwable).getErrorCode());
            switch (((FirebaseAuthException) throwable).getErrorCode()){
                case "ERROR_USER_NOT_FOUND": Snackbar.make(binding.layoutViews, "O e-mail inserido não foi encontrado", Snackbar.LENGTH_LONG).show();
                    break;
                case "ERROR_WRONG_PASSWORD": Snackbar.make(binding.layoutViews, "A palavra-passe não corresponde ao e-mail", Snackbar.LENGTH_LONG).show();
                    break;
                case "ERROR_INVALID_EMAIL": Snackbar.make(binding.layoutViews, "O e-mail inserido é inválido", Snackbar.LENGTH_LONG).show();
                    break;
            }
        }else if (throwable instanceof FirebaseTooManyRequestsException){
            Snackbar.make(binding.layoutViews, "Recebemos tentativas de login inválidas de si. Por favor tente de novo em breve!", Snackbar.LENGTH_LONG).show();
        }else {
            Snackbar.make(binding.layoutViews, "Não foi possivel fazer o pedido", Snackbar.LENGTH_LONG).show();
        }

        throwable.printStackTrace();
    }
}