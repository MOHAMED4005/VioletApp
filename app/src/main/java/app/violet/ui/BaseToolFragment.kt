package app.violet.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import app.violet.R

/**
 * Base fragment that wires up:
 *  - Folder/file picker via SAF (Storage Access Framework)
 *  - A progress + status bar
 *  - Navigation back button
 */
abstract class BaseToolFragment : Fragment() {

    // Subclass sets these to the actual views after inflation
    protected var progressBar: ProgressBar? = null
    protected var tvStatus: TextView? = null

    // Pending callback for the current picker launch
    private var pickerCallback: ((String) -> Unit)? = null
    private var pickerMode: PickerMode = PickerMode.FOLDER

    enum class PickerMode { FOLDER, FILE }

    // ── Folder picker ──────────────────────────────────────────────────────
    private val folderLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val path = uriToPath(uri)
                    pickerCallback?.invoke(path)
                }
            }
            pickerCallback = null
        }

    // ── File picker ────────────────────────────────────────────────────────
    private val fileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val path = uriToPath(uri)
                    pickerCallback?.invoke(path)
                }
            }
            pickerCallback = null
        }

    /** Open the SAF folder chooser and return the result to [callback]. */
    protected fun pickFolder(callback: (String) -> Unit) {
        pickerCallback = callback
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        folderLauncher.launch(intent)
    }

    /** Open the SAF file chooser restricted to images and return to [callback]. */
    protected fun pickFile(callback: (String) -> Unit) {
        pickerCallback = callback
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        fileLauncher.launch(intent)
    }

    /**
     * Convert a SAF uri to an absolute file-system path.
     * Works for primary storage; on secondary volumes returns the uri string as fallback.
     */
    private fun uriToPath(uri: Uri): String {
        val docId = DocumentsContract.getTreeDocumentId(uri)
            ?: DocumentsContract.getDocumentId(uri)
            ?: return uri.toString()

        val split = docId.split(":")
        val type  = split.getOrNull(0) ?: ""
        val rel   = split.getOrNull(1) ?: ""

        return if (type.equals("primary", ignoreCase = true)) {
            "${android.os.Environment.getExternalStorageDirectory()}/$rel"
        } else {
            // For file URIs or other providers
            uri.path ?: uri.toString()
        }
    }

    // ── Status bar helpers ─────────────────────────────────────────────────

    protected fun setStatus(msg: String, progress: Int, @ColorRes colorRes: Int = R.color.subtext) {
        activity?.runOnUiThread {
            tvStatus?.text = msg
            tvStatus?.setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            progressBar?.progress = progress
        }
    }

    protected fun resetStatus() = setStatus("", 0)
}
