package kev.app.timeless;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

import kev.app.timeless.api.Authentication.Auth;
import kev.app.timeless.ui.MapsActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("kev.app.timeless", appContext.getPackageName());
    }

    @Test
    public void testGesture () {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        FragmentScenario.launchInContainer(SupportMapFragment.class, null)
                        .moveToState(Lifecycle.State.RESUMED)
                        .onFragment(new FragmentScenario.FragmentAction<SupportMapFragment>() {
                            @Override
                            public void perform(@NonNull SupportMapFragment supportMapFragment) {

                            }
                        });

        ContextCompat.getMainExecutor(context).execute(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Test
    public void testActivity() {
        ActivityScenario<MapsActivity> activityScenario = ActivityScenario.launch(MapsActivity.class);
        activityScenario.moveToState(Lifecycle.State.STARTED).close();
    }

    @Test
    public void insertFirebaseUser() {
        ActivityScenario
                .launch(MapsActivity.class)
                .moveToState(Lifecycle.State.STARTED)
                .onActivity(activity -> {
                    SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);

                    if (!sharedPreferences.contains("id") && !sharedPreferences.contains("email")) {
                        makeLogin();
                    }

                }).moveToState(Lifecycle.State.RESUMED);
    }

    @Test
    public void logOut() {
        ActivityScenario
                .launch(MapsActivity.class)
                .moveToState(Lifecycle.State.STARTED)
                .onActivity(activity -> {
                    SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);

                    if (sharedPreferences.contains("id") && sharedPreferences.contains("email")) {
                        FirebaseAuth.getInstance().signOut();
                    }
                }).moveToState(Lifecycle.State.RESUMED);
    }

    public void makeLogin() {
        Auth.fazerLogIn(FirebaseAuth.getInstance(), "ntumi73@gmail.com", "kevinntumi").subscribe(() -> System.out.println("finished"), Throwable::printStackTrace);
    }

    @Test
    public void hasCachedUser() {
        ActivityScenario<MapsActivity> activityScenario = ActivityScenario.launch(MapsActivity.class);
        activityScenario.moveToState(Lifecycle.State.STARTED).onActivity(activity -> {
            SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
            editor.putString("id", "2").putString("email", "ntumi73@gmail.com").apply();
        }).moveToState(Lifecycle.State.RESUMED).close();
    }
}