package com.saggitt.omega.compose.screens.preferences

import android.content.Context
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.launcher3.tflite.NNAdapter

@Composable
fun ShowNNResultsPage() {
    var message by remember { mutableStateOf("Loading...") }

    message = fetchFromNeuralNetwork(LocalContext.current)

    Text(text = message)
}
fun fetchFromNeuralNetwork(context: Context): String {
    val nnAdapter = NNAdapter(context)
    return nnAdapter.recommend()
}