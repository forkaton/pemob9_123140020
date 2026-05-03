package com.forkaton.pemob9_123140020.data

import kotlinx.serialization.Serializable

// ── Request Models ──────────────────────────────────────────────────────────

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String = "user"
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val temperature: Double = 0.4,   // Rendah = lebih konsisten untuk audit
    val maxOutputTokens: Int = 2048,
    val topP: Double = 0.95
)

// ── Response Models ──────────────────────────────────────────────────────────

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

// ── UI State Models ──────────────────────────────────────────────────────────

data class AuditResult(
    val input: String,
    val result: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AuditUiState {
    object Idle : AuditUiState()
    object Loading : AuditUiState()
    data class Success(val history: List<AuditResult>) : AuditUiState()
    data class Error(val message: String, val isRetryable: Boolean = true) : AuditUiState()
}

// ── AI Error Sealed Class (Error Handling 20%) ───────────────────────────────

sealed class AuditError : Exception() {
    data class RateLimited(val retryAfterSeconds: Int = 60) : AuditError()
    data class Unauthorized(override val message: String = "API Key tidak valid") : AuditError()
    data class ServerError(override val message: String = "Server Gemini sedang bermasalah") : AuditError()
    data class NetworkError(override val message: String = "Tidak ada koneksi internet") : AuditError()
    data class EmptyResponse(override val message: String = "AI tidak memberikan respons") : AuditError()
}