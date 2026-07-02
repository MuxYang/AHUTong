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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.ahu.ahutong.data.crawler.manager.CookieManager as YcardCookieManager
import com.ahu.ahutong.data.crawler.manager.TokenManager
import com.ahu.ahutong.ui.shape.SmoothRoundedCornerShape
import com.kyant.monet.n1
import com.kyant.monet.withNight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie

private const val CMB_RECHARGE_STYLE_SCRIPT = """
(function(){
  var styleId = 'ahutong-cmb-style';
  if (document.getElementById(styleId)) return;
  var style = document.createElement('style');
  style.id = styleId;
  style.textContent = [
    'html,body,#app,#app-box{background:#eef2f5 !important;color:#1f2328 !important;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Hiragino Sans GB","Microsoft YaHei",sans-serif !important;}',
    'body{overscroll-behavior:none !important;-webkit-font-smoothing:antialiased !important;}',
    '#app,#app-box,.page,.container,.main,.weui-tab__panel{max-width:720px;margin:0 auto !important;}',
    '.weui-btn_primary,.weui-btn_warn,.weui-btn_default{border-radius:16px !important;box-shadow:none !important;}',
    '.weui-btn_primary{background:#1e88e5 !important;border-color:#1e88e5 !important;}',
    '.weui-btn_warn{background:#d94f4f !important;border-color:#d94f4f !important;}',
    '.weui-btn_default{background:#ffffff !important;color:#1f2328 !important;border-color:#d0d7de !important;}',
    '.weui-cells,.weui-panel,.card,.panel{border-radius:20px !important;overflow:hidden !important;background:#ffffff !important;}',
    '.weui-cell{padding-top:14px !important;padding-bottom:14px !important;}',
    '.van-cell,.van-field,.cell,.form-item,.pay-item{border-radius:16px !important;background:#ffffff !important;}',
    '.van-button,.el-button,button{border-radius:16px !important;box-shadow:none !important;}',
    '.van-button--primary,.el-button--primary,button[type=submit]{background:#1e88e5 !important;border-color:#1e88e5 !important;color:#ffffff !important;}',
    '.van-field__label,.label,.title{color:#1f2328 !important;}',
    '.van-field__control,input,textarea,select{color:#1f2328 !important;}',
    '.van-cell-group,.form,.charge-box,.cashier-box{border-radius:20px !important;overflow:hidden !important;background:#ffffff !important;}',
    'input,textarea,select{font-family:inherit !important;}',
    'a{color:#1e88e5 !important;}'
  ].join('');
  document.head.appendChild(style);
})();
"""

@Composable
fun CmbCardRecharge() {
    val context = LocalContext.current
    var entryUrl by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var tokenRequestVersion by remember { mutableIntStateOf(0) }
    var loadRequestVersion by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    fun reloadEntry() {
        progress = 0
        isLoading = true
        errorMessage = null
        webView?.stopLoading()
        loadRequestVersion += 1
    }

    LaunchedEffect(tokenRequestVersion) {
        progress = 0
        isLoading = true
        errorMessage = null
        entryUrl = null
        canGoBack = false
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
            webView?.destroy()
            webView = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Text(
            text = "招商银行充值",
            modifier = Modifier.padding(24.dp, 32.dp, 24.dp, 16.dp),
            style = MaterialTheme.typography.headlineMedium
        )

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        errorMessage?.let { message ->
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(100.n1 withNight 20.n1, SmoothRoundedCornerShape(24.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = message,
                    color = 10.n1 withNight 90.n1,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(100.n1 withNight 20.n1, SmoothRoundedCornerShape(24.dp))
        ) {
            entryUrl?.let { url ->
                val requestVersion = loadRequestVersion
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        createCmbRechargeWebView(
                            context = viewContext,
                            onLoadingChanged = { isLoading = it },
                            onProgressChanged = { progress = it },
                            onNavigationChanged = { canGoBack = it },
                            onMainFrameError = { error ->
                                errorMessage = error
                            },
                            onExternalLink = { externalUrl ->
                                openExternalLink(context, externalUrl)
                            }
                        ).also { created ->
                            syncYcardCookiesToWebView(created)
                            created.tag = requestVersion
                            created.loadUrl(url)
                            webView = created
                        }
                    },
                    update = { currentView ->
                        if (currentView.tag != requestVersion) {
                            syncYcardCookiesToWebView(currentView)
                            currentView.tag = requestVersion
                            currentView.loadUrl(url)
                        }
                        webView = currentView
                    }
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = 30.n1 withNight 70.n1
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCmbRechargeWebView(
    context: android.content.Context,
    onLoadingChanged: (Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationChanged: (Boolean) -> Unit,
    onMainFrameError: (String) -> Unit,
    onExternalLink: (String) -> Unit
): WebView {
    return WebView(context).apply {
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

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                onProgressChanged(newProgress)
                if (newProgress >= 100) {
                    onLoadingChanged(false)
                }
            }
        }

        webViewClient = object : WebViewClient() {
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
                onNavigationChanged(view?.canGoBack() == true)
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onLoadingChanged(false)
                onNavigationChanged(view?.canGoBack() == true)
                if (url?.contains("/cashier-mobile/charge") == true || url?.contains("/charge-app") == true) {
                    view?.evaluateJavascript(CMB_RECHARGE_STYLE_SCRIPT, null)
                }
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    onLoadingChanged(false)
                    onMainFrameError(error?.description?.toString() ?: "页面加载失败，请稍后重试")
                }
                super.onReceivedError(view, request, error)
            }
        }
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
