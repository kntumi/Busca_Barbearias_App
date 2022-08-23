package kev.app.timeless.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.FragmentLoginBinding;
import kev.app.timeless.util.FragmentUtil;

public class LoginFragment extends DaggerFragment{
    private FragmentLoginBinding binding;
    private View.OnClickListener onClickListener;
    private CompositeDisposable compositeDisposable;

    @Inject
    Service service;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);
        return binding.layoutViews;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {

        }

        compositeDisposable = new CompositeDisposable();
        onClickListener = this::observarClick;
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.btn.setOnClickListener(onClickListener);
        binding.textoConta.setOnClickListener(onClickListener);
        binding.barra.setNavigationOnClickListener(onClickListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.btn.setOnClickListener(null);
        binding.textoConta.setOnClickListener(null);
        binding.barra.setNavigationOnClickListener(null);
        compositeDisposable.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.layoutViews.removeAllViews();
        compositeDisposable = null;
        onClickListener = null;
        binding = null;
    }

    private void observarClick(View view) {
        switch (view.getId()){
            case R.id.btn: compositeDisposable.add(service.getAuthService().fazerLogIn(binding.txtEmail.getText().toString().trim(), binding.txtSenha.getText().toString().trim()).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe(disposable -> binding.btn.setHovered(true)).doFinally(() -> binding.btn.setHovered(false)).subscribe(() -> FragmentUtil.observarFragment("MapsFragment", requireActivity().getSupportFragmentManager(), R.id.layoutPrincipal), this::observarThrowable));
                break;
            case View.NO_ID: requireActivity().onBackPressed();
                break;
            case R.id.textoConta: FragmentUtil.observarFragment("RegisterFragment", requireActivity().getSupportFragmentManager(), R.id.layoutPrincipal);
                break;
        }
    }

    private void observarThrowable(Throwable throwable) {
        if (throwable instanceof FirebaseAuthException) {
            FirebaseAuthException firebaseAuthException = (FirebaseAuthException) throwable;

            switch (firebaseAuthException.getErrorCode()){
                case "ERROR_USER_NOT_FOUND": Snackbar.make(binding.layoutViews, "O e-mail inserido não foi encontrado", Snackbar.LENGTH_LONG).show();
                    break;
                case "ERROR_WRONG_PASSWORD": Snackbar.make(binding.layoutViews, "A palavra-passe não corresponde ao e-mail", Snackbar.LENGTH_LONG).show();
                    break;
                case "ERROR_INVALID_EMAIL": Snackbar.make(binding.layoutViews, "O e-mail inserido é inválido", Snackbar.LENGTH_LONG).show();
                    break;
            }

            return;
        }

        if (throwable instanceof FirebaseTooManyRequestsException){
            Snackbar.make(binding.layoutViews, "Recebemos tentativas de login inválidas de si. Por favor tente de novo em breve!", Snackbar.LENGTH_LONG).show();
        }else {
            Snackbar.make(binding.layoutViews, "Não foi possivel fazer o pedido", Snackbar.LENGTH_LONG).show();
        }

        throwable.printStackTrace();
    }
}