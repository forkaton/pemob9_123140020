package com.forkaton.pemob9_123140020.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forkaton.pemob9_123140020.data.*
import com.forkaton.pemob9_123140020.service.AuditService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuditViewModel : ViewModel() {

    private val auditService = AuditService()

    private val _uiState = MutableStateFlow<AuditUiState>(AuditUiState.Idle)
    val uiState: StateFlow<AuditUiState> = _uiState.asStateFlow()

    // History percakapan untuk ditampilkan di UI
    private val _history = mutableListOf<AuditResult>()

    // ── Retry dengan Exponential Backoff (Error Handling 20%) ────────────────

    private suspend fun <T> retryWithBackoff(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000L,
        maxDelayMs: Long = 8000L,
        factor: Double = 2.0,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        repeat(maxRetries - 1) { attempt ->
            val result = block()
            if (result.isSuccess) return result

            val error = result.exceptionOrNull()
            // Hanya retry untuk error yang bisa dipulihkan
            when (error) {
                is AuditError.RateLimited -> {
                    val waitMs = error.retryAfterSeconds * 1000L
                    delay(waitMs.coerceAtMost(maxDelayMs))
                }
                is AuditError.ServerError -> {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
                else -> return result  // NetworkError, Unauthorized → langsung gagal
            }
        }
        return block() // Percobaan terakhir
    }

    // ── Fungsi Utama: Kirim Input untuk Diaudit ───────────────────────────────

    fun analyzeIT(input: String) {
        if (input.isBlank()) return

        // Tambah pesan user ke history UI segera
        _history.add(AuditResult(input = input, result = input, isUser = true))
        _uiState.value = AuditUiState.Loading

        viewModelScope.launch {
            val result = retryWithBackoff {
                auditService.analyzeCompliance(input)
            }

            result
                .onSuccess { auditText ->
                    _history.add(
                        AuditResult(input = input, result = auditText, isUser = false)
                    )
                    _uiState.value = AuditUiState.Success(_history.toList())
                }
                .onFailure { error ->
                    // Rollback pesan user dari history jika gagal total
                    if (_history.isNotEmpty()) _history.removeLastOrNull()

                    val (message, isRetryable) = when (error) {
                        is AuditError.Unauthorized  ->
                            "API Key tidak valid. Periksa file local.properties." to false
                        is AuditError.RateLimited   ->
                            "Terlalu banyak permintaan. Tunggu sebentar lalu coba lagi." to true
                        is AuditError.ServerError   ->
                            "Server Gemini sedang bermasalah. Coba lagi nanti." to true
                        is AuditError.NetworkError  ->
                            "Gagal terhubung: ${error.message}" to true
                        is AuditError.EmptyResponse ->
                            "AI tidak memberikan respons. Coba sederhanakan input." to true
                        else ->
                            "Terjadi kesalahan: ${error.message}" to true
                    }
                    _uiState.value = AuditUiState.Error(message, isRetryable)
                }
        }
    }

    // ── Reset Sesi Audit ──────────────────────────────────────────────────────

    fun resetSession() {
        auditService.clearHistory()
        _history.clear()
        _uiState.value = AuditUiState.Idle
    }
}