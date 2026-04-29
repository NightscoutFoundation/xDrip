package com.eveningoutpost.dexdrip.pdf;

public class PdfExportConfig {
    public final long startTime;
    public final long endTime;
    public final int pageCount;
    public final boolean includeEvents;
    public final boolean includeTreatments;
    public final boolean includeStatistics;

    public PdfExportConfig(long startTime, long endTime, int pageCount,
                           boolean includeEvents, boolean includeTreatments,
                           boolean includeStatistics) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.pageCount = Math.max(1, pageCount);
        this.includeEvents = includeEvents;
        this.includeTreatments = includeTreatments;
        this.includeStatistics = includeStatistics;
    }

    public long sliceStartTime(int pageIndex) {
        long totalDuration = endTime - startTime;
        return startTime + (totalDuration * pageIndex / pageCount);
    }

    public long sliceEndTime(int pageIndex) {
        long totalDuration = endTime - startTime;
        return startTime + (totalDuration * (pageIndex + 1) / pageCount);
    }
}
