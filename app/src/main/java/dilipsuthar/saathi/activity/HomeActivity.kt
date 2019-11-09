package dilipsuthar.saathi.activity

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import dilipsuthar.saathi.R
import dilipsuthar.saathi.databinding.ActivityHomeBinding
import dilipsuthar.saathi.fragment.*
import dilipsuthar.saathi.utils.ConnectivityReceiver
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast
import kotlinx.android.synthetic.main.include_drawer_header_home.*
import kotlinx.android.synthetic.main.include_drawer_header_home.view.*

class HomeActivity : AppCompatActivity() {

    companion object {
        const val TAG: String = "HomeActivity_debug"
    }

    lateinit var binding: ActivityHomeBinding

    private lateinit var actionBar: ActionBar
    private lateinit var headerView: View
    private lateinit var sharedPreferences: SharedPreferences

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var storageReference: StorageReference
    private lateinit var firebaseFirestore: FirebaseFirestore

    // Fragments
    private lateinit var homeFragment: HomeFragment
    private lateinit var myRidesFragment: MyRidesFragment
    private lateinit var contactUsFragment: ContactUsFragment
    private lateinit var aboutUsFragment: AboutUsFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var helpFeedbackFragment: HelpFeedbackFragment

    private var userId: String? = null
    private var currentUser: Any? = null
    private var userProfileUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false)) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.AppTheme)
        }

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser
        storageReference = FirebaseStorage.getInstance().reference
        firebaseFirestore = FirebaseFirestore.getInstance()

        userId = auth.currentUser!!.uid

        initToolbar()
        initTheme()
        initComponent()
        initNavigationDrawer()

        fetchCurrentUserData()

        // Set default fragment
        displayFragment(homeFragment)
        binding.navView.setCheckedItem(R.id.nav_ride_now)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: called")
        fetchCurrentUserData()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Set Current Bike = null
        sharedPreferences.edit().putString(Constant.BIKE_CODE, "-").apply()
        //unregisterReceiver(ConnectivityReceiver())
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
            binding.drawerLayout.closeDrawers()
        else
            super.onBackPressed()
    }

    private fun initTheme() {

    }

    private fun initToolbar() {
        binding.toolbar.popupTheme = R.style.ThemeOverlay_AppCompat_Dark_ActionBar
        if (sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false))
            binding.toolbar.setTitleTextColor(Color.WHITE)
        else
            binding.toolbar.setTitleTextColor(Color.BLACK)
        setSupportActionBar(binding.toolbar)
        actionBar = supportActionBar!!
        actionBar.title = getString(R.string.app_name)
        actionBar.elevation = 0.0F
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu)

        Tools.setSystemBarColor(this, R.color.grey_90)
        Tools.changeNavigationIconColor(binding.toolbar, resources.getColor(R.color.color_accent))
    }

    private fun initComponent() {
        //Check if User new OR old
        /*if (isUserNew())
            binding.textSuccess.text = "Welcome, New user!"
        else
            binding.textSuccess.text = "Welcome back!!"

        binding.buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfileSetupActivity::class.java))
        }*/

        homeFragment = HomeFragment()
        myRidesFragment = MyRidesFragment()
        contactUsFragment = ContactUsFragment()
        aboutUsFragment = AboutUsFragment()
        settingsFragment = SettingsFragment()
        helpFeedbackFragment = HelpFeedbackFragment()

    }

    private fun initNavigationDrawer() {
        val toggle = object : ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
        }

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.drawerArrowDrawable.color = resources.getColor(R.color.color_accent)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener { menuItem ->
//            mToast.showToast(applicationContext, "${menuItem.title} Selected", Toast.LENGTH_SHORT)
            binding.drawerLayout.closeDrawers()

            when (menuItem.itemId) {
                R.id.nav_sign_out -> {
                    auth.signOut()
                    sendToAuth()
                }
                R.id.nav_ride_now -> displayFragment(homeFragment)
                R.id.nav_my_rides -> displayFragment(myRidesFragment)
                R.id.nav_contact_us -> displayFragment(contactUsFragment)
                R.id.nav_about_us -> displayFragment(aboutUsFragment)
                R.id.nav_settings -> displayFragment(settingsFragment)
                R.id.nav_help_feedback -> displayFragment(helpFeedbackFragment)
            }

            return@setNavigationItemSelectedListener true
        }

        headerView = binding.navView.getHeaderView(0)

        // OnClick method for HeaderView
        headerView.avatar.setOnClickListener { startActivity(Intent(this, ProfileSetupActivity::class.java)) }
    }

    private fun isUserNew(): Boolean {
        val metadata = auth.currentUser?.metadata
        return metadata?.creationTimestamp == metadata?.lastSignInTimestamp
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            sendToAuth()
        }
    }

    private fun sendToAuth() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun displayFragment(fragment: Fragment?) {

        Handler().postDelayed({
            if (fragment != null) {
                val transaction = supportFragmentManager.beginTransaction()
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                transaction.replace(R.id.fragment, fragment)
                if (fragment == homeFragment)
                    transaction.setPrimaryNavigationFragment(fragment)
                transaction.commit()
            }
        }, 220)
    }

    private fun fetchCurrentUserData() {
        firebaseFirestore.collection("Users").document(userId!!).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result!!.exists()) {
                        userProfileUrl = task.result!!.getString("image")
//                    actionBar.subtitle = "Welcome, ${task.result!!.getString("name")}"
                        headerView.name.text = task.result!!.getString("name")
                        headerView.email.text = task.result!!.getString("email")

                        val defaultRequestOptions = RequestOptions().placeholder(R.drawable.placeholder)

                        Glide.with(applicationContext)
                            .setDefaultRequestOptions(defaultRequestOptions)
                            .load(userProfileUrl)
                            .into(binding.imageCurrentUser)

                        Glide.with(applicationContext)
                            .setDefaultRequestOptions(defaultRequestOptions)
                            .load(userProfileUrl)
                            .into(headerView.avatar)
                    }
                } else {
                    mToast.showToast(this, "Unable to fetch data", Toast.LENGTH_SHORT)
                }
            }
    }

}
