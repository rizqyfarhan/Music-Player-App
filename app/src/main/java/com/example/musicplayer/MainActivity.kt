package com.example.musicplayer

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playBtn: ImageButton

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val fileName = extractFileName(uri)
                playMusic(fileName, uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playBtn = findViewById(R.id.playBtn)

        playMusic("id", null)

        playBtn.setOnClickListener {
            if (::mediaPlayer.isInitialized) {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    playBtn.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    mediaPlayer.pause()
                    playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                }
            }
        }

        findViewById<TextView>(R.id.chooseText).setOnClickListener {
            openFileChooser()
        }
    }

    private fun openFileChooser() {
        pickFileLauncher.launch("*/*")
    }

    private fun extractFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
        return ""
    }

    private fun playMusic(fileName: String, uri: Uri?) {
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }

        if(uri != null) {
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.prepare()

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

            updateWithMetaData(title)

            retriever.release()
        } else {
            val resId = resources.getIdentifier(fileName, "raw", packageName)
            mediaPlayer = MediaPlayer.create(this, resId)
        }
    }

    private fun updateWithMetaData(title: String?) {
        val titleText = findViewById<TextView>(R.id.title)

        titleText.text = title ?: "Unknown Title"
    }
}