package kev.app.timeless.ui;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import kev.app.timeless.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }
}