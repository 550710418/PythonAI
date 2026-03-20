package com.xiaodaogu.pythonai

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.python.PythonLanguage

@Composable
fun CodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    jumpNonce: Int = 0
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            CodeEditor(ctx).apply {
                setText(code)
                setEditorLanguage(PythonLanguage())
                isLineNumberEnabled = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTextChangedListener {
                    onCodeChange(text.toString())
                }
            }
        },
        update = { editor ->
            if (editor.text.toString() != code) {
                editor.setText(code)
            }
            // jump to end when new script is generated
            if (jumpNonce > 0) {
                editor.setSelection(editor.text.length)
            }
        }
    )
}
