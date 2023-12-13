package com.example.streamshare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.streamshare.R
import com.example.streamshare.ui.theme.StreamShareTheme

@Composable
fun StreamShareApp() {
    StreamShareTheme {
        Surface(
            Modifier.fillMaxWidth()
        ) {
            StreamShareLayout()
        }
    }
}

@Composable
fun StreamShareLayout(){
    Column(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.enter_url),
            modifier = Modifier
                .padding(bottom = 16.dp, top = 40.dp)
                .align(alignment = Alignment.Start)
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun StreamShareLayoutPreview() {
    StreamShareTheme {
        Surface(
            Modifier.fillMaxSize()
        ) {
            StreamShareLayout()
        }
    }
}
