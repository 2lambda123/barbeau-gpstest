/*
 * Copyright (C) 2016 Sean J. Barbeau
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

import com.google.android.gms.common.GoogleApiAvailability;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView tv = (TextView) findViewById(R.id.help_text);
        String versionString = "";
        int versionCode = 0;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder builder = new StringBuilder();
        // Version info
        builder.append("v")
                .append(versionString)
                .append(" (")
                .append(versionCode)
                .append(")\n\n");

        // Majority of content from string resource
        builder.append(getString(R.string.help_text));
        String googleOssLicense = null;

        // License info for Google Play Services, if available
        try {
            GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
            googleOssLicense = gaa.getOpenSourceSoftwareLicenseInfo(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (googleOssLicense != null) {
            builder.append(googleOssLicense);
        }

        builder.append("\n\n");

        tv.setText(builder.toString());
    }
}
