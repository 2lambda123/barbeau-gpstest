package com.android.gpstest.ui.status

import android.location.Location
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.model.DilutionOfPrecision
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.model.SatelliteStatus
import com.android.gpstest.ui.SignalInfoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun StatusScreen(viewModel: SignalInfoViewModel) {
    //
    // Observe LiveData from ViewModel
    //
    val location: Location by viewModel.location.observeAsState(Location("default"))
    val ttff: String by viewModel.ttff.observeAsState("")
    val altitudeMsl: Double by viewModel.altitudeMsl.observeAsState(Double.NaN)
    val dop: DilutionOfPrecision by viewModel.dop.observeAsState(DilutionOfPrecision(Double.NaN,Double.NaN,Double.NaN))
    val satelliteMetadata: SatelliteMetadata by viewModel.satelliteMetadata.observeAsState(SatelliteMetadata(0,0,0,0,0,0))
    // TODO - apply filter and sort on statuses in ViewModel
    val gnssStatuses: List<SatelliteStatus> by viewModel.gnssStatuses.observeAsState(emptyList())
    val sbasStatuses: List<SatelliteStatus> by viewModel.sbasStatuses.observeAsState(emptyList())

    // TODO - dark and light themes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            LocationCard(
                location,
                ttff,
                altitudeMsl,
                dop,
                satelliteMetadata)
//            Filter() // TODO - annotated text - https://foso.github.io/Jetpack-Compose-Playground/material/card/
            GnssStatusCard(gnssStatuses)
            SbasStatusCard(sbasStatuses)
        }
    }
}

@Composable
fun GnssStatusCard(satStatuses: List<SatelliteStatus>) {
    StatusCard(satStatuses, true)
}

@Composable
fun SbasStatusCard(satStatuses: List<SatelliteStatus>) {
    StatusCard(satStatuses, false)
}

@Composable
fun StatusCard(
    satStatuses: List<SatelliteStatus>,
    isGnss: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Column {
            StatusRowHeader(isGnss)
            satStatuses.forEach {
                StatusRow(it)
            }
        }
    }
}

@Composable
fun StatusRow(satelliteStatus: SatelliteStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 5.dp, start = 16.dp, end = 16.dp)
    ) {
        val minWidth = Modifier.defaultMinSize(dimensionResource(R.dimen.min_column_width))
        val minWidthSmall = Modifier.defaultMinSize(36.dp)

        StatusValue(satelliteStatus.svid.toString(), minWidthSmall)
        StatusValue("flag") // FIXME - flag image
        StatusValue(satelliteStatus.carrierFrequencyHz.toString(), minWidthSmall) // FIXME - format label extension function
        StatusValue(satelliteStatus.cn0DbHz.toString(), minWidth) // FIXME - format
        StatusValue(satelliteStatus.hasEphemeris.toString(), minWidth) // FIXME - do all booleans
        StatusValue(satelliteStatus.elevationDegrees.toString(), minWidth) // FIXME - format
        StatusValue(satelliteStatus.azimuthDegrees.toString(), minWidth) // FIXME - format
    }
}

@Composable
fun StatusRowHeader(isGnss: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 5.dp, bottom = 5.dp, start = 16.dp, end = 16.dp)
    ) {
        val minWidth = Modifier.defaultMinSize(
            minWidth = dimensionResource(
                id = R.dimen.min_column_width
            )
        )
        val minWidthSmall = Modifier.defaultMinSize(
            minWidth = 36.dp
        )

        StatusLabel(R.string.id_column_label, minWidthSmall)
        if (isGnss) {
            StatusLabel(R.string.gnss_flag_image_label, minWidth)
        } else {
            StatusLabel(R.string.sbas_flag_image_label, minWidth)
        }
        StatusLabel(R.string.cf_column_label, minWidthSmall)
        StatusLabel(R.string.gps_cn0_column_label, minWidth)
        StatusLabel(R.string.flags_aeu_column_label, minWidth)
        StatusLabel(R.string.elevation_column_label, minWidth)
        StatusLabel(R.string.azimuth_column_label, minWidth)
    }
}

@Composable
fun StatusLabel(@StringRes id: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id),
        modifier = modifier.padding(start = 3.dp, end = 3.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
fun StatusValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 3.dp, end = 3.dp),
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
}