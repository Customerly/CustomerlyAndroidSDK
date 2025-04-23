package io.customerly.sampleapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import io.customerly.androidsdk.Customerly

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Customerly.load(this, "936fd1dc")

        Customerly.requestNotificationPermissionIfNeeded()

        findViewById<Button>(R.id.btnOpenChat).setOnClickListener {
            Customerly.show()
        }

        findViewById<Button>(R.id.btnCloseChat).setOnClickListener {
            Customerly.hide()
        }

        findViewById<Button>(R.id.btnLoginUser).setOnClickListener {
            Customerly.update(
                mapOf(
                    "user_id" to "123",
                    "email" to "gb@customerly.io",
                    "name" to "Giorgio",
                )
            )
        }

        findViewById<Button>(R.id.btnLogoutUser).setOnClickListener {
            Customerly.logout()
        }

        findViewById<Button>(R.id.btnSendEvent).setOnClickListener {
            Customerly.event("ciao")
        }

        findViewById<Button>(R.id.btnSetAttribute).setOnClickListener {
            Customerly.attribute("last_action", "attribute_set")
        }

        findViewById<Button>(R.id.btnShowNewMessage).setOnClickListener {
            Customerly.showNewMessage("i need help")
        }

        findViewById<Button>(R.id.btnSendNewMessage).setOnClickListener {
            Customerly.sendNewMessage("i need help")
        }

        findViewById<Button>(R.id.btnShowArticle).setOnClickListener {
            Customerly.showArticle(
                "getting-started-with-help-center", "this-is-your-first-articleflex"
            )
        }

        findViewById<Button>(R.id.btnRegisterLead).setOnClickListener {
            Customerly.registerLead(
                "lead@example.com",
                mapOf("source" to "android_app", "interest" to "premium_features")
            )
        }
    }
}
