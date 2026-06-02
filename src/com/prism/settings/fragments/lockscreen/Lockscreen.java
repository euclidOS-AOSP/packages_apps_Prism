package com.prism.settings.fragments.lockscreen;

import android.os.Bundle;
import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat; 
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

// OmniJaws 
import com.android.internal.util.euclid.OmniJawsClient;

public class Lockscreen extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";
    private static final String KEY_SMARTSPACE = "lockscreen_smartspace_enabled"; // Added from new diff

    private Preference mWeather;
    private OmniJawsClient mWeatherClient;
    private SwitchPreferenceCompat mSmartspace; // Added from new diff

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load layout
        addPreferencesFromResource(R.xml.lockscreen_settings);
        Context context = getContext();

        // Initialize Smartspace Settings
        mSmartspace = (SwitchPreferenceCompat) findPreference(KEY_SMARTSPACE);
        if (mSmartspace != null) {
            mSmartspace.setOnPreferenceChangeListener(this);
        }

        // Initialize Weather Settings
        mWeather = findPreference(KEY_WEATHER);
        mWeatherClient = OmniJawsClient.get();
        updateWeatherSettings();

        requireActivity().setTitle(R.string.prism_lockscreen_dashboard_title);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // Handle Smartspace toggle changes dynamically
        if (preference == mSmartspace) {
            mSmartspace.setChecked((Boolean) newValue);
            updateWeatherSettings(); // Updates weather visibility/state right away
            return true;
        }
        return false;
    }

    // Handles updating the weather summary based on OmniJaws AND Smartspace state
    private void updateWeatherSettings() {
        // Updated null-check condition 
        if (mWeather == null || mSmartspace == null) return;

        // Note: Using the client instance method since we initialized it in onCreate
        boolean weatherEnabled = mWeatherClient.isOmniJawsEnabled(getContext());
        
        // Weather is allowed only if OmniJaws is active AND Smartspace is NOT checked
        boolean isWeatherConfigurable = !mSmartspace.isChecked() && weatherEnabled;

        mWeather.setEnabled(isWeatherConfigurable);
        mWeather.setSummary(isWeatherConfigurable
                ? R.string.lockscreen_weather_summary
                : R.string.lockscreen_weather_enabled_info);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateWeatherSettings();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PRISM; 
    }
}
