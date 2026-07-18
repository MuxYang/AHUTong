package com.ahu.ahutong.ui.screen.main

internal data class CmbRechargePagePalette(
    val colorScheme: String,
    val background: String,
    val surface: String,
    val surfaceVariant: String,
    val text: String,
    val secondaryText: String,
    val outline: String,
    val accent: String,
    val onAccent: String,
    val success: String,
    val scrim: String
)

/**
 * Builds the styling JavaScript injected into the CMB recharge flow.
 *
 * The script creates or updates one style element. It deliberately does not observe the DOM,
 * register event handlers, read form values, or touch the page's network and payment logic.
 */
internal fun buildCmbRechargeStyleScript(palette: CmbRechargePagePalette): String {
    val css = """
        :root {
          color-scheme: ${palette.colorScheme};
          --ahutong-bg: ${palette.background};
          --ahutong-surface: ${palette.surface};
          --ahutong-surface-variant: ${palette.surfaceVariant};
          --ahutong-text: ${palette.text};
          --ahutong-text-secondary: ${palette.secondaryText};
          --ahutong-outline: ${palette.outline};
          --ahutong-accent: ${palette.accent};
          --ahutong-on-accent: ${palette.onAccent};
          --ahutong-success: ${palette.success};
          --ahutong-scrim: ${palette.scrim};
        }
        html,
        body,
        #app,
        #app > .home {
          min-height: 100%;
          background: var(--ahutong-bg) !important;
          color: var(--ahutong-text) !important;
          font-family: -apple-system, BlinkMacSystemFont, Segoe UI, PingFang SC,
            Hiragino Sans GB, Microsoft YaHei, sans-serif !important;
        }
        body {
          margin: 0;
          overscroll-behavior: none;
          -webkit-font-smoothing: antialiased;
        }
        #app {
          width: 100%;
          margin: 0 auto !important;
        }
        #app .van-nav-bar {
          display: none !important;
        }
        #app .van-hairline--bottom::after,
        #app .van-cell::after {
          border-color: var(--ahutong-outline) !important;
        }
        #app .charge {
          padding: 20px 0 28px !important;
        }
        #app .charge .swiper-container {
          margin-bottom: 16px !important;
          border-radius: 0 !important;
        }
        #app .charge .cardBox {
          margin-top: 0 !important;
          padding: 18px 20px 24px !important;
          border-radius: 24px !important;
          box-shadow: none !important;
        }
        #app .charge .cardBox.electronic {
          overflow: hidden;
          background-position: center !important;
          background-size: 100% 100% !important;
        }
        #app .charge .van-cell {
          margin-bottom: 8px;
          background: var(--ahutong-surface) !important;
          border: 1px solid var(--ahutong-outline);
          border-radius: 16px !important;
          box-shadow: none !important;
        }
        #app .van-cell {
          padding: 14px 8px !important;
          background: transparent !important;
          color: var(--ahutong-text) !important;
        }
        #app .van-cell__title,
        #app .van-field__label,
        #app .van-action-sheet__header {
          color: var(--ahutong-text) !important;
        }
        #app .van-cell__value,
        #app .van-cell__right-icon,
        #app .text-gray,
        #app .van-action-sheet__close {
          color: var(--ahutong-text-secondary) !important;
        }
        #app .van-field__control {
          color: var(--ahutong-text) !important;
          -webkit-text-fill-color: var(--ahutong-text) !important;
          caret-color: var(--ahutong-accent) !important;
          font-family: inherit !important;
        }
        #app .van-field__control::placeholder {
          color: var(--ahutong-text-secondary) !important;
          -webkit-text-fill-color: var(--ahutong-text-secondary) !important;
          opacity: 1;
        }
        #app .closeAmount {
          gap: 8px;
          justify-content: stretch !important;
          margin: 16px 0 24px !important;
        }
        #app .closeAmount .van-button {
          min-width: 0;
          height: 40px !important;
          padding: 0 8px !important;
          flex: 1 1 0;
          overflow: hidden;
          border-width: 1px !important;
          border-radius: 14px !important;
          box-shadow: none !important;
        }
        #app .closeAmount .van-hairline--surround::after {
          content: none !important;
        }
        #app .van-button--warning.van-button--plain {
          background: var(--ahutong-surface-variant) !important;
          border-color: var(--ahutong-accent) !important;
          color: var(--ahutong-accent) !important;
        }
        #app .charge .van-button--info.van-button--block,
        #app .van-button--default.van-button--block {
          height: 48px !important;
          background: var(--ahutong-accent) !important;
          border-color: var(--ahutong-accent) !important;
          border-radius: 16px !important;
          box-shadow: none !important;
          color: var(--ahutong-on-accent) !important;
        }
        #app .van-button__text {
          color: inherit !important;
        }
        #app .charge .text-center.text-gray {
          padding: 0 12px;
          color: var(--ahutong-text-secondary) !important;
          line-height: 1.65;
        }
        #app .van-overlay {
          background: var(--ahutong-scrim) !important;
        }
        #app .van-popup,
        #app .van-action-sheet {
          background: var(--ahutong-surface) !important;
          color: var(--ahutong-text) !important;
        }
        #app .van-action-sheet {
          overflow: hidden;
          border-radius: 28px 28px 0 0 !important;
          box-shadow: none !important;
        }
        #app .van-password-input__security {
          overflow: hidden;
          background: var(--ahutong-surface-variant) !important;
          border-radius: 16px !important;
        }
        #app .van-password-input__security li {
          background: var(--ahutong-surface-variant) !important;
          color: var(--ahutong-text) !important;
        }
        #app .van-password-input__security::after,
        #app .van-password-input__item::after {
          border-color: var(--ahutong-outline) !important;
        }
        #app .van-password-input__security i {
          background: var(--ahutong-text) !important;
        }
        #app .keyboard {
          background: var(--ahutong-surface) !important;
          color: var(--ahutong-text) !important;
        }
        #app .keyboard tr td {
          border-color: var(--ahutong-outline) !important;
          color: var(--ahutong-text);
        }
        #app .keyboard tr td:active {
          background: var(--ahutong-surface-variant);
        }
        #app .resultBox {
          margin: 24px 16px 16px !important;
          padding: 24px 8px 12px !important;
          background: var(--ahutong-surface) !important;
          border: 1px solid var(--ahutong-outline);
          border-radius: 24px !important;
          box-shadow: none !important;
        }
        #app .resultBox .topIcon {
          margin-bottom: 24px !important;
          color: var(--ahutong-success) !important;
        }
        #app .resultBox .cell {
          padding: 14px 12px !important;
          color: var(--ahutong-text) !important;
        }
        #app .text-success {
          color: var(--ahutong-success) !important;
        }
        #app #copyText,
        #app a {
          color: var(--ahutong-accent) !important;
        }
        #app .van-toast {
          background: var(--ahutong-surface-variant) !important;
          color: var(--ahutong-text) !important;
          border-radius: 18px !important;
          box-shadow: none !important;
        }
        #app .van-loading__spinner {
          color: var(--ahutong-accent) !important;
        }
      """.trimIndent()

    return """
        (function() {
          var styleId = 'ahutong-cmb-style';
          var style = document.getElementById(styleId);
          if (!style) {
            style = document.createElement('style');
            style.id = styleId;
            document.head.appendChild(style);
          }
          style.textContent = ${css.toJavaScriptStringLiteral()};
        })();
    """.trimIndent()
}

