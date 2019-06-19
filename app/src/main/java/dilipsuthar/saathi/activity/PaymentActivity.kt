package dilipsuthar.saathi.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dilipsuthar.saathi.R
import dilipsuthar.saathi.databinding.ActivityPaymentBinding
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var sharedPreference: SharedPreferences

    private var bikeNumber: String? = null
    private var distance: String? = null
    private var time: String? = null

    private lateinit var progressDialog: ProgressDialog

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mStorage: FirebaseFirestore
    private var currentUserId: String? = null

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

        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseFirestore.getInstance()

        currentUserId = mAuth.currentUser?.uid

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
                updateDatabase() // Upload user ride history to server

                Handler().postDelayed({
                    progressDialog.dismiss()
                    sharedPreference.edit().putString(Constant.BIKE_CODE, "-").apply()
                    startActivity(Intent(this@PaymentActivity, PaymentDoneActivity::class.java))
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
        val amountPaise: Int
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

    @SuppressLint("SimpleDateFormat")
    private fun updateDatabase() {

        val date = Calendar.getInstance().time
        val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm")

        val rideHistory = HashMap<String, Any?>()
        rideHistory["bike_id"] = bikeNumber
        rideHistory["ride_date_time"] = sdf.format(date)
        rideHistory["travel_distance"] = distance
        rideHistory["travel_time"] = time
        rideHistory["travel_amount"] = getPayableAmount()
        rideHistory["time_stamp"] = FieldValue.serverTimestamp()


        mStorage.collection("Users").document(currentUserId!!).collection("RideHistory").add(rideHistory).addOnCompleteListener {

            if (it.isSuccessful) {

                mToast.showToast(applicationContext, "History saved successfully", Toast.LENGTH_SHORT)

            } else {

                var e: Exception? = it.exception
                e?.printStackTrace()

            }

        }
    }

}
