package dilipsuthar.saathi.utils

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar

object Tools {

    fun setSystemBarColor(act: Activity, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = act.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor =act.resources.getColor(color)
        }
    }

    fun changeNavigationIconColor(toolbar: Toolbar, @ColorInt color: Int) {
        val drawable = toolbar.navigationIcon
        drawable?.mutate()
        drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    fun enableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = true
        }
    }

    fun disableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = false
        }
    }

    fun visibleViews(vararg views: View) {
        for (v in views) {
            v.visibility = View.VISIBLE
        }
    }

    fun inVisibleViews(vararg views: View, type: Int) {

        if (type == 0) {
            for (v in views) {
                v.visibility = View.INVISIBLE
            }
        } else {
            for (v in views) {
                v.visibility = View.GONE
            }
        }
    }

    fun setSnackBarBgColor(ctx: Context, snackbar: Snackbar, color: Int) {
        val sbView = snackbar.view
        sbView.setBackgroundColor(ctx.resources.getColor(color))
    }
}
