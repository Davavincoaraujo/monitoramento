package com.monitoring.api.domain.enums;

public enum FailureType {
    NAVIGATION_FAILED,
    PAGE_CRASH,
    JS_ERROR,
    CONSOLE_ERROR,
    XHR_5XX,
    XHR_4XX,
    ASSET_404,
    CSS_404,
    JS_404,
    IMG_404,
    FONT_404,
    TIMEOUT,
    SLOW_TTFB,
    SLOW_DOM,
    SLOW_LOAD,
    REQUEST_FAILED
}
