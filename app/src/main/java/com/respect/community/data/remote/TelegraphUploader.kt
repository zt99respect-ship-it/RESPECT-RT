package com.respect.community.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import id.zelory.compressor.constraint.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class TelegraphUploader(private val okHttpClient: OkHttpClient) {

    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        var sourceTemp: File? = null
        var compressedFile: File? = null
        var fallbackJpeg: File? = null
        try {
            sourceTemp = copyUriToTempFile(context, imageUri)
                ?: throw IllegalStateException("تعذر قراءة الصورة المختارة من الجهاز")

            compressedFile = runCatching {
                Compressor.compress(context, sourceTemp) {
                    resolution(1600, 1600)
                    quality(85)
                    format(Bitmap.CompressFormat.JPEG)
                    size(MAX_UPLOAD_BYTES)
                }
            }.getOrNull()

            val candidate = if (compressedFile != null && compressedFile.exists() && compressedFile.length() in 1L..MAX_UPLOAD_BYTES) {
                compressedFile
            } else {
                fallbackJpeg = reencodeUnderLimit(context, sourceTemp)
                fallbackJpeg
            }

            if (!candidate.exists() || candidate.length() <= 0L) {
                throw IllegalStateException("الصورة المختارة فارغة أو تالفة")
            }

            val imageMediaType = "image/jpeg".toMediaType()
            val endpoints = listOf(
                "https://api.telegra.ph" to "https://api.telegra.ph/upload",
                "https://telegra.ph" to "https://telegra.ph/upload"
            )

            var lastError: Throwable? = null
            for ((baseUrl, uploadUrl) in endpoints) {
                val attempt = runCatching {
                    val multipartBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            "file",
                            "respect_${System.currentTimeMillis()}.jpg",
                            candidate.asRequestBody(imageMediaType)
                        )
                        .build()

                    val request = Request.Builder()
                        .url(uploadUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .post(multipartBody)
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        val raw = response.body?.string().orEmpty().trim()
                        if (!response.isSuccessful) {
                            val errMsg = parseTelegraphError(raw) ?: "رمز الخطأ ${response.code}"
                            throw IllegalStateException("تعذر رفع الصورة: $errMsg")
                        }
                        if (raw.startsWith("{")) {
                            val errObj = JSONObject(raw)
                            val err = errObj.optString("error")
                            throw IllegalStateException(err.ifBlank { "رفض الخادم الصورة المختارة" })
                        }
                        val array = JSONArray(raw)
                        val src = array.optJSONObject(0)?.optString("src").orEmpty()
                        if (src.isBlank()) {
                            throw IllegalStateException("لم يُرجع تيليغراف رابط الصورة")
                        }
                        if (src.startsWith("http")) src else "${baseUrl.replace("api.", "")}$src"
                    }
                }
                if (attempt.isSuccess) {
                    return@withContext attempt
                }
                lastError = attempt.exceptionOrNull()
            }

            val finalError = lastError ?: IllegalStateException("تعذر رفع الصورة عبر تيليغراف")
            showErrorToast(context, finalError.message ?: "فشل رفع الصورة، يرجى المحاولة بصورة أخرى")
            Result.failure(finalError)
        } catch (t: Throwable) {
            val friendly = t.message?.takeIf { it.isNotBlank() } ?: "حدث خطأ أثناء ضغط أو رفع الصورة"
            showErrorToast(context, friendly)
            Result.failure(IllegalStateException(friendly, t))
        } finally {
            runCatching { sourceTemp?.delete() }
            if (compressedFile != null && compressedFile != sourceTemp) {
                runCatching { compressedFile.delete() }
            }
            if (fallbackJpeg != null && fallbackJpeg != sourceTemp && fallbackJpeg != compressedFile) {
                runCatching { fallbackJpeg.delete() }
            }
        }
    }

    private fun parseTelegraphError(raw: String): String? {
        if (raw.isBlank() || !raw.startsWith("{")) return null
        return runCatching { JSONObject(raw).optString("error").takeIf { it.isNotBlank() } }.getOrNull()
    }

    private suspend fun showErrorToast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun reencodeUnderLimit(context: Context, source: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 1600 || bounds.outHeight / sampleSize > 1600) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(source.absolutePath, decodeOpts)
            ?: throw IllegalStateException("تعذر معالجة ملف الصورة")

        val outFile = File.createTempFile("respect_jpeg_", ".jpg", context.cacheDir)
        var quality = 85
        try {
            while (quality >= 35) {
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
                if (outFile.length() in 1L..MAX_UPLOAD_BYTES) {
                    return outFile
                }
                quality -= 15
            }
        } finally {
            bitmap.recycle()
        }
        return outFile
    }

    private fun copyUriToTempFile(context: Context, uri: Uri): File? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val temp = File.createTempFile("respect_src_", ".tmp", context.cacheDir)
        input.use { ins ->
            FileOutputStream(temp).use { out ->
                ins.copyTo(out)
            }
        }
        return temp
    }

    companion object {
        private const val MAX_UPLOAD_BYTES = 4_500_000L
    }
}
