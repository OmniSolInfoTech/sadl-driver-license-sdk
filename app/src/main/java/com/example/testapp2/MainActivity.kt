package com.example.testapp2
import android.view.View
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import io.verifyid.sadl.Sadl
import io.verifyid.sadl.licensing.Fingerprint
import io.verifyid.sadl.licensing.LicenseManager
import io.verifyid.sadl.licensing.SadlActivator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var statusChip: TextView
    private lateinit var baseUrl: EditText
    private lateinit var apiKey: EditText
    private lateinit var product: EditText
    private lateinit var years: EditText

    private lateinit var btnInit: Button
    private lateinit var btnCheckStatus: Button
    private lateinit var btnActivate: Button
    private lateinit var edtOfflineToken: EditText
    private lateinit var btnImportToken: Button
    private lateinit var btnClearToken: Button

    private lateinit var pdf417: EditText
    private lateinit var btnDecode: Button
    private lateinit var btnPayload: Button
    private lateinit var outputFields: TextView
    private lateinit var photoView: ImageView

    private lateinit var fingerprintTxt: TextView
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind views
        statusChip = findViewById(R.id.statusChip)
        baseUrl = findViewById(R.id.inputBaseUrl)
        apiKey = findViewById(R.id.inputApiKey)
        product = findViewById(R.id.inputProduct)
        years = findViewById(R.id.inputYears)

        btnInit = findViewById(R.id.btnInit)
        btnCheckStatus = findViewById(R.id.btnCheckStatus)
        btnActivate = findViewById(R.id.btnActivate)
        edtOfflineToken = findViewById(R.id.inputOfflineToken)
        btnImportToken = findViewById(R.id.btnImportToken)
        btnClearToken = findViewById(R.id.btnClearToken)

        pdf417 = findViewById(R.id.inputPdf417)
        btnDecode = findViewById(R.id.btnDecode)
        btnPayload = findViewById(R.id.btnPayload)
        outputFields = findViewById(R.id.outputFields)
        photoView = findViewById(R.id.photoPreview)

        fingerprintTxt = findViewById(R.id.fingerprintValue)
        logView = findViewById(R.id.logView)
        logView.movementMethod = ScrollingMovementMethod()

        // Defaults
        baseUrl.setText("http://www.omnicheck.co.za")
        product.setText("SADL-PRO")
        years.setText("1")
        fingerprintTxt.text = Fingerprint.get(applicationContext)

        btnInit.setOnClickListener {
            try {
                Sadl.init(applicationContext)
                appendLog("INIT ok")
                toast("SDK initialized")
            } catch (t: Throwable) {
                appendLog("INIT failed: ${t.message}")
                toast("Init failed: ${t.message}")
            }
        }

        btnCheckStatus.setOnClickListener { updateStatus() }

        btnActivate.setOnClickListener {
            val url = baseUrl.text.toString().trim()
            val key = apiKey.text.toString()
            val prod = product.text.toString().ifBlank { "SADL-PRO" }
            val yrs = years.text.toString().toIntOrNull() ?: 1

            lifecycleScope.launch {
                setBusy(true)
                val res = withContext(Dispatchers.IO) {
                    SadlActivator.activateOnlineAsync(
                        ctx = applicationContext,
                        baseUrl = url,
                        apiKey = key,
                        product = prod,
                        years = yrs
                    )
                }
                when (res) {
                    is SadlActivator.ActivationResult.Success -> {
                        appendLog("ACTIVATE → Success")
                        toast("Activation success")
                    }
                    is SadlActivator.ActivationResult.TokenRejected -> {
                        appendLog("ACTIVATE → Rejected: ${res.status}")
                        toast("Rejected: ${res.status}")
                    }
                    is SadlActivator.ActivationResult.HttpError -> {
                        appendLog("ACTIVATE → HTTP ${res.code}: ${res.body ?: "(no body)"}")
                        toast("HTTP ${res.code}")
                    }
                    is SadlActivator.ActivationResult.NetworkError -> {
                        appendLog("ACTIVATE → Network error: ${res.message}")
                        toast("Network error")
                    }
                    is SadlActivator.ActivationResult.InvalidResponse -> {
                        appendLog("ACTIVATE → Invalid response: ${res.message}")
                        toast("Invalid response")
                    }
                }
                updateStatus()
                setBusy(false)
            }
        }

        btnImportToken.setOnClickListener {
            val token = edtOfflineToken.text.toString().trim()
            if (token.isEmpty()) {
                toast("Paste a token")
                return@setOnClickListener
            }
            val res = SadlActivator.importOfflineToken(applicationContext, token)
            when (res) {
                is SadlActivator.ActivationResult.Success -> {
                    appendLog("IMPORT → Success")
                    toast("Token imported")
                }
                is SadlActivator.ActivationResult.TokenRejected -> {
                    appendLog("IMPORT → Rejected: ${res.status}")
                    toast("Rejected: ${res.status}")
                }
                else -> appendLog("IMPORT → Unexpected: $res")
            }
            updateStatus()
        }

        btnClearToken.setOnClickListener {
            LicenseManager.clearToken(applicationContext)
            appendLog("CLEAR → OK")
            updateStatus()
        }

        btnDecode.setOnClickListener {
            val b64 = pdf417.text.toString().trim()
            if (b64.isEmpty()) {
                toast("Paste PDF417 base64")
                return@setOnClickListener
            }
            try {
                val dl = Sadl.decodeDriversLicenseBase64(b64, includePhoto = true)
                // Used to decode bytes straight from the scanner.
                //val dl2 = Sadl.decodeDriversLicense(b64, includePhoto = true)
                val info = buildString {
                    appendLine("Name: ${dl.firstNames} ${dl.surname}")
                    appendLine("ID: ${dl.idNumber} (${dl.gender})")
                    appendLine("Birth: ${dl.birthDate}")
                    appendLine("Licence #: ${dl.licenseNumber}")
                    appendLine("Issue#: ${dl.licenseIssueNumber}")
                    appendLine("Valid: ${dl.validFrom} → ${dl.validTo}")
                    appendLine("Vehicle codes: ${dl.vehicleCodes.joinToString(",")}")
                    appendLine("Restrictions: ${dl.driverRestrictions}")
                    appendLine("PRDP: ${dl.prdpCode} exp ${dl.prdpExpiry}")
                    appendLine("Photo: ${if (dl.photoJpeg?.isNotEmpty() == true) "${dl.photoJpeg!!.size} bytes" else "none"}")
                }

                outputFields.text = info
                findViewById<View>(R.id.resultRow).isVisible = true

                val jpg = dl.photoJpeg
                if (jpg != null && jpg.isNotEmpty()) {
                    val bmp = BitmapFactory.decodeByteArray(jpg, 0, jpg.size)
                    photoView.setImageBitmap(bmp)
                    photoView.isVisible = true
                    appendLog("DECODE → OK (ID=${dl.idNumber}, photo=${jpg.size} bytes)")
                } else {
                    photoView.setImageDrawable(null)
                    photoView.isVisible = false
                    appendLog("DECODE → OK (ID=${dl.idNumber}, no photo)")
                }

            } catch (t: Throwable) {
                outputFields.text = ""
                photoView.setImageDrawable(null)
                photoView.isVisible = false
                findViewById<View>(R.id.resultRow).isVisible = false
                appendLog("DECODE → FAIL: ${t.message}")
                toast("Decode failed: ${t.message}")
            }
        }

        btnPayload.setOnClickListener {
            val b64 = pdf417.text.toString().trim()
            if (b64.isEmpty()) {
                toast("Paste PDF417 base64")
                return@setOnClickListener
            }
            try {
                val bytes = Sadl.decryptedPayloadBase64(b64)
                appendLog("PAYLOAD → ${bytes.size} bytes")
                toast("Payload ${bytes.size} bytes")
            } catch (t: Throwable) {
                appendLog("PAYLOAD → FAIL: ${t.message}")
                toast("Payload failed: ${t.message}")
            }
        }

        updateStatus()
    }

    private fun updateStatus() {
        val status = LicenseManager.assertValidOrStatus(applicationContext)
        statusChip.text = "Activation: $status"
        appendLog("STATUS = $status")
    }

    private fun setBusy(busy: Boolean) {
        btnActivate.isEnabled = !busy
        btnImportToken.isEnabled = !busy
        btnClearToken.isEnabled = !busy
        btnDecode.isEnabled = !busy
        btnPayload.isEnabled = !busy
    }

    private fun appendLog(line: String) {
        val now = System.currentTimeMillis()
        val ts = android.text.format.DateFormat.format("HH:mm:ss.SSS", now)
        val newText = "[$ts] $line\n" + logView.text
        logView.text = newText
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
