package com.pad.feature.takepictures

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.Handler
import android.os.HandlerThread
import android.view.PixelCopy
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.google.ar.sceneform.ArSceneView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class TakePicturesViewModel(app: Application): AndroidViewModel(app) {

    private val startActivityMutableLiveData = MutableLiveData<Intent>()
    val startActivityLiveData: LiveData<Intent>
        get() = startActivityMutableLiveData

    @Throws(IOException::class)
    private fun saveBitmapToDisk(bitmap: Bitmap, filename: String) {
        val out = File(filename)
        if (!out.parentFile.exists()) {
            out.parentFile.mkdirs()
        }
        try {
            FileOutputStream(filename).use { outputStream ->
                ByteArrayOutputStream().use { outputData ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputData)
                    outputData.writeTo(outputStream)
                    outputStream.flush()
                    outputStream.close()
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            throw IOException("Failed to save bitmap to disk", ex)
        }
    }

    private fun generateFilename(): String {
        val date = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        return getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES).toString() + File.separator + date + "_screenshot.jpg"
    }

    fun takePhoto(view: ArSceneView, snackbar: Snackbar, packageName: String) {
        val filename = generateFilename()

        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888)

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(view, bitmap, { copyResult ->
            if (copyResult == PixelCopy.SUCCESS) {
                try {
                    saveBitmapToDisk(bitmap, filename)
                    snackbar.setAction("Open in Photos") { v ->
                        val photoFile = File(filename)
                        val photoURI: Uri = FileProvider.getUriForFile(this.getApplication(), "$packageName.pad.name.provider", photoFile)
                        val intent = Intent()
                        intent.action = Intent.ACTION_VIEW
                        intent.setDataAndType(photoURI,"image/jpeg")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        startActivityMutableLiveData.value = intent
                    }
                    snackbar.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val toast = Toast.makeText(this.getApplication(), e.toString(),
                            Toast.LENGTH_LONG)
                    toast.show()
                }
            } else {
                val toast = Toast.makeText(this.getApplication(),
                        "Failed to copyPixels: $copyResult", Toast.LENGTH_LONG)
                toast.show()
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }
}