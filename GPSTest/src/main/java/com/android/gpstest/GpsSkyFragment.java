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

import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.gpstest.view.GpsSkyView;

import java.util.LinkedList;
import java.util.List;

public class GpsSkyFragment extends Fragment implements GpsTestListener {

    public final static String TAG = "GpsSkyFragment";

    private GpsSkyView mSkyView;

    private List<View> mLegendLines;

    private List<ImageView> mLegendShapes;

    private TextView mLegendCn0Title, mLegendCn0Units, mLegendCn0LeftText, mLegendCn0CenterText, mLegendCn0RightText;

    private boolean mUseLegacyGnssApi = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.gps_sky, container,false);

        mSkyView = v.findViewById(R.id.sky_view);

        initLegendViews(v);

        GpsTestActivity.getInstance().addListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        int color;
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            // Dark theme
            color = getResources().getColor(android.R.color.secondary_text_dark);
        } else {
            // Light theme
            color = getResources().getColor(R.color.body_text_2_light);
        }
        for (View v : mLegendLines) {
            v.setBackgroundColor(color);
        }
        for (ImageView v: mLegendShapes) {
            v.setColorFilter(color);
        }
    }

    public void onLocationChanged(Location loc) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void gpsStart() {
    }

    public void gpsStop() {
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        mSkyView.setGnssStatus(status);
        mUseLegacyGnssApi = false;
        updateCn0LegendText();
    }

    @Override
    public void onGnssStarted() {
        mSkyView.setStarted();
    }

    @Override
    public void onGnssStopped() {
        mSkyView.setStopped();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        mSkyView.setGnssMeasurementEvent(event);
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                mSkyView.setStarted();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                mSkyView.setStopped();
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                mSkyView.setSats(status);
                mUseLegacyGnssApi = true;
                updateCn0LegendText();
                break;
        }
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        // For performance reasons, only proceed if this fragment is visible
        if (!getUserVisibleHint()) {
            return;
        }

        if (mSkyView != null) {
            mSkyView.onOrientationChanged(orientation, tilt);
        }
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    }

    /**
     * Initialize the views in the C/N0 and Shape legends
     * @param v view in which the legend view IDs can be found via view.findViewById()
     */
    private void initLegendViews(View v) {
        if (mLegendLines == null) {
            mLegendLines = new LinkedList<>();
        } else {
            mLegendLines.clear();
        }

        if (mLegendShapes == null) {
            mLegendShapes = new LinkedList<>();
        } else {
            mLegendShapes.clear();
        }

        // C/N0 Legend lines
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_left_line4));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_left_line3));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_left_line2));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_left_line1));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_center_line));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_right_line1));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_right_line2));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_right_line3));
        mLegendLines.add(v.findViewById(R.id.sky_legend_cn0_right_line4));

        // Shape Legend lines
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line1a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line1b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line2a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line2b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line3a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line3b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line4a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line4b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line5a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line5b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line6a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line6b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line7a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line7b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line8a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line8b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line9a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line9b));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line10a));
        mLegendLines.add(v.findViewById(R.id.sky_legend_shape_line10b));

        // C/N0 Legend text
        mLegendCn0Title = v.findViewById(R.id.sky_legend_cn0_title);
        mLegendCn0Units = v.findViewById(R.id.sky_legend_cn0_units);
        mLegendCn0LeftText = v.findViewById(R.id.sky_legend_cn0_left_text);
        mLegendCn0CenterText = v.findViewById(R.id.sky_legend_cn0_center_text);
        mLegendCn0RightText = v.findViewById(R.id.sky_legend_cn0_right_text);

        // Shape Legend shapes
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_circle));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_square));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_pentagon));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle2));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle3));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle4));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle5));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle6));
        mLegendShapes.add((ImageView) v.findViewById(R.id.sky_legend_triangle7));
    }

    private void updateCn0LegendText() {
        if (!mUseLegacyGnssApi) {
            // C/N0
            mLegendCn0Title.setText(R.string.gps_cn0_column_label);
            mLegendCn0Units.setText(R.string.sky_legend_cn0_units);
            mLegendCn0LeftText.setText(R.string.sky_legend_cn0_low);
            mLegendCn0CenterText.setText(R.string.sky_legend_cn0_middle);
            mLegendCn0RightText.setText(R.string.sky_legend_cn0_high);
        } else {
            // SNR for Android 6.0 and lower (or if user unchecked "Use GNSS APIs" setting)
            mLegendCn0Title.setText(R.string.gps_snr_column_label);
            mLegendCn0Units.setText(R.string.sky_legend_snr_units);
            mLegendCn0LeftText.setText(R.string.sky_legend_snr_low);
            mLegendCn0CenterText.setText(R.string.sky_legend_snr_middle);
            mLegendCn0RightText.setText(R.string.sky_legend_snr_high);
        }

    }
}
