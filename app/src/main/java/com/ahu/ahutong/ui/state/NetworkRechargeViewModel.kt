package com.ahu.ahutong.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahu.ahutong.data.AHUResponse
import com.ahu.ahutong.data.crawler.PayState
import com.ahu.ahutong.data.crawler.api.ycard.YcardApi
import com.ahu.ahutong.data.crawler.manager.TokenManager
import com.ahu.ahutong.data.crawler.utils.generateNonce
import com.ahu.ahutong.data.crawler.utils.getTimestamp
import com.ahu.ahutong.data.crawler.utils.sha256
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import retrofit2.Response

private const val NETWORK_FEE_ITEM_ID = "431"
private const val NETWORK_FEE_ENTRY_APP_ID = "75"
private const val YCARD_ORIGIN = "https://ycard.ahu.edu.cn"
private const val NETWORK_ENTRY_REFERER = "https://ycard.ahu.edu.cn/plat/dating?index=1"

private val networkEntryClient by lazy {
    YcardApi.okHttpClient.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
}

data class NetworkFeeItemPageResponse(
    val msg: String?,
    val code: Int,
    val view: String?,
    val feeitem: NetworkFeeItem?
)

data class NetworkFeeItem(
    val name: String?,
    val layout: String?,
    val maxmoney: String?,
    val daymaxmoney: String?,
    val billing_unit: String?
)

data class NetworkFeeInfoResponse(
    val msg: String?,
    val code: Int,
    val map: NetworkFeeInfoMap?
)

data class NetworkFeeInfoMap(
    val showData: Map<String, String>?,
    val data: NetworkThirdPartyData?
)

data class NetworkThirdPartyData(
    val state_time: String?,
    val state_memo: String?,
    val balance: String?,
    val use_time: String?,
    val tsmAbstract: String?,
    val use_money: String?,
    val use_flow: String?,
    val account: String?,
    val user_state: String?,
    val start_date: String?
)

private data class NetworkOrderResponse(
    val code: Int,
    val success: Boolean,
    val data: NetworkOrderData?,
    val msg: String?
)

private data class NetworkAccountPayInfoResponse(
    val code: Int,
    val success: Boolean,
    val data: NetworkAccountPayInfoData?,
    val msg: String?
)

private data class NetworkFinalPayResponse(
    val code: Int,
    val success: Boolean,
    val data: String?,
    val msg: String?
)

private data class NetworkOrderData(
    val orderid: String
)

private data class NetworkAccountPayInfoData(
    val passwordMap: Map<String, String>?
)

private data class ThirdDataResponse(
    val msg: String?,
    val code: Int
)

data class NetworkRechargeUiData(
    val feeName: String,
    val account: String,
    val stats: List<Pair<String, String>>,
    val quickAmounts: List<String>,
    val maxAmount: String?,
    val thirdPartyData: NetworkThirdPartyData
)

sealed class NetworkRechargePageState {
    object Loading : NetworkRechargePageState()
    data class Ready(val data: NetworkRechargeUiData) : NetworkRechargePageState()
    data class Error(val message: String) : NetworkRechargePageState()
}

class NetworkRechargeViewModel : ViewModel() {

    private val _pageState = MutableStateFlow<NetworkRechargePageState>(NetworkRechargePageState.Loading)
    val pageState: StateFlow<NetworkRechargePageState> = _pageState.asStateFlow()

    private val _payState = MutableStateFlow<PayState>(PayState.Idle)
    val payState: StateFlow<PayState> = _payState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _pageState.value = NetworkRechargePageState.Loading
            _payState.value = PayState.Idle
            val token = withContext(Dispatchers.IO) { TokenManager.awaitToken() }
            if (token.isNullOrBlank()) {
                _pageState.value = NetworkRechargePageState.Error("校园卡登录凭证暂未就绪，请稍后重试")
                return@launch
            }

