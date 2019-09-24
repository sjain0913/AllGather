package edu.gatech.ce.allgather.helpers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.support.v4.app.ActivityCompat
import android.util.Log

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import edu.gatech.ce.allgather.AllGatherApplication

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import edu.gatech.ce.allgather.MainActivity
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.util.CurveFinder
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

/**
 * Created by achatterjee36 on 7/7/2017.
 */

class LocationHelper(_context: Context) : HelperClass(_context) {
    var UPDATE_FREQUENCY: Long = 0
    internal var locationProvider: FusedLocationProviderClient
    internal var mLocationRequest: LocationRequest
    internal var latestLocation: Location? = null
    internal lateinit var mFOS: FileOutputStream
    private lateinit var mCOS: CipherOutputStream
    internal var mLocationCallback: LocationCallback

    override val isReady: Boolean
        get() = latestLocation != null

    init {
        //set the update frequency in milliseconds
        UPDATE_FREQUENCY = 100
        //request device to get location updates at this frequency
        locationProvider = LocationServices.getFusedLocationProviderClient(context)
        mLocationRequest = LocationRequest()
        mLocationRequest.interval = UPDATE_FREQUENCY
        mLocationRequest.fastestInterval = UPDATE_FREQUENCY
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest)
        //now listen for when our request has been approved
        val checkBuilder = LocationSettingsRequest.Builder()
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(checkBuilder.build())
        task.addOnSuccessListener(context as Activity) {
            startLocationUpdates() // request approved, let's start listening for location
        }
        //explain what happens when we get a location update
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                latestLocation = locationResult.lastLocation
                //push latest location to MainActivity UI
                //(context as MainActivity).updateLocationUI()
                if (isRecording) {
                    try {
                        val cal = Calendar.getInstance()
                        val date = cal.time
                        val dateFormat = SimpleDateFormat("HH:mm:ss")
                        val formattedDate = dateFormat.format(date)
                        val s = latestLocation!!.time.toString() + "," +
                                latestLocation!!.latitude + "," +
                                latestLocation!!.longitude + "," +
                                latestLocation!!.altitude + "," +
                                latestLocation!!.bearing + "," +
                                latestLocation!!.accuracy + "\n"
                        mCOS.write(s.toByteArray())
                        // use curve finder code
                        if (AllGatherApplication.USE_CURVES) {
                            CurveFinder.addPoint(latestLocation!!)
                        }
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }

                }
            }
        }
    }


    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MainActivity.REQUEST_LOCATION_PERMISSION)
            return
        }
        locationProvider.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
    }

    private fun stopLocationUpdates() {
        locationProvider.removeLocationUpdates(mLocationCallback)
    }

    @Throws(IOException::class)
    override fun record(f: File) {
        mFOS = FileOutputStream(f)

        //encryption
        val key = context.resources.getString(R.string.encryption_key)
        val dks = DESKeySpec(key.toByteArray(Charsets.UTF_8))
        val skf = SecretKeyFactory.getInstance("DES")
        val desKey = skf.generateSecret(dks)
        val cipher = Cipher.getInstance("DES")
        cipher.init(Cipher.ENCRYPT_MODE, desKey)
        mCOS = CipherOutputStream(mFOS,cipher)

        mCOS.write("local_timestamp_milliseconds, latitude_dd,longitude_dd,altitude_m,bearing_deg,accuracy_m\n".toByteArray())
    }

    override fun name2File(fileName: String): File {
        val folder = File(strUtil.getSpecificStorageFolderPath(fileName.substring(0,10)) + LOCATION_FOLDER)
        if (!folder.mkdirs()) {
            Log.d("AC1", "Loc Directory not created")
        }
        return File(folder, fileName + "_loc.lox")
        //return new File(getOverallAppStorageFolderPath(),fileName+"_loc.csv");
    }

    override fun stopRecord() {
        try {
            mCOS.flush()
            mCOS.close()
            mFOS.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    companion object {
        val LOCATION_FOLDER = "location/"
    }


}
