package dilipsuthar.saathi.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import dilipsuthar.saathi.R
import kotlinx.android.synthetic.main.toast_custom.view.*

object mToast {

    fun showToast(ctx: Context?, msg: String, duration: Int) {
        val inflater = LayoutInflater.from(ctx)
        val layout = inflater.inflate(R.layout.toast_custom, null)
        layout.text.text = msg

        val toast = Toast(ctx)
        toast.duration = duration
        toast.view = layout
        toast.show()

    }

}