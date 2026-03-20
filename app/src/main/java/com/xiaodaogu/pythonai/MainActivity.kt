package com.xiaodaogu.pythonai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import android.content.Context
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PythonAITheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0F1A)) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    var showConfig by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    var scriptText by remember { mutableStateOf("# 你的脚本会出现在这里\n") }
    var apiBaseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var apiModel by remember { mutableStateOf("gpt-5.4") }
    var editorJumpNonce by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("python_ai", Context.MODE_PRIVATE)
        apiBaseUrl = prefs.getString("api_base", "") ?: ""
        apiKey = prefs.getString("api_key", "") ?: ""
        apiModel = prefs.getString("api_model", "gpt-5.4") ?: "gpt-5.4"
        showConfig = apiBaseUrl.isBlank() || apiKey.isBlank()
        showChat = !showConfig
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showEditor -> EditorScreen(
                scriptText = scriptText,
                jumpNonce = editorJumpNonce,
                onScriptChange = { scriptText = it },
                onBackToChat = { showEditor = false }
            )
            showChat -> ChatScreen(
                apiBaseUrl = apiBaseUrl,
                apiKey = apiKey,
                apiModel = apiModel,
                onScriptGenerated = {
                    scriptText = it
                    editorJumpNonce += 1
                },
                onSwitchToEditor = { showEditor = true },
                onOpenSettings = { showConfig = true }
            )
            showConfig -> ApiConfigScreen(
                initialBaseUrl = apiBaseUrl,
                initialApiKey = apiKey,
                initialModel = apiModel,
                onBack = { showConfig = false },
                onSave = { base, key, model ->
                    apiBaseUrl = base
                    apiKey = key
                    apiModel = model
                    val prefs = context.getSharedPreferences("python_ai", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("api_base", base)
                        .putString("api_key", key)
                        .putString("api_model", model)
                        .apply()
                    showChat = true
                }
            )
            else -> IntroScreen(onContinue = { showConfig = true })
        }
    }
}

@Composable
fun IntroScreen(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B0F1A), Color(0xFF121B2F))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Python+AI",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7CF6FF)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "让 AI 写脚本，让脚本跑起来",
                fontSize = 16.sp,
                color = Color(0xFFB8C7FF),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D6CFF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
            ) {
                Text("开始配置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "© 小稻谷 | QQ: 550710418",
                fontSize = 12.sp,
                color = Color(0xFF6B7A99)
            )
        }
    }
}

@Composable
fun ApiConfigScreen(
    initialBaseUrl: String,
    initialApiKey: String,
    initialModel: String,
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var model by remember { mutableStateOf(initialModel) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F1A))
            .padding(20.dp)
    ) {
        Text(
            text = "配置第三方 API",
            color = Color(0xFFEAF0FF),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "请填写 Base URL、Key 和模型名",
            color = Color(0xFF9FB3FF),
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))

        InputField(label = "Base URL", value = baseUrl, onValueChange = { baseUrl = it })
        Spacer(Modifier.height(12.dp))
        InputField(label = "API Key", value = apiKey, onValueChange = { apiKey = it })
        Spacer(Modifier.height(12.dp))
        InputField(label = "Model", value = model, onValueChange = { model = it })

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (!testing) {
                    testing = true
                    testResult = ""
                    testConnection(
                        baseUrl = baseUrl.trim(),
                        apiKey = apiKey.trim(),
                        model = model.trim(),
                        onDone = { ok, msg ->
                            testing = false
                            testResult = if (ok) "连接成功" else "连接失败：$msg"
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3B5E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (testing) "测试中…" else "测试连接")
        }
        if (testResult.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text = testResult, color = Color(0xFFB8C7FF), fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text("返回", color = Color(0xFF9FB3FF))
            }
            Button(
                onClick = {
                    onSave(baseUrl.trim(), apiKey.trim(), model.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D6CFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存并进入")
            }
        }
    }
}

