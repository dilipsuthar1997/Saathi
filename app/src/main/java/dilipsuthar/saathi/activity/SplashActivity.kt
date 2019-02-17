package dilipsuthar.saathi.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import dilipsuthar.saathi.R
import dilipsuthar.saathi.utils.Tools

class SplashActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        Tools.setSystemBarColor(this, R.color.color_accent)

        Handler().postDelayed({
            isUserLoginOrNot()
            finish()
        }, 800)
    }

    private fun isUserLoginOrNot() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            //startActivity(Intent(this, ProfileSetupActivity::class.java))
        }
        else
            startActivity(Intent(this, HomeActivity::class.java))
    }
}
