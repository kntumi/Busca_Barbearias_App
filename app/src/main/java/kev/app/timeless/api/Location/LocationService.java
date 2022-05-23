package kev.app.timeless.api.Location;

import android.location.Location;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;

import io.reactivex.Maybe;

public interface LocationService {
    Maybe<Location> getLocation(CancellationToken cancellationToken);
    Maybe<Task<Void>> insertLocation(String id, String hash);
    Maybe<Task<Void>> removeLocation(String id);
}
