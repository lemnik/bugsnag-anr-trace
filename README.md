# Even Better ANR Reports for Bugsnag

This is an *unofficial* [Bugsnag](https://bugsnag.com) Plugin that can be used to dramatically
improve the quality of Application Not Responding (ANR)  error reports by giving you detailed
insights into what caused them. The Plugin is still experimental, but can produce something
approaching a flame graph and add it to the Bugsnag Event metadata.

```json

```

## Getting Started

> **Warning**
> This project is not yet ready for use in production software. Use at your own risk.

The project can be included in your Android app Gradle config using [Jitpack](https://jitpack.io):

```groovy
    allprojects {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the dependency to your `app/build.gradle`:

```groovy
dependencies {
    // ...
    implementation 'com.github.lemnik:bugsnag-anr-trace:0.0.1'
}
```

Then instantiate the Plugin when you start Bugsnag:

```kotlin
Bugsnag.start(Configuration().apply {
    plugins = listOf(
        // any other Plugins you already use
        ANRTracePlugin()
    )
})
```

## Why is it useful

Application Not Responding (ANR) error reports include breadcrumbs and a stack trace, but the stack
trace in particular doesn't typically include very useful information since its only what was
happening at the instant the ANR was reported. ANRs are typically caused by a series of things
leading up to their report.

By sampling the stack (the same way a lightweight profiler does) we gather a wealth of information
about which methods are actually consuming the most CPU in the time leading up to the crash. The
data gathered by this plugin is detailed enough that it could be used to produce a Profiler-style
flame-graph if you put in the effort.

## Configuration Options

* `startSamplingDelay` - the number of milliseconds before the `ANRTracePlugin` assumes you
  application is in trouble and starts sampling the main thread
* `sampleInterval` - the number of milliseconds between samples, increasing this will give your `main`
  thread more CPU time to recover but will reduce the usefulness of the report data
* `samplerThreadPriority` - the `Thread.priority` of the ANR monitoring thread, lowering this will
  give your `main` thread more CPU time to recover but will reduce the usefulness of the report data
* `breadcrumbs` - configure any breadcrumbs you want `ANRTracePlugin` to leave
* `stackTreeVisitor` - responsible for converting the raw sample data into a format suitable for an
  error report (defaults to `MetadataStackTreeVisitor` which is quite verbose but produces reports
  that are readable without any additional tooling)

## How does it work

The `ANRTracePlugin` monitors your `main`
thread [Looper](https://developer.android.com/reference/android/os/Looper) to ensure it doesn't
become blocked in any way. Once the `main` thread appears to be blocked for more than a set amount
of time (`startSamplingDelay`) the Plugin will begin taking stack trace samples at regular
intervals (`sampleInterval`) until either (a) the `main` thread recovers, or (b) an ANR error is
reported.

The sampling is a relatively expensive operation, and will slow your application down further.
However any application blocking the `main` thread for longer than a second almost certainly has
something wrong anyway. If you believe the `ANRTracePlugin` is making your application ANR more than
it should, you can tune the configuration parameters to better suit your application workload.

The defaults should work pretty-well for most well behaved applications however.