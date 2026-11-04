package com.example.lutielog

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 全局未捕获异常收集器：把崩溃堆栈写入应用私有目录 crash_logs/，
 * 再交给系统默认处理器正常终止进程。用于定位偶发、无法现场复现的闪退。
 */
class CrashCollector(
    context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    private val appContext = context.applicationContext

    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching { writeCrash(t, e) }
        defaultHandler?.uncaughtException(t, e)
    }

    private fun writeCrash(thread: Thread, e: Throwable) {
        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            pw.println("Time: ${System.currentTimeMillis()}")
            pw.println("Thread: ${thread.name}")
            pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            pw.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            pw.println("App version: ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
            pw.println("----------------------------------------")
            e.printStackTrace(pw)
        }
        val dir = File(appContext.filesDir, "crash_logs").apply { mkdirs() }
        val file = File(dir, "crash_${System.currentTimeMillis()}.txt")
        file.writeText(sw.toString())
    }

    companion object {
        fun install(context: Context) {
            val current = Thread.getDefaultUncaughtExceptionHandler()
            if (current is CrashCollector) return
            Thread.setDefaultUncaughtExceptionHandler(CrashCollector(context, current))
        }
    }
}
