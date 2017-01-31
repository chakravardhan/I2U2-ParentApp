package com.i2u2.parent.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.i2u2.parent.R;


/**
 * QuickBlox team
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
