package dilipsuthar.saathi.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageActivity
import com.theartofdev.edmodo.cropper.CropImageView
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.R
import dilipsuthar.saathi.databinding.ActivityProfileSetupBinding
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast
import kotlinx.android.synthetic.main.activity_profile_setup.*
import java.lang.Exception

class ProfileSetupActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "ProfileSetupActivity"
    }

    private lateinit var binding: ActivityProfileSetupBinding
    private var bundle: Bundle? = null
    private lateinit var sharedPreferences: SharedPreferences

    private var userId: String? = null
    private var userName: String? = null
    private var userEMail: String? = null
    private var userMobNumber: String? = null
    private var userProfileUrl: String? = null
    private var userGender: String? = null
    private var userProfileUri: Uri? = null

    private var uploadTask: UploadTask? = null

    private var genderCheckedId: Int? = null
    private var isChanged: Boolean = false

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false)) {
            setTheme(R.style.DarkTheme)
        } else
            setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_setup)

        bundle = intent.extras
        auth = FirebaseAuth.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        firebaseFirestore = FirebaseFirestore.getInstance()

        userId = auth.currentUser!!.uid

        initToolbar()
        initComponent()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Profile"
        if (sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false))
            binding.toolbar.setTitleTextColor(Color.WHITE)
        else
            binding.toolbar.setTitleTextColor(Color.BLACK)
        if (bundle == null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_chevron_left)
        }
        Tools.changeNavigationIconColor(binding.toolbar, resources.getColor(R.color.color_accent))
        Tools.setSystemBarColor(this, R.color.grey_90)
    }

    private fun initComponent() {

        // On Start of activity
        if (bundle != null) {

            // Apply new data to fields
            binding.fieldUserName.setText(bundle!!.getString(Constant.USER_NAME))
            binding.fieldEmail.setText(bundle!!.getString(Constant.USER_EMAIL))
            binding.fieldMobNumber.setText(bundle!!.getString(Constant.USER_MOB_NUMBER))

            userProfileUrl = bundle!!.getString(Constant.USER_PROFILE_URL)
            if (userProfileUrl != null)
                userProfileUri = Uri.parse(userProfileUrl)

            Glide
                .with(applicationContext)
                .load(userProfileUrl)
                .apply(RequestOptions().placeholder(R.drawable.placeholder))
                .into(binding.profileImage)

        } else {

            Tools.visibleViews(binding.progressBarLoading)
            Tools.disableViews(binding.buttonSaveProfile)

            // Apply old data to fields --> From server
            firebaseFirestore.collection("Users").document(userId!!).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (task.result!!.exists()) {

                            binding.fieldUserName.setText(task.result!!.getString("name"))
                            binding.fieldEmail.setText(task.result!!.getString("email"))
                            binding.fieldMobNumber.setText(task.result!!.getString("mobNumber"))

                            userGender = task.result!!.getString("gender")
                            userProfileUrl = task.result!!.getString("image")
                            userProfileUri = Uri.parse(userProfileUrl)

                            when (userGender) {
                                "Male" -> binding.genderRadioGroup.check(R.id.maleRadioButton)
                                "Female" -> binding.genderRadioGroup.check(R.id.femaleRadioButton)
                            }

                            Glide
                                .with(applicationContext)
                                .load(userProfileUrl)
                                .apply(RequestOptions().placeholder(R.drawable.placeholder))
                                .into(binding.profileImage)

                        }
                    }

                    Tools.inVisibleViews(binding.progressBarLoading, type = 0)
                    Tools.enableViews(binding.buttonSaveProfile)

                }
                .addOnFailureListener { exception ->

                    val error = exception.toString()
                    mToast.showToast(applicationContext, "Unable to fetch data", Toast.LENGTH_SHORT)

                    Tools.inVisibleViews(binding.progressBarLoading, type = 0)
                    Tools.enableViews(binding.buttonSaveProfile)

                }

        }

        binding.buttonSaveProfile.setOnClickListener {
            startingStoreUserDataToCloud()
        }

        binding.fabProfileImage.setOnClickListener {
            // Check android version > or == MARSHMALLOW
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Check if permission granted
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                } else {

                    openImagePicker()

                }

            } else
                openImagePicker()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openImagePicker() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1, 1)
            .start(this@ProfileSetupActivity)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {

                userProfileUri = result.uri
                binding.profileImage.setImageURI(userProfileUri)

                isChanged = true

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                val error: Exception = result.error
            }
        }
    }

    private fun startingStoreUserDataToCloud() {

        genderCheckedId = binding.genderRadioGroup.checkedRadioButtonId

        userName = binding.fieldUserName.text.toString()
        userEMail = binding.fieldEmail.text.toString()
        userMobNumber = binding.fieldMobNumber.text.toString()
        when (genderCheckedId) {
            R.id.maleRadioButton -> userGender = maleRadioButton.text.toString()
            R.id.femaleRadioButton -> userGender = femaleRadioButton.text.toString()
        }

        if (!userName!!.isEmpty() && !userEMail!!.isEmpty() && !userMobNumber!!.isEmpty() && !userGender!!.isEmpty() && userProfileUri != null) {

            if (!isEmailValid(userEMail!!)) {

                binding.fieldEmail.error = "Enter valid email"

            }
            else {

                Tools.visibleViews(binding.progressBarLoading)
                Tools.disableViews(binding.fabProfileImage,
                    binding.genderRadioGroup,
                    binding.fieldUserName,
                    binding.fieldEmail,
                    binding.fieldMobNumber,
                    binding.buttonSaveProfile)

                // check if Image changed or not
                if (isChanged) {

                    userId = auth.currentUser!!.uid

                    // Create  path for Profile Image
                    val imagePath = storageReference.child("profile_images").child("$userId.jpg")
                    uploadTask = imagePath.putFile(userProfileUri!!)

                    // Start uploading profile image
                    uploadTask!!.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->

                        if (task.isSuccessful)
                            mToast.showToast(applicationContext, "Image uploaded", Toast.LENGTH_SHORT)
                        else {
                            Tools.inVisibleViews(binding.progressBarLoading, type = 0)

                            task.exception?.let {
                                throw it
                            }
                        }


                        return@Continuation imagePath.downloadUrl

                    }).addOnCompleteListener { task ->

                        if (task.isSuccessful) {
                            // Upload other data to cloud with imageLink -> Calling Method
                            storingUserDataToCloud(task)
                        } else {

                            Tools.inVisibleViews(binding.progressBarLoading, type = 0)

                        }

                    }

                } else {

                    // If image not changed
                    storingUserDataToCloud(null)

                }

            }

        } else {

            mToast.showToast(applicationContext, "All fields are mandatory", Toast.LENGTH_SHORT)

        }

    }

    private fun storingUserDataToCloud(task: Task<Uri>?) {
        val downloadUri: Uri? = if (task != null)
            task.result
        else
            userProfileUri

        // Upload other data to cloud with imageLink
        // Create Hash Map
        val userMap = HashMap<String?, Any?>()
        userMap["name"] = userName
        userMap["email"] = userEMail
        userMap["mobNumber"] = userMobNumber
        userMap["gender"] = userGender
        userMap["image"] = downloadUri.toString()

        firebaseFirestore.collection("Users").document(userId!!).set(userMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    if (bundle != null) {
                        startActivity(Intent(this, HomeActivity::class.java))
                        finish()
                    } else {
                        mToast.showToast(this, "Profile saved", Toast.LENGTH_SHORT)
                    }

                }

                binding.progressBarLoading.visibility = View.INVISIBLE
                Tools.enableViews(binding.fabProfileImage,
                    binding.genderRadioGroup,
                    binding.fieldUserName,
                    binding.fieldEmail,
                    binding.fieldMobNumber,
                    binding.buttonSaveProfile)
            }
            .addOnFailureListener {
                val error = it.message.toString()
                mToast.showToast(applicationContext, "error : $error", Toast.LENGTH_SHORT)

                binding.progressBarLoading.visibility = View.INVISIBLE
                Tools.enableViews(binding.fabProfileImage,
                    binding.genderRadioGroup,
                    binding.fieldUserName,
                    binding.fieldEmail,
                    binding.fieldMobNumber,
                    binding.buttonSaveProfile)
            }
    }


    // TODO: Check this method later
    private fun areAnyEmpty(vararg value: String): Boolean {
        for (v in value) {
            if (!TextUtils.isEmpty(v))
                return true
        }

        return false
    }

    private fun isEmailValid(emil: String): Boolean {
        if (TextUtils.isEmpty(emil) || !android.util.Patterns.EMAIL_ADDRESS.matcher(emil).matches())
            return false

        return true
    }
}
