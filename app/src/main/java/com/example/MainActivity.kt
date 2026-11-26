package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.ToastsWifiMapperTheme
import com.frenchtoast.wifimapper.data.local.AppPreferencesStore
import com.frenchtoast.wifimapper.data.local.RoomWifiRepository
import com.frenchtoast.wifimapper.data.local.WifiMapperDatabase
import com.frenchtoast.wifimapper.platform.android.AndroidLocationProvider
import com.frenchtoast.wifimapper.platform.android.AndroidWifiScanner
import com.frenchtoast.wifimapper.ui.navigation.ToastsWifiMapperApp
import com.frenchtoast.wifimapper.ui.state.WifiMapperViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )

    val db = WifiMapperDatabase.getInstance(applicationContext)
    val prefsStore = AppPreferencesStore(applicationContext)
    val repository = RoomWifiRepository(db, prefsStore)
    val wifiScanner = AndroidWifiScanner(applicationContext)
    val locationPort = AndroidLocationProvider(applicationContext)

    setContent {
      ToastsWifiMapperTheme {
        val vm: WifiMapperViewModel = viewModel(
          factory = WifiMapperViewModel.Factory(
            context = applicationContext,
            repository = repository,
            prefsStore = prefsStore,
            wifiScanner = wifiScanner,
            locationPort = locationPort,
          ),
        )
        ToastsWifiMapperApp(viewModel = vm)
      }
    }
  }
}
