//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.runBlocking
//import com.microsoft.azure.sdk.iot.device.DeviceClientConfig
//import com.microsoft.azure.sdk.iot.device.DeviceClient
//import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol
//import com.microsoft.azure.sdk.iot.device.Message
//
//fun main() {
//    runBlocking {
//        val envVarPath = "/Users/adil-eddress/Downloads/adil/DeviceEnvironment_Camera.csv"
//        val csvreader = csv.DictReader(FileReader(envVarPath))
//        var device: MutableMap<String, String>? = null
//
//        for (row in csvreader) {
//            device = row
//        }
//
//        val iotHubDeviceSecurityType = device?.get("IOTHUB_DEVICE_SECURITY_TYPE")
//        val iotHubDeviceDpsIdScope = device?.get("IOTHUB_DEVICE_DPS_ID_SCOPE")
//        val iotHubDeviceDpsDeviceKey = device?.get("IOTHUB_DEVICE_DPS_DEVICE_KEY")
//        val iotHubDeviceDpsDeviceId = device?.get("IOTHUB_DEVICE_DPS_DEVICE_ID")
//        val iotHubDeviceDpsEndpoint = device?.get("IOTHUB_DEVICE_DPS_ENDPOINT")
//        val modelId = device?.get("model_id")
//
//        val switch = iotHubDeviceSecurityType
//
//        if (switch == "DPS") {
//            val provisioningHost = iotHubDeviceDpsEndpoint ?: "global.azure-devices-provisioning.net"
//            val idScope = iotHubDeviceDpsIdScope
//            val registrationId = iotHubDeviceDpsDeviceId
//            val symmetricKey = iotHubDeviceDpsDeviceKey
//
//            val registrationResult = provisionDevice(provisioningHost, idScope, registrationId, symmetricKey, modelId)
//
//            if (registrationResult.status == "assigned") {
//                println("Device was assigned")
//                println(registrationResult.registrationState.assignedHub)
//                println(registrationResult.registrationState.deviceId)
//
//                val config = DeviceClientConfig.createDeviceClientConfigFromSymmetricKey(
//                    registrationResult.registrationState.assignedHub, registrationId, symmetricKey, IotHubClientProtocol.MQTT
//                )
//                config.productInfo = modelId
//
//                val deviceClient = DeviceClient.createFromDeviceClientConfig(config)
//
//                GlobalScope.launch(Dispatchers.IO) {
//                    deviceClient.open()
//                    sendTelemetryFromNano(deviceClient)
//                    deviceClient.closeNow()
//                }
//            } else {
//                throw RuntimeException("Could not provision device. Aborting Plug and Play device connection.")
//            }
//        } else if (switch == "connectionString") {
//            val connStr = System.getenv("IOTHUB_DEVICE_CONNECTION_STRING")
//            println("Connecting using Connection String $connStr")
//
//            val config = DeviceClientConfig.createFromConnectionString(connStr)
//            config.productInfo = modelId
//
//            val deviceClient = DeviceClient.createFromDeviceClientConfig(config)
//
//            GlobalScope.launch(Dispatchers.IO) {
//                deviceClient.open()
//                sendTelemetryFromNano(deviceClient)
//                deviceClient.closeNow()
//            }
//        } else {
//            throw RuntimeException("At least one choice needs to be made for complete functioning of this sample.")
//        }
//    }
//}
//
//fun provisionDevice(provisioningHost: String, idScope: String?, registrationId: String?, symmetricKey: String?, modelId: String?): RegistrationResult {
//    val provisioningDeviceClient = ProvisioningDeviceClient.createFromSymmetricKey(provisioningHost, idScope, registrationId, symmetricKey)
//    provisioningDeviceClient.provisioningPayload = ProvisioningPayload("modelId" to modelId)
//
//    return provisioningDeviceClient.register()
//}
//
//suspend fun sendTelemetryFromNano(deviceClient: DeviceClient) {
//    val telemetry = """{ "battery": 100 }"""
//    val message = Message(telemetry)
//    message.contentEncoding = "utf-8"
//    message.contentType = "application/json"
//    println("Sent message")
//    deviceClient.sendEventAsync(message, null, null)
//    println("Telemetries have been sent to IoT Central")
//}
