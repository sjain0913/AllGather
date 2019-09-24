package edu.gatech.ce.allgather.helpers

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log

import edu.gatech.ce.allgather.AllGatherApplication

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import edu.gatech.ce.allgather.MainActivity
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.util.CurveFinder
import edu.gatech.ce.allgather.util.StopVehicleFilter
import io.reactivex.disposables.Disposable
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

/**
 * Created by achatterjee36 on 7/7/2017.
 */

class LocationHelper2(_context: Context) : HelperClass(_context) {
    var UPDATE_FREQUENCY: Long = 0
    internal var latestLocation: Location? = null
    private lateinit var mFOS: FileOutputStream
    private lateinit var mCOS: CipherOutputStream

    private var mStopVehicleSubscription: Disposable? = null

    override val isReady: Boolean
        get() = latestLocation != null

    init {
        //set the update frequency in milliseconds
        UPDATE_FREQUENCY = 100

        //request device to get location updates at this frequency
        val locationManager : LocationManager = (context as MainActivity).getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(locationResult: Location?) {
                locationResult ?: return
                latestLocation = locationResult
                if (isRecording) {
                    try {
                        // write to the file
                        val cal = Calendar.getInstance()
                        val date = cal.time
                        val dateFormat = SimpleDateFormat("HH:mm:ss")
                        val formattedDate = dateFormat.format(date)
                        val s = "${latestLocation!!.time},$formattedDate,${latestLocation!!.latitude},${latestLocation!!.longitude}," +
                                "${latestLocation!!.altitude},${latestLocation!!.bearing},${latestLocation!!.accuracy}\n"
                        if(AllGatherApplication.ENCRYPT_DATA)
                            mCOS.write(s.toByteArray())
                        else
                            mFOS.write(s.toByteArray())

                        // send point to filter
                        StopVehicleFilter.addPoint(latestLocation!!)

                        // use curve finder code
                        if (AllGatherApplication.USE_CURVES) {
                            CurveFinder.addPoint(latestLocation!!)
                        }
                    } catch (ex: IOException) {
                        mainActivity.logException(ex)
                        ex.printStackTrace()
                    }

                }
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
            override fun onProviderEnabled(p0: String?) {}
            override fun onProviderDisabled(p0: String?) {}

        }
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), MainActivity.REQUEST_LOCATION_PERMISSION)
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0f,locationListener)
        }

    }


    @Throws(IOException::class)
    override fun record(f: File) {
        mFOS = FileOutputStream(f)

        if(AllGatherApplication.ENCRYPT_DATA)
        {
            //encryption
            val key = context.resources.getString(R.string.encryption_key)
            val dks = DESKeySpec(key.toByteArray(Charsets.UTF_8))
            val skf = SecretKeyFactory.getInstance("DES")
            val desKey = skf.generateSecret(dks)
            val cipher = Cipher.getInstance("DES")
            cipher.init(Cipher.ENCRYPT_MODE, desKey)
            mCOS = CipherOutputStream(mFOS, cipher)
            mCOS.write("local_timestamp_milliseconds, latitude_dd,longitude_dd,altitude_m,bearing_deg,accuracy_m\n".toByteArray())
        }
        else
            mFOS.write("local_timestamp_milliseconds, latitude_dd,longitude_dd,altitude_m,bearing_deg,accuracy_m\n".toByteArray())



    }

    override fun name2File(fileName: String): File {
        val folder = File(strUtil.getSpecificStorageFolderPath(fileName.substring(0,10)) + LOCATION_FOLDER)
        if (!folder.mkdirs()) {
            Log.d("AC1", "Loc Directory not created")
        }
        if(AllGatherApplication.ENCRYPT_DATA)
            return File(folder, fileName + "_loc.lox")
        else
            return File(folder, fileName + "_loc.csv")
    }

    override fun stopRecord() {
        try {
            if(AllGatherApplication.ENCRYPT_DATA)
            {
                mCOS.flush()
                mCOS.close()
            }

            mFOS.close()

            // unsubscribe to stopped vehicle filter
            mStopVehicleSubscription?.dispose()
        } catch (e: IOException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

    }

    companion object {
        val LOCATION_FOLDER = "location/"
        val TAG = "LocationHelper2"
    }


}
