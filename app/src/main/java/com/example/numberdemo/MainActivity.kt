package com.example.numberdemo

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.google.gson.Gson
import com.otaliastudios.cameraview.Facing
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class MainActivity : BaseActivty()  {
    private var count:Int = 0
    private var userChoosenTask: String? = null
    private var mImageUri: Uri? = null
    private var textRecognitionModels = ArrayList<TextRecognitionModel>()
    private var mutableImage: Bitmap?=null
    private var mutableImageTest: Bitmap?=null
    private var uri: Uri?=null
    private var fulltext=ArrayList<String>()
    private var name=ArrayList<String>()
    private var dob=ArrayList<String>()
    private var blocktext = ArrayList<Model>()
    private var linetext = ArrayList<Model>()
    private var resultUri: Uri?=null
    private var id=ArrayList<String>()
    private var pd: ProgressDialog?=null

    private var cameraFacing: Facing = Facing.FRONT
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }
    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_scan -> {
                 permissionCheck(0, 0)
            }
        }
    }
    private fun permissionCheck(check: Int, intentfor: Int) {
        when (check) {
            0 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (CommonUtils.requestAllPermissions(this, AppConstants.REQUEST_STORAGE_PERMISSION_CODE)) {
                    selectionIntent(intentfor)
                }
            } else {
                selectionIntent(intentfor)
            }
            1 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (CommonUtils.requestAllPermissions(this, AppConstants.REQUEST_READ_STORAGE_PERMISSION_CODE)) {
                    selectionIntent(intentfor)
                }
            } else {
                selectionIntent(intentfor)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            AppConstants.REQUEST_STORAGE_PERMISSION_CODE ->
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionCheck(0, 0)
                }

            AppConstants.REQUEST_CAMERA_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask == "Take Photo")
                        selectionIntent(0)
                }
            }
            AppConstants.REQUEST_READ_STORAGE_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals(getString(R.string.choose_photo), ignoreCase = true))
                        selectionIntent(1)
                }
            }
        }
    }
    private fun selectionIntent(selected: Int) {
        when (selected) {
            0 -> cameraIntent()

        }
    }

    private fun cameraIntent() {
        try {
            var varues = ContentValues()
            varues.put(MediaStore.Images.Media.TITLE, "New Picture")
            varues.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            mImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, varues)
            var intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri)
            startActivityForResult(intent, AppConstants.REQUEST_CAMERAINTENT)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AppConstants.REQUEST_CAMERAINTENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        CropImage.activity(mImageUri).start(this)
                    } catch (e: Exception) {
                    }
                }
            }

            CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                var result = CropImage.getActivityResult(data)
                if (resultCode == RESULT_OK) {
                    resultUri = result.uri
                    btn_scan.visibility=View.GONE
                    progress_bar.visibility=View.VISIBLE
                    object : AsyncTask<Void, Void, String>()
                    {
                        var uris = resultUri
                        var number = ""
                        var numberstring = ""
                        @SuppressLint("NewApi")

                        override fun doInBackground(vararg params: Void?): String? {

                            var secret_key = "sk_3be70d13192188232ef082f0";
                            // Read image file to byte array
                            var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uris)
                            // Setup the HTTPS connection to api.openalpr.com
                            var stream = ByteArrayOutputStream()
                            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            var byteArray = stream!!.toByteArray()
                            var encoded = Base64.getEncoder().encode(byteArray);
                            var url =
                                URL("https://api.openalpr.com/v2/recognize_bytes?recognize_vehicle=1&country=gcc&secret_key=" + secret_key);
                            var con = url.openConnection();
                            var http: HttpURLConnection = con as HttpURLConnection
                            http.setRequestMethod("POST"); // PUT is another valid option
                            http.setFixedLengthStreamingMode(encoded.size);
                            http.setDoOutput(true);
                            // Send our Base64 content over the stream
                            var json_content = ""
                            var os = http.getOutputStream()
                            os.write(encoded);
                            var status_code = http.getResponseCode();

                            var inds = BufferedReader(
                                InputStreamReader(
                                    http.getInputStream()
                                )
                            );
                            var inputLine = inds.readLine()
                            json_content += inputLine;
                            inds.close();

                            System.out.println(json_content);
                            return json_content
                        }

                        override fun onPreExecute() {
                            super.onPreExecute()
                            // ...
                        }

                        override fun onPostExecute(result: String?) {
                            super.onPostExecute(result)
                            var gson = Gson();
                            var response: Response = gson.fromJson(result, Response::class.java)
                            var vehiclenumber = response.results.get(0).plate
                            var p = Pattern.compile("[A-Z]+|\\d+")
                            var m = p.matcher(vehiclenumber)
                            var allMatches = ArrayList<String>()
                            while (m.find()) {
                                allMatches.add(m.group())
                            }
                            progress_bar.visibility=View.INVISIBLE
                            btn_scan.visibility = View.VISIBLE
                            val intent = Intent(context, DetailActivity::class.java)
                            val args = Bundle()
                            args.putStringArrayList("detail", allMatches)
                            intent.putExtra("BUNDLE", args)
                            intent.putExtra("BUNDLE", args)
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent)


                        }


                    }.execute()
                    // analyzeImage(MediaStore.Images.Media.getBitmap(contentResolver, resultUri))
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    var error = result.error
                }
            }
        }
    }




}
