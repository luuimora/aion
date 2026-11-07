/**
MIT License

Copyright (c) 2026 Artyom Gorlov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
**/

package com.luuimora.aion

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.annotation.FontRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalTime
import java.time.ZoneId


class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = ClockWidget()
  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    WidgetUpdater.cancelWidgetUpdate(context)
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)

    if (intent.action == "ACTION_TIME_TICK" || intent.action == "ACTION_UPDATE_CLOCK_WIDGET") {
      CoroutineScope(Dispatchers.IO).launch {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(ClockWidget::class.java)

        glanceIds.forEach { glanceId ->
          glanceAppWidget.update(context, glanceId)
        }
      }
    }
    WidgetUpdater.scheduleWidgetUpdate(context)
  }
}

class ClockWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Exact
  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val preferencesData = context.dataStore.data
    provideContent {
      /** Storing the values for further processing **/
      val preferences = preferencesData.collectAsState(emptyPreferences())
      val widgetFormat = preferences.value[MainViewModel.PreferenceKeys.WIDGET_FORMAT] ?: "Digital"
      val mainColor = Color(
        color = preferences.value[MainViewModel.PreferenceKeys.MAIN_COLOR] ?: Color.Transparent.toArgb()
      )
      val fontPath = preferences.value[MainViewModel.PreferenceKeys.CUSTOM_FONT] ?: ""
      val fontFile = File(fontPath)

      val typeface = if (fontFile.exists()) {
        Typeface.createFromFile(fontFile)
      } else {
        Typeface.DEFAULT
      }
      val time = LocalTime.now().toString().slice(0..4)

      /** Values for dynamic resizing **/
      val dynamicFontSize =
        ((LocalSize.current.height.value + LocalSize.current.width.value) * 0.5f).sp

      /** Values to process selected by user second timezone**/
      val selectedZoneStr =
        preferences.value[MainViewModel.PreferenceKeys.SECONDARY_TIMEZONE] ?: "Asia/Jerusalem"
      val secondaryZoneId = try {
        ZoneId.of(selectedZoneStr)
      } catch (_: Exception) {
        ZoneId.of("UTC")
      }
      val secondaryTime = LocalTime.now(secondaryZoneId).toString().slice(0..4)
      val cityLabel = secondaryZoneId.id.substringAfter("/")
      /** When we've got the widget format, we render it accordingly **/
      when (widgetFormat) {
        "Digital" -> {
          GlanceText(
            text = time,
            font = typeface,
            fontSize = dynamicFontSize,
            color = mainColor
          )
        }

        "Duo-Digital" -> {
          Row(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalAlignment = Alignment.CenterVertically,
              modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp).fillMaxHeight()
            ) {
              GlanceText(
                text = time,
                font = typeface,
                fontSize = dynamicFontSize * 0.5f,
                color = mainColor
              )
              GlanceText(
                text = "Local",
                font = typeface,
                fontSize = dynamicFontSize * 0.1f,
                color = mainColor
              )
            }
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalAlignment = Alignment.CenterVertically,
              modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp).fillMaxHeight()
            ) {
              GlanceText(
                text = secondaryTime,
                font = typeface,
                fontSize = dynamicFontSize * 0.5f,
                color = mainColor
              )
              GlanceText(
                text = cityLabel,
                font = typeface,
                fontSize = dynamicFontSize * 0.1f,
                color = mainColor
              )
            }
          }
        }
      }
    }
  }
}

object WidgetUpdater {
  fun scheduleWidgetUpdate(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, ClockWidgetReceiver::class.java).apply {
      action = "ACTION_UPDATE_CLOCK_WIDGET"
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )


    val currentTime = System.currentTimeMillis()

    val millisInCurrentMinute = currentTime % 60000

    val nextMinuteTime = currentTime + (60000 - millisInCurrentMinute)

    if (alarmManager.canScheduleExactAlarms()) {
      alarmManager.setExactAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        nextMinuteTime,
        pendingIntent
      )
    } else {
      alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        nextMinuteTime,
        pendingIntent
      )
    }
  }

  fun cancelWidgetUpdate(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ClockWidgetReceiver::class.java).apply {
      action = "ACTION_UPDATE_CLOCK_WIDGET"
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
  }
}

/** All respect to https://armcha.github.io/,
 * link to original code:
 * https://gist.github.com/armcha/8201fc33b6e6a4aa6a7a66c7cfc1f4bb#file-glancetext-kt  **/

fun textAsBitmap(
  text: String,
  fontSize: TextUnit,
  color: Color = Color.Black,
  letterSpacing: Float = 0.1f,
  font: Typeface
): Bitmap {
  val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
  paint.textSize = fontSize.value
  paint.color = color.toArgb()
  paint.letterSpacing = letterSpacing
  paint.typeface = font

  val baseline = -paint.ascent()
  val width = (paint.measureText(text)).toInt()
  val height = (baseline + paint.descent()).toInt()
  val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(image)
  canvas.drawText(text, 0f, baseline, paint)
  return image
}

/** All respect to https://armcha.github.io/,
 * link to original code:
 * https://gist.github.com/armcha/8201fc33b6e6a4aa6a7a66c7cfc1f4bb#file-glancetext-kt  **/

@Composable
fun GlanceText(
  text: String,
  @FontRes font: Typeface,
  fontSize: TextUnit,
  modifier: GlanceModifier = GlanceModifier,
  color: Color = Color.Black,
  letterSpacing: TextUnit = 0.1.sp
) {
  Image(
    modifier = modifier,
    provider = ImageProvider(
      textAsBitmap(
        text = text,
        fontSize = fontSize,
        color = color,
        letterSpacing = letterSpacing.value,
        font = font
      )
    ),
    contentDescription = null,
  )
}
