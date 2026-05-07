package com.prism.settings.fragments.spoofing;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.euclid.PixelPropsUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class Spoofing extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "Spoofing";

    private static final String KEY_IDENTITY_CATEGORY = "spoofing_identity_category";
    private static final String KEY_FEATURES_CATEGORY = "spoofing_features_category";
    private static final String KEY_APP_SPECIFIC_CATEGORY = "spoofing_app_specific_category";

    private static final String PI_PP_SPOOF = "pi_pp_spoof";
    private static final String PI_PHOTOS_SPOOF = "pi_photos_spoof";
    private static final String PI_SNAPCHAT_SPOOF = "pi_snapchat_spoof";
    private static final String KEY_TENSOR_TARGETS = "tensor_targets_settings";

    private static final String PHOTOS_PACKAGE = "com.google.android.apps.photos";
    private static final String SNAPCHAT_PACKAGE = "com.snapchat.android";
    private static final String VENDING_PACKAGE = "com.android.vending";

    private PreferenceCategory mIdentityCategory;
    private PreferenceCategory mFeaturesCategory;
    private PreferenceCategory mAppSpecificCategory;

    private SwitchPreferenceCompat mGoogleSpoof;
    private SwitchPreferenceCompat mPhotosSpoof;
    private SwitchPreferenceCompat mSnapchatSpoof;
    private Preference mTensorTargets;

    private Handler mHandler;
    private Runnable mPendingKill;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.spoofing_settings);
        requireActivity().setTitle(R.string.prism_spoofing_dashboard_title);

        mHandler = new Handler(Looper.getMainLooper());

        if (PixelPropsUtils.isCustomForkBuild()) {
            if (getPreferenceScreen() != null) {
                getPreferenceScreen().removeAll();
            }
            return;
        }

        mIdentityCategory = findPreference(KEY_IDENTITY_CATEGORY);
        mFeaturesCategory = findPreference(KEY_FEATURES_CATEGORY);
        mAppSpecificCategory = findPreference(KEY_APP_SPECIFIC_CATEGORY);

        mGoogleSpoof = findPreference(PI_PP_SPOOF);
        mPhotosSpoof = findPreference(PI_PHOTOS_SPOOF);
        mSnapchatSpoof = findPreference(PI_SNAPCHAT_SPOOF);
        mTensorTargets = findPreference(KEY_TENSOR_TARGETS);

        if (PixelPropsUtils.isMainlinePixelDevice()) {
            if (mIdentityCategory != null && mGoogleSpoof != null) {
                mIdentityCategory.removePreference(mGoogleSpoof);
            }
        } else if (mGoogleSpoof != null) {
            mGoogleSpoof.setOnPreferenceChangeListener(this);
        }

        if (mTensorTargets != null && PixelPropsUtils.isTensorPixelDevice()) {
            if (mFeaturesCategory != null) {
                mFeaturesCategory.removePreference(mTensorTargets);
            }
        }

        mPhotosSpoof = initAppSpoof(mPhotosSpoof, PHOTOS_PACKAGE);
        mSnapchatSpoof = initAppSpoof(mSnapchatSpoof, SNAPCHAT_PACKAGE);
    }

    private SwitchPreferenceCompat initAppSpoof(SwitchPreferenceCompat pref, String pkg) {
        if (pref == null) return null;

        try {
            requireContext().getPackageManager().getPackageInfo(pkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            if (mAppSpecificCategory != null) {
                mAppSpecificCategory.removePreference(pref);
            }
            return null;
        }

        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGoogleSpoof) {
            scheduleKill(null);
        } else if (preference == mPhotosSpoof) {
            scheduleKill(PHOTOS_PACKAGE);
        } else if (preference == mSnapchatSpoof) {
            scheduleKill(SNAPCHAT_PACKAGE);
        }

        return true;
    }

    private void scheduleKill(String pkg) {
        if (mPendingKill != null) {
            mHandler.removeCallbacks(mPendingKill);
        }

        Toast.makeText(
                getContext(),
                R.string.spoofing_applying_changes,
                Toast.LENGTH_SHORT
        ).show();

        mPendingKill = () -> {
            if (pkg == null) {
                killGooglePackages();
            } else {
                killIfRunning(pkg);
            }
        };

        mHandler.postDelayed(mPendingKill, 500);
    }

    private void killGooglePackages() {
        try {
            PackageManager pm = requireContext().getPackageManager();

            for (ApplicationInfo app : pm.getInstalledApplications(0)) {
                if (app.packageName.startsWith("com.google")) {
                    killIfRunning(app.packageName);
                }
            }

            killIfRunning(VENDING_PACKAGE);
        } catch (Exception e) {
            Log.e(TAG, "Failed to kill Google packages", e);
        }
    }

    private void killIfRunning(String pkg) {
        try {
            ActivityManager am =
                    (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);

            if (am == null) return;

            for (ActivityManager.RunningAppProcessInfo proc : am.getRunningAppProcesses()) {
                if (proc.pkgList == null) continue;

                for (String p : proc.pkgList) {
                    if (pkg.equals(p)) {
                        am.forceStopPackage(pkg);
                        Log.d(TAG, "Killed " + pkg);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to kill " + pkg, e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mHandler != null && mPendingKill != null) {
            mHandler.removeCallbacks(mPendingKill);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.PRISM;
    }
}
