package com.prism.settings.fragments.lockscreen;

import android.os.Bundle;
import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

// OmniJaws
import com.android.internal.util.euclid.OmniJawsClient;

public class Lockscreen extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    
    private static final String KEY_WEATHER = "lockscreen_weather_enabled";

    private Preference mWeather;
    private OmniJawsClient mWeatherClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.lockscreen_settings);
        Context context = getContext();

        // Initialize Weather Settings
        mWeather = findPreference(KEY_WEATHER);
        mWeatherClient = new OmniJawsClient(context);
        updateWeatherSettings();

        requireActivity().setTitle(R.string.prism_lockscreen_dashboard_title);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    // Handles updating the weather summary dynamically
    private void updateWeatherSettings() {
        if (mWeatherClient == null || mWeather == null) return;

        boolean weatherEnabled = mWeatherClient.isOmniJawsEnabled();
        mWeather.setEnabled(weatherEnabled);
        mWeather.setSummary(weatherEnabled
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
