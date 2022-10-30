package com.softbankrobotics.dx.peppercodescannersample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.`object`.actuation.FreeFrame
import com.aldebaran.qi.sdk.`object`.actuation.GoTo
import com.aldebaran.qi.sdk.builder.GoToBuilder
import com.google.android.gms.vision.barcode.Barcode
import com.softbankrobotics.dx.peppercodescanner.BarcodeReaderActivity
import kotlinx.android.synthetic.main.activity_go_to_world_tutorial.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.conversation_layout.conversation_view


class MainActivity : AppCompatActivity() {
    private val savedLocations = hashMapOf<String, FreeFrame>()
    private var goTo: GoTo? = null
    private var qiContext: QiContext? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val BARCODE_READER_ACTIVITY_REQUEST = 1208
        private const val KEY_MESSAGE = "key_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout.setOnClickListener {
            val launchIntent = Intent(this, BarcodeReaderActivity::class.java)
            // Uncomment the next line to remove the scanner overlay
            //launchIntent.putExtra(KEY_SCAN_OVERLAY_VISIBILITY, false)
            startActivityForResult(launchIntent, BARCODE_READER_ACTIVITY_REQUEST)
        }
    }

    override fun onStart() {
        super.onStart()
        hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Scan error")
            return
        }

        if (requestCode == BARCODE_READER_ACTIVITY_REQUEST && data != null) {
            val barcode: Barcode? =
                data.getParcelableExtra(BarcodeReaderActivity.KEY_CAPTURED_BARCODE)
            val message = "Scan result: ${barcode?.rawValue}"

            val launchIntent = Intent(this, ResultActivity::class.java)
            launchIntent.putExtra(KEY_MESSAGE, message)
            startActivity(launchIntent)
        }
    }

    private fun goToLocation(location: String) {
        // Get the FreeFrame from the saved locations.
        val freeFrame = savedLocations[location]

        // Extract the Frame asynchronously.
        val frameFuture = freeFrame?.async()?.frame()
        frameFuture?.andThenCompose { frame ->
            // Create a GoTo action.
            val goTo = GoToBuilder.with(qiContext)
                .withFrame(frame)
                .build()
                .also { this.goTo = it }

            // Display text when the GoTo action starts.
            goTo.addOnStartedListener {
                val message = "Moving..."
                Log.i(TAG, message)
                displayLine(message, ConversationItemType.INFO_LOG)
            }
            this.goTo = goTo

            // Execute the GoTo action asynchronously.
            goTo.async().run()
        }?.thenConsume {
            if (it.isSuccess) {
                Log.i(TAG, "Location reached: $location")
                waitForInstructions()
            } else if (it.hasError()) {
                Log.e(TAG, "Go to location error", it.error)
                waitForInstructions()
            }
        }
    }

    private fun waitForInstructions() {
        val message = "Waiting for instructions..."
        Log.i(TAG, message)
        displayLine(message, ConversationItemType.INFO_LOG)
        runOnUiThread {
            save_button.isEnabled = true
            goto_button.isEnabled = true
        }
    }

    private fun displayLine(text: String, type: ConversationItemType) {
        runOnUiThread { conversation_view.addLine(text, type) }
    }
}
