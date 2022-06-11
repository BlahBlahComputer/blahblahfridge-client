package sogang.capstone.blahblahfridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Log.INFO
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DefaultActivity : AppCompatActivity() {
    val REQ_SELECT_IMAGE = 2001
    var filePathCallback: ValueCallback<Array<Uri>>? = null
    var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.default_layout)

        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if(cameraPermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        val storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if(storagePermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }

        val writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if(writeStoragePermission == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
        }

        val myWebView: WebView = findViewById(R.id.webview)
        myWebView.webChromeClient = object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
               var isCapture = fileChooserParams?.isCaptureEnabled ?: false
                selectImage(filePathCallback)
                return true
            }
        }
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
        }
        myWebView.webViewClient = WebViewClient()
        myWebView.loadUrl("https://www.blahblahfridge.site")
    }

    fun selectImage(filePathCallback: ValueCallback<Array<Uri>>?) {
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        var state = Environment.getExternalStorageState()
        if(!TextUtils.equals(state, Environment.MEDIA_MOUNTED)) return

        var cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.resolveActivity(packageManager)?.let {
            var photoFile: File? = createImageFile()
            Log.d("test", "Test")
            photoFile?.also {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val strpa = applicationContext.packageName
                    cameraImageUri = FileProvider.getUriForFile(this, strpa + ".provider", it)
                }
                else {
                    cameraImageUri = Uri.fromFile(it)
                }
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
            }
        }

        var intent = Intent(Intent.ACTION_PICK).apply {
            type = MediaStore.Images.Media.CONTENT_TYPE
            data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        Intent.createChooser(intent, "사진 가져올 방법을 선택하세요").run {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
            startActivityForResult(this, REQ_SELECT_IMAGE)
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = filesDir
        return File(
            storageDir,
            "${timeStamp}.jpeg",
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQ_SELECT_IMAGE -> {
                if(resultCode == Activity.RESULT_OK) {
                    filePathCallback?.let {
                        var imageData = data

                        if(imageData == null) {
                            imageData = Intent()
                            imageData.data = cameraImageUri
                        }

                        if(imageData?.data == null) {
                            imageData?.data = cameraImageUri
                        }

                        it.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, imageData))
                        filePathCallback = null
                    }
                } else {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
