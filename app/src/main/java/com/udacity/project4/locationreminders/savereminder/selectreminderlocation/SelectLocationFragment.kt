package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var mGoogleMap: GoogleMap

    private lateinit var mMarker: Marker
    private lateinit var mLastLocation: Location
    private val defaultZoom = 18f


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)


        checkLocationSetting()
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        // setting map style
        try {
            val styleSuccess = mGoogleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map
                )
            )
            if (!styleSuccess) {
                Log.e(TAG, "Style failed")
            }
        } catch (exception: Resources.NotFoundException) {
            Log.e(TAG, "Style Exception: ", exception)
        }

        // mapLongClick
        mGoogleMap.setOnMapLongClickListener {
            mGoogleMap.clear()
            val snippet = String.format(
                Locale.getDefault(), "Lat: %1$.5f, Long: %2$.5f",
                it.latitude,
                it.longitude
            )
            mMarker = mGoogleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
            )
            mMarker.showInfoWindow()
        }

        mGoogleMap.setOnPoiClickListener {
            mGoogleMap.clear()
            mMarker = mGoogleMap.addMarker(
                MarkerOptions()
                    .position(it.latLng)
                    .title(it.name)
            )
            mGoogleMap.addCircle(
                CircleOptions()
                    .center(it.latLng)
                    .radius(7.0)
                    .strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(4F)
            )
            mMarker.showInfoWindow()
        }

        // getting device location
        getLocation()
    }

    private fun onLocationSelected() {

        if (this::mMarker.isInitialized) {

            _viewModel.reminderSelectedLocationStr.value = mMarker.title
            _viewModel.latitude.value = mMarker.position.latitude
            _viewModel.longitude.value = mMarker.position.longitude

            findNavController().popBackStack()
        } else {
            Toast.makeText(
                context,
                resources.getString(R.string.select_location),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun checkLocationSetting(isResolved: Boolean = true) {
        val requestLocation = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val locationBuilder = LocationSettingsRequest.Builder().addLocationRequest(requestLocation)
        val clientSetting = LocationServices.getSettingsClient(requireContext())

        clientSetting.checkLocationSettings(locationBuilder.build())
            .addOnFailureListener { failed ->
                if (failed is ResolvableApiException && isResolved) {

                    try {
                        failed.startResolutionForResult(
                            activity,
                            REQUEST_TURN_DEVICE_LOCATION_ON
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Log.d(TAG, "Error location settings: " + e.message)
                    }
                } else {
                    Snackbar.make(
                        this.requireView(),
                        R.string.location_required_error,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(android.R.string.ok) {
                            checkLocationSetting()
                        }.show()
                }
            }
    }

    private fun getLocation() {

        val defaultLocation =
            LatLng(47.5456551, 122.0101731)
        val fusedLocationProvider =
            LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            if (checkPermission()) {
                mGoogleMap.isMyLocationEnabled = true
                fusedLocationProvider.lastLocation
                    .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        if (task.result != null) {
                            mLastLocation = task.result!!
                            mGoogleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        mLastLocation.latitude,
                                        mLastLocation.longitude
                                    ),
                                    defaultZoom
                                )
                            )
                        }
                    } else {
                        Log.e(TAG, "Location Null : "+ task.exception)
                        mGoogleMap.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, defaultZoom)
                        )
                        mGoogleMap.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }

            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        } catch (exception: SecurityException) {
            Log.e("Security Exception", exception.message.toString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        // Change the map type based on the user's selection.
        R.id.normal_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mGoogleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun checkPermission(): Boolean {
        return context?.let {
            ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } == PackageManager.PERMISSION_GRANTED
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            Snackbar.make(
                binding.selectLocationFragment,
                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
            ).setAction(android.R.string.ok) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
                )
            }.show()
        }
    }

}



