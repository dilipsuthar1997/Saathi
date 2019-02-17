package dilipsuthar.saathi.activity

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.dlazaro66.qrcodereaderview.QRCodeReaderView
import dilipsuthar.saathi.R
import dilipsuthar.saathi.databinding.ActivityScanQrBinding
import dilipsuthar.saathi.fragment.HomeFragment
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.utils.mToast
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.dialog_ride_confirmation.*

class ScanQrActivity : AppCompatActivity(), QRCodeReaderView.OnQRCodeReadListener {

    private lateinit var binding: ActivityScanQrBinding
    private lateinit var dialog: Dialog
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scan_qr)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        initComponent()

    }

    private fun initComponent() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.qrCodeView.setOnQRCodeReadListener(this)
            binding.qrCodeView.setQRDecodingEnabled(true)
            binding.qrCodeView.setBackCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 112)
        }

        binding.switchFlashLight.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.qrCodeView.setTorchEnabled(true)
            } else {
                binding.qrCodeView.setTorchEnabled(false)
            }
        }

        binding.switchAutoFocus.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.qrCodeView.forceAutoFocus()
            } else {
                binding.qrCodeView.setAutofocusInterval(5000L)
            }
        }
    }

    override fun onQRCodeRead(text: String?, points: Array<out PointF>?) {
        showConfirmDialog(text)
        binding.qrCodeView.setQRDecodingEnabled(false)
    }

    override fun onResume() {
        super.onResume()
        binding.qrCodeView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        binding.qrCodeView.stopCamera()
    }

    private fun showConfirmDialog(bikeNum: String?) {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_ride_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        dialog.txtBikeNumber.text = bikeNum

        dialog.btnStartRide.setOnClickListener {
            sharedPreferences.edit().putString(Constant.BIKE_CODE, bikeNum).apply()
            sharedPreferences.edit().putBoolean(Constant.IS_ON_RIDE, true).apply()
            finish()
        }

        dialog.btnCancel.setOnClickListener {
            mToast.showToast(application, "Cancel", Toast.LENGTH_SHORT)
            dialog.cancel()
            binding.qrCodeView.setQRDecodingEnabled(true)
        }
    }

}
