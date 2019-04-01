package dilipsuthar.saathi.fragment


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.SystemClock
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request

import dilipsuthar.saathi.R
import dilipsuthar.saathi.activity.HomeActivity
import dilipsuthar.saathi.activity.PaymentActivity
import dilipsuthar.saathi.activity.ScanQrActivity
import dilipsuthar.saathi.databinding.FragmentHomeBinding
import dilipsuthar.saathi.directionhelpers.GoogleMapModel
import dilipsuthar.saathi.utils.Constant
import dilipsuthar.saathi.utils.Tools
import dilipsuthar.saathi.utils.mToast
import kotlinx.android.synthetic.main.include_drawer_header_home.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 *
 */
class HomeFragment : Fragment(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener {

    companion object {
        const val TAG: String = "HomeFragmentDebug"
        const val FINE_LOCATION: String = Manifest.permission.ACCESS_FINE_LOCATION
        const val COARSE_LOCATION: String = Manifest.permission.ACCESS_COARSE_LOCATION
        const val LOCATION_PERMISSION_RC = 1234
        const val DEFAULT_ZOOM: Float = 15.5F
    }

    private lateinit var binding: FragmentHomeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private var distance: Float = 0F

    // Google Map
    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private var mGoogleApiClient: GoogleApiClient? = null
    private lateinit var latLng: LatLng
    private lateinit var startLatLng: LatLng
    private lateinit var mCurrLocationMarker: Marker
    private lateinit var mOldLocationMarker: Marker
    private lateinit var currentPolyline: Polyline

    private var geoCoder: Geocoder? = null  // For getting address from location

    private var currentLocation: Location? = null
    private var oldLocation: Location? = null

    private var isOnRide: Boolean? = null

    private lateinit var locationManager: LocationManager
    private var isGPSEnabled = false
    private var isNetworkEnabled = false
    private var isLocationPermissionGranted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        // For checking internet connection
        //------

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        isOnRide = sharedPreferences.getBoolean(Constant.IS_ON_RIDE, false)

        initComponent()
        checkLocationAndDataService()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        mToast.showToast(context!!, "onResume: called", Toast.LENGTH_SHORT)
        initComponent()

        isOnRide = sharedPreferences.getBoolean(Constant.IS_ON_RIDE, false)
        if (isOnRide!!) {
            val num = sharedPreferences.getString(Constant.BIKE_CODE, "-")

            activity!!.textBikeNumber.text = num
            binding.txtCurrentBikeOnCard.text = "Bike: $num"
            Tools.visibleViews(binding.lytOnRideDetails)

            binding.txtTimer.base = SystemClock.elapsedRealtime()
            binding.txtTimer.start()
            binding.txtTimer.setOnChronometerTickListener {
                activity!!.textTravelTime.text = it.text
            }

            mToast.showToast(context!!, num!!, Toast.LENGTH_SHORT)
            binding.textScanCode.text = "End Ride"
            initComponent()

            // Start ride timer


        } else {
            initComponent()
        }

        if (mGoogleApiClient != null)
            mGoogleApiClient!!.connect()
    }

    override fun onPause() {
        super.onPause()
        /*if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.re
        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferences.edit().putString(Constant.BIKE_CODE, "-").apply()
        sharedPreferences.edit().putBoolean(Constant.IS_ON_RIDE, false).apply()
    }

    private fun initComponent() {
        binding.fabLocateMe.setOnClickListener {
            //            getDeviceLocation()
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
        }

        binding.btnScanCode.setOnClickListener {
            if (!isOnRide!!) {
                binding.textScanCode.text = "Unlock Bike"
                startActivity(Intent(activity, ScanQrActivity::class.java))
            } else {
                // Start Payment activity here
                showAlertDialog()
            }
        }

        /*if (!isOnRide!!) {

            binding.btnScanCode.setOnClickListener {
                startActivity(Intent(activity, ScanQrActivity::class.java))
            }
        } else {
            binding.textScanCode.text = "End Ride"
            binding.btnScanCode.setOnClickListener {
                sharedPreferences.edit().putString(Constant.BIKE_CODE, "-").apply()
                sharedPreferences.edit().putBoolean(Constant.IS_ON_RIDE, false).apply()
                activity!!.textBikeNumber.text = sharedPreferences.getString(Constant.BIKE_CODE, "-")
                binding.textScanCode.text = "Unlock Bike"
            }
        }*/

    }

    private fun initMap() {
        Log.d(TAG, "initMap: Map init successfully!!")
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun checkLocationAndDataService() {
        Log.d(TAG, "checkLocationAndDataService: called")
        locationManager = activity!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGPSEnabled) {
            Log.d(TAG, "checkLocationAndDataService: condition false, openDialog")
            val dialog = AlertDialog.Builder(activity!!)
            dialog.setTitle("GPS Setting")
            dialog.setMessage("GPS is not enabled. Do you want to go to setting menu?")
            dialog.setCancelable(false)
            dialog.setPositiveButton("SETTINGS") { _, _ ->
                activity!!.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            dialog.setNegativeButton("CANCEL") { _, _ ->

            }
            dialog.show()
        } else {
            getLocationPermission()
        }

        checkAgainLocationAndDataService()
    }

    private fun checkAgainLocationAndDataService() {
        if (isNetworkEnabled && isGPSEnabled)
            getLocationPermission()
    }

    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: called")
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (ActivityCompat.checkSelfPermission(activity!!, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(activity!!, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                isLocationPermissionGranted = true
                // Initialize mMap
                initMap()
            } else
                ActivityCompat.requestPermissions(activity!!, permissions, LOCATION_PERMISSION_RC)
        } else
            ActivityCompat.requestPermissions(activity!!, permissions, LOCATION_PERMISSION_RC)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.d(TAG, "onRequestPermissionResult: called")
        isLocationPermissionGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_RC -> {
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults.indices) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mToast.showToast(activity!!, "Permission denied", Toast.LENGTH_SHORT)
                            isLocationPermissionGranted = false
                            return
                        }
                    }
                    mToast.showToast(activity!!, "Permission granted", Toast.LENGTH_SHORT)
                    isLocationPermissionGranted = true
                    initMap()
                }
            }
        }
    }

    /*@SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the true location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!)

        if (isLocationPermissionGranted) {
            val location = mFusedLocationProviderClient.lastLocation
            location.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "onComplete: found location!")
                    currentLocation = task.result as Location

                    if (currentLocation == null)
                        Snackbar.make(binding.root, "Unable to find location", Snackbar.LENGTH_SHORT).show()
                    else {
                        oldLocation = currentLocation
                        moveCamera(LatLng(currentLocation!!.latitude, currentLocation!!.longitude), DEFAULT_ZOOM, "Me")
                        getAddress(currentLocation!!)
                        // TODO: get Address of current location
                    }

                } else {
                    Log.d(TAG, "onComplete: current location is null")
                    mToast.showToast(activity!!, "Unable to get current location", Toast.LENGTH_SHORT)
                }
            }
        }
    }*/

    private fun getAddress(location: Location?) {
        location!!.let {
            var addresses: List<Address> = emptyList()

            addresses = geoCoder!!.getFromLocation(location.latitude, location.longitude, 1)

            val address = addresses[0].getAddressLine(0)
            val city = addresses[0].locality
            val state = addresses[0].adminArea
            val countryCode = addresses[0].countryCode

            binding.txtLocationAddress.text = address
        }
    }

    /*@SuppressLint("MissingPermission")
    private fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        Log.d(TAG, "moveCamera: moving the camera to lat: ${latLng.latitude}, lng: ${latLng.longitude}")
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom)

        mMap.clear()

        mMap.animateCamera(cameraUpdate)
        mMap.isMyLocationEnabled = true
        if (title != "Me")
            mMap.addMarker(MarkerOptions().position(latLng).title(title))

        addNearbyMarker(latLng)
    }*/

    private fun addNearbyMarker(latLng: LatLng) {
        var lat = latLng.latitude
        var lng = latLng.longitude
        val counter = floatArrayOf(0.001f, -0.002f, 0.003f, -0.0028f, -0.0054f)
        var bikeNum = 2019000

        for (i in 0 until counter.size) {
            lat = lat.plus(counter[i])
            lng = lng.plus(counter[i])
            bikeNum += 1

            val ltlg = LatLng(lat, lng)
            when (i) {
                0 -> mMap.addMarker(
                    MarkerOptions().position(ltlg).title("$bikeNum").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.bike_pin_x32
                        )
                    )
                )
                1 -> mMap.addMarker(
                    MarkerOptions().position(ltlg).title("$bikeNum").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.bike_pin_x32
                        )
                    )
                )
                2 -> mMap.addMarker(
                    MarkerOptions().position(ltlg).title("$bikeNum").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.bike_pin_x32
                        )
                    )
                )
                3 -> mMap.addMarker(
                    MarkerOptions().position(ltlg).title("$bikeNum").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.bike_pin_x32
                        )
                    )
                )
                4 -> mMap.addMarker(
                    MarkerOptions().position(ltlg).title("$bikeNum").icon(
                        BitmapDescriptorFactory.fromResource(
                            R.drawable.bike_pin_x32
                        )
                    )
                )

            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mToast.showToast(activity!!, "Map is ready", Toast.LENGTH_SHORT)
        mMap = googleMap!!
