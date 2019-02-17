package dilipsuthar.saathi.utils

import android.app.Application

class Font : Application() {
    override fun onCreate() {
        super.onCreate()
        TypefaceUtil.overrideFont(applicationContext, "SERIF", "font/raleway_regular.ttf")
    }
}
