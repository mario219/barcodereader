package com.example.barcodereader

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val scan = findViewById<Button>(R.id.button)
        scan.setOnClickListener {
            checkPermission(permission.READ_EXTERNAL_STORAGE, PICK_IMAGE_REQUEST_CODE) {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                startActivityForResult(intent, 101)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.apply {
                val imageBitmap = getImageBitmapFromUri(this) ?: return@apply
                val inputImage = InputImage.fromBitmap(imageBitmap, 0)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            checkResult(barcodes)
                        }
                        .addOnFailureListener {
                            Toast.makeText(applicationContext, "No QR Code found", Toast.LENGTH_SHORT).show()
                        }
            }
        }
    }

    private fun checkResult(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            // See API reference for complete list of supported types
            when (barcode.valueType) {
                Barcode.TYPE_WIFI -> {
                    val ssid = barcode.wifi!!.ssid
                    val password = barcode.wifi!!.password
                    val type = barcode.wifi!!.encryptionType
                }
                Barcode.TYPE_URL -> {
                    val title = barcode.url!!.title
                    val url = barcode.url!!.url
                    showResult(url)
                }
            }
        }
    }

    private fun showResult(url: String) {
        val result = findViewById<TextView>(R.id.result)
        result.visibility = View.VISIBLE
        result.text = "Result: $url"
        Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermission(permission: String, requestCode: Int, performIntent: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            performIntent()
        }
    }

    private fun getImageBitmapFromUri(imageUri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(contentResolver, imageUri)
            ) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST_CODE = 101
    }
}