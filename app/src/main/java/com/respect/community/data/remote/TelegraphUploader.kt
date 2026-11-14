package com.respect.community.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class TelegraphUploader(private val okHttpClient: OkHttpClient) {

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var sourceTemp: File? = null
        var compressedFile: File? = null
        try {
            sourceTemp = copyUriToTempFile(context, imageUri)
                ?: return@withContext Result.failure(IllegalStateException("تعذر قراءة الصورة المختارة"))

            compressedFile = runCatching {
                Compressor.compress(context, sourceTemp) {
                    resolution(1600, 1600)
                    quality(85)
                    format(Bitmap.CompressFormat.JPEG)
                    size(4_500_000L) // Safe < 5MB Telegraph limit
                }
            }.getOrDefault(sourceTemp)

            val endpoints = listOf(
                "https://api.telegra.ph" to "https://api.telegra.ph/upload",
                "https://telegra.ph" to "https://telegra.ph/upload"
            )

            var lastError: Throwable? = null
            for ((baseUrl, uploadUrl) in endpoints) {
                val attempt = runCatching {
                    val body = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "respect_upload.jpg",
                            compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
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
                        if (src.startsWith("http")) src else "${baseUrl.replace("api.", "")}$src"
                    }
                }
                if (attempt.isSuccess) {
                    return@withContext attempt
                }
                lastError = attempt.exceptionOrNull()
            }
            Result.failure(lastError ?: IllegalStateException("تعذر رفع الصورة إلى Telegraph"))
        } catch (t: Throwable) {
            Result.failure(t)
        } finally {
            runCatching { sourceTemp?.delete() }
            if (compressedFile != sourceTemp) {
                runCatching { compressedFile?.delete() }
            }
        }
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val temp = File.createTempFile("respect_src_", ".jpg", context.cacheDir)
        input.use { ins ->
            FileOutputStream(temp).use { out ->
                ins.copyTo(out)
            }
        }
        return temp
    }
}
