package kev.app.timeless.api.Authentication;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.Completable;
import io.reactivex.Maybe;

public interface AuthService {
    Completable fazerLogIn(String email, String password);
    Completable criarConta(String email, String password);
    Completable apagarConta(FirebaseUser user);
    Maybe<Boolean> reautenticar(AuthCredential authCredential);
    void fazerLogOut();
}
