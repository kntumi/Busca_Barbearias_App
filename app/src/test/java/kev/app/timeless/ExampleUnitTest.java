package kev.app.timeless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void verifyIsNull() {
        String hash = "";
        GeoLocation location = hash == null || hash.isEmpty() ? null : GeoHash.locationFromHash(hash);
        assertNotNull(location);
    }

    @Test
    public void sharedPreferenceTest() {
        Class<SharedPreferences.Editor> editorClass = SharedPreferences.Editor.class;

        SharedPreferences.Editor editor = null;

        String methodToSearch = "putString";

        for (Method method : editorClass.getDeclaredMethods()) {
            if (!TextUtils.equals(method.getName(), methodToSearch)) {
                continue;
            }

            try {
                //editor = (SharedPreferences.Editor) method.invoke(null, );
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println(method.getName());
            break;
        }

        editor.apply();
    }

    @Test
    public void testWhile () {
        int i = 0;

        while (i < 2) {
            System.out.println(i);
            i++;
        }


    }
}