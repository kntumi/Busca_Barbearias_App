package kev.app.timeless.api.Location;

import android.annotation.SuppressLint;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.HashMap;

import io.reactivex.Maybe;

public class Location {
    @SuppressLint("MissingPermission")
    public static Maybe<android.location.Location> getLocation(FusedLocationProviderClient fusedLocationProviderClient, CancellationToken cancellationToken) {
        return Maybe.create(emitter -> {
            fusedLocationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationToken)
                    .addOnCompleteListener(task -> {
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(task.getResult());
                        }
                    }).addOnFailureListener(e -> {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    public static Maybe<Task<Void>> insertLocation (String id, String hash, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia").document(id).set(new HashMap<>(Collections.singletonMap("hash", hash)), SetOptions.mergeFields("hash")).addOnCompleteListener(task -> {
            if (!emitter.isDisposed()) {
                emitter.onSuccess(task);
            }
        }).addOnFailureListener(e -> {
            if (!emitter.isDisposed()) {
                emitter.onError(e);
            }
        }));
    }

    public static Maybe<Task<Void>> removeLocation (String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .update("hash", FieldValue.delete())
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task);
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }
}