@Composable
fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(text = label, color = Color(0xFFB8C7FF), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4D6CFF),
                unfocusedBorderColor = Color(0xFF2C3B5E),
                focusedLabelColor = Color(0xFF4D6CFF),
                unfocusedLabelColor = Color(0xFF9FB3FF),
                cursorColor = Color(0xFF7CF6FF),
                focusedTextColor = Color(0xFFEAF0FF),
                unfocusedTextColor = Color(0xFFEAF0FF)
            ),
            singleLine = true
        )
    }
}

@Composable
fun ChatScreen(
    apiBaseUrl: String,
    apiKey: String,
    apiModel: String,
    onScriptGenerated: (String) -> Unit,
    onSwitchToEditor: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val messages = remember {
        mutableStateListOf(
            ChatMessage("assistant", "你好，我可以帮你生成 Python 脚本。"),
        )
    }
    var input by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F1A))
    ) {
        TopBar(title = "Python+AI") {
            IconButton(onClick = onOpenSettings) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "设置", tint = Color(0xFF9FB3FF))
            }
        }
        MessageList(messages = messages, modifier = Modifier.weight(1f))
        ChatInputBar(
            input = input,
            onInputChange = { input = it },
            onSend = {
                if (input.isNotBlank() && !loading) {
                    val prompt = input.trim()
                    messages.add(ChatMessage("user", prompt))
                    input = ""
                    loading = true
                    generateScript(
                        baseUrl = apiBaseUrl,
                        apiKey = apiKey,
                        model = apiModel,
                        prompt = prompt,
                        onDone = { script, error ->
                            loading = false
                            if (error != null) {
                                messages.add(ChatMessage("assistant", "请求失败：$error"))
                            } else {
                                messages.add(ChatMessage("assistant", "脚本已生成，已同步到编辑器。"))
                                onScriptGenerated(script)
                            }
                        }
                    )
                }
            }
        )
        if (loading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color(0xFF7CF6FF)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = onSwitchToEditor,
                containerColor = Color(0xFF4D6CFF),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = "切换编辑器",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun TopBar(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = Color(0xFFEAF0FF),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        messages.forEach { message ->
            MessageBubble(message)
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) Color(0xFF4D6CFF) else Color(0xFF162036)
    val textColor = Color(0xFFEAF0FF)
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ChatInputBar(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("输入你的需求…", color = Color(0xFF6B7A99)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4D6CFF),
                unfocusedBorderColor = Color(0xFF2C3B5E),
                focusedTextColor = Color(0xFFEAF0FF),
                unfocusedTextColor = Color(0xFFEAF0FF),
                cursorColor = Color(0xFF7CF6FF)
            )
        )
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onSend,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D6CFF)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("发送")
        }
    }
}