//        mMap.clear()

        // Customize the style of Google Map
        setCustomTheme(mMap)


        // call getDeviceLocation
        try {
            if (isLocationPermissionGranted) {
//                getDeviceLocation()

                mMap.isMyLocationEnabled = true
                mMap.isBuildingsEnabled = true
                mMap.isTrafficEnabled = false
                mMap.uiSettings.isZoomControlsEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false

                geoCoder = Geocoder(activity!!, Locale.getDefault())    // For address
                buildGoogleApiClient()
            } else {
                mMap.isMyLocationEnabled = false
                getLocationPermission()
            }
        } catch (e: SecurityException) {

        }
    }

    private fun buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient: called")
        mGoogleApiClient = GoogleApiClient.Builder(context!!)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build()

        if (mGoogleApiClient != null)
            mGoogleApiClient!!.connect()
    }

    override fun onConnected(bundle: Bundle?) {
        Log.d(TAG, "onConnected: called")

        if (mGoogleApiClient!!.isConnected) {

            try {

                val mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
                if (mLastLocation != null) {
                    mMap.clear()
                    latLng = LatLng(mLastLocation.latitude, mLastLocation.longitude)
                    startLatLng = latLng
                    getAddress(mLastLocation)
//                    mCurrLocation = mMap.addMarker(MarkerOptions().position(latLng).title("You'r here").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_x64)))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
                    addNearbyMarker(latLng)

                    if (!isOnRide!!) {
                        oldLocation = mLastLocation
                        startLatLng = latLng
//                        drawPolylineMap()
                    } else {
                        // TODO: Start ride & drawing line on MAP
                        drawPolylineMap()
                    }
                }

                mLocationRequest = LocationRequest()
                mLocationRequest.interval = 5000
                mLocationRequest.fastestInterval = 3000
                mLocationRequest.priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY

                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this)


            } catch (e: SecurityException) {

            }

        }
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.d(TAG, "onConnectionSuspend: called")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed: called")
    }

    @SuppressLint("SetTextI18n")
    override fun onLocationChanged(location: Location?) {
        /*if (mCurrLocation != null) {
            mCurrLocation.remove()
        }*/
        if (location != null) {
            currentLocation = location
            //mToast.showToast(context!!, "Location changed", Toast.LENGTH_SHORT)
            latLng = LatLng(location.latitude, location.longitude)
            //mCurrLocation = mMap.addMarker(MarkerOptions().position(latLng).title("You'r here").icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_x64)))
            getAddress(location)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))

            if (!isOnRide!!) {
                oldLocation = location
                startLatLng = latLng
            } else {
                // TODO: Start ride & drawing line on MAP
                mMap.clear()
                drawPolylineMap()
                distance = oldLocation!!.distanceTo(currentLocation)   // Calculate distance btn 2 location's
                sharedPreferences.edit().putString(Constant.TRAVEL_DISTANCE, distance.toString()).apply()
                if (distance < 1000) {
                    activity!!.textDistance.text = "${distance.toInt()} m"
                    binding.txtDistance.text = "${distance.toInt()} m"
                }
                else {
                    activity!!.textDistance.text = "${distance.toInt() / 1000} Km"
                    binding.txtDistance.text = "${distance.toInt() / 1000} Km"
                }
            }
        }
    }

    private fun drawPolylineMap() {
        val routUrl = getDirectionUrl(startLatLng, LatLng(latLng.latitude, latLng.longitude), "driving")
        GetDirection(routUrl).execute()
    }

    /*private fun moveCamera(latLng: LatLng, zoom: Float, title: String) {
        Log.d(TAG, "moveCamera: called")
        mMap.addMarker(MarkerOptions().position(latLng).title(title).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))

    }*/

    private fun getDirectionUrl(origin: LatLng, dest: LatLng, drivingMode: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=$drivingMode&key=${getString(R.string.google_maps_key)}"
    }
    //------------------------ doInBackground process ------------------------------------------------------------------
    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url: String) : AsyncTask<Void, Void, List<List<LatLng>>>() {

        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            Log.d(TAG, "GetDirection: doInBackground: called")

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body().string()
            Log.d(TAG, "GetDirection: doInBackground: data: $data")
            val result = ArrayList<List<LatLng>>()
            try {

                if (result.isNotEmpty()) {
                    result.clear()
                }

                val respObj = Gson().fromJson(data, GoogleMapModel::class.java)
                val path = ArrayList<LatLng>()

                for (i in 0..(respObj.routes[0].legs[0].steps.size - 1)) {

                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))

                }

                result.add(path)

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "doInBackground: Error: ${e.message}")
            }

            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineOption = PolylineOptions()
            for (i in result.indices) {
                lineOption.addAll(result[i])
                lineOption.width(10F)
                lineOption.color(resources.getColor(R.color.color_accent))
                lineOption.geodesic(true)
            }

            mMap.addPolyline(lineOption)
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }

        return poly
    }
