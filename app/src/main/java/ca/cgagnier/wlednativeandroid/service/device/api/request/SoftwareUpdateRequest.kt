package ca.cgagnier.wlednativeandroid.service.device.api.request

import ca.cgagnier.wlednativeandroid.model.Device
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

class SoftwareUpdateRequest(
    device: Device,
    val binaryFile: File,
    val callback: (suspend (Response<ResponseBody>) -> Unit)? = null
) : Request(device)