package com.monitoring.api.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

public record TimeseriesResponse(
    Long siteId,
    LocalDateTime from,
    LocalDateTime to,
    String bucket,
    List<DataPoint> dataPoints
) {}
