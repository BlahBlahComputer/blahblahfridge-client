package sogang.capstone.blahblahfridge

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Log.INFO
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class DefaultActivity : AppCompatActivity() {
    val FILECHOOSER_NORMAL_REQ_CODE = 2001
    val FILECHOOSER_LOLLIPOP_REQ_CODE = 2002

    var filePathCallbackNormal: ValueCallback<Uri>? = null
    var filePathCallbackLollipop: ValueCallback<Array<Uri>>? = null
    var cameraImageUri: Uri? = null

    lateinit var myWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.default_layout)

        checkVerify()

        myWebView = findViewById(R.id.webview)
        myWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(true)
        }
        myWebView.webChromeClient = object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if(filePathCallbackLollipop != null) {
                    filePathCallbackLollipop!!.onReceiveValue(null)
                    filePathCallbackLollipop = null
                }
                filePathCallbackLollipop = filePathCallback

                val isCapture = fileChooserParams!!.isCaptureEnabled
                runCamera(isCapture)

                return true
            }

            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                Log.d("MyApplication", "$message -- From line $lineNumber of $sourceID")
            }
        }
        myWebView.webViewClient = WebViewClient()
        myWebView.loadUrl("https://www.blahblahfridge.site")
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkVerify() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(applicationContext, "권한을 허용해주세요", Toast.LENGTH_SHORT).show()
            }
            else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                    ), 1
                )
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            FILECHOOSER_NORMAL_REQ_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (filePathCallbackNormal == null) return

                    val result = data?.data

                    filePathCallbackNormal!!.onReceiveValue(result)
                    filePathCallbackNormal = null
                }
            }
            FILECHOOSER_LOLLIPOP_REQ_CODE -> {
                if(resultCode == Activity.RESULT_OK) {
                    var imageData = data
                    if(filePathCallbackLollipop == null) return

                    if(imageData == null) imageData = Intent()
                    if(imageData.data == null) imageData.data = cameraImageUri

                    filePathCallbackLollipop!!.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, imageData))
                    filePathCallbackLollipop = null
                }
                else {
                    if(filePathCallbackLollipop != null) {
                        filePathCallbackLollipop!!.onReceiveValue(null)
                        filePathCallbackLollipop = null
                    }

                    if(filePathCallbackNormal != null) {
                        filePathCallbackNormal!!.onReceiveValue(null)
                        filePathCallbackNormal = null
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun runCamera(isCapture: Boolean) {
        val intentCamera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val path = Environment.getExternalStorageDirectory()
        val file = File(path, "upload.jpeg")
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val strpa = applicationContext.packageName
            cameraImageUri = FileProvider.getUriForFile(this, strpa + ".fileprovider", file)
        }
        else {
            cameraImageUri = Uri.fromFile(file)
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

        if(!isCapture) {
            val pickIntent = Intent(Intent.ACTION_PICK)
            pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE)
            pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

            val chooserIntent = Intent.createChooser(pickIntent, "사진 가져올 방법을 선택하세요")

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(intentCamera))
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE)
        }
        else {
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
