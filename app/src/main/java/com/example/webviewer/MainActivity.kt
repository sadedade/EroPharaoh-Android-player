package com.example.webviewer

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val serverPort = 6969
    private val startFile = "index.html"
    private lateinit var webServer: WebServer
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the web server
        webServer = WebServer(this, serverPort)
        try {
            webServer.start()
            Toast.makeText(this, "EroPharaoh on port $serverPort", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error starting EroPharaoh", Toast.LENGTH_SHORT).show()
        }

        // Hide the action bar
        supportActionBar?.hide()

        // Make the status bar transparent and hide the navigation and status bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            // Hide the navigation bar
            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Hide the status bar
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        try {
            // Enable JavaScript
            val webSettings: WebSettings = webView.settings
            webSettings.javaScriptEnabled = true

            // Load the local HTML file from the web server
            webView.loadUrl("http://localhost:$serverPort/$startFile")

        } catch (e: Exception) {
            // Log any exceptions that occur
            e.printStackTrace()
        }

        // Set up onBackPressedCallback to reload the WebView
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Reload the WebView when the back button is pressed
                webView.reload()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the web server when the app is destroyed
        webServer.stop()
    }

    private class WebServer(private val context: Context, port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            return try {
                val uri = session.uri.substring(1) // Remove the leading "/"
                val inputStream = context.assets.open(uri)
                val mimeType = getMimeType(uri)
                newChunkedResponse(Response.Status.OK, mimeType, inputStream)
            } catch (e: IOException) {
                newFixedLengthResponse("Error reading file: ${e.message}")
            }
        }

        private fun getMimeType(uri: String): String {
            return when {
                uri.endsWith(".html") -> "text/html"
                uri.endsWith(".swf") -> "application/x-shockwave-flash"
                uri.endsWith(".png") -> "image/png"
                else -> "text/plain"
            }
        }
    }
}
