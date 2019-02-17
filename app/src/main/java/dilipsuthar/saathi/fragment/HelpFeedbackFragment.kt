package dilipsuthar.saathi.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import dilipsuthar.saathi.R
/**
 * A simple [Fragment] subclass.
 *
 */
class HelpFeedbackFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help_feedback, container, false)
    }


}
