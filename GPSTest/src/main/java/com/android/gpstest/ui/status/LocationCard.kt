package com.android.gpstest.ui.status

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.text.TextUtils
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.data.FixState
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.model.DilutionOfPrecision
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.util.*
import com.android.gpstest.util.FormatUtils.formatBearingAccuracy
import com.android.gpstest.util.PreferenceUtils.gnssFilter
import com.android.gpstest.util.SharedPreferenceUtil.coordinateFormat
import com.android.gpstest.util.SharedPreferenceUtil.shareIncludeAltitude
import java.text.SimpleDateFormat

@Preview
@Composable
fun LocationCardPreview(
    @PreviewParameter(LocationPreviewParameterProvider::class) location: Location
) {
    LocationCard(
        location,
        "5 sec",
        1.4,
        DilutionOfPrecision(1.0, 2.0, 3.0),
        SatelliteMetadata(0, 0, 0, 0, 0, 0),
        FixState.Acquired
    )
}

class LocationPreviewParameterProvider : PreviewParameterProvider<Location> {
    override val values = sequenceOf(previewLocation())
}

fun previewLocation(): Location {
    val l = Location("preview")
    l.apply {
        latitude = 28.38473847
        longitude = -87.32837456
        time = 1633375741711
        altitude = 13.5
        speed = 21.5f
        bearing = 240f
        if (SatelliteUtils.isSpeedAndBearingAccuracySupported()) {
            bearingAccuracyDegrees = 5.6f
            speedAccuracyMetersPerSecond = 6.1f
        }
    }
    return l
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LocationCard(
    location: Location,
    ttff: String,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
    satelliteMetadata: SatelliteMetadata,
    fixState: FixState,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                copyToClipboard(context, location)
            },
        elevation = 2.dp
    ) {
        Box {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
            ) {
                LabelColumn1()
                ValueColumn1(location, altitudeMsl, dop)
                LabelColumn2(location)
                ValueColumn2(location, ttff, dop, satelliteMetadata)
            }
            LockIcon(fixState)
        }
    }
}

@Composable
fun ValueColumn1(
    location: Location,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Latitude(location)
        Longitude(location)
        Altitude(location)
        AltitudeMsl(altitudeMsl)
        Speed(location)
        SpeedAccuracy(location)
        Pdop(dop)
    }
}

@Composable
fun Latitude(location: Location) {
    LocationValue(FormatUtils.formatLatOrLon(location.latitude, CoordinateType.LATITUDE))
}

@Composable
fun Longitude(location: Location) {
    LocationValue(FormatUtils.formatLatOrLon(location.longitude, CoordinateType.LONGITUDE))
}

@Composable
fun Altitude(location: Location) {
    LocationValue(FormatUtils.formatAltitude(location))
}

@Composable
fun AltitudeMsl(altitudeMsl: Double) {
    LocationValue(FormatUtils.formatAltitudeMsl(altitudeMsl))
}

@Composable
fun Speed(location: Location) {
    LocationValue(FormatUtils.formatSpeed(location))
}

@Composable
fun SpeedAccuracy(location: Location) {
    LocationValue(FormatUtils.formatSpeedAccuracy(location))
}

@Composable
fun Pdop(dop: DilutionOfPrecision) {
    if (dop.positionDop.isNaN()) {
        LocationValue("")
    } else {
        LocationValue(stringResource(R.string.pdop_value, dop.positionDop))
    }
}

@Composable
fun ValueColumn2(
    location: Location,
    ttff: String,
    dop: DilutionOfPrecision,
    satelliteMetadata: SatelliteMetadata,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Time(location)
        TTFF(ttff)
        Accuracy(location)
        NumSats(satelliteMetadata)
        Bearing(location)
        BearingAccuracy(location)
        HVDOP(dop)
    }
}

@Composable
fun Time(location: Location) {
    if (location.time == 0L || !PreferenceUtils.isTrackingStarted()) {
        LocationValue("")
    } else {
        formatTime(location.time)
    }
}

@Composable
private fun formatTime(time: Long) {
    // SimpleDateFormat can only do 3 digits of fractional seconds (.SSS)
    val SDF_TIME_24_HOUR = "HH:mm:ss.SSS"
    val SDF_TIME_12_HOUR = "hh:mm:ss.SSS a"
    val SDF_DATE_24_HOUR = "HH:mm:ss.SSS MMM d, yyyy z"
    val SDF_DATE_12_HOUR = "hh:mm:ss.SSS a MMM d, yyyy z"

    // See #117
    @SuppressLint("SimpleDateFormat")
    val timeFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_TIME_24_HOUR else SDF_TIME_12_HOUR
        )
    }
    @SuppressLint("SimpleDateFormat")
    val timeAndDateFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_DATE_24_HOUR else SDF_DATE_12_HOUR
        )
    }

    if (LocalConfiguration.current.screenWidthDp > 450 || !DateTimeUtils.isTimeValid(time)) { // 450dp is a little larger than the width of a Samsung Galaxy S8+
        val dateAndTime = timeAndDateFormat.format(time).trimZeros()
        // Time and date
        if (DateTimeUtils.isTimeValid(time)) {
            LocationValue(dateAndTime)
        } else {
            ErrorTime(dateAndTime, time)
        }
    } else {
        // Time
        LocationValue(timeFormat.format(time).trimZeros())
    }
}

