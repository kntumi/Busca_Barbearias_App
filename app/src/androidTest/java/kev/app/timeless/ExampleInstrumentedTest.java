package kev.app.timeless;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.microsoft.maps.MapPermissionsDelegate;
import com.microsoft.maps.MapPermissionsRequestArgs;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapStyleSheets;
import com.microsoft.maps.MapView;

import org.junit.Test;
import org.junit.runner.RunWith;

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
        ContextCompat.getMainExecutor(InstrumentationRegistry.getInstrumentation().getTargetContext()).execute(new Runnable() {
            @Override
            public void run() {
                ConstraintLayout layout = new ConstraintLayout(InstrumentationRegistry.getInstrumentation().getTargetContext());

                MapView mapView = new MapView(InstrumentationRegistry.getInstrumentation().getTargetContext(), MapRenderMode.RASTER);
                mapView.setCredentialsKey("AnvR3YzuwrdaDnBhRT7Anhep_nPYeeW4fQbzC6pp9GnoMx93h6DksUTXqDOlui0r");
                mapView.setMapStyleSheet(MapStyleSheets.roadLight());

                layout.addView(mapView);
                mapView.setOnTouchListener((view, motionEvent) -> {
                    System.out.println(motionEvent.getAction());

                    switch(motionEvent.getAction()) {
                        case (MotionEvent.ACTION_DOWN) : System.out.println("Action was DOWN");
                            return true;
                        case (MotionEvent.ACTION_MOVE) : System.out.println("Action was MOVE");
                            return true;
                        case (MotionEvent.ACTION_UP) : System.out.println("Action was UP");
                            return true;
                        case (MotionEvent.ACTION_CANCEL) : System.out.println("Action was CANCEL");
                            return true;
                        case (MotionEvent.ACTION_OUTSIDE) : System.out.println("Movement occurred outside bounds " + "of current screen element");
                            return true;
                        default : return view.onTouchEvent(motionEvent);
                    }
                });
            }
        });
    }
}