/**
 * Locates only the result page's return button and reports its normalized viewport bounds.
 * Native Compose content uses those bounds for a click overlay; no page click is intercepted.
 */
internal fun buildCmbRechargeSuccessReturnBoundsScript(): String =
    """
        (function() {
          var path = window.location.pathname.replace(/\/+$/, '').toLowerCase();
          var isKnownResultPage =
            window.location.protocol === 'https:' &&
            window.location.hostname.toLowerCase() === 'epay92.ahu.edu.cn' &&
            (window.location.port === '' || window.location.port === '443') &&
            path === '/cashier-mobile/chargeresult';
          var resultBox = document.querySelector('#app .resultBox');
          if (!isKnownResultPage || !resultBox || resultBox.getClientRects().length === 0) {
            return null;
          }
          var buttons = document.querySelectorAll(
            '#app button.van-button.van-button--default.van-button--normal.van-button--block.van-button--round'
          );
          if (buttons.length !== 1 || buttons[0].disabled) return null;
          var button = buttons[0];
          var style = window.getComputedStyle(button);
          if (
            style.display === 'none' ||
            style.visibility === 'hidden' ||
            style.opacity === '0' ||
            button.getClientRects().length === 0
          ) return null;
          button.style.pointerEvents = 'none';
          var viewport = window.visualViewport;
          var viewportLeft = viewport ? viewport.offsetLeft : 0;
          var viewportTop = viewport ? viewport.offsetTop : 0;
          var viewportWidth = viewport ? viewport.width : window.innerWidth;
          var viewportHeight = viewport ? viewport.height : window.innerHeight;
          if (viewportWidth <= 0 || viewportHeight <= 0) return null;
          var rect = button.getBoundingClientRect();
          var left = Math.max(rect.left, viewportLeft);
          var top = Math.max(rect.top, viewportTop);
          var right = Math.min(rect.right, viewportLeft + viewportWidth);
          var bottom = Math.min(rect.bottom, viewportTop + viewportHeight);
          if (right <= left || bottom <= top) return null;
          return [
            (left - viewportLeft) / viewportWidth,
            (top - viewportTop) / viewportHeight,
            (right - left) / viewportWidth,
            (bottom - top) / viewportHeight
          ];
        })();
    """.trimIndent()

private fun String.toJavaScriptStringLiteral(): String = buildString(length + 2) {
    append('"')
    this@toJavaScriptStringLiteral.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\u2028' -> append("\\u2028")
            '\u2029' -> append("\\u2029")
            else -> append(character)
        }
    }
    append('"')
}