private fun String.trimZeros(): String {
    return this.replace(".000", "")
        .replace(",000", "")
}

@Composable
fun TTFF(ttff: String) {
    LocationValue(ttff)
}

/**
 * Horizontal and vertical location accuracies based on the provided location
 * @param location
 */
@Composable
fun Accuracy(location: Location) {
    LocationValue(FormatUtils.formatAccuracy(location))
}

@Composable
fun NumSats(satelliteMetadata: SatelliteMetadata) {
    val fontStyle = if (gnssFilter().isNotEmpty()) {
        // Make text italic so it matches filter text
        FontStyle.Italic
    } else {
        FontStyle.Normal
    }
    LocationValue(
        stringResource(
            R.string.gps_num_sats_value,
            satelliteMetadata.numSatsUsed,
            satelliteMetadata.numSatsInView,
            satelliteMetadata.numSatsTotal
        ),
        fontStyle
    )
}

@Composable
fun Bearing(location: Location) {
    if (location.hasBearing()) {
        LocationValue(stringResource(R.string.gps_bearing_value, location.bearing))
    } else {
        LocationValue("")
    }
}

@Composable
fun BearingAccuracy(location: Location) {
    LocationValue(formatBearingAccuracy(location))
}

@Composable
fun HVDOP(dop: DilutionOfPrecision) {
    if (dop.horizontalDop.isNaN() || dop.verticalDop.isNaN()) {
        LocationValue("")
    } else {
        LocationValue(
            stringResource(
                R.string.hvdop_value, dop.horizontalDop,
                dop.verticalDop
            )
        )
    }
}

@Composable
fun LabelColumn1() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.latitude_label)
        LocationLabel(R.string.longitude_label)
        LocationLabel(R.string.altitude_label)
        LocationLabel(R.string.altitude_msl_label)
        LocationLabel(R.string.speed_label)
        LocationLabel(R.string.speed_acc_label)
        LocationLabel(R.string.pdop_label)
    }
}

@Composable
fun LabelColumn2(location: Location) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.fix_time_label)
        LocationLabel(R.string.ttff_label)
        LocationLabel(if (SatelliteUtils.isVerticalAccuracySupported(location)) R.string.hor_and_vert_accuracy_label else R.string.accuracy_label)
        LocationLabel(R.string.num_sats_label)
        LocationLabel(R.string.bearing_label)
        LocationLabel(R.string.bearing_acc_label)
        LocationLabel(R.string.hvdop_label)
    }
}

@Composable
fun LocationLabel(@StringRes id: Int) {
    if (reduceSpacing()) {
        Text(
            text = stringResource(id),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = letterSpacing(),
        )
    } else {
        Text(
            text = stringResource(id),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun LocationValue(text: String, fontStyle: FontStyle = FontStyle.Normal) {
    if (reduceSpacing()) {
        Text(
            text = text,
            modifier = Modifier.padding(end = 2.dp),
            fontSize = 13.sp,
            letterSpacing = letterSpacing(),
            fontStyle = fontStyle
        )
    } else {
        Text(
            text = text,
            modifier = Modifier.padding(end = 4.dp),
            fontSize = 13.sp,
            fontStyle = fontStyle
        )
    }
}

@Composable
fun ErrorTime(timeText: String, timeMs: Long) {
    var openDialog = remember { mutableStateOf(false) }
    // Red time box
    Box(
        Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.error)
            .clickable {
                openDialog.value = true
            }
    ) {
        Text(
            text = timeText,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colors.onError
        )
    }

    // Alert Dialog
    val format = remember {
        SimpleDateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.LONG
        )
    }

    val message by remember {
        mutableStateOf(
            Application.app.getString(
                R.string.error_time_message, format.format(timeMs),
                DateTimeUtils.NUM_DAYS_TIME_VALID
            )
        )
    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                // TODO - hyperlink URL in text
                Text(stringResource(R.string.error_time_title))
            },
            text = {
                Column() {
                    Text(message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
private fun reduceSpacing(): Boolean {
    return LocalConfiguration.current.screenWidthDp < 365 // Galaxy S21+ is 384dp wide, Galaxy S8 is 326dp
}

private fun letterSpacing(): TextUnit {
    // Reduce text spacing on narrow displays to make both columns fit
    return (-0.01).em
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LockIcon(fixState: FixState) {
    var visible by remember { mutableStateOf(false) }
    visible = fixState == FixState.Acquired
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn() + expandVertically(expandFrom = Alignment.CenterVertically),
        exit = scaleOut() + shrinkVertically(shrinkTowards = Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_baseline_lock_24),
            contentDescription = stringResource(id = R.string.lock),
            tint = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(6.dp)
        )
    }
}

private fun copyToClipboard(context: Context, location: Location) {
    if (location.latitude == 0.0 && location.longitude == 0.0) return
    val formattedLocation = UIUtils.formatLocationForDisplay(
        location, null, shareIncludeAltitude(),
        null, null, null, coordinateFormat()
    )
    if (!TextUtils.isEmpty(formattedLocation)) {
        IOUtils.copyToClipboard(formattedLocation)
        // Android 12 and higher generates a Toast automatically
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_LONG)
                .show()
        }
    }
}