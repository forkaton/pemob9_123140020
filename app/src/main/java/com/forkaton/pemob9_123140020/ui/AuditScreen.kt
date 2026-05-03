package com.forkaton.pemob9_123140020.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forkaton.pemob9_123140020.data.*

// ── Tema Warna Neon Purple (Cyberpunk Vibe) ──────────────────────────────────
private val DarkBackground = Color(0xFF0D0015) // Sangat gelap, ungu kehitaman
private val SurfaceDark = Color(0xFF1A0526)    // Warna kartu dasar
private val NeonPurpleLight = Color(0xFFD000FF) // Ungu neon menyala
private val NeonPurpleDark = Color(0xFF7000FF)  // Ungu tua
private val TextWhite = Color(0xFFF3E5F5)
private val ErrorNeon = Color(0xFFFF0055)       // Merah neon untuk error

// Gradasi untuk elemen aktif
private val NeonGradient = Brush.linearGradient(
    colors = listOf(NeonPurpleDark, NeonPurpleLight)
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(viewModel: AuditViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val placeholderExamples = listOf(
        "Perusahaan kami menggunakan password default pada semua router...",
        "Tidak ada kebijakan backup data, semua disimpan di satu server...",
        "Karyawan menggunakan akun admin bersama tanpa log aktivitas...",
        "Sistem ERP kami belum di-patch sejak 2 tahun lalu..."
    )
    var placeholderIndex by remember { mutableIntStateOf(0) }

    val history = when (val s = uiState) {
        is AuditUiState.Success -> s.history
        else -> emptyList()
    }

    LaunchedEffect(history.size) {
        if (history.isNotEmpty()) listState.animateScrollToItem(history.size - 1)
    }

    Scaffold(
        containerColor = DarkBackground, // Background utama gelap
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Komponen 1: Badge Sistem (Kotak kecil)
                        Box(
                            modifier = Modifier
                                .background(NeonPurpleDark, RoundedCornerShape(4.dp))
                                .border(1.dp, NeonPurpleLight.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SYS",
                                color = TextWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Komponen 2: Tipografi Judul Utama
                        Column {
                            Text(
                                text = "AEGIS AUDITOR", // Ganti nama di sini jika mau
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = NeonPurpleLight,
                                letterSpacing = 1.5.sp
                            )
                            // Subtitle bergaya terminal command
                            Text(
                                text = "CORE // GEMINI_2.5_FLASH",
                                fontSize = 9.sp,
                                color = TextWhite.copy(alpha = 0.5f),
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.resetSession() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Reset sesi",
                                tint = ErrorNeon
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    scrolledContainerColor = SurfaceDark
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Area Chat / History ───────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (history.isEmpty() && uiState !is AuditUiState.Loading) {
                    item { WelcomeCard(examples = placeholderExamples) }
                }

                items(history) { auditResult ->
                    AuditBubble(auditResult = auditResult)
                }

                if (uiState is AuditUiState.Loading) {
                    item { TypingIndicator() }
                }

                if (uiState is AuditUiState.Error) {
                    item {
                        ErrorCard(
                            message = (uiState as AuditUiState.Error).message,
                            isRetryable = (uiState as AuditUiState.Error).isRetryable
                        )
                    }
                }
            }

            // ── Input Area ────────────────────────────────────────────────────
            InputArea(
                text = inputText,
                isLoading = uiState is AuditUiState.Loading,
                onTextChange = { inputText = it },
                onSend = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty()) {
                        viewModel.analyzeIT(trimmed)
                        inputText = ""
                        placeholderIndex = (placeholderIndex + 1) % placeholderExamples.size
                    }
                }
            )
        }
    }
}

// ── Welcome Card ──────────────────────────────────────────────────────────────
@Composable
private fun WelcomeCard(examples: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, NeonGradient, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "SYSTEM INITIATED",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = NeonPurpleLight,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Deskripsikan kebijakan, konfigurasi, atau skenario IT Anda. AI akan menganalisis risiko keamanan berdasarkan standar ISO 27001 & COBIT.",
                fontSize = 14.sp,
                color = TextWhite,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("INPUT EXAMPLES_ >", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = NeonPurpleDark)
            Spacer(modifier = Modifier.height(8.dp))
            examples.forEach { example ->
                Text(
                    "▶ $example",
                    fontSize = 13.sp,
                    color = TextWhite.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ── Audit Bubble ──────────────────────────────────────────────────────────────
@Composable
private fun AuditBubble(auditResult: AuditResult) {
    val isUser = auditResult.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, NeonPurpleLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👁️‍🗨️", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) NeonGradient else Brush.linearGradient(listOf(SurfaceDark, SurfaceDark)))
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else NeonPurpleDark.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = auditResult.result,
                fontSize = 14.sp,
                color = TextWhite,
                lineHeight = 22.sp
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonGradient),
                contentAlignment = Alignment.Center
            ) {
                Text("Me", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

// ── Typing Indicator ──────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, NeonPurpleLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("👁️‍🗨️", fontSize = 16.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .background(SurfaceDark)
                .border(1.dp, NeonPurpleDark.copy(alpha = 0.5f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400),
                            repeatMode = RepeatMode.Reverse,
                            initialStartOffset = StartOffset(index * 150)
                        ),
                        label = "alpha$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(alpha)
                            .background(NeonPurpleLight, CircleShape)
                    )
                }
            }
        }
    }
}

// ── Error Card ────────────────────────────────────────────────────────────────
@Composable
private fun ErrorCard(message: String, isRetryable: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A000A))
            .border(1.dp, ErrorNeon, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorNeon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "SYSTEM OVERLOAD",
                    fontWeight = FontWeight.Bold,
                    color = ErrorNeon,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    message,
                    color = TextWhite,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ── Input Area ────────────────────────────────────────────────────────────────
@Composable
private fun InputArea(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Enter audit scenario...",
                        fontSize = 14.sp,
                        color = TextWhite.copy(alpha = 0.4f)
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = NeonPurpleLight,
                    focusedBorderColor = NeonPurpleLight,
                    unfocusedBorderColor = NeonPurpleDark.copy(alpha = 0.5f),
                    disabledBorderColor = SurfaceDark
                )
            )
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (text.isNotBlank() && !isLoading) NeonGradient else Brush.linearGradient(listOf(SurfaceDark, SurfaceDark)))
                    .clickable(enabled = text.isNotBlank() && !isLoading) { onSend() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) TextWhite else TextWhite.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}