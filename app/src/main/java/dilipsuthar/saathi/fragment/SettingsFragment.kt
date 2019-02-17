package dilipsuthar.saathi.fragment


import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil

import dilipsuthar.saathi.R
import dilipsuthar.saathi.activity.HomeActivity
import dilipsuthar.saathi.databinding.FragmentSettingsBinding
import dilipsuthar.saathi.utils.Constant

/**
 * A simple [Fragment] subclass.
 *
 */
class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        updateUI(sharedPreferences)

        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            binding.switchNightMode.isChecked = true
        }

        binding.switchNightMode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                sharedPreferences.edit().putBoolean(Constant.TOGGLE_DARK_THEME, isChecked).apply()
                restartApp()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                sharedPreferences.edit().putBoolean(Constant.TOGGLE_DARK_THEME, isChecked).apply()
                restartApp()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
    }

    private fun restartApp() {
        val i = Intent(activity, HomeActivity::class.java)
        startActivity(i)
        activity!!.finish()
    }

    private fun updateUI() {

    }

    private fun updateUI(sharedPreferences: SharedPreferences) {
        binding.switchNightMode.isChecked = sharedPreferences.getBoolean(Constant.TOGGLE_DARK_THEME, false)
    }

}
