package com.example.weatherapp

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.models.Weather
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mProgressDialog:Dialog?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)




        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your Location provider is turned off,Please turn it on",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            Dexter.withContext(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    @RequiresApi(Build.VERSION_CODES.S)
                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        if (p0!!.areAllPermissionsGranted()) {
//                            Add request location data
                            requestLocationData()
                        }

                        if (p0.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission, Please enable them",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        p0: MutableList<PermissionRequest>?,
                        p1: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    private fun getLocationWeatherDetails(latitude:Double?,longitude:Double?) {
        if (Constants.isNetworkAvailable(this)) {
            val retroFit: Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()

            val service: WeatherService= retroFit
                .create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(latitude!!,
                longitude!!,Constants.METRIC_UNIT,Constants.APP_ID)


            showCustomProgressDialog()


            listCall.enqueue(object: Callback<WeatherResponse>{
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if(response.isSuccessful){
                        hideCustomProgressDialog()
                        val weatherList: WeatherResponse? = response.body()
                        Log.i("Weather Response","$weatherList")
                    }
                    else{
                        val rc= response.code()
                        when(rc){
                            400->{
                                Log.e("ERROR 400","Bad Connection")
                            }
                            404->{
                                Log.e("ERROR 404","Error 404 Not Found")
                            }
                            else->{
                                Log.e("Generic error","Error")

                            }

                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    hideCustomProgressDialog()
                    Log.e("ERRORrrrrr",t.message.toString())
                }

            })



        } else {
            Toast.makeText(this, "No INTERNET connection!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("Looks like you have turned off permissions please enable them")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("CANCEL") { dialog, _ ->
                dialog.dismiss()
            }.show()

    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestLocationData() {
        val mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY
        mFusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            val mLastLocation: Location? = p0.lastLocation
            val latitude = mLastLocation?.latitude
            Log.i("Current Latitude", latitude.toString())
            val longitude = mLastLocation?.longitude
            Log.i("Current longitude", longitude.toString())
            getLocationWeatherDetails(latitude,longitude)
        }
    }

    private fun isLocationEnabled(): Boolean {
//        This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )

    }

    private fun showCustomProgressDialog(){
        mProgressDialog=Dialog(this@MainActivity)
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog!!.show()
    }

    private fun hideCustomProgressDialog(){
        if(mProgressDialog!=null){
            mProgressDialog!!.dismiss()
        }
    }
}