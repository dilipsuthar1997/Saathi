package dilipsuthar.saathi.activity

import android.Manifest
import android.app.Dialog
import android.app.ProgressDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.R
import dilipsuthar.saathi.database.UserSignInData
import dilipsuthar.saathi.databinding.ActivityLoginBinding
import dilipsuthar.saathi.dialog.CustomDialog
import dilipsuthar.saathi.utils.ConnectivityReceiver
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.dialog_otp_verify.*
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var rootView: View
    private lateinit var dialog: Dialog
    private lateinit var dialogWarning: Dialog
    private lateinit var progressDialog: ProgressDialog
    private var snackBar: Snackbar? = null
    private var isOtpSent = false
    var isDataConnected = false

    private lateinit var sharedPreferences: SharedPreferences

    //Firebase
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private lateinit var auth: FirebaseAuth
    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false)) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        rootView = binding.linearLayout

        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        auth = FirebaseAuth.getInstance()

        Tools.setSystemBarColor(this, R.color.grey_90)

        initComponent()
        getALlPermission()
        initGoogleAuth()
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        isDataConnected = isConnected
        showNetworkMessage(isConnected)
    }

    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {

                val account = task.getResult(ApiException::class.java)

                signInWithGoogleAuth(account!!)
                showLoadingDialog("Authenticating...")

            } catch (e: ApiException) {

                Log.w(TAG, "onActivityResult: Google sign in failed", e)

            }
        }

    }



    private fun initComponent() {
        binding.editTextMobNumber.addTextChangedListener { object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s!!.toString().length == 10)
                    Tools.enableViews(binding.buttonLogin)
                else
                    Tools.disableViews(binding.buttonLogin)
            }

        } }
        binding.buttonLogin.setOnClickListener {
            startPhoneAuth()
        }

        binding.buttonGoogleSignin.setOnClickListener {
            startGoogleAuth()
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential?) {
                // For Auto SignIn
                //signInWithPhoneAuthCredential(credential!!)
            }

            override fun onVerificationFailed(e: FirebaseException?) {
                // For Auto Verification code
            }

            override fun onCodeSent(verificationId: String?, token: PhoneAuthProvider.ForceResendingToken?) {
                Log.d(TAG, "onCodeSent: $verificationId")
                isOtpSent = true

                storedVerificationId = verificationId!!
                resendToken = token!!

                Tools.inVisibleViews(binding.progressBar, type = 0)
                Tools.enableViews(binding.editTextMobNumber, binding.buttonLogin, binding.checkBox)
                showOTPDialog()
            }

            override fun onCodeAutoRetrievalTimeOut(p0: String?) {
                mToast.showToast(applicationContext, p0!!, Toast.LENGTH_SHORT)
                Tools.inVisibleViews(binding.progressBar, type = 0)
                Tools.enableViews(binding.buttonLogin, binding.editTextMobNumber, binding.checkBox)
            }

        }
    }

    private fun startPhoneAuth() {
        val phoneNumber: String = binding.editTextMobNumber.text.toString()

        if (validatePhoneNumber(phoneNumber) && isTermAccepted()) {

            Tools.visibleViews(binding.progressBar)
            Tools.disableViews(binding.buttonLogin, binding.editTextMobNumber, binding.checkBox)

            requestVerificationCode(phoneNumber)

            // Start timer when request for OTP
            object : CountDownTimer(20*1000, 1000) {
                override fun onFinish() {
                    if (!isOtpSent){
                        mToast.showToast(applicationContext, "OTP not send.", Toast.LENGTH_SHORT)
                        Tools.inVisibleViews(binding.progressBar, type = 0)
                        Tools.enableViews(binding.editTextMobNumber, binding.buttonLogin, binding.checkBox)
                    }
                }

                override fun onTick(millisUntilFinished: Long) {

                }
            }.start()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user

                    //Store phone user data into Database for later use
                    val userData = UserSignInData.setPhoneAuthUserData(user)

                    //Check if User new
                    val newUser: Boolean? = task.result!!.additionalUserInfo.isNewUser
                    newUser?.let {
                        if (it)
                            goToProfileScreen(userData)
                        else {
                            goToHomeScreen()
                        }

                    }

                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    when {
                        task.exception is FirebaseAuthInvalidCredentialsException -> {
                            // The verification code entered was invalid
                            dialog.editTextOTP.error = "Invalid Code"
                            Tools.inVisibleViews(dialog.progressBarConfirm, type = 1)
                            Tools.enableViews(dialog.buttonConfirm, dialog.editTextOTP)

                        }
                        task.exception is FirebaseTooManyRequestsException ->
                            mToast.showToast(applicationContext, "To many attempts occur", Toast.LENGTH_SHORT)

                        else -> mToast.showToast(applicationContext, "There was some error in login in", Toast.LENGTH_SHORT)

                    }
                }
            }
    }

    private fun showOTPDialog() {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_otp_verify)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.show()

        startTimer(30)

        dialog.buttonConfirm.setOnClickListener {
            if (!dialog.editTextOTP.text.toString().isEmpty()) {
                Tools.visibleViews(dialog.progressBarConfirm)
                Tools.disableViews(dialog.buttonConfirm, dialog.editTextOTP)
                val otp = dialog.editTextOTP.text.toString()

                val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, otp)

                signInWithPhoneAuthCredential(credential)
            } else
                dialog.editTextOTP.error = "Length must be 6"

        }

        dialog.textResendCode.setOnClickListener {
            mToast.showToast(this, "Resending", Toast.LENGTH_SHORT)
            resendVerificationCode(binding.editTextMobNumber.text.toString())
        }
    }

    private fun showLoadingDialog(msg: String) {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage(msg)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()
    }

    private fun requestVerificationCode(number: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            getPhoneNumber(number),
            20,
            TimeUnit.SECONDS,
            this,
            callbacks
        )
    }

    private fun resendVerificationCode(number: String) {
        startTimer(30)

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            getPhoneNumber(number),
            20,
            TimeUnit.SECONDS,
            this,
            callbacks,
            resendToken
        )
    }

    private fun startTimer(second: Long) {
        object: CountDownTimer(second*1000, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                Tools.visibleViews(dialog.textTimer, dialog.progressBarTimer)
                Tools.disableViews(dialog.textResendCode)
                dialog.textTimer.text = (millisUntilFinished/1000).toString()
            }


            override fun onFinish() {
                dialog.textResendCode.setTextColor(resources.getColor(R.color.color_accent))
                Tools.inVisibleViews(dialog.textTimer, dialog.progressBarTimer, type = 0)
                Tools.enableViews(dialog.textResendCode)
            }

        }.start()
    }

    private fun initGoogleAuth() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("873368720645-l33c23s840l4lr7kvltfgq1oojmrmla8.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun startGoogleAuth() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun signInWithGoogleAuth(account: GoogleSignInAccount) {
        Log.d(TAG, "signInWithGoogleAuth: ${account.id!!}")

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->

            if (task.isSuccessful) {

                val user = task.result?.user

                //Store Google user data into database for later use
                val userData = UserSignInData.setGoogleUserData(user)

                val newUser: Boolean? = task.result!!.additionalUserInfo.isNewUser
                newUser?.let {
                    if (newUser)
                        goToProfileScreen(userData)
                    else {
                        goToHomeScreen()
                    }
                }

            } else {

                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInWithGoogleAuth: signInWithCredential:failure", task.exception)
                mToast.showToast(applicationContext, "Authentication failed", Toast.LENGTH_SHORT)
                progressDialog.cancel()

            }

        }

    }

    private fun goToHomeScreen() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun goToProfileScreen(userData: UserSignInData) {

        val bundle = Bundle()
        bundle.putString(Constant.USER_NAME, userData.getName)
        bundle.putString(Constant.USER_EMAIL, userData.getEmail)
        bundle.putString(Constant.USER_MOB_NUMBER, userData.getPhoneNum)
        bundle.putString(Constant.USER_PROFILE_URL, userData.getPhotoUrl)

        val i = Intent(this, ProfileSetupActivity::class.java)
        i.putExtras(bundle)
        startActivity(i)
        finish()

    }

    private fun getPhoneNumber(number: String): String {
        val countryCode = binding.textCountryCode.text.toString()
        return "$countryCode$number"
    }

    private fun validatePhoneNumber(num: String): Boolean {
        if (TextUtils.isEmpty(num)) {
            binding.editTextMobNumber.error = "Please enter number"
            return false
        } else if (num.length < 10) {
            binding.editTextMobNumber.error = "Enter valid number"
            return false
        }

        return true
    }

    private fun isTermAccepted(): Boolean {
        if (binding.checkBox.isChecked)
            return true

        mToast.showToast(this, "Please accept term & conditions", Toast.LENGTH_SHORT)
        return false
    }

    private fun getALlPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA)
        ActivityCompat.requestPermissions(this, permissions, MULT_PERMISSION_RC)
    }

    private fun showNetworkMessage(isConnected: Boolean) {
        if (!isConnected) {
            snackBar = Snackbar.make(binding.root, "You are offline", Snackbar.LENGTH_LONG)
            snackBar?.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
            Tools.setSnackBarBgColor(applicationContext, snackBar!!, R.color.red_500)
            snackBar?.show()
        } else {
            snackBar?.dismiss()
        }
    }

    companion object {
        const val TAG: String = "LoginAuth"
        const val REQUEST_CODE_SIGN_IN: Int = 2
        const val MULT_PERMISSION_RC: Int = 1
    }
}