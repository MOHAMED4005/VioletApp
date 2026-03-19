package app.violet.ui.vertimerge

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import app.violet.R
import app.violet.databinding.FragmentVertimergeBinding
import app.violet.ui.BaseToolFragment
import app.violet.util.*
import kotlinx.coroutines.*
import java.io.File

class VertimergeFragment : BaseToolFragment() {

    private var _binding: FragmentVertimergeBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVertimergeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hook up progress + status views for base class
        progressBar = binding.statusBar.progressBar
        tvStatus    = binding.statusBar.tvStatus

        // Header title + back
        binding.header.tvTitle.text = getString(R.string.nav_vertimerge)
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Restore saved paths
        binding.fieldInput.etPath.setText(Prefs.get(Prefs.Key.VM_INPUT))
        binding.fieldOutput.etPath.setText(Prefs.get(Prefs.Key.VM_OUTPUT))
        binding.fieldInput.tvLabel.text  = "INPUT FOLDER (CHAPTERS)"
        binding.fieldOutput.tvLabel.text = "OUTPUT FOLDER"

        // Restore method
        val savedMethod = Prefs.get(Prefs.Key.VM_METHOD, "pixels")
        if (savedMethod == "pages") {
            binding.chipPages.isChecked = true
            showPagesGroup()
        }

        // Restore values
        binding.etPixelHeight.setText(Prefs.get(Prefs.Key.VM_PX_HEIGHT, "4000"))
        binding.etPageCount.setText(Prefs.get(Prefs.Key.VM_PG_COUNT, "10"))

        val savedSave = Prefs.get(Prefs.Key.VM_SAVE_MODE, "images")
        if (savedSave == "zip") binding.chipSaveZip.isChecked = true

        // Restore format
        when (Prefs.get(Prefs.Key.VM_FORMAT, "JPG")) {
            "PNG"  -> binding.chipPng.isChecked  = true
            "WEBP" -> binding.chipWebp.isChecked = true
            else   -> binding.chipJpg.isChecked  = true
        }

        // Browse buttons
        binding.fieldInput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldInput.etPath.setText(path)
                Prefs.set(Prefs.Key.VM_INPUT, path)
            }
        }
        binding.fieldOutput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldOutput.etPath.setText(path)
                Prefs.set(Prefs.Key.VM_OUTPUT, path)
            }
        }

        // Method chips
        binding.chipGroupMethod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.contains(R.id.chip_pixels)) {
                showPixelsGroup()
                Prefs.set(Prefs.Key.VM_METHOD, "pixels")
            } else {
                showPagesGroup()
                Prefs.set(Prefs.Key.VM_METHOD, "pages")
            }
        }

        // Save typed values
        binding.etPixelHeight.setOnFocusChangeListener { _, _ ->
            Prefs.set(Prefs.Key.VM_PX_HEIGHT, binding.etPixelHeight.text.toString())
        }
        binding.etPageCount.setOnFocusChangeListener { _, _ ->
            Prefs.set(Prefs.Key.VM_PG_COUNT, binding.etPageCount.text.toString())
        }

        // Save mode chips
        binding.chipGroupSave.setOnCheckedStateChangeListener { _, ids ->
            Prefs.set(Prefs.Key.VM_SAVE_MODE, if (ids.contains(R.id.chip_save_zip)) "zip" else "images")
        }

        // Format chips
        binding.chipGroupFormat.setOnCheckedStateChangeListener { _, ids ->
            val fmt = when {
                ids.contains(R.id.chip_png)  -> "PNG"
                ids.contains(R.id.chip_webp) -> "WEBP"
                else                          -> "JPG"
            }
            Prefs.set(Prefs.Key.VM_FORMAT, fmt)
        }

        // Start
        binding.btnStart.setOnClickListener { startProcessing() }
    }

    private fun showPixelsGroup() {
        binding.groupPixels.visibility = View.VISIBLE
        binding.groupPages.visibility  = View.GONE
    }

    private fun showPagesGroup() {
        binding.groupPixels.visibility = View.GONE
        binding.groupPages.visibility  = View.VISIBLE
    }

    private fun startProcessing() {
        val inp = binding.fieldInput.etPath.text.toString().trim()
        val out = binding.fieldOutput.etPath.text.toString().trim()
        if (inp.isEmpty() || out.isEmpty()) {
            setStatus("⚠  Please set input and output folders.", 0, R.color.error)
            return
        }

        val usePages   = binding.chipPages.isChecked
        val pixHeight  = binding.etPixelHeight.text.toString().toIntOrNull() ?: 4000
        val pgCount    = binding.etPageCount.text.toString().toIntOrNull() ?: 10
        val saveAsZip  = binding.chipSaveZip.isChecked
        val fmt        = when {
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

                    // Discover chapters
                    val subs = inDir.subDirs()
                    val chapters = if (subs.isNotEmpty()) {
                        subs.map { it to it.imageFiles() }.filter { it.second.isNotEmpty() }
                    } else {
                        val imgs = inDir.imageFiles()
                        if (imgs.isNotEmpty()) listOf(inDir to imgs) else emptyList()
                    }

                    if (chapters.isEmpty()) {
                        setStatus("No images found.", 0, R.color.error)
                        return@withContext
                    }

                    chapters.forEachIndexed { ci, (folder, images) ->
                        val name   = folder.name
                        val chOut  = File(outDir, name).also { it.mkdirs() }
                        val pct    = (ci * 100 / chapters.size)
                        setStatus("Processing $name…", pct)

                        if (usePages) {
                            mergeByPages(images, chOut, pgCount, fmt, saveAsZip, name) { cur, tot ->
                                val p = pct + (cur * 100 / maxOf(tot, 1)) / chapters.size
                                setStatus("Processing $name… ($cur/$tot)", p.coerceIn(0, 99))
                            }
                        } else {
                            mergeByPixels(images, chOut, pixHeight, fmt) { cur, tot ->
                                val p = pct + (cur * 100 / maxOf(tot, 1)) / chapters.size
                                setStatus("Processing $name… ($cur/$tot)", p.coerceIn(0, 99))
                            }
                        }
                    }
                    setStatus("✓  Done!", 100, R.color.success)
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
