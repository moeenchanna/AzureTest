package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.azure.sdk.iot.device.DeviceClient
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol
import com.microsoft.azure.sdk.iot.device.Message
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord


class MainActivity : AppCompatActivity() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                processCSV()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun processCSV() {
        try {
            // Read the CSV file from the "raw" directory
            val csvResourceId = R.raw.device_environment_camera
            val inputStream = resources.openRawResource(csvResourceId)
            val reader: Reader = BufferedReader(InputStreamReader(inputStream))

            val csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)

            var device: Map<String, String>? = null

            for (record: CSVRecord in csvParser) {
                device = record.toMap()
            }

            val iotHubDeviceSecurityType = device?.get("IOTHUB_DEVICE_SECURITY_TYPE")
            val iotHubDeviceDpsIdScope = device?.get("IOTHUB_DEVICE_DPS_ID_SCOPE")
            val iotHubDeviceDpsDeviceKey = device?.get("IOTHUB_DEVICE_DPS_DEVICE_KEY")
            val iotHubDeviceDpsDeviceId = device?.get("IOTHUB_DEVICE_DPS_DEVICE_ID")
            val iotHubDeviceDpsEndpoint = device?.get("IOTHUB_DEVICE_DPS_ENDPOINT")
            val modelId = device?.get("model_id")

            val switch = iotHubDeviceSecurityType

            if (switch == "DPS") {
                val provisioningHost = iotHubDeviceDpsEndpoint ?: "global.azure-devices-provisioning.net"
                val idScope = iotHubDeviceDpsIdScope
                val registrationId = iotHubDeviceDpsDeviceId
                val symmetricKey = iotHubDeviceDpsDeviceKey

                val registrationResult = provisionDevice(provisioningHost, idScope, registrationId, symmetricKey, modelId)

                if (registrationResult.status == "assigned") {
                    log("Device was assigned")
                    log(registrationResult.registrationState.assignedHub)
                    log(registrationResult.registrationState.deviceId)
                    showToast("Device was assigned")
                    showToast(registrationResult.registrationState.assignedHub)
                    showToast(registrationResult.registrationState.deviceId)

                    val config = DeviceClientConfig.createDeviceClientConfigFromSymmetricKey(
                        registrationResult.registrationState.assignedHub, registrationId, symmetricKey, IotHubClientProtocol.MQTT
                    )
                    config.productInfo = modelId

                    val deviceClient = DeviceClient.createFromDeviceClientConfig(config)

                    GlobalScope.launch(Dispatchers.IO) {
                        deviceClient.open()
                        sendTelemetryFromNano(deviceClient)
                        deviceClient.closeNow()
                    }
                } else {
                    throw RuntimeException("Could not provision device. Aborting Plug and Play device connection.")
                }
            } else if (switch == "connectionString") {
                val connStr = System.getenv("IOTHUB_DEVICE_CONNECTION_STRING")
                log("Connecting using Connection String $connStr")
                showToast("Connecting using Connection String $connStr")

                val config = DeviceClientConfig.createFromConnectionString(connStr)
                config.productInfo = modelId

                val deviceClient = DeviceClient.createFromDeviceClientConfig(config)

                GlobalScope.launch(Dispatchers.IO) {
                    deviceClient.open()
                    sendTelemetryFromNano(deviceClient)
                    deviceClient.closeNow()
                }
            } else {
                throw RuntimeException("At least one choice needs to be made for complete functioning of this sample.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Show an error toast message
            showToast("Error processing CSV: ${e.message}")
            log("Error processing CSV: ${e.message}")
        }
    }



    private fun log(message: String) {
        // Log to the Android system log (Logcat)
        android.util.Log.d("MainActivity", message)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            // Show a toast message on the UI thread
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun provisionDevice(provisioningHost: String, idScope: String?, registrationId: String?, symmetricKey: String?, modelId: String?): RegistrationResult {
        val provisioningDeviceClient = ProvisioningDeviceClient.createFromSymmetricKey(provisioningHost, idScope, registrationId, symmetricKey)
        provisioningDeviceClient.provisioningPayload = ProvisioningPayload("modelId" to modelId)

        return provisioningDeviceClient.register()
    }

     private fun sendTelemetryFromNano(deviceClient: DeviceClient) {
        val telemetry = """{ "battery": 100 }"""
        val message = Message(telemetry)
        message.contentEncoding = "utf-8"
        message.contentType = "application/json"
         log("Sent message")
         showToast("Sent message")
        deviceClient.sendEventAsync(message, null, null)
         showToast("Telemetries have been sent to IoT Central")
        println("Telemetries have been sent to IoT Central")
    }

}

