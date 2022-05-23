package kev.app.timeless.api.Authentication;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.Completable;
import io.reactivex.Maybe;

public class Auth {
    public static Completable fazerLogIn(FirebaseAuth firebaseAuth, String email, String password){
        return Completable.create(emitter -> firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> emitter.onComplete()).addOnFailureListener(emitter::onError));
    }

    public static void fazerLogOut(FirebaseAuth firebaseAuth){
        firebaseAuth.signOut();
    }

    public static Completable criarConta(FirebaseAuth firebaseAuth, String email, String password) {
        return Completable.create(emitter -> firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> emitter.onComplete()).addOnFailureListener(emitter::onError));
    }

    public static Completable apagarConta(FirebaseUser user) {
        return Completable.create(emitter -> user.delete().addOnSuccessListener(aVoid -> {
            if (!emitter.isDisposed()) {
                emitter.onComplete();
            }
        }).addOnFailureListener(e -> {
            if (!emitter.isDisposed()) {
                emitter.onError(e);
            }
        }));
    }

    public static Maybe<Boolean> reautenticar(FirebaseAuth firebaseAuth, AuthCredential authCredential) {
        return Maybe.create(emitter -> {
            if (firebaseAuth.getCurrentUser() == null) {
                return;
            }

            firebaseAuth.getCurrentUser().reauthenticate(authCredential)
                    .addOnCompleteListener(task -> {
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(task.isSuccessful());
                        }
                    }).addOnFailureListener(e -> {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    });
        });
    }
}