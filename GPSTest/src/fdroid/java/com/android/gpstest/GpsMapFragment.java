/*
 * Copyright (C) 2008-2013 The Android Open Source Project,
 * Sean J. Barbeau
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

package com.android.gpstest;

import android.content.SharedPreferences;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;

import java.util.ArrayList;

public class GpsMapFragment extends Fragment implements GpsTestListener {

    // Amount of time the user must not touch the map for the automatic camera movements to kick in
    public static final long MOVE_MAP_INTERACTION_THRESHOLD = 5 * 1000; // milliseconds

    public final static String TAG = "GpsMapFragment";

    Bundle mSavedInstanceState;

    private MapView mMap;

    RotationGestureOverlay mRotationGestureOverlay;

    Marker mMarker;

    ArrayList<OverlayItem> mOverlayItems = new ArrayList<OverlayItem>();

    //private LatLng mLatLng;

    // Camera control
    private long mLastMapTouchTime = 0;

    //private CameraPosition mlastCameraPosition;

    private boolean mGotFix;

    // User preferences for map rotation and tilt based on sensors
    private boolean mRotate;

    private boolean mTilt;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Configuration.getInstance().load(Application.get(), PreferenceManager.getDefaultSharedPreferences(Application.get()));
        mMap = new MapView(inflater.getContext());
        mMap.setMultiTouchControls(true);
        mMap.setBuiltInZoomControls(false);
        mMap.getController().setZoom(3.0f);

        mRotationGestureOverlay = new RotationGestureOverlay(mMap);
        mRotationGestureOverlay.setEnabled(true);
        mMap.getOverlays().add(mRotationGestureOverlay);

        GpsTestActivity.getInstance().addListener(this);

        return mMap;
    }

    @Override
    public void onPause() {
        super.onPause();

        mMap.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences settings = Application.getPrefs();
//        if (mMap != null) {
//            if (mMap.getMapType() != Integer.valueOf(
//                    settings.getString(getString(R.string.pref_key_map_type),
//                            String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
//            )) {
//                mMap.setMapType(Integer.valueOf(
//                        settings.getString(getString(R.string.pref_key_map_type),
//                                String.valueOf(GoogleMap.MAP_TYPE_NORMAL))
//                ));
//            }
//        }
        mRotate = settings
                .getBoolean(getString(R.string.pref_key_rotate_map_with_compass), false);
        mTilt = settings.getBoolean(getString(R.string.pref_key_tilt_map_with_sensors), true);
        mMap.onResume();
    }

    public void onClick(View v) {
    }

    public void gpsStart() {
        mGotFix = false;
    }

    public void gpsStop() {
    }

    public void onLocationChanged(Location loc) {
//        //Update real-time location on map
//        if (mListener != null) {
//            mListener.onLocationChanged(loc);
//        }
//
//        mLatLng = new LatLng(loc.getLatitude(), loc.getLongitude());
//
//        if (mMap != null) {
//            //Get bounds for detection of real-time location within bounds
//            LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
//            if (!mGotFix &&
//                    (!bounds.contains(mLatLng) ||
//                            mMap.getCameraPosition().zoom < (mMap.getMaxZoomLevel() / 2))) {
//                CameraPosition cameraPosition = new CameraPosition.Builder()
//                        .target(mLatLng)
//                        .zoom(CAMERA_INITIAL_ZOOM)
//                        .bearing(CAMERA_INITIAL_BEARING)
//                        .tilt(CAMERA_INITIAL_TILT)
//                        .build();
//
//                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//            }
//            mGotFix = true;
//        }

        GeoPoint startPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        mMap.getController().setCenter(startPoint);
        mMap.getController().setZoom(20.0f);

        if (mMarker == null) {
            mMarker = new Marker(mMap);
        }

        mMarker.setPosition(startPoint);
        mMarker.setTitle(String.format("%.6f\u00B0, %.6f\u00B0, %.1f m", loc.getLatitude(), loc.getLongitude(), loc.getAltitude()));

        if (!mMap.getOverlays().contains(mMarker)) {
            // This is the first fix when this fragment is active
            mMarker.setIcon(ContextCompat.getDrawable(Application.get(), R.drawable.ic_marker));
            mMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mMap.getOverlays().add(mMarker);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
    }

    @Override
    public void onGnssStarted() {
    }

    @Override
    public void onGnssStopped() {
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        // For performance reasons, only proceed if this fragment is visible
        if (!getUserVisibleHint()) {
            return;
        }
        if (mMap == null) {
            return;
        }

        /*
        If we have a location fix, and we have a preference to rotate the map based on sensors,
        and the user hasn't touched the map lately, then do the map camera reposition
        */
        if (mMarker != null && mRotate
                && System.currentTimeMillis() - mLastMapTouchTime
                > MOVE_MAP_INTERACTION_THRESHOLD) {
            mMap.setMapOrientation((float) -orientation);
        }
    }
}
