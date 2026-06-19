package com.garden.flowerid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonParser
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var resultCard: View
    private lateinit var flowerImage: ImageView
    private lateinit var plantNameText: TextView
    private lateinit var scientificNameText: TextView
    private lateinit var confidenceText: TextView
    private lateinit var weedStatusBanner: View
    private lateinit var weedStatusIcon: TextView
    private lateinit var weedStatusText: TextView
    private lateinit var weedReasonText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var learnMoreLink: TextView
    private lateinit var loadingOverlay: View
    private lateinit var closeResultButton: ImageButton
    private lateinit var favoriteButton: ImageButton
    private lateinit var scanAgainButton: Button
    private lateinit var instructionText: TextView

    private var lastCapturedBitmap: Bitmap? = null
    private var currentCommonName: String? = null
    private var currentScientificName: String? = null
    private var currentConfidence: Double = 0.0
    private var currentWeedInfo: WeedDatabase.WeedInfo? = null
    private var currentDescription: String? = null
    private var currentWikipediaUrl: String? = null
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val PERM_REQUEST = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)
        resultCard = findViewById(R.id.resultCard)
        flowerImage = findViewById(R.id.flowerImage)
        plantNameText = findViewById(R.id.plantNameText)
        scientificNameText = findViewById(R.id.scientificNameText)
        confidenceText = findViewById(R.id.confidenceText)
        weedStatusBanner = findViewById(R.id.weedStatusBanner)
        weedStatusIcon = findViewById(R.id.weedStatusIcon)
        weedStatusText = findViewById(R.id.weedStatusText)
        weedReasonText = findViewById(R.id.weedReasonText)
        descriptionText = findViewById(R.id.descriptionText)
        learnMoreLink = findViewById(R.id.learnMoreLink)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        closeResultButton = findViewById(R.id.closeResultButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        scanAgainButton = findViewById(R.id.scanAgainButton)
        instructionText = findViewById(R.id.instructionText)

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.CAMERA), PERM_REQUEST
        )

        captureButton.setOnClickListener { takePhoto() }
        closeResultButton.setOnClickListener { hideResult() }
        scanAgainButton.setOnClickListener { hideResult() }
        favoriteButton.setOnClickListener { toggleFavorite() }
        findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.favoritesListButton).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQUEST && allPermissionsGranted()) startCamera()
        else Toast.makeText(this, R.string.permission_camera_required, Toast.LENGTH_LONG).show()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.camera_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imgCapture = imageCapture ?: return
        val apiKey = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("api_key", "") ?: ""

        if (apiKey.isBlank()) {
            Toast.makeText(this, R.string.set_api_key_first, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        captureButton.isEnabled = false
        imgCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = imageProxyToBitmap(image)
                    image.close()
                    showLoading(true)
                    identifyPlant(bitmap, apiKey)
                }

                override fun onError(e: ImageCaptureException) {
                    captureButton.isEnabled = true
                    Toast.makeText(this@MainActivity, R.string.capture_failed, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val matrix = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    private fun identifyPlant(bitmap: Bitmap, apiKey: String) {
        // Scale down to max 800px on longest side for faster upload
        val maxDim = 800f
        val scale = minOf(1f, maxDim / maxOf(bitmap.width, bitmap.height).toFloat())
        val scaled = if (scale < 1f)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        else bitmap

        lastCapturedBitmap = scaled

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("organs", "auto")
            .addFormDataPart(
                "images", "plant.jpg",
                stream.toByteArray().toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey&lang=en&nb-results=3")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = runOnUiThread {
                showLoading(false)
                captureButton.isEnabled = true
                Toast.makeText(this@MainActivity, getString(R.string.network_error, e.message), Toast.LENGTH_LONG).show()
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                val bodyStr = response.body?.string()
                runOnUiThread {
                    showLoading(false)
                    captureButton.isEnabled = true
                    when {
                        response.isSuccessful && bodyStr != null -> parseAndShow(bodyStr)
                        response.code == 401 -> Toast.makeText(
                            this@MainActivity,
                            R.string.invalid_api_key,
                            Toast.LENGTH_LONG
                        ).show()
                        response.code == 404 -> Toast.makeText(
                            this@MainActivity,
                            R.string.plant_not_recognized,
                            Toast.LENGTH_LONG
                        ).show()
                        else -> {
                            val detail = try {
                                JsonParser.parseString(bodyStr).asJsonObject.get("message")?.asString
                            } catch (e: Exception) { null }
                            val message = getString(R.string.api_error, response.code) +
                                (if (detail != null) ": $detail" else "")
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }

    private fun parseAndShow(json: String) {
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val results = root.getAsJsonArray("results")
            if (results == null || results.size() == 0) {
                Toast.makeText(this, R.string.plant_not_recognized, Toast.LENGTH_LONG).show()
                return
            }
            val top = results[0].asJsonObject
            val species = top.getAsJsonObject("species")
            val scientificName = species.get("scientificNameWithoutAuthor")?.asString ?: "Unknown"
            val commonNames = species.getAsJsonArray("commonNames")
            val commonName = if (commonNames != null && commonNames.size() > 0)
                commonNames[0].asString else scientificName
            val score = (top.get("score")?.asDouble ?: 0.0) * 100

            showResult(commonName, scientificName, score, WeedDatabase.identify(scientificName))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.could_not_parse_response, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResult(
        commonName: String,
        scientificName: String,
        confidence: Double,
        weedInfo: WeedDatabase.WeedInfo?
    ) {
        flowerImage.setImageBitmap(lastCapturedBitmap)
        plantNameText.text = commonName
        scientificNameText.text = scientificName
        confidenceText.text = getString(R.string.confidence_format, confidence)

        currentCommonName = commonName
        currentScientificName = scientificName
        currentConfidence = confidence
        currentWeedInfo = weedInfo
        currentDescription = null
        currentWikipediaUrl = null
        updateFavoriteButtonState()

        if (weedInfo != null) {
            weedStatusBanner.setBackgroundColor(0xFFC62828.toInt())
            weedStatusIcon.text = "☠"
            weedStatusText.text = getString(R.string.weed_status_weed)
            weedReasonText.text = weedInfo.reason
            weedReasonText.visibility = View.VISIBLE
        } else {
            weedStatusBanner.setBackgroundColor(0xFF2E7D32.toInt())
            weedStatusIcon.text = "✓"
            weedStatusText.text = getString(R.string.weed_status_safe)
            weedReasonText.visibility = View.GONE
        }

        resultCard.visibility = View.VISIBLE
        instructionText.visibility = View.GONE

        fetchDescription(commonName, scientificName)
    }

    private fun fetchDescription(commonName: String, scientificName: String) {
        descriptionText.text = getString(R.string.loading_description)
        descriptionText.visibility = View.VISIBLE
        learnMoreLink.visibility = View.GONE

        fetchWikipediaSummary(commonName) { found ->
            if (!found) fetchWikipediaSummary(scientificName) { foundFallback ->
                if (!foundFallback) runOnUiThread { descriptionText.visibility = View.GONE }
            }
        }
    }

    private fun fetchWikipediaSummary(title: String, onDone: (Boolean) -> Unit) {
        val url = "https://en.wikipedia.org/api/rest_v1/page/summary/" +
            Uri.encode(title.replace(" ", "_"))
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GardenWeedID-Android/1.0")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) = onDone(false)

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (!response.isSuccessful) {
                    onDone(false)
                    return
                }
                try {
                    val obj = JsonParser.parseString(response.body?.string()).asJsonObject
                    val extract = obj.get("extract")?.asString
                    val pageUrl = obj.getAsJsonObject("content_urls")
                        ?.getAsJsonObject("desktop")?.get("page")?.asString
                    if (extract.isNullOrBlank()) {
                        onDone(false)
                        return
                    }
                    runOnUiThread {
                        currentDescription = extract
                        currentWikipediaUrl = pageUrl
                        descriptionText.text = extract
                        if (pageUrl != null) {
                            learnMoreLink.visibility = View.VISIBLE
                            learnMoreLink.setOnClickListener {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl)))
                            }
                        }
                    }
                    onDone(true)
                } catch (e: Exception) {
                    onDone(false)
                }
            }
        })
    }

    private fun toggleFavorite() {
        val scientificName = currentScientificName ?: return
        if (FavoritesStore.isFavorite(this, scientificName)) {
            FavoritesStore.remove(this, scientificName)
            Toast.makeText(this, R.string.removed_from_favorites_toast, Toast.LENGTH_SHORT).show()
        } else {
            FavoritesStore.add(
                this,
                commonName = currentCommonName ?: scientificName,
                scientificName = scientificName,
                confidence = currentConfidence,
                isWeed = currentWeedInfo != null,
                weedReason = currentWeedInfo?.reason,
                description = currentDescription,
                wikipediaUrl = currentWikipediaUrl,
                bitmap = lastCapturedBitmap
            )
            Toast.makeText(this, R.string.added_to_favorites_toast, Toast.LENGTH_SHORT).show()
        }
        updateFavoriteButtonState()
    }

    private fun updateFavoriteButtonState() {
        val isFav = currentScientificName?.let { FavoritesStore.isFavorite(this, it) } ?: false
        favoriteButton.setImageResource(
            if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_outline
        )
    }

    private fun hideResult() {
        resultCard.visibility = View.GONE
        instructionText.visibility = View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        client.dispatcher.executorService.shutdown()
    }
}
