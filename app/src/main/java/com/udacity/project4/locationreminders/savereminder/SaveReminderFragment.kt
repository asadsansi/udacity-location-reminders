package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject


private const val DEVICE_LOCATION_ON = 29
private const val FOREGROUND_AND_BACKGROUND_PERMISSION = 33
private const val FOREGROUND_PERMISSIONS = 34
private const val TAG = "SaveReminderFragment"

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {

    private var resultCode = 0
    private lateinit var mContext: Context

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var reminderDataItem: ReminderDataItem
    private lateinit var geofencingClient: GeofencingClient


    override fun onResume() {
        super.onResume()
        mContext = requireContext()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofencingClient = LocationServices.getGeofencingClient(mContext)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(
                    SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
                )
            )
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value ?: ""
            val location = _viewModel.reminderSelectedLocationStr.value ?: ""
            val longitude = _viewModel.longitude.value
            val latitude = _viewModel.latitude.value


            reminderDataItem = ReminderDataItem(
                title, description, location, latitude, longitude
            )

            checkForegroundAndBackgroundPermissionAndLaunchGeofencing()
        }
    }


    private val geoPendingIntent: PendingIntent by lazy {
        val i = Intent(mContext, GeofenceBroadcastReceiver::class.java)
        i.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT)

    }

    @SuppressLint("MissingPermission")
    private fun addingNewGeofence() {

        if (_viewModel.validateAndSaveReminder(reminderDataItem)) {
            val geoFence = Geofence.Builder().setRequestId(reminderDataItem.id)
                .setCircularRegion(
                    reminderDataItem.latitude!!,
                    reminderDataItem.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()

            val requestGeoFencing = GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(geoFence)
                .build()

            geofencingClient.addGeofences(requestGeoFencing, geoPendingIntent)?.run {
                addOnSuccessListener {
                    Log.e("Success: Added Geofence", geoFence.requestId)
                }
                addOnFailureListener {
                    Toast.makeText(mContext, R.string.geofences_not_added, Toast.LENGTH_LONG).show()
                    if (it.message != null) {
                        Log.e(TAG, it.message.toString())
                    }
                }
            }
            _viewModel.onClear()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkForegroundAndBackgroundPermissionAndLaunchGeofencing() {
        if (checkPermissionsForegroundAndBackgroundLocation()) {
            checkLocationSettingsAndLaunchGeofence()
        } else {
            requestPermissionForegroundAndBackgroundLocation()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermissionForegroundAndBackgroundLocation() {
        if (checkPermissionsForegroundAndBackgroundLocation())
            return

        var arrayPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        resultCode = when {
            versionQOrLater -> {
                arrayPermissions += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                FOREGROUND_AND_BACKGROUND_PERMISSION
            }
            else -> FOREGROUND_PERMISSIONS
        }

        requestPermissions(arrayPermissions, resultCode)
    }

    private fun foregroundApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            mContext, Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun backgroundApproved(): Boolean {
        return if (versionQOrLater) {
            val granted = PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
            granted
        } else {
            true
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsForegroundAndBackgroundLocation(): Boolean {
        return foregroundApproved() && backgroundApproved()
    }

    private fun checkLocationSettingsAndLaunchGeofence(isResolved: Boolean = true) {
        val requestLocation = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        val clientSetting = LocationServices.getSettingsClient(mContext)
        val locationBuilder = LocationSettingsRequest.Builder().addLocationRequest(requestLocation)

        val locationSettingResponse =
            clientSetting.checkLocationSettings(locationBuilder.build())
                .addOnFailureListener { failed ->
                    if (failed is ResolvableApiException && isResolved) {
                        try {
                            startIntentSenderForResult(
                                failed.resolution.intentSender, DEVICE_LOCATION_ON, null,
                                0,
                                0,
                                0,
                                null
                            )

                        } catch (e: IntentSender.SendIntentException) {
                            Log.d(TAG, "Error location settings: " + e.message)
                        }
                    } else {
                        Snackbar.make(
                            this.requireView(),
                            R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                        ).setAction(android.R.string.ok) {
                            checkLocationSettingsAndLaunchGeofence()
                        }.show()
                    }
                }
        locationSettingResponse.addOnCompleteListener {
            if (it.isSuccessful) {
                addingNewGeofence()
            }
        }
    }

    private val versionQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == resultCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettingsAndLaunchGeofence()
        } else {
            Snackbar.make(
                binding.layoutContrainer,
                R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
            ).setAction(android.R.string.ok) {
                requestPermissionForegroundAndBackgroundLocation()
            }.show()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // if device location is on
        if (requestCode == DEVICE_LOCATION_ON) {
            checkLocationSettingsAndLaunchGeofence(false)
        }
    }

}

