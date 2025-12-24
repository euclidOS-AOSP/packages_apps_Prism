/*
 * Copyright (C) 2025 euclidOS
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
package com.prism.settings.utils

import android.content.Context
import android.os.Handler
import android.os.SystemProperties
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class euclidPreferenceController(context: Context) :
    AbstractPreferenceController(context) {

    private val defaultFallback =
        mContext.getString(R.string.device_info_default)

    private val handler = Handler()

    /**
     * Reads a system property and replaces '_' with spaces
     */
    private fun getProp(propName: String): String {
        return SystemProperties
            .get(propName, defaultFallback)
            .replace("_", " ")
    }

    /**
     * Reads a system property with fallback property
     * and replaces '_' with spaces
     */
    private fun getProp(propName: String, fallbackProp: String): String {
        val value = SystemProperties.get(propName)
            .ifEmpty { SystemProperties.get(fallbackProp, defaultFallback) }

        return value.replace("_", " ")
    }

    private fun getEuclidProcessor(): String =
        getProp(PROP_EUCLID_PROCESSOR, "ro.euclid.processor")

    private fun getEuclidDevice(): String =
        getProp(PROP_EUCLID_DEVICE, "ro.euclid.display.device")

    private fun getEuclidVersion(): String =
        getProp(PROP_EUCLID_VERSION, "ro.euclid.version.display")

    private fun getEuclidBuildType(): String {
        return if (
            SystemProperties.get(EUCLID_BUILD_TYPE)
                .equals("OFFICIAL", true)
        ) {
            "OFFICIAL"
        } else {
            "UNOFFICIAL"
        }
    }

    private fun getMaintainer(): String =
        getProp(PROP_EUCLID_MAINTAINER)

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val deviceInfoPreference =
            screen.findPreference<LayoutPreference>(KEY_HW_INFO)

        deviceInfoPreference?.apply {

            findViewById<TextView>(R.id.processor_summary)?.text =
                getEuclidProcessor()

            findViewById<TextView>(R.id.device_name)?.text =
                getEuclidDevice()

            findViewById<TextView>(R.id.euclid_version)?.text =
                getEuclidVersion()

            findViewById<TextView>(R.id.memory_summary)?.text =
                "${DeviceInfoUtil.getTotalRam()} / " +
                        DeviceInfoUtil.getStorageTotal(mContext)

            findViewById<TextView>(R.id.battery_summary)?.text =
                DeviceInfoUtil.getBatteryCapacity(mContext)

            findViewById<TextView>(R.id.official)?.text =
                getEuclidBuildType()

            // ✅ Maintainer (hide entire row if empty)
            val maintainerRow = findViewById<View>(R.id.maintainer)
            val maintainerText =
                findViewById<TextView>(R.id.maintainer_summary)

            val maintainerValue = getMaintainer()

            if (maintainerValue == defaultFallback || maintainerValue.isBlank()) {
                maintainerRow?.visibility = View.GONE
            } else {
                maintainerRow?.visibility = View.VISIBLE
                maintainerText?.text = maintainerValue
            }
        }
    }

    override fun isAvailable(): Boolean = true

    override fun getPreferenceKey(): String = KEY_DEVICE_INFO

    companion object {
        private const val KEY_HW_INFO = "my_device_hw_header"
        private const val KEY_DEVICE_INFO = "my_device_info_header"
        private const val KEY_BUILD_BANNER = "banner_logo"

        private const val PROP_EUCLID_DEVICE =
            "ro.euclid.display.device"
        private const val PROP_EUCLID_PROCESSOR =
            "ro.euclid.processor"
        private const val PROP_EUCLID_VERSION =
            "ro.euclid.version.display"
        private const val PROP_EUCLID_MAINTAINER =
            "ro.maintainer.name"

        private const val EUCLID_BUILD_TYPE =
            "ro.euclid.buildtype"
    }
}
