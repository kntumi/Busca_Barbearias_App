package kev.app.timeless.api.Location;

import android.location.Location;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.Task;

import io.reactivex.Maybe;
import kev.app.timeless.model.Result;

public interface LocationService {
    Task<Location> getLocation(CancellationToken cancellationToken);
    Task<Void> insertLocation(String id, String hash);
    Task<Void> removeLocation(String id);
}
