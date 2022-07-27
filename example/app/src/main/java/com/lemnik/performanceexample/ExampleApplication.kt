package com.lemnik.performanceexample

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.lemnik.anrtrace.ANRTracePlugin
import com.lemnik.anrtrace.ClassNameFormatter
import com.lemnik.anrtrace.MetadataStackTreeVisitor

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Bugsnag.start(this, Configuration.load(this).apply {
            addPlugin(
                ANRTracePlugin(
                    stackTreeVisitor = MetadataStackTreeVisitor(
                        classNameFormat = ClassNameFormatter.ShortPackageName,
                    ),
                )
            )
        })
    }
}