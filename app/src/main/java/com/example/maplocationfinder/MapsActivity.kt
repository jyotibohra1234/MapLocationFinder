package com.example.maplocationfinder

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.maplocationfinder.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.PolylineOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.Exception
import kotlin.collections.ArrayList
import com.google.gson.GsonBuilder
import org.jetbrains.anko.onClick

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    var markerPoints: ArrayList<Any> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fetchLocation()
        binding.buttonDirections.onClick {

            // Checks, whether start and end locations are captured
            if (markerPoints.size >= 2) {
                val origin = markerPoints[0] as LatLng
                val dest = markerPoints[1] as LatLng
                // Getting URL to the Google Directions API for routes
                val URL = getDirectionUrl(origin,dest)
                GetDirections(URL).execute()
            }

        }
    }


    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
            return
        }
                val mapFragment = supportFragmentManager
                    .findFragmentById(R.id.map) as SupportMapFragment
                mapFragment.getMapAsync(this)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1000 -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                fetchLocation()
        }
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
        mMap = googleMap

        mMap.isMyLocationEnabled()
        val location1 = LatLng(19.1854, 72.8585)
        mMap.addMarker(MarkerOptions().position(location1).title("I am here!!").draggable(true))
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location1,5f))

        mMap.setOnMapClickListener(object: GoogleMap.OnMapClickListener{

            override fun onMapClick(latLng: LatLng) {
                if (markerPoints.size > 3) {
                    markerPoints.clear()
                    mMap.clear()
                }

                // Adding new item to the ArrayList
                markerPoints.add(latLng)

                // Creating MarkerOptions
                val options = MarkerOptions().snippet(latLng.latitude.toString() + ":" + latLng.longitude.toString())
                // Setting the position of the marker
                options.position(latLng).draggable(true)

                if (markerPoints.size == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("A")
                } else if (markerPoints.size == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("B")
                }else if (markerPoints.size == 3) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("C")
                }else if (markerPoints.size == 4) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)).title("D")
                }

                // Add new marker to the Google Map Android API V2
                sequenceOf(mMap.addMarker(options))
            }
        })

    }

    private fun getDirectionUrl(from : LatLng, to : LatLng) : String{
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val mode = "mode=driving"
        val key = "AIzaSyDi0lxukma-AbNbSLoS9CiLNUtlTVwXzzA"
        // Building the parameters to the web service
        val parameters: String = origin.toString() + "&" + dest + "&" + sensor + "&" + mode + "&" +key
        // Output format
        val output = "json"
        // Building the url to the web service
        val url = "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
        return url
    }

    private inner class GetDirections(val urlForDirection : String): AsyncTask<Void, Void, List<List<LatLng>>>(){

        override fun doInBackground(vararg p0: Void?): List<List<LatLng>> {
            val gson = GsonBuilder().create()
            val client = OkHttpClient()
            val request = Request.Builder().url(urlForDirection).build()
            val response = client.newCall(request).execute()
            val data = response.body?.toString()
            val result = ArrayList<List<LatLng>>()
            try {

                val respObj = gson.fromJson(data,MapData::class.java)
                val path = ArrayList<LatLng>()
                for (i in 0..(respObj.routes[0].legs[0].steps.size)){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
            result.add(path)
            }
            catch (e : Exception){
              e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>?) {
            val lineoption = PolylineOptions()
            for (i in result!!.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.BLUE)
                lineoption.geodesic(true)
            }
            mMap.addPolyline(lineoption)
        }

    }

    fun decodePolyline(encoded: String): Collection<LatLng> {
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
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
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
}