package io.customerly.sampleapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.customerly.androidsdk.Customerly

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Customerly.init(this, "936fd1dc")

        Customerly.setOnNewMessageReceived { accountId, message, timestamp, userId, conversationId ->
            Log.d("CustomerlySDK", "New message: $message")
        }

        findViewById<Button>(R.id.btnOpenChat).setOnClickListener {
            Customerly.show(this)
        }

        findViewById<Button>(R.id.btnCloseChat).setOnClickListener {
            Customerly.hide()
        }
    }
}
