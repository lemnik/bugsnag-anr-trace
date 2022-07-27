package com.lemnik.performanceexample

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.lemnik.bugsnag.performance.ANRTracePlugin
import com.lemnik.bugsnag.performance.MetadataStackTreeVisitor

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugsnag.start(this, Configuration("3a1e27dedb64f0a002ea4325f9cdd643").apply {
            addPlugin(
                ANRTracePlugin(
                    stackTreeVisitor = MetadataStackTreeVisitor(
                        classNameFormat = MetadataStackTreeVisitor.ShortPackageName,
                    )
                )
            )
        })
    }
}