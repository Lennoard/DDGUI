package com.androidvip.ddgui.presentation.disclaimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.androidvip.ddgui.R

@Composable
fun DisclaimerRoute(
    viewModel: DisclaimerViewModel,
    onContinue: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    DisclaimerUiEffect.ContinueToApp -> onContinue()
                }
            }
        }
    }

    DisclaimerScreen(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun DisclaimerScreen(
    state: DisclaimerUiState,
    onEvent: (DisclaimerUiEvent) -> Unit
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.disclaimer),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.disclaimer_headline),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.disclaimer1),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = stringResource(R.string.disclaimer2),
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .toggleable(
                                value = state.accepted,
                                role = Role.Checkbox,
                                onValueChange = {
                                    onEvent(DisclaimerUiEvent.AcceptanceChanged(it))
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.accepted,
                            onCheckedChange = null
                        )
                        Text(
                            text = stringResource(R.string.disclaimer_accept),
                            modifier = Modifier.weight(1f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Button(
                    onClick = { onEvent(DisclaimerUiEvent.Continue) },
                    enabled = state.accepted,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}
