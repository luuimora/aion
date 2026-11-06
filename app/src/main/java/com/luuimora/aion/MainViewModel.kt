package com.luuimora.aion

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale.getDefault


/** Package-level declaration of DataStore for widget preferences **/
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {

  init {
    // Load saved preferences on startup
    viewModelScope.launch {
      val prefs = getApplication<Application>().dataStore.data.first()
      val savedColorInt = prefs[PreferenceKeys.MAIN_COLOR]
      if (savedColorInt != null) {
        mainColor.value = Color(savedColorInt)
      }
      widgetFormat.value = prefs[PreferenceKeys.WIDGET_FORMAT] ?: "Digital"
      fontFile = prefs[PreferenceKeys.CUSTOM_FONT]

      val timeZones = ZoneId.getAvailableZoneIds()
        .filter { ("/" in it || it.length > 3 || it.uppercase(getDefault()) != it) }
        .distinctBy { it.substringAfter("/") }
        .sorted()
      timezones.value = timeZones
    }
  }

  /** Contains main color of widget **/
  val mainColor = MutableStateFlow<Color?>(null)

  /** Private value for hue of main color and method to change it **/
  private var mainHue = MutableStateFlow(0f)
  fun setHue(hue: Float) {
    mainHue.value = hue
    mainColor.value = Color.hsl(mainHue.value, mainSat.value, lightness = mainLight.value)
  }

  /** Private value for saturation of main color and method to change it **/
  private var mainSat = MutableStateFlow(0.5f)
  fun setSat(sat: Float) {
    mainSat.value = sat
    mainColor.value = Color.hsl(mainHue.value, mainSat.value, lightness = mainLight.value)
  }

  /** Private value for lightness of main color and method to change it **/
  private var mainLight = MutableStateFlow(0.5f)
  fun setLight(value: Float) {
    mainLight.value = value
    mainColor.value = Color.hsl(mainHue.value, mainSat.value, lightness = mainLight.value)
  }

  /** Public flow for selected widget format **/
  val widgetFormat = MutableStateFlow("")

  /** Public flow for selected timezone format **/
  val secondaryTimezone = MutableStateFlow("Asia/Jerusalem")

  /** Method to update selected widget format in ViewModel **/
  fun updateWidgetFormat(format: String) {
    widgetFormat.value = format
  }

  /** Method to update selected secondary timezone in ViewModel **/
  fun updateSecondaryTimezone(zone: String) {
    try {
      ZoneId.of(zone)
      secondaryTimezone.value = zone
    } catch (e: Exception) {
      return
    }
    //secondaryTimezone.value = zone
  }

  val timezones = MutableStateFlow(emptyList<String>())

  /** Initialization of DataStore **/
  object PreferenceKeys {
    val WIDGET_FORMAT = stringPreferencesKey("widget_format")
    val MAIN_COLOR = intPreferencesKey("main_color")

    val CUSTOM_FONT = stringPreferencesKey("uri_font")
    val SECONDARY_TIMEZONE = stringPreferencesKey("secondary_tmz")
  }

  /** Method that invokes by UI to update DataStore AND trigger re-rendering of widgets **/
  fun updateData(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      context.dataStore.edit { settings ->
        settings[PreferenceKeys.WIDGET_FORMAT] = widgetFormat.value
        settings[PreferenceKeys.MAIN_COLOR] = mainColor.value?.toArgb() ?: 0
        settings[PreferenceKeys.CUSTOM_FONT] = fontFile ?: ""
        settings[PreferenceKeys.SECONDARY_TIMEZONE] = secondaryTimezone.value
      }
      /** Updating the widget after updating settings in DataStore **/
      val manager = GlanceAppWidgetManager(context)
      val glanceIds = manager.getGlanceIds(ClockWidget::class.java)

      glanceIds.forEach { glanceId ->
        ClockWidget().update(context, glanceId)
      }
    }
  }

  /** Value for font file path in cache **/
  var fontFile: String? = null

}