/*
 * Copyright 2019-2021 Google LLC, Sean Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.android.gpstest.model.GnssType
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.util.PreferenceUtil
import com.android.gpstest.util.SatelliteUtils
import com.android.gpstest.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

private const val TAG = "SharedGnssStatusManager"

/**
 * Wraps the GnssStatus updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedGnssStatusManager constructor(
    private val context: Context,
    externalScope: CoroutineScope
) {
    // State of GnssStatus
    private val _statusState = MutableStateFlow<GnssStatusState>(GnssStatusState.Stopped)
    val statusState: StateFlow<GnssStatusState> = _statusState

    // State of ongoing GNSS fix
    private val _fixState = MutableStateFlow<FixState>(FixState.NotAcquired)
    val fixState: StateFlow<FixState> = _fixState

    // State of first GNSS fix
    private val _firstFixState = MutableStateFlow<FirstFixState>(FirstFixState.NotAcquired)
    val firstFixState: StateFlow<FirstFixState> = _firstFixState

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _gnssStatusUpdates = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val callback: GnssStatus.Callback = object : GnssStatus.Callback() {
            override fun onStarted() {
                _statusState.value = GnssStatusState.Started
            }

            override fun onStopped() {
                _statusState.value = GnssStatusState.Stopped
            }

            override fun onFirstFix(ttffMillis: Int) {
                // TODO - Figure out if we should call through to both fix states here - can lead to double-triggering actions downstream
                _firstFixState.value = FirstFixState.Acquired(ttffMillis)
                _fixState.value = FixState.Acquired
            }

            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    _fixState.value = checkHaveFix(location)
                } else {
                    _fixState.value = FixState.NotAcquired
                }
                //Log.d(TAG, "New gnssStatus: ${status}")
                // Send the new location to the Flow observers
                trySend(status)
            }
        }

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) close()

        Log.d(TAG, "Starting GnssStatus updates")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.registerGnssStatusCallback(
                    ContextCompat.getMainExecutor(context),
                    callback
                )
            } else {
                locationManager.registerGnssStatusCallback(
                    callback,
                    Handler(Looper.getMainLooper())
                )
            }
        } catch (e: Exception) {
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping GnssStatus updates")
            locationManager.unregisterGnssStatusCallback(callback) // clean up when Flow collection ends
            _fixState.value = FixState.NotAcquired
            _firstFixState.value = FirstFixState.NotAcquired
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    @ExperimentalCoroutinesApi
    fun statusFlow(): Flow<GnssStatus> {
        return _gnssStatusUpdates
    }
}

private fun checkHaveFix(location: Location): FixState {
    return if (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos >
        TimeUnit.MILLISECONDS.toNanos(PreferenceUtil.minTimeMillis() * 2)
    ) {
        // We lost the GNSS fix for two requested update intervals - notify
        FixState.NotAcquired
    } else {
        // We have a GNSS fix - notify
        FixState.Acquired
    }
}

// Started/stopped states
sealed class GnssStatusState {
    object Started : GnssStatusState()
    object Stopped : GnssStatusState()
}

// GNSS ongoing fix acquired states
sealed class FixState {
    object Acquired : FixState()
    object NotAcquired : FixState()
}

// GNSS first fix state
sealed class FirstFixState {
    /**
     * [ttffMillis] the time from start of GNSS to first fix in milliseconds.
     */
    data class Acquired(val ttffMillis: Int) : FirstFixState()
    object NotAcquired : FirstFixState()
}

fun GnssStatus.toSatelliteStatus() : List<SatelliteStatus> {
    val satStatuses: MutableList<SatelliteStatus> = ArrayList()

    for (i in 0 until this.satelliteCount) {
        val satStatus = SatelliteStatus(
            this.getSvid(i),
            SatelliteUtils.getGnssConstellationType(this.getConstellationType(i)),
            this.getCn0DbHz(i),
            this.hasAlmanacData(i),
            this.hasEphemerisData(i),
            this.usedInFix(i),
            this.getElevationDegrees(i),
            this.getAzimuthDegrees(i)
        )
        if (SatelliteUtils.isCfSupported() && this.hasCarrierFrequencyHz(i)) {
            satStatus.hasCarrierFrequency = true
            satStatus.carrierFrequencyHz = this.getCarrierFrequencyHz(i)
        }
        if (satStatus.gnssType == GnssType.SBAS) {
            satStatus.sbasType = SatelliteUtils.getSbasConstellationType(satStatus.svid)
        }
        satStatuses.add(satStatus)
    }
    return satStatuses
}