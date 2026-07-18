package com.ahu.ahutong.ui.screen.main

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import com.ahu.ahutong.data.crawler.manager.CookieManager as YcardCookieManager
import com.ahu.ahutong.data.crawler.manager.TokenManager
import com.ahu.ahutong.ui.shape.SmoothRoundedCornerShape
import com.kyant.monet.a1
import com.kyant.monet.n1
import com.kyant.monet.withNight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import java.net.URI

internal data class CmbRechargeNormalizedBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CmbCardRecharge(
    onExit: () -> Unit,
    onRechargeSuccessExit: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val pageBackgroundColor = 96.n1 withNight 10.n1
    val pageStylePalette = CmbRechargePagePalette(
        colorScheme = if (isDarkTheme) "dark" else "light",
        background = pageBackgroundColor.toCssColor(),
        surface = (100.n1 withNight 20.n1).toCssColor(),
        surfaceVariant = (92.n1 withNight 26.n1).toCssColor(),
        text = (10.n1 withNight 90.n1).toCssColor(),
        secondaryText = (45.n1 withNight 72.n1).toCssColor(),
        outline = (85.n1 withNight 36.n1).toCssColor(),
        accent = (40.a1 withNight 80.a1).toCssColor(),
        onAccent = (100.n1 withNight 10.n1).toCssColor(),
        success = (if (isDarkTheme) Color(0xFF81C784) else Color(0xFF2E7D32)).toCssColor(),
        scrim = if (isDarkTheme) "rgba(0, 0, 0, 0.62)" else "rgba(0, 0, 0, 0.38)"
    )
    val pageStyleScript = remember(pageStylePalette) {
        buildCmbRechargeStyleScript(pageStylePalette)
    }
    val latestPageStyleScript = rememberUpdatedState(pageStyleScript)
    val latestRechargeSuccessExit = rememberUpdatedState(onRechargeSuccessExit)
    var entryUrl by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var tokenRequestVersion by remember { mutableIntStateOf(0) }
    var loadRequestVersion by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isRechargeSuccessPage by remember { mutableStateOf(false) }
    var successReturnBounds by remember {
        mutableStateOf<CmbRechargeNormalizedBounds?>(null)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onExit)

    fun reloadEntry() {
        progress = 0
        isLoading = true
        errorMessage = null
        isRechargeSuccessPage = false
        successReturnBounds = null
        webView?.stopLoading()
        loadRequestVersion += 1
    }

    LaunchedEffect(tokenRequestVersion) {
        progress = 0
        isLoading = true
        errorMessage = null
        entryUrl = null
        isRechargeSuccessPage = false
        successReturnBounds = null
        val token = withContext(Dispatchers.IO) { TokenManager.awaitToken() }
        if (token.isNullOrBlank()) {
            errorMessage = "校园卡登录凭证暂未就绪，请稍后重试"
            isLoading = false
            return@LaunchedEffect
        }
        entryUrl = buildCmbRechargeEntryUrl(token)
        loadRequestVersion += 1
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.cmbRechargeState?.boundsLocator?.dispose()
            webView?.destroy()
            webView = null
        }
    }

    LaunchedEffect(pageStyleScript) {
        webView?.let { currentView ->
            applyCmbRechargePageStyle(currentView, currentView.url, pageStyleScript)
            currentView.cmbRechargeState?.boundsLocator?.locate(currentView.url)
        }
    }

    val pageContentColor = 10.n1 withNight 90.n1
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = pageBackgroundColor,
        contentColor = pageContentColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "招商银行充值",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBackgroundColor,
                    navigationIconContentColor = pageContentColor,
                    titleContentColor = pageContentColor
                )
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .background(pageBackgroundColor)
        ) {
            entryUrl?.let { url ->
                val requestVersion = loadRequestVersion
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        createCmbRechargeWebView(
                            context = viewContext,
                            pageBackgroundColor = pageBackgroundColor.toArgb(),
                            pageStyleScript = { latestPageStyleScript.value },
                            onLoadingChanged = { isLoading = it },
                            onProgressChanged = { progress = it },
                            onSuccessPageChanged = { isSuccessPage ->
                                isRechargeSuccessPage = isSuccessPage
                                if (!isSuccessPage) successReturnBounds = null
                            },
                            onSuccessReturnBoundsChanged = { successReturnBounds = it },
                            onMainFrameError = { error ->
                                errorMessage = error
                            },
                            onExternalLink = { externalUrl ->
                                openExternalLink(context, externalUrl)
                            }
                        ).also { created ->
                            syncYcardCookiesToWebView(created)
                            created.cmbRechargeState?.requestVersion = requestVersion
                            created.loadUrl(url)
                            webView = created
                        }
                    },
                    update = { currentView ->
                        currentView.setBackgroundColor(pageBackgroundColor.toArgb())
                        if (currentView.cmbRechargeState?.requestVersion != requestVersion) {
                            syncYcardCookiesToWebView(currentView)
                            currentView.cmbRechargeState?.requestVersion = requestVersion
                            currentView.loadUrl(url)
                        }
                        webView = currentView
                    }
                )
            }

            if (isRechargeSuccessPage) {
                successReturnBounds?.let { bounds ->
                    CmbRechargeSuccessReturnOverlay(
                        bounds = bounds,
                        onClick = {
                            successReturnBounds = null
                            latestRechargeSuccessExit.value()
                        }
                    )
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = 30.n1 withNight 70.n1
                )
            }

            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )
            }

            errorMessage?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                        .fillMaxWidth()
                        .background(100.n1 withNight 20.n1, SmoothRoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = message,
                        color = pageContentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "重试",
                        modifier = Modifier.clickable {
                            if (entryUrl == null) {
                                errorMessage = null
                                isLoading = true
                                tokenRequestVersion += 1
                            } else {
                                reloadEntry()
                            }
                        },
                        color = 30.n1 withNight 70.n1,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun CmbRechargeSuccessReturnOverlay(
    bounds: CmbRechargeNormalizedBounds,
    onClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = maxWidth * bounds.left,
                    y = maxHeight * bounds.top
                )
                .width(maxWidth * bounds.width)
                .height(maxHeight * bounds.height)
                .clip(SmoothRoundedCornerShape(20.dp))
                .clickable(
                    onClickLabel = "返回应用首页",
                    role = Role.Button,
                    onClick = onClick
                )
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCmbRechargeWebView(
    context: android.content.Context,
    pageBackgroundColor: Int,
    pageStyleScript: () -> String,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onSuccessPageChanged: (Boolean) -> Unit,
    onSuccessReturnBoundsChanged: (CmbRechargeNormalizedBounds?) -> Unit,
    onMainFrameError: (String) -> Unit,
    onExternalLink: (String) -> Unit
): WebView {
    return WebView(context).apply {
        setBackgroundColor(pageBackgroundColor)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadsImagesAutomatically = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
        val boundsLocator = CmbRechargeBoundsLocator(this, onSuccessReturnBoundsChanged)
        tag = CmbRechargeWebViewState(boundsLocator = boundsLocator)

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
                if (newProgress >= 100) {
                    onLoadingChanged(false)
                }
            }
        }

        webViewClient = object : WebViewClient() {
            private fun updateSuccessPage(url: String?): Boolean {
                val isSuccessPage = isCmbRechargeSuccessUrl(url)
                onSuccessPageChanged(isSuccessPage)
                if (!isSuccessPage) boundsLocator.clear()
                return isSuccessPage
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val targetUri = request?.url ?: return false
                val scheme = targetUri.scheme?.lowercase().orEmpty()
                if (scheme.isBlank()) return false
                if (scheme != "http" && scheme != "https") {
                    onExternalLink(targetUri.toString())
                    return true
                }
                return if (isInternalCmbRechargeUrl(targetUri)) {
                    false
                } else {
                    onExternalLink(targetUri.toString())
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                onLoadingChanged(true)
                boundsLocator.clear()
                updateSuccessPage(url)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoadingChanged(false)
                updateSuccessPage(url)
                if (view != null) {
                    applyCmbRechargePageStyle(view, url, pageStyleScript())
                    boundsLocator.locate(url)
                }
                super.onPageFinished(view, url)
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                if (updateSuccessPage(url) && view != null) boundsLocator.locate(url)
                super.doUpdateVisitedHistory(view, url, isReload)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    onLoadingChanged(false)
                    boundsLocator.clear()
                    onSuccessPageChanged(false)
                    onMainFrameError(error?.description?.toString() ?: "页面加载失败，请稍后重试")
                }
                super.onReceivedError(view, request, error)
            }
        }
    }
}

