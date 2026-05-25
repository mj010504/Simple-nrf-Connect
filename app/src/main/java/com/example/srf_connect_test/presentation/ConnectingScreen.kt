package com.example.srf_connect_test.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.srf_connect_test.ui.theme.Srf_connect_testTheme

@Composable
fun ConnectingScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Connecting...")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConnectingScreenPreview() {
    Srf_connect_testTheme { ConnectingScreen() }
}
