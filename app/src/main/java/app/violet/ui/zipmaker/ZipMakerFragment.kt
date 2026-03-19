package app.violet.ui.zipmaker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import app.violet.R
import app.violet.databinding.FragmentZipmakerBinding
import app.violet.ui.BaseToolFragment
import app.violet.util.*
import kotlinx.coroutines.*
import java.io.File

class ZipMakerFragment : BaseToolFragment() {

    private var _binding: FragmentZipmakerBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZipmakerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.statusBar.progressBar
        tvStatus    = binding.statusBar.tvStatus

        binding.header.tvTitle.text = getString(R.string.nav_zipmaker)
        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.fieldInput.tvLabel.text  = "CHAPTERS FOLDER"
        binding.fieldOutput.tvLabel.text = "OUTPUT FOLDER"

        binding.fieldInput.etPath.setText(Prefs.get(Prefs.Key.ZM_INPUT))
        binding.fieldOutput.etPath.setText(Prefs.get(Prefs.Key.ZM_OUTPUT))

        binding.fieldInput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldInput.etPath.setText(path)
                Prefs.set(Prefs.Key.ZM_INPUT, path)
            }
        }
        binding.fieldOutput.btnBrowse.setOnClickListener {
            pickFolder { path ->
                binding.fieldOutput.etPath.setText(path)
                Prefs.set(Prefs.Key.ZM_OUTPUT, path)
            }
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
        binding.btnStart.isEnabled = false
        resetStatus()

        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inDir  = File(inp)
                    val outDir = File(out).also { it.mkdirs() }

                    val subs = inDir.subDirs().takeIf { it.isNotEmpty() }
                        ?: listOf(inDir)

                    val chapters = subs.map { it to it.imageFiles() }
                        .filter { it.second.isNotEmpty() }

                    if (chapters.isEmpty()) {
                        setStatus("No images found.", 0, R.color.error)
                        return@withContext
                    }

                    chapters.forEachIndexed { i, (folder, images) ->
                        val name = folder.name
                        setStatus("Zipping $name…", i * 100 / chapters.size)
                        val zipFile = File(outDir, "$name.zip")
                        makeZip(images, zipFile) { cur, tot ->
                            val p = i * 100 / chapters.size + cur * 100 / maxOf(tot, 1) / chapters.size
                            setStatus("Zipping $name… ($cur/$tot)", p.coerceIn(0, 99))
                        }
                    }
                    setStatus("✓  All ZIPs created!", 100, R.color.success)
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