private class CmbRechargeWebViewState(
    val boundsLocator: CmbRechargeBoundsLocator,
    var requestVersion: Int = -1
)

private val WebView.cmbRechargeState: CmbRechargeWebViewState?
    get() = tag as? CmbRechargeWebViewState

private class CmbRechargeBoundsLocator(
    private val webView: WebView,
    private val onBoundsChanged: (CmbRechargeNormalizedBounds?) -> Unit
) {
    private var generation = 0
    private var consecutiveMisses = 0
    private var lastBounds: CmbRechargeNormalizedBounds? = null
    private var pendingPoll: Runnable? = null
    private var isDisposed = false

    fun clear() {
        if (isDisposed) return
        generation += 1
        cancelPendingPoll()
        consecutiveMisses = 0
        publish(null)
    }

    fun locate(url: String?) {
        if (isDisposed) return
        generation += 1
        cancelPendingPoll()
        consecutiveMisses = 0
        val currentGeneration = generation
        if (!isCmbRechargeSuccessUrl(url)) {
            publish(null)
            return
        }
        publish(null)
        locate(currentGeneration)
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        generation += 1
        cancelPendingPoll()
        lastBounds = null
    }

    private fun locate(currentGeneration: Int) {
        if (
            isDisposed ||
            currentGeneration != generation ||
            !isCmbRechargeSuccessUrl(webView.url)
        ) {
            return
        }
        webView.evaluateJavascript(buildCmbRechargeSuccessReturnBoundsScript()) { rawResult ->
            if (
                isDisposed ||
                currentGeneration != generation ||
                !isCmbRechargeSuccessUrl(webView.url)
            ) {
                return@evaluateJavascript
            }
            val bounds = parseCmbRechargeNormalizedBounds(rawResult)
            if (bounds != null) {
                consecutiveMisses = 0
                publish(bounds)
            } else {
                consecutiveMisses += 1
                publish(null)
            }
            scheduleNextPoll(
                currentGeneration = currentGeneration,
                delayMillis = when {
                    bounds != null -> 250L
                    consecutiveMisses <= 30 -> 100L
                    else -> 1_000L
                }
            )
        }
    }

    private fun scheduleNextPoll(currentGeneration: Int, delayMillis: Long) {
        val poll = Runnable {
            pendingPoll = null
            locate(currentGeneration)
        }
        pendingPoll = poll
        if (!webView.postDelayed(poll, delayMillis)) pendingPoll = null
    }

    private fun cancelPendingPoll() {
        pendingPoll?.let(webView::removeCallbacks)
        pendingPoll = null
    }

    private fun publish(bounds: CmbRechargeNormalizedBounds?) {
        if (lastBounds == bounds) return
        lastBounds = bounds
        onBoundsChanged(bounds)
    }
}

