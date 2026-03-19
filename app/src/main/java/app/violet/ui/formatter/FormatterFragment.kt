package app.violet.ui.formatter

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import app.violet.R
import app.violet.databinding.FragmentFormatterBinding
import app.violet.ui.BaseToolFragment
import app.violet.util.*
import kotlinx.coroutines.*
import java.io.File

class FormatterFragment : BaseToolFragment() {

    private var _binding: FragmentFormatterBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFormatterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.statusBar.progressBar
        tvStatus    = binding.statusBar.tvStatus

        binding.header.tvTitle.text = getString(R.string.nav_formatter)
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.fieldInput.tvLabel.text  = "IMAGES FOLDER"
        binding.fieldOutput.tvLabel.text = "OUTPUT FOLDER"

        // Restore saved paths
        binding.fieldInput.etPath.setText(Prefs.get(Prefs.Key.FC_INPUT))
        binding.fieldOutput.etPath.setText(Prefs.get(Prefs.Key.FC_OUTPUT))

        // Restore format
        when (Prefs.get(Prefs.Key.FC_FORMAT, "JPG")) {
            "PNG"  -> binding.chipPng.isChecked  = true
            "WEBP" -> binding.chipWebp.isChecked = true
            else   -> binding.chipJpg.isChecked  = true
        }

        // Browse buttons
        binding.fieldInput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldInput.etPath.setText(path)
                Prefs.set(Prefs.Key.FC_INPUT, path)
            }
        }
        binding.fieldOutput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldOutput.etPath.setText(path)
                Prefs.set(Prefs.Key.FC_OUTPUT, path)
            }
        }

        // Save format on chip change
        binding.chipGroupFormat.setOnCheckedStateChangeListener { _, ids ->
            val fmt = when {
                ids.contains(R.id.chip_png)  -> "PNG"
                ids.contains(R.id.chip_webp) -> "WEBP"
                else                          -> "JPG"
            }
            Prefs.set(Prefs.Key.FC_FORMAT, fmt)
        }

        binding.btnStart.setOnClickListener { startProcessing() }
    }

    private fun startProcessing() {
        val inp = binding.fieldInput.etPath.text.toString().trim()
        val out = binding.fieldOutput.etPath.text.toString().trim()
        if (inp.isEmpty() || out.isEmpty()) {
            setStatus("⚠  Please set input and output folders.", 0, R.color.error)
            return
        }

        val format = when {
            binding.chipPng.isChecked  -> Bitmap.CompressFormat.PNG
            binding.chipWebp.isChecked -> Bitmap.CompressFormat.WEBP_LOSSY
            else                        -> Bitmap.CompressFormat.JPEG
        }

        binding.btnStart.isEnabled = false
        resetStatus()

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inDir  = File(inp)
                    val outDir = File(out).also { it.mkdirs() }
                    val images = inDir.imageFiles()

                    if (images.isEmpty()) {
                        setStatus("No images found.", 0, R.color.error)
                        return@withContext
                    }

                    convertFormat(images, outDir, format) { cur, tot ->
                        val pct = if (tot > 0) cur * 100 / tot else 0
                        setStatus("Converting… ($cur/$tot)", pct.coerceIn(0, 99))
                    }

                    setStatus("✓  Conversion complete!", 100, R.color.success)
                } catch (e: Exception) {
                    setStatus("Error: ${e.message}", 0, R.color.error)
                }
            }
            binding.btnStart.isEnabled = true
        }
    }

    override fun onDestroyView() {
        scope.cancel()
        super.onDestroyView()
        _binding = null
    }
}
