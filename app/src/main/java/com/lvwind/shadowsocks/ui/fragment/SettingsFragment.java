package com.lvwind.shadowsocks.ui.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import com.lvwind.shadowsocks.R;

/**
 * Created by LvWind on 16/6/27.
 */
public class SettingsFragment extends PreferenceFragment {

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
}
