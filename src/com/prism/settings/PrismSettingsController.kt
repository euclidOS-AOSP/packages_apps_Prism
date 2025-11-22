/*
 * Copyright (C) 2024-2025 MistOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prism.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class PrismSettingsController(context: Context) : AbstractPreferenceController(context) {

    // Cache for findViewById results to improve performance
    private val viewCache = mutableMapOf<Int, View?>()

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<LayoutPreference>(KEY_PRISM)?.let { prismPref ->
            setupPrismClickListeners(prismPref)
        }
    }

    private fun setupPrismClickListeners(preference: LayoutPreference) {
        val prismClickMap = mapOf<Int, String>(
            R.id.themes to "com.android.settings.Settings\$PrismThemesActivity",
            R.id.lockscreen to "com.android.settings.Settings\$PrismLockscreenActivity",
            R.id.controll to "com.android.settings.Settings\$PrismQuickSettingsActivity",
            R.id.statusbar to "com.android.settings.Settings\$PrismStatusBarActivity"
        )
        prismClickMap.forEach { (viewId, activityName) ->
            // Use cached findViewById to improve performance
            val view = viewCache.getOrPut(viewId) { 
                preference.findViewById<View>(viewId) 
            }
            view?.setOnClickListener {
                try {
                    mContext.startActivity(createIntent(activityName))
                } catch (e: Exception) {
                    // Graceful error handling for missing activities
                    android.util.Log.w("PrismController", "Failed to start activity: $activityName", e)
                }
            }
        }
    }

    private fun createIntent(activityName: String): Intent {
        return Intent().setComponent(ComponentName("com.android.settings", activityName))
    }

    override fun isAvailable(): Boolean = true

    override fun getPreferenceKey(): String = KEY_PRISM

    companion object {
        private const val KEY_PRISM = "prism_dashboard_quick_access"
    }
}
