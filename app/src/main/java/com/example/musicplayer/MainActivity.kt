package com.example.musicplayer

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var playBtn: ImageButton
    private lateinit var replayBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var startTime: TextView
    private lateinit var endTime: TextView
    private lateinit var handler: Handler
    private var userIsDraggingSeekBar = false

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

        handler = Handler()

        playBtn = findViewById(R.id.playBtn)
        replayBtn = findViewById(R.id.replayBtn)
        forwardBtn = findViewById(R.id.forwardBtn)
        seekBar = findViewById(R.id.seekBar)
        startTime = findViewById(R.id.startTime)
        endTime = findViewById(R.id.endTime)

        playMusic("id", null)

        playBtn.setOnClickListener {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.pause()
                    playBtn.setImageResource(R.drawable.baseline_play_arrow_24) // Change to play icon
                } else {
                    mediaPlayer!!.start()
                    playBtn.setImageResource(R.drawable.baseline_pause_24) // Change to pause icon
                    updateSeekBar()
                }
            }
        }

        findViewById<TextView>(R.id.chooseText).setOnClickListener {
            openFileChooser()
        }

        replayBtn.setOnClickListener {
            seekReplay(5000)
        }

        forwardBtn.setOnClickListener {
            seekForward(5000)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                    startTime.text = formatTime(progress)

                    if (!mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                        playBtn.setImageResource(R.drawable.baseline_pause_24)
                        updateSeekBar()
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userIsDraggingSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    mediaPlayer?.seekTo(it.progress)
                }
                userIsDraggingSeekBar = false
            }
        })

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
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
        }

        mediaPlayer = if (uri != null) {
            MediaPlayer()
        } else {
            val resId = resources.getIdentifier(fileName, "raw", packageName)
            MediaPlayer.create(this, resId)
        }

        mediaPlayer?.let {
            if (uri != null) {
                it.setDataSource(this, uri)
                it.prepare()

                seekBar.max = it.duration

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, uri)

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val songImage = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                val songImageBytes = retriever.embeddedPicture

                updateWithMetaData(title, artist, songImage, songImageBytes)

                retriever.release()
            } else {
                updateWithMetaData(
                    MediaMetadataRetriever().extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    MediaMetadataRetriever().extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    null,
                    null
                )
            }
        }
    }

    private fun updateWithMetaData(title: String?, artist: String?, songImage: String?, songImageBytes: ByteArray?) {
        val titleText = findViewById<TextView>(R.id.title)
        val artistText = findViewById<TextView>(R.id.artist)
        val songImageView = findViewById<ImageView>(R.id.songImage)

        titleText.text = title ?: "Unknown Title"
        artistText.text = artist ?: "Unknown Artist"

        if (songImageBytes != null) {
            val songImageBitmap = BitmapFactory.decodeByteArray(songImageBytes, 0, songImageBytes.size)
            songImageView.setImageBitmap(songImageBitmap)
        } else {
            songImageView.setImageResource(R.drawable.blue_music_notes)
        }

        endTime.text = formatTime(mediaPlayer?.duration ?: 0)
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            seekBar.max = it.duration

            handler.postDelayed(object : Runnable {
                override fun run() {
                    val currentPosition = it.currentPosition
                    if (!userIsDraggingSeekBar) {
                        seekBar.progress = currentPosition
                        startTime.text = formatTime(currentPosition)
                    }

                    if (currentPosition >= it.duration) {
                        playBtn.setImageResource(R.drawable.baseline_play_arrow_24)
                        it.pause()
                        handler.removeCallbacks(this)  // Stop the update loop
                    } else {
                        handler.postDelayed(this, 1000)
                    }
                }
            }, 0)
        }
    }


    private fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }

    private fun seekForward(miliseconds: Int) {
        mediaPlayer?.let {
            val currentPosition = it.currentPosition
            val duration = it.duration
            val newPosition = currentPosition + miliseconds
            if (newPosition < duration) {
                it.seekTo(newPosition)
            } else {
                it.seekTo(duration)
            }
        }
    }

    private fun seekReplay(miliseconds: Int) {
        mediaPlayer?.let{
            val currentPosition = it.currentPosition
            val newPosition = currentPosition - miliseconds
            if (newPosition > 0) {
                it.seekTo(newPosition)
            } else {
                it.seekTo(0)
            }
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}