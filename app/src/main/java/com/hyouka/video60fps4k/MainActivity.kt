package com.hyouka.video60fps4k

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.android.material.color.DynamicColors
import com.hyouka.video60fps4k.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedUri: Uri? = null
    private var outputFile: File? = null
    private var selectedResolution = OutputResolution.UHD_4K
    private var selectedFps = OutputFps.FPS_60
    private val executor = Executors.newSingleThreadExecutor()

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult

        selectedUri = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }

        binding.sourceName.text = queryDisplayName(uri)
        binding.resultCard.visibility = View.GONE
        binding.statusText.text = getString(R.string.status_idle)
        binding.convertButton.isEnabled = true
        updateSelectionSummary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.progressBar.visibility = View.GONE

        binding.resolution4k.isChecked = true
        binding.fps60.isChecked = true

        binding.chooseButton.setOnClickListener {
            picker.launch(arrayOf("video/*"))
        }

        binding.convertButton.setOnClickListener {
            selectedUri?.let(::convertVideo)
        }

        binding.shareButton.setOnClickListener {
            outputFile?.let(::shareVideo)
        }

        binding.resolution1080p.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedResolution = OutputResolution.HD_1080P
                updateSelectionSummary()
            }
        }
        binding.resolution2k.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedResolution = OutputResolution.QHD_2K
                updateSelectionSummary()
            }
        }
        binding.resolution4k.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedResolution = OutputResolution.UHD_4K
                updateSelectionSummary()
            }
        }

        binding.fps60.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_60
                updateSelectionSummary()
            }
        }
        binding.fps90.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_90
                updateSelectionSummary()
            }
        }
        binding.fps120.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_120
                updateSelectionSummary()
            }
        }
        binding.fps144.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_144
                updateSelectionSummary()
            }
        }
        binding.fps240.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_240
                updateSelectionSummary()
            }
        }
        binding.fps360.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                selectedFps = OutputFps.FPS_360
                updateSelectionSummary()
            }
        }

        updateSelectionSummary()
    }

    private fun updateSelectionSummary() {
        val size = "${selectedResolution.width}×${selectedResolution.height}"
        binding.sourceInfo.text = getString(
            R.string.output_summary,
            size,
            selectedFps.value
        )
        binding.convertButton.text = getString(
            R.string.convert_format,
            selectedResolution.label,
            selectedFps.value
        )
    }

    private fun convertVideo(uri: Uri) {
        val settings = ConversionSettings(selectedResolution, selectedFps)
        setProcessingState(true)

        executor.execute {
            var inputFile: File? = null
            try {
                inputFile = File(cacheDir, "input-${System.currentTimeMillis()}.mp4")
                val source = contentResolver.openInputStream(uri)
                requireNotNull(source) { "Unable to read selected video." }
                source.use { input ->
                    inputFile.outputStream().use { output -> input.copyTo(output) }
                }

                val moviesDir =
                    getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: File(filesDir, "Movies")
                moviesDir.mkdirs()

                val out = File(
                    moviesDir,
                    "Hyouka_${settings.resolution.label}_${settings.fps.value}FPS_${System.currentTimeMillis()}.mp4"
                )
                outputFile = out

                val command = VideoProcessingSpec.buildCommand(
                    inputFile.absolutePath,
                    out.absolutePath,
                    settings
                )
                val session = FFmpegKit.execute(command)
                val success = ReturnCode.isSuccess(session.returnCode)

                runOnUiThread {
                    setProcessingState(false)
                    if (success) {
                        binding.statusText.text = getString(R.string.status_done)
                        binding.resultInfo.text = getString(
                            R.string.result_format,
                            out.name,
                            settings.resolution.width,
                            settings.resolution.height,
                            settings.fps.value
                        )
                        binding.resultCard.visibility = View.VISIBLE
                        Toast.makeText(
                            this,
                            getString(R.string.toast_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        out.delete()
                        binding.statusText.text = getString(R.string.status_error)
                        Toast.makeText(
                            this,
                            "FFmpeg failed: " + (session.failStackTrace ?: "unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    inputFile.delete()
                }
            } catch (t: Throwable) {
                inputFile?.delete()
                runOnUiThread {
                    setProcessingState(false)
                    binding.statusText.text = getString(R.string.status_error)
                    Toast.makeText(
                        this,
                        t.message ?: getString(R.string.unknown_error),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setProcessingState(processing: Boolean) {
        binding.chooseButton.isEnabled = !processing
        binding.convertButton.isEnabled = !processing && selectedUri != null
        setChipGroupEnabled(binding.resolutionGroup, !processing)
        setChipGroupEnabled(binding.fpsGroup, !processing)
        binding.progressBar.visibility = if (processing) View.VISIBLE else View.GONE
        binding.progressBar.isIndeterminate = true

        if (processing) {
            binding.statusText.text = getString(
                R.string.status_processing_format,
                selectedResolution.label,
                selectedFps.value
            )
        }
    }

    private fun setChipGroupEnabled(group: ViewGroup, enabled: Boolean) {
        for (index in 0 until group.childCount) {
            group.getChildAt(index).isEnabled = enabled
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: getString(R.string.selected_video)
    }

    private fun shareVideo(file: File) {
        val uri = FileProvider.getUriForFile(
            this,
            "com.hyouka.video60fps4k.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