@Composable
fun EditorScreen(
    scriptText: String,
    jumpNonce: Int,
    onScriptChange: (String) -> Unit,
    onBackToChat: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var showPipDialog by remember { mutableStateOf(false) }
    var pipPkg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F1A))
    ) {
        TopBar(title = "脚本编辑") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showPipDialog = true }) {
                    Text("安装库", color = Color(0xFF7CF6FF))
                }
                IconButton(onClick = {
                    if (!Python.isStarted()) {
                        Python.start(AndroidPlatform(context))
                    }
                    running = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val py = Python.getInstance()
                        val runner = py.getModule("runner")
                        val result = runner.callAttr("run_script", scriptText).toString()
                        withContext(Dispatchers.Main) {
                            output = result
                            running = false
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "运行",
                        tint = Color(0xFF7CF6FF)
                    )
                }
            }
        }

        OutlinedTextField(
            value = scriptText,
            onValueChange = onScriptChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4D6CFF),
                unfocusedBorderColor = Color(0xFF2C3B5E),
                focusedTextColor = Color(0xFFEAF0FF),
                unfocusedTextColor = Color(0xFFEAF0FF),
                cursorColor = Color(0xFF7CF6FF)
            )
        )

        if (running) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                color = Color(0xFF7CF6FF)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackToChat) {
                    Text("返回聊天", color = Color(0xFF9FB3FF))
                }
                Row {
                    TextButton(onClick = { output = "" }) {
                        Text("清空输出", color = Color(0xFF9FB3FF))
                    }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(output))
                    }) {
                        Text("复制输出", color = Color(0xFF9FB3FF))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "运行输出：\n$output",
                color = Color(0xFFB8C7FF),
                fontSize = 12.sp
            )
        }
    }

    if (showPipDialog) {
        AlertDialog(
            onDismissRequest = { showPipDialog = false },
            title = { Text("安装 Python 库") },
            text = {
                OutlinedTextField(
                    value = pipPkg,
                    onValueChange = { pipPkg = it },
                    placeholder = { Text("例如: requests") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPipDialog = false
                    val pkg = pipPkg.trim()
                    if (pkg.isEmpty()) {
                        Toast.makeText(context, "请输入库名", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    if (!Python.isStarted()) {
                        Python.start(AndroidPlatform(context))
                    }
                    running = true
                    CoroutineScope(Dispatchers.IO).launch {
                        val py = Python.getInstance()
                        val runner = py.getModule("runner")
                        val result = runner.callAttr("pip_install", pkg).toString()
                        withContext(Dispatchers.Main) {
                            output = result
                            running = false
                            Toast.makeText(context, "安装完成", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("安装") }
            },
            dismissButton = {
                TextButton(onClick = { showPipDialog = false }) { Text("取消") }
            }
        )
    }
}

@Immutable
data class ChatMessage(
    val role: String,
    val content: String
)

fun generateScript(
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    onDone: (String, String?) -> Unit
) {
    if (baseUrl.isBlank() || apiKey.isBlank()) {
        onDone("", "未配置 API")
        return
    }
    val client = OkHttpClient.Builder()
        .callTimeout(60, TimeUnit.SECONDS)
        .build()
    val bodyJson = JSONObject().apply {
        put("model", model.ifBlank { "gpt-5.4" })
        put("input", prompt)
        put("temperature", 0.2)
    }
    val reqBody = bodyJson.toString().toRequestBody("application/json".toMediaType())
    val req = Request.Builder()
        .url(baseUrl.trimEnd('/') + "/responses")
        .addHeader("Authorization", "Bearer $apiKey")
        .addHeader("Content-Type", "application/json")
        .post(reqBody)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    withContext(Dispatchers.Main) { onDone("", "HTTP ${resp.code}: ${body.take(200)}") }
                    return@use
                }
                val json = JSONObject(body)
                val content = if (json.has("output_text")) {
                    json.getString("output_text")
                } else if (json.has("choices")) {
                    json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                } else {
                    body
                }
                withContext(Dispatchers.Main) { onDone(content, null) }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onDone("", e.message ?: "network error") }
        }
    }
}

fun testConnection(
    baseUrl: String,
    apiKey: String,
    model: String,
    onDone: (Boolean, String?) -> Unit
) {
    if (baseUrl.isBlank() || apiKey.isBlank()) {
        onDone(false, "缺少 Base URL 或 API Key")
        return
    }
    val client = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()
    val req = Request.Builder()
        .url(baseUrl.trimEnd('/') + "/models")
        .addHeader("Authorization", "Bearer $apiKey")
        .get()
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                withContext(Dispatchers.Main) {
                    if (!resp.isSuccessful) {
                        onDone(false, "HTTP ${resp.code}: ${body.take(200)}")
                    } else {
                        onDone(true, null)
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onDone(false, e.message ?: "network error") }
        }
    }
}

@Composable
fun PythonAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4D6CFF),
            secondary = Color(0xFF7CF6FF),
            background = Color(0xFF0B0F1A),
            surface = Color(0xFF0B0F1A)
        ),
        typography = Typography(),
        content = content
    )
}
