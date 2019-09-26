package com.example.languagetutor

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.TextUtils
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import butterknife.BindView
import butterknife.ButterKnife
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    val TAG = "MainActivity"

    private val RECORD_REQUEST_CODE = 101
    private var textToSpeech: TextToSpeech? = null

    private var stringList: MutableList<String>? = null
    private var speechAPI: SpeechAPI? = null
    private var mVoiceRecorder: VoiceRecorder? = null
    private val mVoiceCallback = object : VoiceRecorder.Callback() {

        override fun onVoiceStart() {
            if (speechAPI != null) {
                speechAPI!!.startRecognizing(mVoiceRecorder!!.sampleRate)
            }
        }

        override fun onVoice(data: ByteArray, size: Int) {
            if (speechAPI != null) {
                speechAPI!!.recognize(data, size)
            }
        }

        override fun onVoiceEnd() {
            if (speechAPI != null) {
                speechAPI!!.finishRecognizing()
            }
        }

    }
    private var adapter: ArrayAdapter<*>? = null
    private val mSpeechServiceListener =
        SpeechAPI.Listener { text, isFinal ->
            if (isFinal) {
                mVoiceRecorder!!.dismiss()
            }
            if (textMessage != null && !TextUtils.isEmpty(text)) {
                runOnUiThread {
                    if (isFinal) {
                        textMessage!!.text = null
                        stringList!!.add(0, text)
                        adapter!!.notifyDataSetChanged()
                    } else {
                        textMessage!!.text = text
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)
        speechAPI = SpeechAPI(this@MainActivity)
        stringList = ArrayList()
        textToSpeech = TextToSpeech(applicationContext,
            TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val ttsLang = textToSpeech?.setLanguage(Locale.US)

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!")
                    } else {
                        Log.i("TTS", "Language Supported.")
                    }
                    Log.i("TTS", "Initialization success.")
                } else {
                    Toast.makeText(
                        applicationContext,
                        "TTS Initialization failed!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, stringList!!
        )
        listview.setOnItemClickListener { parent, view, position, id ->
            val speechStatus = textToSpeech?.speak((stringList as ArrayList<String>)[position], TextToSpeech.QUEUE_FLUSH, null)

            if (speechStatus == TextToSpeech.ERROR) {
                Log.e("TTS", "Error in converting Text to Speech!")
            }

        }
        listview?.adapter = adapter
    }

    override fun onStop() {
        stopVoiceRecorder()

        // Stop Cloud Speech API
        speechAPI!!.removeListener(mSpeechServiceListener)
        speechAPI!!.destroy()
        speechAPI = null

        super.onStop()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        if (isGrantedPermission(RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder()
        } else {
            makeRequest(RECORD_AUDIO)
        }
        speechAPI!!.addListener(mSpeechServiceListener)
    }

    private fun isGrantedPermission(permission: String): Int {
        return ContextCompat.checkSelfPermission(this, permission)
    }

    private fun makeRequest(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), RECORD_REQUEST_CODE)
    }

    private fun startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
        }
        mVoiceRecorder = VoiceRecorder(mVoiceCallback)
        mVoiceRecorder!!.start()
    }

    private fun stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder!!.stop()
            mVoiceRecorder = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == RECORD_REQUEST_CODE) {
            if (grantResults.size == 0 && grantResults[0] == PackageManager.PERMISSION_DENIED
                && grantResults[0] == PackageManager.PERMISSION_DENIED
            ) {
                finish()
            } else {
                startVoiceRecorder()
            }
        }
    }
}
