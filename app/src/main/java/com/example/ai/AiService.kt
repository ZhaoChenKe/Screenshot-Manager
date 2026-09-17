package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AiAnalysisResult(
    val title: String,
    val categoryId: String,
    val tags: List<String>,
    val summary: String
)

interface AiService {
    suspend fun analyzeScreenshot(
        ocrText: String,
        bitmap: Bitmap?,
        customApiKey: String?
    ): AiAnalysisResult?
}

class GeminiAiService : AiService {

    companion object {
        private const val TAG = "GeminiAiService"
        private const val MODEL_NAME = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeScreenshot(
        ocrText: String,
        bitmap: Bitmap?,
        customApiKey: String?
    ): AiAnalysisResult? = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) {
            customApiKey
        } else {
            BuildConfig.GEMINI_API_KEY
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "No valid Gemini API key configured, skipping cloud AI analysis")
            return@withContext null
        }

        try {
            val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val prompt = """
                你是一个专业的手机截图内容分析助手。根据以下截图中的文字和图片信息，生成结构化分析。
                可选分类ID（只能选其中一个）：
                - shopping (购物)
                - chat (聊天)
                - work (工作)
                - study (学习)
                - game (游戏，包含各类手游网游、对局战绩、MVP、排位结算、出装抽卡、深渊、Steam等。若包含游戏元素，即便有微信/QQ登录或游戏商城，仍必须归为 game)
                - travel (旅行)
                - finance (消费)
                - express (快递)
                - social (社交)
                - doc (资料)
                - location (地址)
                - order (订单)
                - web (网页)
                - other (其他)

                请严格按照以下 JSON 格式输出，不要包含任何 markdown 格式代码块：
                {
                  "title": "简明扼要的标题（15字以内）",
                  "categoryId": "对应的分类ID",
                  "tags": ["标签1", "标签2", "标签3"],
                  "summary": "1至2句话提炼核心信息（如价格、时间、地点、单号等）"
                }

                截图提取文字：
                $ocrText
            """.trimIndent()

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            // Optionally attach compressed low-res thumbnail if OCR text is short
            if (ocrText.length < 50 && bitmap != null) {
                try {
                    val stream = ByteArrayOutputStream()
                    // Compress thumbnail
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
                    val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", base64Data)
                    }
                    partsArray.put(JSONObject().put("inlineData", inlineData))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to compress thumbnail for Gemini request", e)
                }
            }

            val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
            val requestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API error: ${response.code} - ${response.message}")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: return@withContext null
                val rootJson = JSONObject(responseBodyStr)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val resParts = contentObj?.optJSONArray("parts")
                    val rawText = resParts?.optJSONObject(0)?.optString("text") ?: ""

                    // Parse JSON output
                    val cleanJsonStr = rawText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val parsed = JSONObject(cleanJsonStr)
                    val title = parsed.optString("title", "")
                    val categoryId = parsed.optString("categoryId", "other")
                    val summary = parsed.optString("summary", "")

                    val tagsList = mutableListOf<String>()
                    val tagsArray = parsed.optJSONArray("tags")
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            val t = tagsArray.optString(i).trim()
                            if (t.isNotBlank()) tagsList.add(t)
                        }
                    }

                    return@withContext AiAnalysisResult(
                        title = title,
                        categoryId = categoryId,
                        tags = tagsList,
                        summary = summary
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed during Gemini AI call", e)
        }

        return@withContext null
    }
}