            runCatching {
                warmUpNetworkRechargeEntry(token)
                fetchSelectState()
                val feeItem = fetchFeeItem()
                val feeItemData = feeItem.data
                if (feeItem.code != 0 || feeItemData == null) {
                    throw IllegalStateException(feeItem.msg ?: "网费充值配置加载失败")
                }
                val info = fetchNetworkInfo()
                val infoData = info.data
                if (infoData == null) {
                    throw IllegalStateException(info.msg ?: "未获取到网费账户信息")
                }
                val quickAmounts = feeItemData.layout
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                val showData = infoData.showData
                val thirdPartyData = infoData.thirdPartyData
                if (thirdPartyData == null) {
                    throw IllegalStateException("网费账户数据不完整")
                }
                val account = thirdPartyData.account.orEmpty()
                val priorityKeys = setOf("用户状态", "储值余额", "本期已使用费用", "本期已使用时长", "本期已使用流量")
                val orderedStats = mutableListOf<Pair<String, String>>().apply {
                    showData["用户状态"]?.let { add("用户状态" to it) }
                    showData["储值余额"]?.let { add("储值余额" to it) }
                    showData["本期已使用费用"]?.let { add("本期已使用费用" to it) }
                    showData["本期已使用时长"]?.let { add("本期已使用时长" to it) }
                    showData["本期已使用流量"]?.let { add("本期已使用流量" to it) }
                    addAll(
                        showData.entries
                            .filterNot { it.key in priorityKeys }
                            .map { it.key to it.value }
                    )
                }

                _pageState.value = NetworkRechargePageState.Ready(
                    NetworkRechargeUiData(
                        feeName = feeItemData.name ?: "网费充值",
                        account = account,
                        stats = orderedStats,
                        quickAmounts = quickAmounts,
                        maxAmount = feeItemData.maxmoney ?: feeItemData.daymaxmoney,
                        thirdPartyData = thirdPartyData
                    )
                )
            }.onFailure { throwable ->
                Log.e("NetworkRechargeViewModel", "Failed to load network recharge info", throwable)
                _pageState.value = NetworkRechargePageState.Error(
                    throwable.message ?: "网费充值信息加载失败"
                )
            }
        }
    }

    fun pay(amount: String, password: String) {
        if (amount.toDoubleOrNull() ?: 0.0 <= 0) {
            _payState.value = PayState.Failed("请输入有效金额")
            return
        }
        if (password.length != 6 || !password.all { it.isDigit() }) {
            _payState.value = PayState.Failed("请输入6位校园卡密码")
            return
        }
        val page = (_pageState.value as? NetworkRechargePageState.Ready)?.data
        if (page == null) {
            _payState.value = PayState.Failed("网费账户信息尚未加载完成")
            return
        }

        _payState.value = PayState.InProgress
        viewModelScope.launch {
            try {
                val orderResult = getPaymentOrder(amount, page.thirdPartyData)
                if (orderResult.code != 0 || orderResult.data == null) {
                    _payState.value = PayState.Failed(orderResult.msg ?: "创建订单失败")
                    return@launch
                }
                val orderId = orderResult.data.orderid

                val accountPayInfoResult = getAccountPayInfo(orderId)
                val passwordMap = accountPayInfoResult.data?.passwordMap
                if (accountPayInfoResult.code != 0 || passwordMap.isNullOrEmpty()) {
                    _payState.value = PayState.Failed(accountPayInfoResult.msg ?: "获取支付信息失败")
                    return@launch
                }

                val (uuid, mapString) = passwordMap.entries.first()
                val keymap = buildPasswordCipherMap(mapString)
                val cipherText = password.map { ch ->
                    keymap[ch] ?: error("无效的校园卡密码映射")
                }.joinToString("")

                val finalResponse = executeFinalPay(
                    orderId = orderId,
                    password = cipherText,
                    uuid = uuid
                )

                if (finalResponse.code == 0) {
                    _payState.value = PayState.Succeeded(orderId)
                } else {
                    _payState.value = PayState.Failed(finalResponse.msg ?: "网费充值失败")
                }
            } catch (t: Throwable) {
                Log.e("NetworkRechargeViewModel", "Failed to pay network recharge", t)
                _payState.value = PayState.Failed(t.message ?: "网费充值失败，请稍后重试")
            }
        }
    }

    fun resetPayState() {
        _payState.value = PayState.Idle
    }

    private suspend fun warmUpNetworkRechargeEntry(token: String) {
        withContext(Dispatchers.IO) {
            val url = "$YCARD_ORIGIN/charge/feeitem/toAppitem".toHttpUrl()
                .newBuilder()
                .addQueryParameter("feeitemid", NETWORK_FEE_ITEM_ID)
                .addQueryParameter("appId", NETWORK_FEE_ENTRY_APP_ID)
                .addQueryParameter("loginFrom", "h5")
                .addQueryParameter("synAccessSource", "h5")
                .addQueryParameter("synjones-auth", token)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Referer", NETWORK_ENTRY_REFERER)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36")
                .get()
                .build()
            networkEntryClient.newCall(request).execute().use { response ->
                val accepted = response.code in 300..399 || response.isSuccessful
                if (!accepted) {
                    throw IllegalStateException("网费入口预热失败: ${response.code}")
                }
            }
        }
    }

    private suspend fun fetchSelectState() {
        val responseWrapper = AHUResponse<Unit>()
        val formBody = FormBody.Builder()
            .add("feeitemid", NETWORK_FEE_ITEM_ID)
            .add("type", "select")
            .add("level", "0")
            .build()
        val result = executeThirdData(formBody, responseWrapper, "网费充值入口预热失败")
        if (result.code != 0) {
            throw IllegalStateException(result.msg ?: "网费充值入口预热失败")
        }
    }

    private suspend fun fetchFeeItem(): AHUResponse<NetworkFeeItem> {
        val responseWrapper = AHUResponse<NetworkFeeItem>()
        val response = YcardApi.API.getSingleFeeItem(NETWORK_FEE_ITEM_ID)
        return parseJsonResponse(response, responseWrapper) { body ->
            val parsed = Gson().fromJson(body, NetworkFeeItemPageResponse::class.java)
            if (parsed.code == 200 && parsed.feeitem != null) {
                responseWrapper.code = 0
                responseWrapper.msg = "success"
                responseWrapper.data = parsed.feeitem
            } else {
                responseWrapper.code = -1
                responseWrapper.msg = parsed.msg ?: "网费充值配置加载失败"
            }
        }
    }

    private suspend fun fetchNetworkInfo(): AHUResponse<NetworkFeeInfoPayload> {
        val responseWrapper = AHUResponse<NetworkFeeInfoPayload>()
        val formBody = FormBody.Builder()
            .add("feeitemid", NETWORK_FEE_ITEM_ID)
            .add("type", "IEC")
            .add("level", "0")
            .build()
        val response = YcardApi.API.getFeeItemThirdData(formBody)
        return parseJsonResponse(response, responseWrapper) { body ->
            val parsed = Gson().fromJson(body, NetworkFeeInfoResponse::class.java)
            val map = parsed.map
            if (parsed.code == 200 && map?.data != null) {
                responseWrapper.code = 0
                responseWrapper.msg = "success"
                responseWrapper.data = NetworkFeeInfoPayload(
                    showData = map.showData.orEmpty(),
                    thirdPartyData = map.data
                )
            } else {
                responseWrapper.code = -1
                responseWrapper.msg = parsed.msg ?: "网费账户信息加载失败"
            }
        }
    }

    private suspend fun getPaymentOrder(
        amount: String,
        thirdPartyData: NetworkThirdPartyData
    ): AHUResponse<NetworkOrderData> {
        val responseWrapper = AHUResponse<NetworkOrderData>()
        val formBody = buildSignedFormBody(
            linkedMapOf(
                "feeitemid" to NETWORK_FEE_ITEM_ID,
                "tranamt" to amount,
                "flag" to "choose",
                "source" to "app",
                "paystep" to "0",
                "abstracts" to "",
                "redirect_url" to "https://ycard.ahu.edu.cn/plat",
                "third_party" to Gson().toJson(thirdPartyData)
            )
        )
        val response = YcardApi.API.pay(formBody)
        return parseJsonResponse(response, responseWrapper) { body ->
            val parsed = Gson().fromJson(body, NetworkOrderResponse::class.java)
            if (parsed.code == 200 && parsed.data != null) {
                responseWrapper.code = 0
                responseWrapper.msg = "success"
                responseWrapper.data = parsed.data
            } else {
                responseWrapper.code = -1
                responseWrapper.msg = parsed.msg
            }
        }
    }

    private suspend fun getAccountPayInfo(orderId: String): AHUResponse<NetworkAccountPayInfoData> {
        val responseWrapper = AHUResponse<NetworkAccountPayInfoData>()
        val formBody = buildSignedFormBody(
            linkedMapOf(
                "paytypeid" to "64",
                "paytype" to "ACCOUNTTSM",
                "paystep" to "2",
                "orderid" to orderId
            )
        )
        val response = YcardApi.API.pay(formBody)
        return parseJsonResponse(response, responseWrapper) { body ->
            val parsed = Gson().fromJson(body, NetworkAccountPayInfoResponse::class.java)
            if (parsed.code == 200 && parsed.data?.passwordMap?.isNotEmpty() == true) {
                responseWrapper.code = 0
                responseWrapper.msg = "success"
                responseWrapper.data = parsed.data
            } else {
                responseWrapper.code = -1
                responseWrapper.msg = parsed.msg
            }
        }
    }

    private suspend fun executeFinalPay(
        orderId: String,
        password: String,
        uuid: String
    ): AHUResponse<String> {
        val responseWrapper = AHUResponse<String>()
        val formBody = buildSignedFormBody(
            linkedMapOf(
                "orderid" to orderId,
                "paystep" to "2",
                "paytype" to "ACCOUNTTSM",
                "paytypeid" to "64",
                "userAgent" to "h5",
                "ccctype" to "000",
                "password" to password,
                "uuid" to uuid,
                "isWX" to "0"
            )
        )
        val response = YcardApi.API.pay(formBody)
        return parseJsonResponse(response, responseWrapper) { body ->
            val parsed = Gson().fromJson(body, NetworkFinalPayResponse::class.java)
            if (parsed.code == 200 && parsed.success && !parsed.data.isNullOrBlank()) {
                responseWrapper.code = 0
                responseWrapper.msg = "success"
                responseWrapper.data = parsed.data
            } else {
                responseWrapper.code = -1
                responseWrapper.msg = parsed.msg
            }
        }
    }

    private suspend fun <T> parseJsonResponse(
        response: Response<ResponseBody>,
        wrapper: AHUResponse<T>,
        parser: (String) -> Unit
    ): AHUResponse<T> {
        return withContext(Dispatchers.IO) {
            try {
                if (!response.isSuccessful) {
                    wrapper.code = response.code()
                    wrapper.msg = "请求接口失败: ${response.message()}"
                    return@withContext wrapper
                }
                val body = response.body()?.string()
                if (body.isNullOrBlank()) {
                    wrapper.code = -1
                    wrapper.msg = "服务器返回内容为空"
                    return@withContext wrapper
                }
                parser(body)
                wrapper
            } catch (t: Throwable) {
                wrapper.code = -1
                wrapper.msg = t.message ?: "发生未知错误"
                wrapper
            }
        }
    }

    private suspend fun executeThirdData(
        formBody: FormBody,
        wrapper: AHUResponse<Unit>,
        errorMessage: String
    ): AHUResponse<Unit> {
        val response = YcardApi.API.getFeeItemThirdData(formBody)
        return parseJsonResponse(response, wrapper) { body ->
            val responseBody = Gson().fromJson(body, ThirdDataResponse::class.java)
            if (responseBody.code == 200) {
                wrapper.code = 0
                wrapper.msg = "success"
                wrapper.data = Unit
            } else {
                wrapper.code = -1
                wrapper.msg = responseBody.msg ?: errorMessage
            }
        }
    }

    private fun buildSignedFormBody(params: LinkedHashMap<String, String>): FormBody {
        val appId = "56321"
        val timestamp = getTimestamp()
        val signType = "SHA256"
        val nonce = generateNonce()
        val signParams = linkedMapOf(
            "APP_ID" to appId,
            "NONCE" to nonce,
            "SIGN_TYPE" to signType,
            "TIMESTAMP" to timestamp
        ).apply {
            params.filterValues { it.isNotEmpty() }
                .toSortedMap()
                .forEach { (key, value) -> put(key, value) }
        }
        val signSource = signParams.entries.joinToString("&") { (key, value) -> "$key=$value" } +
                "&SECRET_KEY=0osTIhce7uPvDKHz6aa67bhCukaKoYl4"
        val sign = sha256(signSource).uppercase()

        val builder = FormBody.Builder()
        params.forEach { (key, value) -> builder.add(key, value) }
        builder.add("APP_ID", appId)
        builder.add("TIMESTAMP", timestamp)
        builder.add("SIGN_TYPE", signType)
        builder.add("NONCE", nonce)
        builder.add("SIGN", sign)
        return builder.build()
    }

    private fun buildPasswordCipherMap(mapString: String): Map<Char, Char> {
        require(mapString.length == 10) { "无效的密码映射表" }
        val plainDigits = "0123456789"
        return mapString.mapIndexed { index, cipherDigit ->
            cipherDigit to plainDigits[index]
        }.toMap()
    }
}

data class NetworkFeeInfoPayload(
    val showData: Map<String, String>,
    val thirdPartyData: NetworkThirdPartyData?
)
