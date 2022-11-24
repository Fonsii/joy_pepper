package com.softbankrobotics.dx.peppercodescannersample

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aldebaran.qi.sdk.QiContext
import com.aldebaran.qi.sdk.QiSDK
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks
import com.aldebaran.qi.sdk.`object`.actuation.*
import com.aldebaran.qi.sdk.builder.GoToBuilder
import com.aldebaran.qi.sdk.builder.SayBuilder
import com.aldebaran.qi.sdk.builder.TransformBuilder
import com.google.android.gms.vision.barcode.Barcode
import com.softbankrobotics.dx.peppercodescanner.BarcodeReaderActivity
import com.softbankrobotics.dx.pepperextras.actuation.StubbornGoTo
import com.softbankrobotics.dx.pepperextras.actuation.StubbornGoToBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.conversation_layout.conversation_view
import java.util.concurrent.Future


class MainActivity : AppCompatActivity(), RobotLifecycleCallbacks {
    private var conversationBinder: ConversationBinder? = null
    // Store the selected location.
    private var selectedLocation: String? = null
    // Store the saved locations.
    private val savedLocations = hashMapOf<String, FreeFrame>()
    // The QiContext provided by the QiSDK.
    private var qiContext: QiContext? = null
    // Store the Actuation service.
    private var actuation: Actuation? = null
    // Store the Mapping service.
    private var mapping: Mapping? = null
    // Store the GoTo action.
    private var goTo: GoTo? = null

    companion object {
        private const val TAG = "MainActivity"
        private const val BARCODE_READER_ACTIVITY_REQUEST = 1208
        private const val KEY_MESSAGE = "key_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanLayout.setOnClickListener {
            val launchIntent = Intent(this, BarcodeReaderActivity::class.java)
            // Uncomment the next line to remove the scanner overlay
            //launchIntent.putExtra(KEY_SCAN_OVERLAY_VISIBILITY, false)
            startActivityForResult(launchIntent, BARCODE_READER_ACTIVITY_REQUEST)
        }
        save_button.setOnClickListener {
            handleSaveClick()
        }
        // Register the RobotLifecycleCallbacks to this Activity.
        QiSDK.register(this, this)
    }

    override fun onDestroy() {
        // Unregister the RobotLifecycleCallbacks for this Activity.
        QiSDK.unregister(this, this)
        super.onDestroy()
    }

    private fun saveLocation(location: String) {
        // Get the robot frame asynchronously.
        val robotFrameFuture = actuation?.async()?.robotFrame()
        robotFrameFuture?.andThenConsume {
            // Create a FreeFrame representing the current robot frame.
            val locationFrame = mapping?.makeFreeFrame()
            val transform = TransformBuilder.create().fromXTranslation(0.0)
            locationFrame?.update(it, transform, 0L)

            // Store the FreeFrame.
            if (locationFrame != null)
                savedLocations[location] = locationFrame
        }
    }

    private fun handleSaveClick() {
        val location = add_item_edit.text.toString()
        add_item_edit.text.clear()
        // Save location only if new.
        if (location.isNotEmpty() && !savedLocations.containsKey(location)) {
            //displayLine("Location added: $location", ConversationItemType.INFO_LOG)
            saveLocation(location)
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
        Log.e(TAG, "ENTRA ${requestCode} : ${BARCODE_READER_ACTIVITY_REQUEST} : ${resultCode} : ${data}")
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "Scan error")
            return
        }

        if (requestCode == BARCODE_READER_ACTIVITY_REQUEST && data != null) {
            Log.e(TAG, "ENTRA 2")
            val barcode: Barcode? =
                data.getParcelableExtra(BarcodeReaderActivity.KEY_CAPTURED_BARCODE)
            val location = barcode?.rawValue
            val found = savedLocations.containsKey(location)
            var sayText: String = "Location scanned. Going to location ${location}."

            if (!found) {
                sayText = "I'm sorry. I don't remember saving a location for ${location}."
            }
            //val say = SayBuilder.with(this.qiContext)
            //    .withText(sayText)
            //    .build()
            Log.e(TAG, sayText)
            //say.run()
            if (!found){
                return
            }
            if (location != null) {
                goToLocation(location)
                Log.e("GoTo", "Location reached. Heading back to base.")
                goToLocation("base")
            }
        }
    }

    private fun goToLocation(location: String) {
        // Get the FreeFrame from the saved locations.
        val freeFrame = savedLocations[location]

        val goTo = StubbornGoToBuilder.with(qiContext!!)
            .withFinalOrientationPolicy(OrientationPolicy.ALIGN_X)
            .withMaxRetry(10)
            .withMaxSpeed(0.5f)
            .withMaxDistanceFromTargetFrame(0.3)
            .withWalkingAnimationEnabled(true)
            .withFrame(freeFrame!!.frame()).build()
        goTo.run()
    }

    private fun waitForInstructions() {
        val message = "Waiting for instructions..."
        Log.i(TAG, message)
        //displayLine(message, ConversationItemType.INFO_LOG)
        runOnUiThread {
            save_button.isEnabled = true
        }
    }

    private fun displayLine(text: String, type: ConversationItemType) {
        runOnUiThread { conversation_view.addLine(text, type) }
    }

    /*
    * Metodos que se tienen que implementar para que sea un RobotCycleCallback
    */
    override fun onRobotFocusGained(qiContext: QiContext) {
        Log.i(TAG, "Focus gained.")
        // Store the provided QiContext and services.
        this.qiContext = qiContext
        // Bind the conversational events to the view.
        //val conversationStatus = qiContext.conversation.status(qiContext.robotContext)
        //conversationBinder = conversation_view.bindConversationTo(conversationStatus)

        actuation = qiContext.actuation
        mapping = qiContext.mapping
        /*
        val say = SayBuilder.with(qiContext)
            .withText("Hi. I can remember locations and go to them by scanning a QR code.")
            .build()

        say.run()
        */
        waitForInstructions()
    }

    override fun onRobotFocusLost() {
        Log.e(TAG, "Focus lost.")
        /*
        // Remove the QiContext.
        qiContext = null

        conversationBinder?.unbind()

        // Remove on started listeners from the GoTo action.
        goTo?.removeAllOnStartedListeners()
        */
    }

    override fun onRobotFocusRefused(reason: String) {
        // Nothing here.
    }
}
