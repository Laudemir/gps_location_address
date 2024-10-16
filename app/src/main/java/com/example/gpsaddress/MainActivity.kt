package com.example.gpsaddress

import android.os.Bundle //ok
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity //ok
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.Manifest //ok
import android.content.Context
import android.content.pm.PackageManager //ok
import android.location.Address
import android.location.Geocoder //ok
import android.location.Location //ok
import android.net.ConnectivityManager
import android.widget.TextView //ok
import android.widget.Toast //ok
import androidx.core.app.ActivityCompat //ok
import androidx.core.content.ContextCompat
import com.google.android.gms.location.* //ok
//import com.google.android.gms.location.Priority
import java.util.* //ok
import android.net.NetworkCapabilities
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.widget.Button


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback // The problem probably here
    private lateinit var locationTextView: TextView
    private lateinit var addressTextView: TextView
    private lateinit var sendButton: Button


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            val locationText = locationTextView.text.toString() + "\n\n" +
                     addressTextView.text

                senByWhatsApp(locationText)
        }

        // Define o LocationCallback para receber atualizações de localização
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocationUI(location)
                }
            }
        }


        // Inicializa o FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Referências aos TextViews para exibir a localização e o endereço
        locationTextView = findViewById(R.id.locationTextView)
        addressTextView = findViewById(R.id.addressTextView)

        // Verificar e solicitar permissões
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    // Verifica se a permissão foi concedida
    private fun checkLocationPermission(): Boolean {
        val fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED
    }

    // Solicita a permissão de localização
    private fun requestLocationPermission() {
        val LOCATION_PERMISSION_REQUEST_CODE = 0
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Inicia as atualizações de localização
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000 // Intervalo de atualização de 3 segundos
        ).setMinUpdateIntervalMillis(5000) // Intervalo mínimo de 5 segundos entre atualizações
            .build()

        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }


    private fun updateLocationUI(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val altitude = String.format("%.0f", location.altitude)
        val accuracy = String.format("%.0f", location.accuracy)
        //val speed = String.format("%.1f", location.speed * 3.6) // Convertendo para km/h
        //val bearing = String.format("%.0f", location.bearing)
        val time = location.time
        val date = Date(time)
        val sdf = android.icu.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = sdf.format(date)
        //val provider = location.provider


        locationTextView.text = """
           Latitude: $latitude
           Longitude: $longitude
           
           Altitude: $altitude m
           Precisão: $accuracy m
           
           Data Hora: $formattedDate
        """.trimIndent()

        if (isNetworkAvailable(this)) {

            val geocoder = Geocoder(this)

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    geocoder.getFromLocation(latitude, longitude, 1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                handleAddresses(addresses)
                            }

                            override fun onError(errorMessage: String?) { Log.e("Geocoder", "Error getting address: $errorMessage")
                                runOnUiThread {
                                    addressTextView.text = "Unable to determine address. Please try again."
                                }
                            }
                        })
                }
                else -> {
                    try {
                        val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                        handleAddresses(addresses)
                    } catch (e: Exception) {
                        Log.e("Geocoder", "Error getting address", e)
                        addressTextView.text = "Unable to determine address. Please try again."
                    }
                }
            }
        } else {
            addressTextView.text = "No internet connection"
        }
    }


    //----------------------------------------------------------------------------------
    private fun senByWhatsApp(text: String){
        val whasAppShare = WhasAppShare(this)
        whasAppShare.share(text)
    }

    private fun handleAddresses(addresses: List<Address>?) {
        addresses?.firstOrNull()?.let { address ->
            val formattedAddress = formatAddress(address)
            runOnUiThread {
                addressTextView.text = formattedAddress
            }
        } ?: run {
            runOnUiThread {
                addressTextView.text = "Endereço não encontrado"
            }
        }
    }

    private fun formatAddress(address: Address): String {

        return """
                ENDEREÇO:           
                ${address.thoroughfare}, ${address.subThoroughfare}
                Cidade: ${address.subAdminArea}
                Bairro: ${address.subLocality}
                País: ${address.countryName}, ${address.countryCode}
                Estado: ${address.adminArea}
                CEP: ${address.postalCode} 
                
                LOCALIZAÇÃO:
                https://www.google.com/maps/search/?api=1&query=${address.latitude},${address.longitude}    
        """.trimIndent()
    }


    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // fim das funoces que foram criadas

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            startLocationUpdates()
        }
    }

    // Lida com o resultado da solicitação de permissão
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}



