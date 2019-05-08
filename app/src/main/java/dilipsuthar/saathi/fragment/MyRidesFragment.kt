package dilipsuthar.saathi.fragment


import android.app.ProgressDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

import dilipsuthar.saathi.R
import dilipsuthar.saathi.adapter.AdapterRideHistory
import dilipsuthar.saathi.databinding.FragmentMyRidesBinding
import dilipsuthar.saathi.model.ItemRideHistory
import dilipsuthar.saathi.utils.mToast

/**
 * A simple [Fragment] subclass.
 *
 */
class MyRidesFragment : Fragment() {

    private lateinit var binding: FragmentMyRidesBinding
    private var items: ArrayList<ItemRideHistory> = ArrayList()
    private lateinit var adapter: AdapterRideHistory

    private lateinit var progressDialog: ProgressDialog

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebaseFirestore: FirebaseFirestore
    private lateinit var documentSnapshot: DocumentSnapshot
    private var userId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_rides, container, false)

        mAuth = FirebaseAuth.getInstance()
        mFirebaseFirestore = FirebaseFirestore.getInstance()
        userId = mAuth.currentUser!!.uid

        initComponent(binding.root)

        return binding.root
    }

    private fun initComponent(view: View) {

        // Show loading dialog
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage("Please wait...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)
        progressDialog.show()

        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.setHasFixedSize(true)

        // Fetching data and show in Lists
        fetchRideHistoryDataAndShow()

    }

    private fun fetchRideHistoryDataAndShow() {
        items.clear()

        val query = mFirebaseFirestore.collection("Users")
            .document(userId)
            .collection("RideHistory")
            .orderBy("time_stamp", Query.Direction.DESCENDING)

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->

            if (querySnapshot != null) {

                for ((i, doc) in querySnapshot.withIndex()) {
                    val rideHistory = ItemRideHistory(
                        doc.getString("bike_id"),
                        doc.getString("ride_date_time"),
                        doc.getDate("time_stamp"),
                        doc.getString("travel_amount"),
                        doc.getString("travel_distance"),
                        doc.getString("travel_time"))

                    items.add(rideHistory)
                }

                adapter = AdapterRideHistory(items)
                binding.recyclerView.adapter = adapter
                progressDialog.dismiss()

            } else {

                mToast.showToast(activity, "Unable to fetch user history", Toast.LENGTH_SHORT)
                progressDialog.dismiss()

            }

        }

    }

}
