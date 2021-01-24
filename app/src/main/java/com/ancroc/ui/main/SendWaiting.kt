package com.ancroc.ui.main

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ancroc.R
import crocmobile.Crocmobile
import crocmobile.SendCallbacks
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import kotlin.concurrent.thread


class SendWaiting : AppCompatActivity() {
    val TAG = "SendWaiting"
    private lateinit var sharedSecret: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_waiting)
        //setSupportActionBar(findViewById(R.id.toolbar))

        sharedSecret = findViewById<TextView>(R.id.sharedSecret)
        var isText = false

        // Try path to file
        var pathOrText: String? = intent.getStringExtra("path")
        if (pathOrText == null) {
            // Try text
            pathOrText = intent.getStringExtra("text")
            isText = true

            if (pathOrText == null) {
                // Try intent plaintext
                pathOrText = intent.getStringExtra(Intent.EXTRA_TEXT)
                isText = true

                if (pathOrText == null) {
                    // Try intent file
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        val file = uriToTempFile(it)
                        pathOrText = file.absolutePath
                        isText = false
                    }
                }
            }
        }

        Log.d(TAG, "path: $pathOrText")

        val sendImpl = object: SendCallbacks {
            override fun onCrocError(e: Exception?) {
                Log.e(TAG, "Error", e)
            }

            override fun onSharedSecret(secret: String?) {
                runOnUiThread {
                    sharedSecret.text = secret
                }
            }
        }

        thread {
            val res = Crocmobile.send(
                cacheDir.path,
                pathOrText,
                isText,
                sendImpl
            )
            Log.d(TAG, "Send result: $res")
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "GOT INTENT")
    }

    fun uriToTempFile(uri: Uri): File {
        val contentResolver = contentResolver
        val mime = MimeTypeMap.getSingleton()
        val type = mime.getExtensionFromMimeType(contentResolver.getType(uri))

        val tempFile = createTempFile("ancroc-", ".$type")

        val inputStream =
            contentResolver.openInputStream(uri)
                ?: throw Error("Could not open input stream")

        val out: OutputStream =
            FileOutputStream(tempFile)
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) {
            out.write(buf, 0, len)
        }
        out.close()
        inputStream.close()

        return tempFile
    }
}