package com.example.weatherly.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherly.data.advice.AdviceIntent
import com.example.weatherly.data.advice.WeatherAdvisor
import com.example.weatherly.data.model.ChatMessage
import com.example.weatherly.data.model.ChatRole
import com.example.weatherly.data.model.WeatherData
import com.example.weatherly.ui.components.AppBackground
import com.example.weatherly.ui.components.Coral
import com.example.weatherly.ui.components.Cyan
import com.example.weatherly.ui.components.TextPrimary
import com.example.weatherly.ui.components.TextSecondary

private data class Suggestion(val label: String, val question: String, val intent: AdviceIntent)

private val Suggestions = listOf(
    Suggestion("☂️ Umbrella?", "Should I carry an umbrella today?", AdviceIntent.UMBRELLA),
    Suggestion("🧥 Jacket?", "Do I need a jacket?", AdviceIntent.JACKET),
    Suggestion("🚶 Walk / jog", "Is it good for a walk or jog right now?", AdviceIntent.WALKING),
    Suggestion("🚗 Driving", "Is it safe for driving right now?", AdviceIntent.DRIVING),
    Suggestion("🥾 Hiking", "Is today a good day for hiking?", AdviceIntent.HIKING),
    Suggestion("👕 What to wear", "What should I wear today?", AdviceIntent.CLOTHING)
)

@Composable
fun ChatScreen(
    weatherViewModel: WeatherViewModel,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val weatherState by weatherViewModel.state.collectAsStateWithLifecycle()
    val units by weatherViewModel.units.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val sending by chatViewModel.sending.collectAsStateWithLifecycle()

    val weather: WeatherData? = (weatherState as? WeatherUiState.Success)?.data

    var input by remember { mutableStateOf("") }

    BackHandler(onBack = onBack)

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, sending) {
        val count = messages.size + if (sending) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun submit(text: String) {
        chatViewModel.send(text, weather, units)
        input = ""
    }

    // Suggestion chips are answered instantly from the forecast — no API call.
    fun answerLocally(s: Suggestion) {
        val reply = weather?.let { WeatherAdvisor.advise(s.intent, it, units) }
            ?: "Open the weather screen first so I can read your local conditions, then ask again."
        chatViewModel.addLocalExchange(s.question, reply)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            ChatHeader(
                subtitle = weather?.locationName?.let { "Grounded in $it" }
                    ?: "Open the weather screen to load conditions",
                onBack = onBack,
                onNewChat = { chatViewModel.clear() }
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item { EmptyState() }
                }
                itemsIndexed(messages) { _, msg -> MessageBubble(msg) }
                if (sending) {
                    item { TypingBubble() }
                }
            }

            SuggestionRow(onPick = { answerLocally(it) })

            InputBar(
                value = input,
                onValueChange = { input = it },
                onSend = { submit(input) },
                enabled = !sending
            )
        }
    }
}

@Composable
private fun ChatHeader(subtitle: String, onBack: () -> Unit, onNewChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ask Weatherly", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onNewChat) {
                Icon(Icons.Filled.Edit, contentDescription = "New chat", tint = TextSecondary)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🌤️", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text("Ask me about your weather", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tap a quick question below, or type your own.",
            color = TextSecondary, fontSize = 13.sp
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatRole.USER
    val bubbleColor = when {
        msg.isError -> Coral.copy(alpha = 0.14f)
        isUser -> Cyan
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        msg.isError -> Coral
        isUser -> Color.White
        else -> TextPrimary
    }
    val shape = RoundedCornerShape(
        topStart = 18.dp, topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = msg.text,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun TypingBubble() {
    Row(horizontalArrangement = Arrangement.Start, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            CircularProgressIndicator(
                color = Cyan,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text("Thinking…", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SuggestionRow(onPick: (Suggestion) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Suggestions) { s ->
            AssistChip(
                onClick = { onPick(s) },
                label = { Text(s.label) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = TextPrimary
                )
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask about the weather…") },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (enabled) onSend() })
        )
        Spacer(Modifier.width(8.dp))
        val canSend = enabled && value.isNotBlank()
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(if (canSend) Cyan else Cyan.copy(alpha = 0.4f))
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}