internal fun parseCmbRechargeNormalizedBounds(rawResult: String?): CmbRechargeNormalizedBounds? {
    val value = rawResult?.trim().orEmpty()
    if (!value.startsWith('[') || !value.endsWith(']')) return null
    val parts = value.substring(1, value.length - 1).split(',')
    if (parts.size != 4) return null
    val numbers = parts.map { it.trim().toDoubleOrNull() ?: return null }
    return validateCmbRechargeNormalizedBounds(
        left = numbers[0],
        top = numbers[1],
        width = numbers[2],
        height = numbers[3]
    )
}

internal fun validateCmbRechargeNormalizedBounds(
    left: Double,
    top: Double,
    width: Double,
    height: Double
): CmbRechargeNormalizedBounds? {
    val values = listOf(left, top, width, height)
    if (values.any { !it.isFinite() }) return null
    if (left !in 0.0..1.0 || top !in 0.0..1.0) return null
    if (width !in 0.05..1.0 || height !in 0.01..0.35) return null
    if (left + width > 1.001 || top + height > 1.001) return null
    return CmbRechargeNormalizedBounds(
        left = left.toFloat(),
        top = top.toFloat(),
        width = width.toFloat(),
        height = height.toFloat()
    )
}

private fun applyCmbRechargePageStyle(webView: WebView, url: String?, script: String) {
    if (!isCmbRechargeStyleTarget(url)) return
    webView.evaluateJavascript(script, null)
}

internal fun isCmbRechargeSuccessUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val scheme = uri.scheme.orEmpty().lowercase()
    val host = uri.host.orEmpty().lowercase()
    val path = uri.path.orEmpty().trimEnd('/').lowercase()
    return scheme == "https" &&
        host == "epay92.ahu.edu.cn" &&
        uri.port in setOf(-1, 443) &&
        path == "/cashier-mobile/chargeresult"
}

internal fun isCmbRechargeStyleTarget(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    val host = uri.host.orEmpty().lowercase()
    val path = uri.path.orEmpty().lowercase()
    return when (host) {
        "epay92.ahu.edu.cn" -> path == "/cashier-mobile" || path.startsWith("/cashier-mobile/")
        "ycard.ahu.edu.cn" -> path.startsWith("/charge-app")
        else -> false
    }
}

private fun buildCmbRechargeEntryUrl(token: String): String {
    return Uri.Builder()
        .scheme("https")
        .authority("ycard.ahu.edu.cn")
        .appendPath("berserker-base")
        .appendPath("redirect")
        .appendQueryParameter("appId", "253")
        .appendQueryParameter("loginFrom", "h5")
        .appendQueryParameter("synAccessSource", "h5")
        .appendQueryParameter("synjones-auth", token)
        .appendQueryParameter("type", "app")
        .build()
        .toString()
}

private fun isInternalCmbRechargeUrl(url: Uri): Boolean {
    val host = url.host.orEmpty().lowercase()
    return host == "ahu.edu.cn" || host.endsWith(".ahu.edu.cn")
}

private fun openExternalLink(context: android.content.Context, url: String) {
    val targetUri = runCatching { Uri.parse(url) }.getOrNull()
    if (targetUri == null) {
        Toast.makeText(context, "无法打开外部链接", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, targetUri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "无法打开外部链接", Toast.LENGTH_SHORT).show()
    }
}

private fun syncYcardCookiesToWebView(webView: WebView) {
    val webCookieManager = android.webkit.CookieManager.getInstance()
    YcardCookieManager.cookieJar.getAllCookies().forEach { cookie ->
        val targetUrl = buildCookieTargetUrl(cookie)
        val cookieValue = buildString {
            append(cookie.name)
            append("=")
            append(cookie.value)
            append("; Path=")
            append(cookie.path)
            append("; Domain=")
            append(cookie.domain)
            if (cookie.secure) append("; Secure")
            if (cookie.httpOnly) append("; HttpOnly")
        }
        webCookieManager.setCookie(targetUrl, cookieValue)
    }
    webCookieManager.flush()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        webCookieManager.setAcceptThirdPartyCookies(webView, true)
    }
}

private fun buildCookieTargetUrl(cookie: Cookie): String {
    val scheme = if (cookie.secure) "https" else "http"
    val domain = cookie.domain.trimStart('.')
    return "$scheme://$domain"
}

private fun Color.toCssColor(): String = "#%06X".format(toArgb() and 0xFFFFFF)
