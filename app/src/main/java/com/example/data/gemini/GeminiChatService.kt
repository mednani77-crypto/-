package com.example.data.gemini

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiChatService {
    // Model specified by prompt instructions: gemini-3.8-flash for general tasks
    private const val MODEL_NAME = "gemini-3.8-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private const val SYSTEM_INSTRUCTION_TEXT = """
أنت 'مساعد سيسكو التجاري والمحاسبي الذكي' (SYSCO AI Commercial Assistant)، المستشار الرسمي الذكي لمندوبي المبيعات لشركة SYSCO LTD في جمهورية جيبوتي (Djibouti).
مهامك واختصاصاتك:
1. تقديم الاستشارات التجارية الميدانية وإدارة كتالوج المواد الغذائية والسلع التموينية (الأرز البسمتي، زيوت الطهي، السكر، الحليب المجفف، الشاي الكيني، المعلبات والمكرونة).
2. متابعة حسابات العملاء والمديونيات في مناطق جيبوتي (بلبلا Balbala، شارع 26، المنطقة الحرة Free Zone، حي الهدى، وسط المدينة).
3. التعامل بالعملتين المعتمدتين: الدولار الأمريكي (USD $) والفرنك الجيبوتي (DJF) وفق سعر الصرف الافتراضي (1 USD = 177.72 DJF).
4. مساعدة المندوب في صياغة رسائل واتساب احترافية ومؤدبة لمطالبة العملاء بالسداد أو إرسال عروض الأسعار الترويجية.
5. الإجابة بأسلوب عربي فصيح، مهني، مشجع ومختصر ومناسب لبيئة العمل التجاري الميداني في جيبوتي.
"""

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("يرجى إدخال مفتاح GEMINI_API_KEY في لوحة أسرار AI Studio (Secrets Panel) للاتصال بالذكاء الاصطناعي.")
            )
        }

        try {
            val root = JSONObject()

            // 1. System Instruction
            val sysInstructionObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", SYSTEM_INSTRUCTION_TEXT))
            sysInstructionObj.put("parts", sysParts)
            root.put("systemInstruction", sysInstructionObj)

            // 2. Multi-turn conversation contents
            val contentsArray = JSONArray()
            // Add prior turns
            for (msg in history.takeLast(10)) {
                val turnObj = JSONObject()
                turnObj.put("role", msg.role)
                val parts = JSONArray()
                parts.put(JSONObject().put("text", msg.content))
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Add the new user turn
            val newTurn = JSONObject()
            newTurn.put("role", "user")
            val newParts = JSONArray()
            newParts.put(JSONObject().put("text", userMessage))
            newTurn.put("parts", newParts)
            contentsArray.put(newTurn)

            root.put("contents", contentsArray)

            // 3. Generation Config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            root.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("فشل الاتصال بـ Gemini (${response.code}): $responseString")
                )
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(Exception("لم يرد أي محتوى من نموذج الذكاء الاصطناعي"))
            }

            val candidate = candidates.getJSONObject(0)
            val contentObj = candidate.optJSONObject("content")
            val partsArray = contentObj?.optJSONArray("parts")
            val replyText = partsArray?.optJSONObject(0)?.optString("text")

            if (replyText.isNullOrBlank()) {
                Result.failure(Exception("استجابة فارغة من Gemini"))
            } else {
                Result.success(replyText.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
