package com.forkaton.pemob9_123140020.service

import com.forkaton.pemob9_123140020.BuildConfig
import com.forkaton.pemob9_123140020.data.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class AuditService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val baseUrl =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    // ── System Prompt (Prompt Engineering 25%) ────────────────────────────────
    // Pembaruan: Instruksi ketat untuk PLAIN TEXT dan struktur berbasis kapitalisasi
    private val systemPrompt = """
        Anda adalah Senior Information Systems Auditor bersertifikasi (CISA, ISO 27001 Lead Auditor)
        dengan pengalaman lebih dari 15 tahun di bidang tata kelola teknologi informasi.

        PERAN ANDA:
        Menganalisis skenario IT yang diberikan pengguna, mengidentifikasi celah keamanan, 
        risiko kepatuhan, dan kelemahan tata kelola.

        STANDAR YANG DIGUNAKAN:
        ISO/IEC 27001:2022, COBIT 2019, NIST, dan Peraturan Indonesia (PP 71/2019, UU PDP 2022).

        INSTRUKSI FORMATING SANGAT PENTING (WAJIB DIIKUTI):
        1. DILARANG KERAS menggunakan format Markdown seperti tanda bintang, pagar, atau garis bawah untuk cetak tebal/miring.
        2. DILARANG menggunakan karakter pemisah seperti '---' atau '***'.
        3. Gunakan huruf kapital penuh (ALL CAPS) HANYA untuk judul bagian.
        4. Gunakan poin dengan simbol standar (•) atau penomoran angka (1, 2, 3).
        5. Berikan jarak satu baris kosong antar bagian agar mudah dibaca.

        FORMAT OUTPUT WAJIB:
        
        RINGKASAN AUDIT
        [Tulis 2-3 kalimat ringkasan di sini]

        TEMUAN RISIKO UTAMA
        1. [Temuan pertama]
        2. [Temuan kedua]

        TINGKAT KEPARAHAN DAN DAMPAK
        • TINGGI : [Daftar temuan kritis]
        • SEDANG : [Daftar temuan menengah]
        • RENDAH : [Daftar temuan ringan]

        REKOMENDASI MITIGASI
        • [Langkah mitigasi pertama]
        • [Langkah mitigasi kedua]

        SKOR KEPATUHAN ESTIMASI
        [Skor 0-100] - [Penjelasan singkat mengapa skor tersebut diberikan]
    """.trimIndent()

    private val conversationHistory = mutableListOf<Content>()

    fun clearHistory() {
        conversationHistory.clear()
    }

    // ── Core API Call ─────────────────────────────────────────────────────────
    suspend fun analyzeCompliance(userInput: String): Result<String> {
        if (userInput.isBlank()) {
            return Result.failure(IllegalArgumentException("Input tidak boleh kosong"))
        }

        val messageToSend = if (conversationHistory.isEmpty()) {
            "$systemPrompt\n\nSkenario IT untuk diaudit:\n$userInput"
        } else {
            userInput
        }

        conversationHistory.add(
            Content(parts = listOf(Part(text = messageToSend)), role = "user")
        )

        val requestBody = GeminiRequest(
            contents = conversationHistory.toList(),
            generationConfig = GenerationConfig(
                temperature = 0.3, // Diturunkan sedikit agar lebih kaku dan patuh pada format
                maxOutputTokens = 2048
            )
        )

        return try {
            val httpResponse = client.post(baseUrl) {
                parameter("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val rawBody = httpResponse.body<String>()

            val response = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }.decodeFromString<GeminiResponse>(rawBody)

            val rawResultText = response.candidates
                .firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (!rawResultText.isNullOrBlank()) {
                // LAPISAN PEMBERSIH (SAFETY NET)
                // Berfungsi membuang sisa-sisa markdown jika AI tidak patuh
                val cleanResultText = rawResultText
                    .replace(Regex("\\*\\*"), "") // Hapus ** (bold)
                    .replace(Regex("\\*"), "•")   // Ubah * tunggal menjadi bullet
                    .replace(Regex("(?m)^#+\\s"), "") // Hapus # (heading markdown)
                    .replace("---", "") // Hapus garis pemisah
                    .trim()

                conversationHistory.add(
                    Content(parts = listOf(Part(text = cleanResultText)), role = "model")
                )
                Result.success(cleanResultText)
            } else {
                conversationHistory.removeLastOrNull()
                Result.failure(AuditError.EmptyResponse())
            }

        } catch (e: ResponseException) {
            conversationHistory.removeLastOrNull()
            when (e.response.status.value) {
                401, 403 -> Result.failure(AuditError.Unauthorized())
                429      -> Result.failure(AuditError.RateLimited())
                in 500..599 -> Result.failure(AuditError.ServerError())
                else     -> Result.failure(AuditError.NetworkError("HTTP ${e.response.status.value}"))
            }
        } catch (e: Exception) {
            conversationHistory.removeLastOrNull()
            Result.failure(AuditError.NetworkError(e.message ?: "Kesalahan tidak diketahui"))
        }
    }
}