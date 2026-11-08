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
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import com.luuimora.aion.ui.theme.AionTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds


class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val getFont = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        val localFile = File(filesDir, "font.ttf")
        contentResolver.openInputStream(uri)?.use { inputStream ->
          localFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
          }
          viewModel.fontFile = localFile.path
        }
      }
    }

    checkAndRequestExactAlarmPermission()
    enableEdgeToEdge()
    setContent {
      val selectedColor = viewModel.mainColor.collectAsState()
      val hueSliderPosition = viewModel.mainHue.collectAsState(180f)
      val satSliderPosition = viewModel.mainSat.collectAsState(0.5f)
      val valSliderPosition = viewModel.mainValue.collectAsState(0.5f)
      val selectedWidgetFormat = viewModel.widgetFormat.collectAsState()
      val widgetOptions = listOf("Duo-Digital", "Digital")
      val basicPadding = 18.dp
      val showInfo = remember { mutableStateOf(false) }
      val indicateSaved = remember { mutableStateOf(false) }
      val selectedTimezone = viewModel.secondaryTimezone.collectAsState()
      var timezoneDropdownExpanded by remember { mutableStateOf(false) }
      val timeZoneMenu = viewModel.timezones.collectAsState()
      val searchFilter = remember { mutableStateOf("") }
      val systemColor = dynamicDarkColorScheme(applicationContext).primary

      AionTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) {

          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(basicPadding)
              .verticalScroll(rememberScrollState())
          ) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .align(Alignment.CenterHorizontally),
              colors = CardDefaults.cardColors(
                containerColor = selectedColor.value
                  ?: MaterialTheme.colorScheme.surfaceVariant
              )
            ) {
              Box(modifier = Modifier.fillMaxSize()) {
                Text(text = "Main widget color", Modifier.align(Alignment.Center))
              }
            }
            Card(
              modifier = Modifier
                .height(125.dp)
                .fillMaxWidth()
                .padding(basicPadding),
              colors = CardDefaults.cardColors(
                containerColor = systemColor
              )
            ) {
              Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                  viewModel.mainColor.value = systemColor
                  val hsv = FloatArray(3)
                  Color.colorToHSV(systemColor.toArgb(), hsv)
                  viewModel.setHue(hsv[0])
                  viewModel.setSat(hsv[1])
                  viewModel.setValue(hsv[2])
                }
              }, modifier = Modifier
                .padding(basicPadding)
                .align(Alignment.CenterHorizontally))
              {
                Text("Click to set system's wallpaper main color")
              }
            }

            Text(
              text = "Hue:\n${hueSliderPosition.value}",
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(basicPadding)
            )
            Slider(
              value = hueSliderPosition.value,
              onValueChange = { viewModel.setHue(it) },
              valueRange = 0f..360f
            )
            Text(
              text = "Saturation:\n${satSliderPosition.value}",
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(basicPadding)
            )
            Slider(
              value = satSliderPosition.value,
              onValueChange = { viewModel.setSat(it) },
              valueRange = 0f..1f
            )
            Text(
              text = "Value:\n${valSliderPosition.value}",
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(basicPadding)
            )
            Slider(
              value = valSliderPosition.value,
              onValueChange = { viewModel.setValue(it) },
              valueRange = 0f..1f
            )
            Text(
              "Widget style selector:",
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(basicPadding)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
              Row(modifier = Modifier.align(Alignment.Center)) {
                widgetOptions.forEach { text ->
                  Column(
                    modifier = Modifier.padding(basicPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                  ) {
                    Text(text)
                    RadioButton(
                      selected = text == selectedWidgetFormat.value,
                      enabled = true,
                      onClick = { viewModel.updateWidgetFormat(text) }
                    )
                  }
                }
              }
            }
            /** Timezone selector **/
            if (selectedWidgetFormat.value == "Duo-Digital") {
              Text(
                "Secondary Timezone:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(basicPadding)
              )

              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = { timezoneDropdownExpanded = true }) {
                  Text(selectedTimezone.value)
                }
                if (timezoneDropdownExpanded) {
                  Dialog(onDismissRequest = { timezoneDropdownExpanded = false }) {
                    Card(
                      modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .padding(16.dp)
                    ) {
                      Column(
                        modifier = Modifier
                          .fillMaxSize()
                          .padding(8.dp)
                      ) {
                        Text(
                          text = "Select Timezone",
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(8.dp)
                        )
                        TextField(
                          label = { Text("Search:") },
                          value = searchFilter.value,
                          onValueChange = { searchFilter.value = it },
                          modifier = Modifier.padding(8.dp)
                        )
                        val filteredTimezones = timeZoneMenu.value.filter {
                          it.contains(searchFilter.value, ignoreCase = true)
                        }

                        LazyColumn(
                          modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                        ) {
                          items(
                            count = filteredTimezones.size,
                            itemContent = {
                              DropdownMenuItem(
                                text = {
                                  Text(
                                    filteredTimezones[it]
                                      .substringAfter("/")
                                      .replace("_", " ")
                                  )
                                },
                                onClick = {
                                  viewModel.updateSecondaryTimezone(filteredTimezones[it])
                                  timezoneDropdownExpanded = false
                                }
                              )
                            },
                            key = { timeZoneMenu.value[it] }
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
            Row(
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              OutlinedButton(onClick = {
                getFont.launch("font/ttf")
              }, enabled = true, modifier = Modifier.padding(6.dp)) {
                Text("Load font from device")
              }
              OutlinedButton(onClick = {
                viewModel.fontFile = null
              }, enabled = true, modifier = Modifier.padding(6.dp)) {
                Text("Reset font")
              }
            }
            Row(
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(onClick = {
                viewModel.updateData(applicationContext)
                indicateSaved.value = true
                viewModel.viewModelScope.launch {
                  delay(1000.milliseconds)
                  indicateSaved.value = false
                }
              }, enabled = true, modifier = Modifier.padding(6.dp)) {
                if (indicateSaved.value) {
                  Text("Done!")
                } else {
                  Text("Save")
                }
              }
              OutlinedButton(
                onClick = {
                  showInfo.value = true
                },
                modifier = Modifier.padding(6.dp),
                content = {
                  Icon(
                    painterResource(R.drawable.info_24px),
                    "Info"
                  )
                },
              )
            }
          }
          /** License dialog + Copyright **/
          if (showInfo.value) {
            Dialog(
              onDismissRequest = { showInfo.value = false }
            ) {
              Card {
                Text(
                  modifier = Modifier.padding(12.dp), text =
                    "Copyright 2026 Artyom Gorlov, Israel\n" +
                            "\n" +
                            "Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n" +
                            "\n" +
                            "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n" +
                            "\n" +
                            "THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n"
                )
              }
            }
          }
        }
      }
    }
  }

  private fun checkAndRequestExactAlarmPermission() {
    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

    // API check
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (!alarmManager.canScheduleExactAlarms()) {
        // No permission, request it
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
      }
    }
  }
}
