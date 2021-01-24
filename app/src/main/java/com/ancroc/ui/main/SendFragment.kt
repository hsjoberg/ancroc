package com.ancroc.ui.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ancroc.R
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks


const val OPEN_FILE = 2

class SendFragment : Fragment(), PickiTCallbacks {
    private val TAG = "SendFragment"
    private lateinit var pageViewModel: PageViewModel
    private lateinit var pickit: PickiT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
        pickit = PickiT(context, this, activity);
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_send, container, false)
        root.findViewById<Button>(R.id.sendButton).setOnClickListener {
            onClickSend()
        }
        root.findViewById<Button>(R.id.sendTextButton).setOnClickListener {
            onClickSendText()
        }
        return root
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): SendFragment {
            return SendFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }

    fun checkSelfPermission(): Boolean {
        if (context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                10
            )
            return false
        }
        return true
    }

    fun onClickSend() {
        if (!checkSelfPermission()) {
            return
        }
    }

    fun onClickSendText() {
        context?.let {
            val builder = androidx.appcompat.app.AlertDialog.Builder(it).apply {
                setTitle("Send text")
                val viewInflated: View = LayoutInflater.from(context)
                    .inflate(R.layout.fragment_send_text_dialog, view as ViewGroup?, false)
                val input = viewInflated.findViewById<EditText>(R.id.sendTextInput)
                setView(viewInflated)
                setPositiveButton("Send") { _, _ ->
                    if (input.text.toString().isNotEmpty()) {
                        val intent = Intent(context, SendWaiting::class.java).apply {
                            putExtra("text", input.text.toString())
                        }
                        startActivity(intent)
                    }
                }
                setNegativeButton("Cancel") { _, _ -> }
                setNeutralButton("Paste from clipboard") { _, _ ->  }
            }
            val dialog = builder.show()

            // Outside because we don't want to close the modal when clicking on "Paste from clipboard"
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                val clipboardManager: ClipboardManager? = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val textInput = dialog.findViewById<EditText>(R.id.sendTextInput)
                textInput!!.setText(
                   textInput.text.toString() + clipboardManager?.primaryClip?.getItemAt(0)?.text
                )
            }
        }
    }

    fun openPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, OPEN_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OPEN_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Log.d(TAG, "uri: " + data.data?.path)
                pickit.getPath(data.data, 28)//Build.VERSION.SDK_INT);
            }
        } else {
            Log.d(TAG, "$resultCode")
        }
    }

    override fun PickiTonUriReturned() {}

    override fun PickiTonProgressUpdate(progress: Int) {}

    override fun PickiTonStartListener() {}

    override fun PickiTonCompleteListener(
        path: String?,
        wasDriveFile: Boolean,
        wasUnknownProvider: Boolean,
        wasSuccessful: Boolean,
        reason: String?
    ) {
        if (!wasSuccessful) {
            Log.d(TAG, "Fail: $reason")
            return
        }
        Log.d(TAG, "Path: $path")

        val intent = Intent(context, SendWaiting::class.java).apply {
            putExtra("path", path)
        }
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val index = permissions.indexOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                openPicker()
            }
        }
    }
}