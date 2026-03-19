package app.violet.ui.watermark

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import app.violet.R
import app.violet.databinding.FragmentWatermarkBinding
import app.violet.ui.BaseToolFragment
import app.violet.util.*
import kotlinx.coroutines.*
import java.io.File

class WatermarkFragment : BaseToolFragment() {

    private var _binding: FragmentWatermarkBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatermarkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.statusBar.progressBar
        tvStatus    = binding.statusBar.tvStatus

        binding.header.tvTitle.text = getString(R.string.nav_watermark)
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Labels
        binding.fieldLogo.tvLabel.text    = "LOGO / WATERMARK IMAGE"
        binding.fieldChapter.tvLabel.text = "CHAPTER FOLDER"
        binding.fieldOutput.tvLabel.text  = "OUTPUT FOLDER"

        // Restore saved paths
        binding.etStartPage.setText(Prefs.get(Prefs.Key.WM_START))
        binding.etEndPage.setText(Prefs.get(Prefs.Key.WM_END))
        binding.fieldLogo.etPath.setText(Prefs.get(Prefs.Key.WM_LOGO))
        binding.fieldChapter.etPath.setText(Prefs.get(Prefs.Key.WM_CHAPTER))
        binding.fieldOutput.etPath.setText(Prefs.get(Prefs.Key.WM_OUTPUT))

        // Browse — start page (image file)
        binding.btnStartPage.setOnClickListener {
            pickFile { path ->
                binding.etStartPage.setText(path)
                Prefs.set(Prefs.Key.WM_START, path)
            }
        }
        // Browse — end page (image file)
        binding.btnEndPage.setOnClickListener {
            pickFile { path ->
                binding.etEndPage.setText(path)
                Prefs.set(Prefs.Key.WM_END, path)
            }
        }
        // Browse — logo (image file)
        binding.fieldLogo.btnBrowse.setOnClickListener {
            pickFile { path ->
                binding.fieldLogo.etPath.setText(path)
                Prefs.set(Prefs.Key.WM_LOGO, path)
            }
        }
        // Browse — chapter folder
        binding.fieldChapter.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldChapter.etPath.setText(path)
                Prefs.set(Prefs.Key.WM_CHAPTER, path)
            }
        }
        // Browse — output folder
        binding.fieldOutput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldOutput.etPath.setText(path)
                Prefs.set(Prefs.Key.WM_OUTPUT, path)
            }
        }

        binding.btnStart.setOnClickListener { startProcessing() }
    }

    private fun startProcessing() {
        val startPath   = binding.etStartPage.text.toString().trim()
        val endPath     = binding.etEndPage.text.toString().trim()
        val logoPath    = binding.fieldLogo.etPath.text.toString().trim()
        val chapterPath = binding.fieldChapter.etPath.text.toString().trim()
        val outputPath  = binding.fieldOutput.etPath.text.toString().trim()

        val missing = listOf(
            startPath   to "start page",
            endPath     to "end page",
            logoPath    to "logo",
            chapterPath to "chapter folder",
            outputPath  to "output folder"
        ).firstOrNull { it.first.isEmpty() }

        if (missing != null) {
            setStatus("⚠  Please set ${missing.second}.", 0, R.color.error)
            return
        }

        binding.btnStart.isEnabled = false
        resetStatus()

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val logoFile  = File(logoPath)
                    val logo      = BitmapFactory.decodeFile(logoFile.absolutePath)
                        ?: throw IllegalArgumentException("Cannot load logo image.")

                    val inDir     = File(chapterPath)
                    val outDir    = File(outputPath).also { it.mkdirs() }

                    // Discover chapters
                    val subs = inDir.subDirs()
                    val chapters = if (subs.isNotEmpty()) {
                        subs.map { it to it.imageFiles() }.filter { it.second.isNotEmpty() }
                    } else {
                        val imgs = inDir.imageFiles()
                        if (imgs.isNotEmpty()) listOf(inDir to imgs) else emptyList()
                    }

                    if (chapters.isEmpty()) {
                        setStatus("No images found in chapter folder.", 0, R.color.error)
                        logo.recycle()
                        return@withContext
                    }

                    val total = chapters.size
                    chapters.forEachIndexed { ci, (folder, images) ->
                        val name  = folder.name
                        val chOut = File(outDir, name).also { it.mkdirs() }
                        setStatus("Watermarking $name…", ci * 100 / total)

                        // Build full page list: start + chapter pages + end
                        val allPages = buildList {
                            add(File(startPath))
                            addAll(images)
                            add(File(endPath))
                        }

                        allPages.forEachIndexed { idx, srcFile ->
                            val isCover = idx == 0 || idx == allPages.lastIndex
                            val dest    = File(chOut, "%02d.jpg".format(idx + 1))
                            val page    = BitmapFactory.decodeFile(srcFile.absolutePath)
                                ?: return@forEachIndexed

                            if (isCover) {
                                page.saveTo(dest)
                                page.recycle()
                            } else {
                                val stamped = stampWatermark(page, logo)
                                stamped.saveTo(dest)
                                stamped.recycle()
                                page.recycle()
                            }
                        }
                    }

                    logo.recycle()
                    setStatus("✓  Watermarking complete!", 100, R.color.success)
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
