package kev.app.timeless.api.Location;

import android.annotation.SuppressLint;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Collections;
import java.util.HashMap;

public class Location {
    @SuppressLint("MissingPermission")
    public static Task<android.location.Location> getLocation(FusedLocationProviderClient fusedLocationProviderClient, CancellationToken cancellationToken) {
        return fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken);
    }

    public static Task<Void> insertLocation (String id, String hash, FirebaseFirestore firestore) {
        return firestore.collection("Barbearia").document(id).set(new HashMap<>(Collections.singletonMap("hash", hash)), SetOptions.mergeFields("hash"));
    }

    public static Task<Void> removeLocation (String id, FirebaseFirestore firestore) {
        return firestore.collection("Barbearia").document(id).update("hash", FieldValue.delete());
    }
}