package com.niko.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

private val Bg = Color(0xFF090711)
private val Panel = Color(0xFF171225)
private val Purple = Color(0xFF9B6DFF)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val client = OkHttpClient()

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) onVoiceText?.invoke(spoken)
    }
    private var onVoiceText: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent {
            var input by remember { mutableStateOf("") }
            var apiKey by remember { mutableStateOf("") }
            var showKey by remember { mutableStateOf(false) }
            var busy by remember { mutableStateOf(false) }
            val messages = remember { mutableStateListOf("NIKO" to "سلام زهرا! من نیکو هستم. برای شروع پیام بده یا روی میکروفون بزن.") }

            fun addUser(text: String) {
                messages.add("زهرا" to text)
                val lower = text.lowercase(Locale.ROOT)
                if (lower.contains("زنگ بزن") || lower.contains("تماس بگیر")) {
                    val digits = Regex("[+0-9۰-۹]{7,}").find(text)?.value
                    if (digits != null) {
                        val normalized = digits.map { if (it in '۰'..'۹') '0' + (it - '۰') else it }.joinToString("")
                        startActivity(Intent(Intent.ACTION_DIAL).apply { data = android.net.Uri.parse("tel:$normalized") })
                        messages.add("NIKO" to "صفحه تماس رو باز کردم؛ برای برقراری تماس خودت دکمه تماس رو بزن.")
                    } else messages.add("NIKO" to "شماره یا نام مخاطب رو مشخص کن. فعلاً تماس با شماره از طریق صفحه شماره‌گیر انجام می‌شه.")
                    return
                }
                if (apiKey.isBlank()) {
                    messages.add("NIKO" to "برای پاسخ هوشمند آنلاین، کلید API معتبر را در تنظیمات وارد کن. فعلاً می‌تونم فرمان‌های پایه رو انجام بدم.")
                    return
                }
                busy = true
                val body = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("messages", org.json.JSONArray().put(JSONObject().put("role", "system").put("content", "You are NIKO, a warm, witty personal assistant. Address the user as Zahra. Reply in the language the user uses."))
                        .put(JSONObject().put("role", "user").put("content", text)))
                }.toString()
                val request = Request.Builder().url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType())).build()
                Thread {
                    try {
                        client.newCall(request).execute().use { response ->
                            val raw = response.body?.string().orEmpty()
                            val answer = if (response.isSuccessful) JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                            else "اتصال برقرار نشد (${response.code}). کلید API و اینترنت را بررسی کن."
                            runOnUiThread {
                                messages.add("NIKO" to answer)
                                speak(answer)
                                busy = false
                            }
                        }
                    } catch (_: Exception) {
                        runOnUiThread { messages.add("NIKO" to "اتصال اینترنت یا سرویس هوش مصنوعی در دسترس نیست."); busy = false }
                    }
                }.start()
            }

            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
                Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
                    Text("NIKO AI", color = Purple, fontSize = 28.sp)
                    Text("دستیار شخصی زهرا", color = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages) { (who, msg) ->
                            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(12.dp)) {
                                Text(who, color = Purple)
                                Text(msg, color = Color.White)
                            }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("پیامت رو بنویس…") })
                        IconButton(onClick = { if (input.isNotBlank()) { val v=input; input=""; addUser(v) } }) {
                            Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Purple)
                        }
                        IconButton(onClick = {
                            onVoiceText = { input = it; val v = input; input = ""; addUser(v) }
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "با نیکو صحبت کن")
                            }
                            try { speechLauncher.launch(intent) } catch (_: Exception) {}
                        }) { Icon(Icons.Default.Mic, contentDescription = "میکروفون", tint = Purple) }
                    }
                    TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "بستن تنظیمات API" else "تنظیم اتصال هوش مصنوعی") }
                    if (showKey) {
                        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("OpenAI API key") }, singleLine = true)
                        Text("کلید را خصوصی نگه دار؛ این نسخه آن را فقط در حافظه همین اجرای برنامه نگه می‌دارد.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale("fa", "IR")
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "niko_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
