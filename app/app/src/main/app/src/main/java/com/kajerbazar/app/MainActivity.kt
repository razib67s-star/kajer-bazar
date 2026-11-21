package com.kajerbazar.app

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import android.widget.TextView
import android.view.Gravity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this).apply {
            text = "কাজের বাজারে আপনাকে স্বাগতম\nWelcome to Kajer Bazar"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        setContentView(textView)
    }
}
