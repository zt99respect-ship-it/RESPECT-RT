package com.respect.community.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.io.ByteArrayOutputStream

class TelegraphUploader(private val okHttpClient: OkHttpClient) {

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = readAndCompressImage(context, imageUri)
                ?: error("تعذر قراءة الصورة المختارة")

            val endpoints = listOf(
                "https://telegra.ph" to "https://telegra.ph/upload",
                "https://graph.org" to "https://graph.org/upload"
            )

            var lastError: Throwable? = null
            for ((baseUrl, uploadUrl) in endpoints) {
                val result = runCatching {
                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "respect_upload.jpg",
                            bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                        )
                        .build()

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .post(body)
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            error("فشل الرفع (${response.code})")
                        }
                        val array = JSONArray(raw)
                        val src = array.optJSONObject(0)?.optString("src").orEmpty()
                        if (src.isBlank()) {
                            error("استجابة غير صالحة من Telegraph")
                        }
                        if (src.startsWith("http")) src else "$baseUrl$src"
                    }
                }
                if (result.isSuccess) {
                    return@runCatching result.getOrThrow()
                }
                lastError = result.exceptionOrNull()
            }

            throw lastError ?: IllegalStateException("تعذر رفع الصورة إلى Telegraph")
        }
    }

    private fun readAndCompressImage(context: Context, uri: Uri): ByteArray? {
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size) ?: return rawBytes

        val maxDim = 1600
        val scaled = if (decoded.width > maxDim || decoded.height > maxDim) {
            val ratio = maxDim.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
            val w = (decoded.width * ratio).toInt().coerceAtLeast(1)
            val h = (decoded.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, w, h, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        scaled.recycle()
        return out.toByteArray()
    }
}
