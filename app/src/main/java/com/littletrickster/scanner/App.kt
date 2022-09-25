package com.littletrickster.scanner

import android.app.Application
import org.opencv.android.OpenCVLoader


class App : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initDebug()
    }
}