//----------------------------------------------------------------------------------------------------------------------

    private fun setCustomTheme(map: GoogleMap) {

        /*val hour = getClockTime()

        when (hour) {
            in 0..6 -> map.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.night))
            in 7..18 -> map.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.uber_style))
            in 19..24 -> map.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.night))
        }*/

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(activity, R.raw.uber_style))
    }

    private fun getClockTime(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY)
    }

    private fun showAlertDialog() {
        val alertDialog = AlertDialog.Builder(activity!!)
        alertDialog.setTitle("End Ride")
        alertDialog.setMessage("Are you sure you want to end ride?")
        alertDialog.setPositiveButton("Yes") { dialog, which ->

            // End ride and go to payment screen.
            mMap.clear()
            binding.txtTimer.stop()

            //sharedPreferences.edit().putString(Constant.BIKE_CODE, "-").apply()
            sharedPreferences.edit().putBoolean(Constant.IS_ON_RIDE, false).apply()
            sharedPreferences.edit().putString(Constant.TRAVEL_DISTANCE, distance.toString()).apply()
            sharedPreferences.edit().putString(Constant.TRAVEL_TIME, binding.txtTimer.text.toString()).apply()

            activity!!.textBikeNumber.text = sharedPreferences.getString(Constant.BIKE_CODE, "-")
            binding.txtCurrentBikeOnCard.text = "Bike: ${sharedPreferences.getString(Constant.BIKE_CODE, "-")}"
            Tools.inVisibleViews(binding.lytOnRideDetails, type = 1)
            binding.txtTimer.base = SystemClock.elapsedRealtime()

            binding.textScanCode.text = "Unlock Bike"
            onResume()
            checkLocationAndDataService()

            // TODO: Start payment activity here.
            dialog.dismiss()
            startActivity(Intent(activity!!, PaymentActivity::class.java))

        }
        alertDialog.setNegativeButton("No") { dialog, which ->
            mToast.showToast(activity!!, "No", Toast.LENGTH_SHORT)
        }
        alertDialog.show()
    }

}
