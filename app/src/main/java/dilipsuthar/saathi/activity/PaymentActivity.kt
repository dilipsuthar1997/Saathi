package dilipsuthar.saathi.activity

import android.app.ProgressDialog
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.widget.RadioButton
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import dilipsuthar.saathi.R
import dilipsuthar.saathi.databinding.ActivityPaymentBinding
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var sharedPreference: SharedPreferences

    private var bikeNumber: String? = null
    private var distance: String? = null
    private var time: String? = null

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {

        // First check for theme style
        //--------------------------------
        sharedPreference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreference.getBoolean(Constant.TOGGLE_DARK_THEME, false))
            setTheme(R.style.DarkTheme)
        else
            setTheme(R.style.AppTheme)
        //----------------------------------------
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_payment)

        bikeNumber = sharedPreference.getString(Constant.BIKE_CODE, "No Bike")
        distance = sharedPreference.getString(Constant.TRAVEL_DISTANCE, "0 m")
        time = sharedPreference.getString(Constant.TRAVEL_TIME, "0 sec")

        initToolbar()
        initComponent()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = "Pay now"

        if (sharedPreference.getBoolean(Constant.TOGGLE_DARK_THEME, false))
            binding.toolbar.setTitleTextColor(Color.WHITE)
        else
            binding.toolbar.setTitleTextColor(Color.BLACK)

        Tools.setSystemBarColor(this, R.color.color_accent_dark)
    }

    private fun initComponent() {
        binding.txtBikeNumber.text = bikeNumber
        binding.txtTotalDistance.text = distance!!.toFloat().toInt().toString()
        binding.txtTravelTime.text = time
        binding.txtTotalAmount.text = getPayableAmount()

        binding.btnPayNow.setOnClickListener {
            if (isPaymentSelected()) {
                showLoadingDialog()

                Handler().postDelayed({
                    progressDialog.dismiss()
                    sharedPreference.edit().putString(Constant.BIKE_CODE, "-").apply()
                    finish()
                }, 3000)
            } else {
                mToast.showToast(this, "Please select payment option.", Toast.LENGTH_SHORT)
            }
        }
    }

    private fun getPayableAmount() : String {
        val dis = distance!!.toFloat()
        val amountRs: Int = ((dis.toInt()/1000) * 5)
        var amountPaise = 0
        amountPaise = ((dis.toInt() % 1000) * 5)

        return "Rs. $amountRs.$amountPaise"
    }

    private fun isPaymentSelected() : Boolean {
        return binding.paymentRadioGroup.checkedRadioButtonId != -1
    }

    private fun showLoadingDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Please wait...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

}
