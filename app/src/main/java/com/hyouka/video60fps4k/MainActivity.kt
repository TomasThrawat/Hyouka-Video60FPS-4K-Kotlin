package com.hyouka.video60fps4k

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.hyouka.video60fps4k.databinding.ActivityMainBinding
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedUri: Uri? = null
    private var outputFile: File? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        selectedUri = uri
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
        }
        binding.sourceName.text = queryDisplayName(uri)
        binding.sourceInfo.text = "Output: 3840×2160 • 60 FPS"
        binding.convertButton.isEnabled = true
        binding.resultCard.visibility = View.GONE
        binding.statusText.text = getString(R.string.status_idle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.progressBar.visibility = View.GONE

        binding.chooseButton.setOnClickListener {
            picker.launch(arrayOf("video/*"))
        }
        binding.convertButton.setOnClickListener {
            selectedUri?.let(::convertVideo)
        }
        binding.shareButton.setOnClickListener {
            outputFile?.let(::shareVideo)
        }
    }

    private fun convertVideo(uri: Uri) {
        setProcessingState(true)

        executor.execute {
            var inputFile: File? = null
            try {
                inputFile = File(cacheDir, "input-" + System.currentTimeMillis() + ".mp4")
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
                    "Hyouka_60FPS_4K_" + System.currentTimeMillis() + ".mp4"
                )
                outputFile = out

                val session = FFmpegKit.execute(buildCommand(inputFile, out))
                val success = ReturnCode.isSuccess(session.returnCode)

                runOnUiThread {
                    setProcessingState(false)
                    if (success) {
                        binding.statusText.text = getString(R.string.status_done)
                        binding.resultInfo.text =
                            out.name + "
3840×2160 • 60 FPS • H.264 + AAC"
                        binding.resultCard.visibility = View.VISIBLE
                        Toast.makeText(
                            this,
                            "Video processed successfully",
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
                    inputFile?.delete()
                }
            } catch (t: Throwable) {
                inputFile?.delete()
                runOnUiThread {
                    setProcessingState(false)
                    binding.statusText.text = getString(R.string.status_error)
                    Toast.makeText(
                        this,
                        t.message ?: "Unknown error",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setProcessingState(processing: Boolean) {
        binding.chooseButton.isEnabled = !processing
        binding.convertButton.isEnabled = !processing && selectedUri != null
        binding.progressBar.visibility = if (processing) View.VISIBLE else View.GONE
        binding.progressBar.isIndeterminate = true
        if (processing) binding.statusText.text = getString(R.string.status_processing)
    }

    private fun buildCommand(input: File, output: File): String {
        val filter =
            "minterpolate=fps=60:mi_mode=mci:mc_mode=aobmc:me_mode=bidir:vsbmc=1," +
                "scale=3840:2160:force_original_aspect_ratio=decrease," +
                "pad=3840:2160:(ow-iw)/2:(oh-ih)/2"

        return listOf(
            "-y",
            "-i", quote(input.absolutePath),
            "-vf", quote(filter),
            "-map", "0:v:0",
            "-map", "0:a?",
            "-c:v", "libx264",
            "-preset", "medium",
            "-crf", "18",
            "-pix_fmt", "yuv420p",
            "-r", "60",
            "-fps_mode", "cfr",
            "-c:a", "aac",
            "-b:a", "192k",
            "-movflags", "+faststart",
            quote(output.absolutePath)
        ).joinToString(" ")
    }

    private fun quote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun queryDisplayName(uri: Uri): String {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: "Selected video"
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
