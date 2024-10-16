package com.example.gpsaddress

import android.content.Context
import android.content.Intent
import android.widget.Toast

class WhasAppShare(private val context: Context) {
    fun share(text: String) {
        try{
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.setPackage("com.whatsapp")
            intent.putExtra(Intent.EXTRA_TEXT, text)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp não instalado", Toast.LENGTH_SHORT).show()
        }
    }

}