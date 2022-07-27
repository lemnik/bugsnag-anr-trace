package com.lemnik.performanceexample

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val doWork = BusyClass()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.runAnimation).setOnClickListener {
            findViewById<ViewFlipper>(R.id.textAnimation).startFlipping()
        }

        findViewById<Button>(R.id.almostANR).setOnClickListener {
            Log.i("Invoke Count", "Count=${doWork(SystemClock.elapsedRealtime() + 2000L)}")
        }

        findViewById<Button>(R.id.anr).setOnClickListener {
            Log.i("Invoke Count", "Count=${doWork()}")
        }

        findViewById<Button>(R.id.triggerError).setOnClickListener {
            throw RuntimeException("the magic error")
        }
    }
}