package edu.gatech.ce.allgather.helpers

//import for all the hardware stuff
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import edu.gatech.ce.allgather.MainActivity
import android.R.attr.orientation
import android.view.WindowManager
import android.widget.Toast
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.R
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import android.R.attr.data
import java.text.DateFormat
import java.util.*
import java.text.SimpleDateFormat


/**
 * Created by achatterjee36 on 6/14/2017.
 */

class AccelerationHelper(_context: Context) : HelperClass(_context), SensorEventListener {
    private val mSensorManager: SensorManager
    private val mLinearAcceleration: Sensor
    private val mMagneticForce: Sensor
    private val mRotation: Sensor
    private val mGyroscope: Sensor
    private lateinit var mFOS: FileOutputStream
    private lateinit var mCOS: CipherOutputStream
    private val latestAcceleration = floatArrayOf(0f, 0f, 0f)
    private val latestAngularPosition = floatArrayOf(0f, 0f, 0f, 0f)
    private val latestOrientation = floatArrayOf(0f, 0f, 0f, 0f)
    private val latestAngularVelocity = floatArrayOf(0f, 0f, 0f)
    private val latestMagneticForce = floatArrayOf(0f, 0f, 0f)


    override val isReady: Boolean
        get() = true // TODO: implement

    init {
        // test stream acceleration data
        mSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagneticForce = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mRotation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_UI)

        mSensorManager.registerListener(object : SensorEventListener{

            override fun onSensorChanged(event: SensorEvent) {
                latestMagneticForce[0] = event.values[0]
                latestMagneticForce[1] = event.values[1]
                latestMagneticForce[2] = event.values[2]
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

        }, mMagneticForce, SensorManager.SENSOR_DELAY_UI)

        mSensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                //(context as MainActivity).updateRotationUI()
                latestAngularPosition[0] = event.values[0]
                latestAngularPosition[1] = event.values[1]
                latestAngularPosition[2] = event.values[2]
                latestAngularPosition[3] = event.values[3]
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }, mRotation, SensorManager.SENSOR_DELAY_UI)

        mSensorManager.registerListener(object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                //(context as MainActivity).updateAngularVelocityUI()
                latestAngularVelocity[0] = event.values[0]
                latestAngularVelocity[1] = event.values[1]
                latestAngularVelocity[2] = event.values[2]
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

            }
        }, mGyroscope, SensorManager.SENSOR_DELAY_UI)
    }

    override fun name2File(fileName: String): File {
        val folder = File(strUtil.getSpecificStorageFolderPath(fileName.substring(0,10)) + ACCELERATION_FOLDER)
        if (!folder.mkdirs()) {
            Log.d("AC1", "Acc Directory not created")
        }
        if(AllGatherApplication.ENCRYPT_DATA)
            return File(folder, fileName + "_acc.acx")
        else
            return File(folder, fileName + "_acc.csv")
        //return new File(getOverallAppStorageFolderPath(),fileName+"_acc.csv");
    }

    @Throws(IOException::class)
    override fun record(f: File) {
        mFOS = FileOutputStream(f)
        // get orientation
        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowmanager.defaultDisplay.rotation

        if(AllGatherApplication.ENCRYPT_DATA)
        {
            //encryption
            val key = context.resources.getString(R.string.encryption_key)
            val dks = DESKeySpec(key.toByteArray(Charsets.UTF_8))
            val skf = SecretKeyFactory.getInstance("DES")
            val desKey = skf.generateSecret(dks)
            val cipher = Cipher.getInstance("DES")
            cipher.init(Cipher.ENCRYPT_MODE, desKey)
            mCOS = CipherOutputStream(mFOS,cipher)

            //write orientation and header
            mCOS.write("orientation,${rotation.toString()}\n".toByteArray())
            mCOS.write("timestamp_nanosecond,local_timestamp_milliseconds,accel_x_mps2,accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2,rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps,angvelocity_y_radps,angvelocity_z_radps\n".toByteArray())
        }
        else
        {
            //write orientation and header
            mFOS.write("orientation,${rotation.toString()}\n".toByteArray())
            mFOS.write("timestamp_nanosecond,local_timestamp_milliseconds,accel_x_mps2,accel_y_mps2,accel_z_mps2,rotation_x_sin_theta_by_2,rotation_y_sin_theta_by_2,rotation_z_sin_theta_by_2,rotation_cos_theta_by_2,angvelocity_x_radps,angvelocity_y_radps,angvelocity_z_radps\n".toByteArray())
        }


    }

    override fun stopRecord() {
        try {
            if(AllGatherApplication.ENCRYPT_DATA)
            {
                mCOS.flush()
                mCOS.close()
            }

            mFOS.close()

        } catch (e: IOException) {
            mainActivity.logException(e)
            e.printStackTrace()
        }

    }
//Is nano second local time?? Ask her

    override fun onSensorChanged(event: SensorEvent) {
        //(context as MainActivity).updateAccelerationUI(event.values[0],event.values[1],event.values[2])
        latestAcceleration[0] = event.values[0]
        latestAcceleration[1] = event.values[1]
        latestAcceleration[2] = event.values[2]

        if (isRecording) {
            try {
                val cal = Calendar.getInstance()
                val date = cal.time
                val dateFormat = SimpleDateFormat("HH:mm:ss")
                val formattedDate = dateFormat.format(date)
                val s = event.timestamp.toString() + "," +
                        System.currentTimeMillis() + "," +
                        latestAcceleration[0] + "," +
                        latestAcceleration[1] + "," +
                        latestAcceleration[2] + "," +
                        latestAngularPosition[0] + "," +
                        latestAngularPosition[1] + "," +
                        latestAngularPosition[2] + "," +
                        latestAngularPosition[3] + "," +
                        latestAngularVelocity[0] + "," +
                        latestAngularVelocity[1] + "," +
                        latestAngularVelocity[2] + "\n"
                if(AllGatherApplication.ENCRYPT_DATA)
                    mCOS.write(s.toByteArray())
                else
                    mFOS.write(s.toByteArray())
            } catch (ex: IOException) {
                mainActivity.logException(ex)
                ex.printStackTrace()
            }

        }
        else
        {
            //get roll, pitch, azimuth
            var rotMatrix = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            SensorManager.getRotationMatrix(rotMatrix, null, latestAcceleration, latestMagneticForce)
            var orientationAngles = floatArrayOf(0f, 0f, 0f)
            SensorManager.getOrientation(rotMatrix, orientationAngles)
            (context as MainActivity).updateCalibrationUI(orientationAngles[2],orientationAngles[1])
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    companion object {

        private val ACCELERATION_FOLDER = "acceleration/"
    }
}
