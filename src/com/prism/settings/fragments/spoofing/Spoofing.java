package com.prism.settings.fragments.spoofing;

import com.android.internal.logging.nano.MetricsProto;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.Handler;
import android.net.Uri;
import android.util.Log;
import android.content.Intent;
import android.content.Context;
import android.content.ContentResolver;
import android.provider.Settings;
import android.os.UserHandle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.util.euclid.SystemRestartUtils;
import com.android.internal.util.euclid.KeyProviderManager;

import com.android.settings.preferences.KeyboxDataPreference;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class Spoofing extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ENABLE_SPOOF = "pi_enable_spoof";
    private static final String KEY_GMS_CERT_SPOOF = "pi_gms_cert_chain";
    private static final String KEY_GAMES_SPOOF = "pi_games_spoof";
    private static final String KEY_PHOTOS_SPOOF = "pi_photos_spoof";
    private static final String KEY_NETFLIX_SPOOF = "pi_netflix_spoof";
    private static final String KEYBOX_DATA_KEY = "keybox_data_setting";
    private static final String KEY_PIF_JSON_FILE_PREFERENCE = "pif_json_file_preference";

    private SwitchPreferenceCompat mEnableSpoof;
    private SwitchPreferenceCompat mDisableForceIntegrity;
    private SwitchPreferenceCompat mGamesSpoof;
    private SwitchPreferenceCompat mPhotosSpoof;
    private SwitchPreferenceCompat mNetflixSpoof;

    private KeyboxDataPreference mKeyboxDataPreference;
    private Preference mPifJsonFilePreference;

    private Handler mHandler;

    private ActivityResultLauncher<Intent> mKeyboxFilePickerLauncher;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.spoofing_settings, rootKey);
        getActivity().setTitle(R.string.prism_spoofing_dashboard_title);

        mHandler = new Handler();

        // Keybox picker
        mKeyboxFilePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                    result.getData() != null) {

                    Uri uri = result.getData().getData();
                    Preference pref = findPreference(KEYBOX_DATA_KEY);
                    if (pref instanceof KeyboxDataPreference) {
                        ((KeyboxDataPreference) pref).handleFileSelected(uri);
                    }

                    if (mDisableForceIntegrity != null) {
                        mDisableForceIntegrity.setEnabled(KeyProviderManager.isKeyboxAvailable());
                    }
                }
            }
        );

        // Initialize toggles
        mEnableSpoof        = findPreference(KEY_ENABLE_SPOOF);
        mDisableForceIntegrity = findPreference(KEY_GMS_CERT_SPOOF);
        mGamesSpoof         = findPreference(KEY_GAMES_SPOOF);
        mPhotosSpoof        = findPreference(KEY_PHOTOS_SPOOF);
        mNetflixSpoof       = findPreference(KEY_NETFLIX_SPOOF);

        if (mDisableForceIntegrity != null) {
            mDisableForceIntegrity.setEnabled(KeyProviderManager.isKeyboxAvailable());
        }

        mKeyboxDataPreference = findPreference(KEYBOX_DATA_KEY);
        if (mKeyboxDataPreference != null) {
            mKeyboxDataPreference.setFilePickerLauncher(mKeyboxFilePickerLauncher);
        }

        mPifJsonFilePreference = findPreference(KEY_PIF_JSON_FILE_PREFERENCE);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {

        if (pref == mPifJsonFilePreference) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            startActivityForResult(intent, 10001);
            return true;
        }

        if ("show_pif_properties".equals(pref.getKey())) {
            showPropertiesDialog();
            return true;
        }

        return super.onPreferenceTreeClick(pref);
    }

    // PIF JSON Import
    @Override
    public void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == 10001 && res == Activity.RESULT_OK) {
            Uri uri = data.getData();

            try (InputStream inputStream =
                    getActivity().getContentResolver().openInputStream(uri)) {

                if (inputStream != null) {
                    String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    JSONObject obj = new JSONObject(json);

                    for (Iterator<String> it = obj.keys(); it.hasNext();) {
                        String key = it.next();
                        String value = obj.getString(key);
                        SystemProperties.set("persist.sys.pihooks_" + key, value);
                    }
                }
            } catch (Exception e) {
                Log.e("Spoofing", "JSON error", e);
            }

            mHandler.postDelayed(() ->
                SystemRestartUtils.showSystemRestartDialog(getContext()), 1200);
        }
    }

    // Show all PI-hooked properties
    private void showPropertiesDialog() {
        try {
            JSONObject json = new JSONObject();

            String[] keys = {
                "persist.sys.pihooks_ID",
                "persist.sys.pihooks_BRAND",
                "persist.sys.pihooks_DEVICE",
                "persist.sys.pihooks_FINGERPRINT",
                "persist.sys.pihooks_MANUFACTURER",
                "persist.sys.pihooks_MODEL",
                "persist.sys.pihooks_PRODUCT",
                "persist.sys.pihooks_SECURITY_PATCH",
                "persist.sys.pihooks_DEVICE_INITIAL_SDK_INT",
                "persist.sys.pihooks_TYPE",
                "persist.sys.pihooks_TAG",
                "persist.sys.pihooks_RELEASE",
                "persist.sys.pihooks_DEBUG"
            };

            for (String key : keys) {
                String value = SystemProperties.get(key, null);
                if (value != null) {
                    json.put(key.replace("persist.sys.pihooks_", ""), value);
                }
            }

            new AlertDialog.Builder(getContext())
                .setTitle(R.string.show_pif_properties_title)
                .setMessage(json.toString(4))
                .setPositiveButton(android.R.string.ok, null)
                .show();

        } catch (JSONException e) {
            Log.e("Spoofing", "Error showing props", e);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PRISM;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    // 🔥 FULL RESET SUPPORT
    public static void reset(Context context) {
        ContentResolver resolver = context.getContentResolver();

        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PI_ENABLE_SPOOF, 1, UserHandle.USER_CURRENT);

        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PI_GMS_CERT_CHAIN, 0, UserHandle.USER_CURRENT);

        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PI_GAMES_SPOOF, 0, UserHandle.USER_CURRENT);

        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PI_PHOTOS_SPOOF, 1, UserHandle.USER_CURRENT);

        Settings.Secure.putIntForUser(resolver,
                Settings.Secure.PI_NETFLIX_SPOOF, 0, UserHandle.USER_CURRENT);

        // vbmeta update
        SystemProperties.set("persist.sys.vbmeta.update", "true");

        // Clear PIF spoof properties
        String[] pifProps = {
                "persist.sys.pihooks_ID",
                "persist.sys.pihooks_BRAND",
                "persist.sys.pihooks_DEVICE",
                "persist.sys.pihooks_FINGERPRINT",
                "persist.sys.pihooks_MANUFACTURER",
                "persist.sys.pihooks_MODEL",
                "persist.sys.pihooks_PRODUCT",
                "persist.sys.pihooks_SECURITY_PATCH",
                "persist.sys.pihooks_DEVICE_INITIAL_SDK_INT",
                "persist.sys.pihooks_TYPE",
                "persist.sys.pihooks_TAG",
                "persist.sys.pihooks_RELEASE",
                "persist.sys.pihooks_DEBUG"
        };

        for (String key : pifProps) {
            SystemProperties.set(key, "");
        }
    }
}
