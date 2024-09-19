/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only
 * Please see LICENSE in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.font.rubikFontFamily
import io.element.android.libraries.designsystem.theme.weatherResponseBackground
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable
import java.time.format.TextStyle
import java.util.Locale

@OptIn(FormatStringsInDatetimeFormats::class)
@Composable
fun WeatherWidget(
    weatherData: WeatherData
) {
    val (isFahrenheit, setIsFahrenheit) = remember { mutableStateOf(false) }
    val (location, current, forecast) = weatherData

    // Handlers to toggle between Fahrenheit and Celsius
    val showFahrenheit = { setIsFahrenheit(true) }
    val showCelsius = { setIsFahrenheit(false) }

    val dynamicWidth = (300 + forecast.forecastday.size * 50).dp
    val customFormat = LocalDateTime.Format {
        year(); chars("-"); monthNumber(); chars("-"); dayOfMonth();char(' ');hour();char(':');minute()
    }


    Box(
        modifier = Modifier
            .background(ElementTheme.colors.weatherResponseBackground)
            .padding(18.dp)
            .clip(RoundedCornerShape(8.dp))
            .width(dynamicWidth)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row{
                        WeatherCurrentIcon(url = current.condition.icon)
                        Column {
                            Text(
                                text = if (isFahrenheit) "${current.temp_f}°F" else "${current.temp_c}°C",
                                fontFamily = rubikFontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row {
                                Text(
                                    text = "F",
                                    fontSize = if (isFahrenheit) 12.sp else 10.sp,
                                    fontFamily = rubikFontFamily,
                                    color = if (isFahrenheit) Color.LightGray else Color.White,
                                    modifier = Modifier.clickable(onClick = showFahrenheit)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "C",
                                    fontSize = if (isFahrenheit) 10.sp else 12.sp,
                                    fontFamily = rubikFontFamily,
                                    color = if (isFahrenheit) Color.White else Color.LightGray,
                                    modifier = Modifier.clickable(onClick = showCelsius)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = current.condition.text,
                        fontFamily = rubikFontFamily,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column (verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally){
                    Text(text = "${location.name}, ${location.country}", color = Color.White,fontFamily = rubikFontFamily)
                    Text(text = LocalDateTime.parse(location.localtime, customFormat).date.format(LocalDate.Format { byUnicodePattern("dd/MM/yyyy") }), color = Color.White,fontFamily = rubikFontFamily)
                    Text(text = LocalDateTime.parse(location.localtime, customFormat).time.format(LocalTime.Format{amPmHour() ; char(':') ; minute(); char(' ') ;amPmMarker("am","pm")}), color = Color.White,fontFamily = rubikFontFamily)
                }
            }
            Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                forecast.forecastday.forEachIndexed { index, day ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp, vertical = 8.dp)
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RectangleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            WeatherIcon(url = day.day.condition.icon)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LocalDate.parse(day.date).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                                fontSize = 12.sp,
                                color = Color.White ,
                                fontFamily = rubikFontFamily,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFahrenheit) "${day.day.maxtemp_f}°F / ${day.day.mintemp_f}°F" else "${day.day.maxtemp_c}°C / ${day.day.mintemp_c}°C",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontFamily = rubikFontFamily,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Replace with actual implementations of these data classes and composables
@Serializable data class WeatherData(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)

@Serializable data class Location(val name: String, val country: String, val localtime: String)
@Serializable data class Current(val temp_c: Double, val temp_f: Double, val condition: Condition)
@Serializable data class Forecast(val forecastday: List<ForecastDay>)
@Serializable data class ForecastDay(val date: String, val day: Day)
@Serializable data class Day(val maxtemp_c: Double, val maxtemp_f: Double, val mintemp_c: Double, val mintemp_f: Double, val condition: Condition)
@Serializable data class Condition(val text: String, val icon: String)

@Composable
fun WeatherCurrentIcon(url: String) {
    AsyncImage(model = "https:$url", contentDescription = "Weather Icon",modifier = Modifier.size(64.dp))
}

@Composable
fun WeatherIcon(url: String) {
    AsyncImage(model = "https:$url", contentDescription = "Weather Icon",modifier = Modifier.size(30.dp))
}

@PreviewsDayNight
@Composable
internal fun TimelineItemWeatherWidgetPreview(
) = ElementPreview {
    val loc=Location("Delhi","India","2023-09-24 ")
    val curr= Current(24.0,24.0,Condition("Sunny","//cdn.weatherapi.com/weather/64x64/day/113.png"))
    val forecast=Forecast(listOf(ForecastDay("2023-09-24",Day(40.0,40.0,20.0,20.0,Condition("Sunny","https://cdn.weatherapi.com/weather/64x64/day/113.png")))))

    val weatherData = WeatherData(loc,curr,forecast)
    WeatherWidget(
        weatherData=weatherData,
    )
}
