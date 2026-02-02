package com.monitoring.runner.playwright;

import com.microsoft.playwright.Page;

public class PerfCollector {
    private Integer ttfbMs;
    private Integer domMs;
    private Integer loadMs;
    
    public void collectMetrics(Page page) {
        try {
            Object timings = page.evaluate(
                "() => {\n" +
                "  const t = performance.timing;\n" +
                "  return {\n" +
                "    ttfb: t.responseStart - t.requestStart,\n" +
                "    dom: t.domContentLoadedEventEnd - t.navigationStart,\n" +
                "    load: t.loadEventEnd - t.navigationStart\n" +
                "  };\n" +
                "}"
            );
            
            if (timings instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) timings;
                
                ttfbMs = toInteger(map.get("ttfb"));
                domMs = toInteger(map.get("dom"));
                loadMs = toInteger(map.get("load"));
            }
        } catch (Exception e) {
            // Ignore collection errors
        }
    }
    
    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    public Integer getTtfbMs() {
        return ttfbMs;
    }
    
    public Integer getDomMs() {
        return domMs;
    }
    
    public Integer getLoadMs() {
        return loadMs;
    }
}
