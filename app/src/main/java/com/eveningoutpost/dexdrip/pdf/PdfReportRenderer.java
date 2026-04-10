package com.eveningoutpost.dexdrip.pdf;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserEvent;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;

public class PdfReportRenderer {

    private static final String TAG = PdfReportRenderer.class.getSimpleName();

    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final float MARGIN = 30f;

    private final Context context;
    private final PdfExportConfig config;
    private final boolean doMgdl;
    private final double highMark;
    private final double lowMark;
    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateTimeFormat;

    private final Paint titlePaint;
    private final Paint headerPaint;
    private final Paint bodyPaint;
    private final Paint smallPaint;
    private final Paint linePaint;
    private final Paint gridPaint;
    private final Paint thresholdPaint;
    private final Paint graphBgPaint;

    public PdfReportRenderer(Context context, PdfExportConfig config) {
        this.context = context;
        this.config = config;
        this.doMgdl = Pref.getString("units", "mgdl").equals("mgdl");
        double highPref = Double.parseDouble(Pref.getString("highValue", "170"));
        double lowPref = Double.parseDouble(Pref.getString("lowValue", "70"));
        // Preferences store in display units — convert to mg/dl for internal use
        this.highMark = doMgdl ? highPref : highPref * Constants.MMOLL_TO_MGDL;
        this.lowMark = doMgdl ? lowPref : lowPref * Constants.MMOLL_TO_MGDL;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(14f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setAntiAlias(true);

        headerPaint = new Paint();
        headerPaint.setColor(Color.DKGRAY);
        headerPaint.setTextSize(10f);
        headerPaint.setAntiAlias(true);

        bodyPaint = new Paint();
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTextSize(8f);
        bodyPaint.setAntiAlias(true);

        smallPaint = new Paint();
        smallPaint.setColor(Color.GRAY);
        smallPaint.setTextSize(7f);
        smallPaint.setAntiAlias(true);

        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2196F3"));
        linePaint.setStrokeWidth(1.5f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(0.5f);
        gridPaint.setStyle(Paint.Style.STROKE);

        thresholdPaint = new Paint();
        thresholdPaint.setStrokeWidth(0.8f);
        thresholdPaint.setStyle(Paint.Style.STROKE);
        thresholdPaint.setPathEffect(new DashPathEffect(new float[]{4f, 4f}, 0f));

        graphBgPaint = new Paint();
        graphBgPaint.setColor(Color.WHITE);
        graphBgPaint.setStyle(Paint.Style.FILL);
    }

    public File render() throws IOException {
        final String dir = getExternalDir();
        new File(dir).mkdirs();
        final String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        final File outputFile = new File(dir, "xDrip-Report-" + timestamp + ".pdf");

        final PdfDocument document = new PdfDocument();

        try {
            for (int i = 0; i < config.pageCount; i++) {
                final long sliceStart = config.sliceStartTime(i);
                final long sliceEnd = config.sliceEndTime(i);
                renderPage(document, i, sliceStart, sliceEnd);
            }

            final FileOutputStream fos = new FileOutputStream(outputFile);
            document.writeTo(fos);
            fos.close();
        } finally {
            document.close();
        }

        return outputFile;
    }

    private void renderPage(PdfDocument document, int pageIndex, long sliceStart, long sliceEnd) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        canvas.drawColor(Color.WHITE);

        float yOffset = MARGIN;

        yOffset = drawHeader(canvas, pageIndex, sliceStart, sliceEnd, yOffset);

        final List<BgReading> readings = BgReading.latestForGraph(100000, sliceStart, sliceEnd);
        final List<UserEvent> events = config.includeEvents ? UserEvent.latestForGraph(sliceStart, sliceEnd) : null;
        final List<Treatments> treatments = config.includeTreatments ? Treatments.latestForGraph(100000, sliceStart, sliceEnd) : null;

        if (config.includeStatistics && readings != null && !readings.isEmpty()) {
            yOffset = drawStatsBar(canvas, readings, yOffset);
        }

        canvas.drawLine(MARGIN, yOffset, PAGE_WIDTH - MARGIN, yOffset, gridPaint);
        yOffset += 5f;

        final float footerY = PAGE_HEIGHT - MARGIN - 15f;
        final boolean hasSidePanel = config.includeEvents || config.includeTreatments;
        final float graphRight = hasSidePanel ? PAGE_WIDTH * 0.73f : PAGE_WIDTH - MARGIN;
        final float endpointLabelMargin = 25f; // space for start/end readouts
        final RectF graphArea = new RectF(MARGIN + 30f + endpointLabelMargin, yOffset, graphRight - endpointLabelMargin, footerY - 5f);

        if (readings != null && !readings.isEmpty()) {
            drawGlucoseGraph(canvas, graphArea, readings, sliceStart, sliceEnd);

            if (events != null && !events.isEmpty()) {
                drawEventMarkers(canvas, graphArea, events, sliceStart, sliceEnd);
            }
        }

        if (hasSidePanel) {
            final RectF panelArea = new RectF(graphRight + 10f, yOffset, PAGE_WIDTH - MARGIN, footerY - 5f);
            drawSidePanel(canvas, panelArea, events, treatments);
        }

        drawFooter(canvas, footerY);

        document.finishPage(page);
    }

    private float drawHeader(Canvas canvas, int pageIndex, long sliceStart, long sliceEnd, float yOffset) {
        final String title = context.getString(R.string.pdf_report_title);
        final String dateRange = dateFormat.format(new Date(sliceStart)) + " \u2013 " + dateFormat.format(new Date(sliceEnd));
        final String pageText = context.getString(R.string.pdf_page) + " " + (pageIndex + 1) + " " + context.getString(R.string.pdf_of) + " " + config.pageCount;

        canvas.drawText(title, MARGIN, yOffset + 14f, titlePaint);
        canvas.drawText(dateRange, MARGIN + 200f, yOffset + 14f, headerPaint);

        final float pageTextWidth = headerPaint.measureText(pageText);
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageTextWidth, yOffset + 14f, headerPaint);

        return yOffset + 22f;
    }

    private float drawStatsBar(Canvas canvas, List<BgReading> readings, float yOffset) {
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int lows = 0;
        int highs = 0;
        int inRange = 0;

        for (BgReading r : readings) {
            double v = r.calculated_value;
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
            if (v < lowMark) lows++;
            else if (v > highMark) highs++;
            else inRange++;
        }

        int total = readings.size();
        double avg = sum / total;
        double variance = 0;
        for (BgReading r : readings) {
            double diff = r.calculated_value - avg;
            variance += diff * diff;
        }
        double stddev = Math.sqrt(variance / total);
        double inRangePct = (inRange * 100.0) / total;

        final String unit = doMgdl ? "mg/dl" : "mmol/l";

        final String statsText = String.format(Locale.US,
                "%s: %.0f %s  |  %s: %.0f%%  |  %s: %d  |  %s: %d  |  %s: %.0f  |  %s: %.0f  |  %s: %.1f",
                context.getString(R.string.pdf_avg), unitize(avg), unit,
                context.getString(R.string.pdf_in_range), inRangePct,
                context.getString(R.string.pdf_lows), lows,
                context.getString(R.string.pdf_highs), highs,
                context.getString(R.string.pdf_min), unitize(min),
                context.getString(R.string.pdf_max), unitize(max),
                context.getString(R.string.pdf_stddev), unitize(stddev));

        canvas.drawText(statsText, MARGIN, yOffset + 10f, headerPaint);
        return yOffset + 18f;
    }

    private void drawGlucoseGraph(Canvas canvas, RectF area, List<BgReading> readings, long sliceStart, long sliceEnd) {
        canvas.drawRect(area, graphBgPaint);

        // Dynamic Y range: starts at 0, includes thresholds, fits all data
        double dataMax = Double.MIN_VALUE;
        for (BgReading r : readings) {
            if (r.calculated_value > dataMax) dataMax = r.calculated_value;
        }
        dataMax = Math.max(dataMax, highMark);
        double dataPadding = dataMax * 0.05;
        final double yMin = 0;
        final double yMax = dataMax + dataPadding;

        // Generate grid values dynamically based on range
        final double gridStepDisplay;
        if (doMgdl) {
            double range = yMax - yMin;
            if (range <= 100) gridStepDisplay = 20;
            else if (range <= 200) gridStepDisplay = 50;
            else gridStepDisplay = 50;
        } else {
            double rangeMmol = (yMax - yMin) * Constants.MGDL_TO_MMOLL;
            if (rangeMmol <= 5) gridStepDisplay = 1;
            else if (rangeMmol <= 10) gridStepDisplay = 2;
            else gridStepDisplay = 3;
        }
        double gridStart = doMgdl ? Math.ceil(yMin / gridStepDisplay) * gridStepDisplay
                : Math.ceil(yMin * Constants.MGDL_TO_MMOLL / gridStepDisplay) * gridStepDisplay;
        double gridEnd = doMgdl ? yMax : yMax * Constants.MGDL_TO_MMOLL;
        for (double gv = gridStart; gv <= gridEnd; gv += gridStepDisplay) {
            double mgdlVal = doMgdl ? gv : gv * Constants.MMOLL_TO_MGDL;
            float y = valueToY(mgdlVal, yMin, yMax, area);
            if (y >= area.top && y <= area.bottom) {
                canvas.drawLine(area.left, y, area.right, y, gridPaint);
                canvas.drawText(String.format(Locale.US, doMgdl ? "%.0f" : "%.1f", gv), MARGIN, y + 3f, smallPaint);
            }
        }

        long duration = sliceEnd - sliceStart;
        long timeStep;
        if (duration <= 6 * 3600 * 1000L) timeStep = 3600 * 1000L;
        else if (duration <= 24 * 3600 * 1000L) timeStep = 3 * 3600 * 1000L;
        else if (duration <= 7 * 24 * 3600 * 1000L) timeStep = 12 * 3600 * 1000L;
        else timeStep = 24 * 3600 * 1000L;

        long firstGrid = ((sliceStart / timeStep) + 1) * timeStep;
        for (long t = firstGrid; t < sliceEnd; t += timeStep) {
            float x = timeToX(t, sliceStart, sliceEnd, area);
            canvas.drawLine(x, area.top, x, area.bottom, gridPaint);
            String label = (timeStep >= 24 * 3600 * 1000L) ? dateFormat.format(new Date(t)) : timeFormat.format(new Date(t));
            canvas.drawText(label, x - 12f, area.bottom + 10f, smallPaint);
        }

        thresholdPaint.setColor(Color.parseColor("#FF9800"));
        float highY = valueToY(highMark, yMin, yMax, area);
        canvas.drawLine(area.left, highY, area.right, highY, thresholdPaint);

        thresholdPaint.setColor(Color.parseColor("#F44336"));
        float lowY = valueToY(lowMark, yMin, yMax, area);
        canvas.drawLine(area.left, lowY, area.right, lowY, thresholdPaint);

        Paint rangePaint = new Paint();
        rangePaint.setColor(Color.parseColor("#1A4CAF50"));
        rangePaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(area.left, highY, area.right, lowY, rangePaint);

        Path glucosePath = new Path();
        boolean first = true;
        for (BgReading r : readings) {
            float x = timeToX(r.timestamp, sliceStart, sliceEnd, area);
            float y = valueToY(r.calculated_value, yMin, yMax, area);
            y = Math.max(area.top, Math.min(area.bottom, y));
            if (first) {
                glucosePath.moveTo(x, y);
                first = false;
            } else {
                glucosePath.lineTo(x, y);
            }
        }
        canvas.drawPath(glucosePath, linePaint);

        // Peak/valley labels
        drawPeakLabels(canvas, area, readings, sliceStart, sliceEnd, yMin, yMax);

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.GRAY);
        borderPaint.setStrokeWidth(0.5f);
        borderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(area, borderPaint);
    }

    private void drawPeakLabels(Canvas canvas, RectF area, List<BgReading> readings,
                                long sliceStart, long sliceEnd, double yMin, double yMax) {
        if (readings.size() < 3) return;

        // Sort readings by timestamp ascending (they come desc from the query)
        java.util.ArrayList<BgReading> sorted = new java.util.ArrayList<>(readings);
        java.util.Collections.sort(sorted, (a, b) -> Long.compare(a.timestamp, b.timestamp));

        // 1-hour moving average smoothing
        final long smoothWindow = 30 * 60 * 1000L; // half-window = 30 min
        double[] smoothed = new double[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            double sum = 0;
            int count = 0;
            long t = sorted.get(i).timestamp;
            for (int j = 0; j < sorted.size(); j++) {
                if (Math.abs(sorted.get(j).timestamp - t) <= smoothWindow) {
                    sum += sorted.get(j).calculated_value;
                    count++;
                }
            }
            smoothed[i] = sum / count;
        }

        // Find local extrema on smoothed curve
        java.util.ArrayList<Integer> extrema = new java.util.ArrayList<>();
        for (int i = 1; i < smoothed.length - 1; i++) {
            boolean isPeak = smoothed[i] > smoothed[i - 1] && smoothed[i] > smoothed[i + 1];
            boolean isValley = smoothed[i] < smoothed[i - 1] && smoothed[i] < smoothed[i + 1];
            if (isPeak || isValley) {
                extrema.add(i);
            }
        }

        // Prominence filter: only keep if change from last kept exceeds threshold
        final double minDelta = doMgdl ? 27 : 27; // ~1.5 mmol/l in mg/dl
        java.util.ArrayList<Integer> significant = new java.util.ArrayList<>();
        double lastKeptValue = smoothed.length > 0 ? smoothed[0] : 0;
        for (int idx : extrema) {
            if (Math.abs(smoothed[idx] - lastKeptValue) >= minDelta) {
                significant.add(idx);
                lastKeptValue = smoothed[idx];
            }
        }

        // Build array of curve Y positions for collision detection
        final float[] curveYAtPixel = new float[(int) area.width() + 1];
        java.util.Arrays.fill(curveYAtPixel, Float.NaN);
        for (BgReading r : sorted) {
            float px = timeToX(r.timestamp, sliceStart, sliceEnd, area);
            int pxi = (int) (px - area.left);
            if (pxi >= 0 && pxi < curveYAtPixel.length) {
                curveYAtPixel[pxi] = valueToY(r.calculated_value, yMin, yMax, area);
            }
        }
        // Interpolate gaps
        float lastValid = Float.NaN;
        for (int i = 0; i < curveYAtPixel.length; i++) {
            if (!Float.isNaN(curveYAtPixel[i])) {
                lastValid = curveYAtPixel[i];
            } else if (!Float.isNaN(lastValid)) {
                curveYAtPixel[i] = lastValid;
            }
        }

        // For each significant extremum, find the true peak/valley within ±30 min
        final long searchWindow = 30 * 60 * 1000L;
        Paint peakPaint = new Paint();
        peakPaint.setColor(Color.parseColor("#555555"));
        peakPaint.setTextSize(7f);
        peakPaint.setAntiAlias(true);
        peakPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        final float textHeight = 7f; // approximate text height matching font size

        for (int idx : significant) {
            boolean isPeak = idx > 0 && idx < smoothed.length - 1
                    && smoothed[idx] > smoothed[idx - 1] && smoothed[idx] > smoothed[idx + 1];

            // Search ±30 min for the actual max (if peak) or min (if valley)
            long centerTime = sorted.get(idx).timestamp;
            BgReading bestReading = sorted.get(idx);
            for (BgReading candidate : sorted) {
                if (Math.abs(candidate.timestamp - centerTime) > searchWindow) continue;
                if (isPeak && candidate.calculated_value > bestReading.calculated_value) {
                    bestReading = candidate;
                } else if (!isPeak && candidate.calculated_value < bestReading.calculated_value) {
                    bestReading = candidate;
                }
            }

            float x = timeToX(bestReading.timestamp, sliceStart, sliceEnd, area);
            String label = String.format(Locale.US, doMgdl ? "%.0f" : "%.1f", unitize(bestReading.calculated_value));
            float labelWidth = peakPaint.measureText(label);
            float labelX = x - labelWidth / 2f;
            labelX = Math.max(area.left + 2f, Math.min(area.right - labelWidth - 2f, labelX));

            float curveY = valueToY(bestReading.calculated_value, yMin, yMax, area);
            float labelY = findNonOverlappingY(curveYAtPixel, area, labelX, labelWidth, textHeight, curveY, isPeak);

            canvas.drawText(label, labelX, labelY, peakPaint);
        }

        // Start and end value readouts — placed in the reserved margins outside the curve area
        BgReading firstReading = sorted.get(0);
        BgReading lastReading = sorted.get(sorted.size() - 1);
        for (int ei = 0; ei < 2; ei++) {
            BgReading endpoint = (ei == 0) ? firstReading : lastReading;
            float curveY = valueToY(endpoint.calculated_value, yMin, yMax, area);
            curveY = Math.max(area.top + textHeight, Math.min(area.bottom - 2f, curveY));
            String elabel = String.format(Locale.US, doMgdl ? "%.0f" : "%.1f", unitize(endpoint.calculated_value));
            float elabelWidth = peakPaint.measureText(elabel);

            float elabelX;
            if (ei == 0) {
                // Left margin: right-align just before the curve area
                elabelX = area.left - elabelWidth - 2f;
            } else {
                // Right margin: left-align just after the curve area
                elabelX = area.right + 2f;
            }

            // Vertically centered on the data point
            float labelY = curveY + textHeight / 2f;
            labelY = Math.max(area.top + textHeight, Math.min(area.bottom - 2f, labelY));

            canvas.drawText(elabel, elabelX, labelY, peakPaint);
        }
    }

    // Find a Y position for a label that doesn't overlap the glucose curve
    private float findNonOverlappingY(float[] curveYAtPixel, RectF area, float labelX, float labelWidth, float textHeight, float curveY, boolean preferAbove) {
        float labelY = preferAbove ? curveY - 4f : curveY + textHeight + 4f;

        for (int attempt = 0; attempt < 20; attempt++) {
            // Label bbox with 3pt padding to avoid barely touching
            float bboxTop = labelY - textHeight - 3f;
            float bboxBottom = labelY + 5f;

            boolean overlaps = false;
            int startPx = Math.max(0, (int) (labelX - area.left));
            int endPx = Math.min(curveYAtPixel.length - 1, (int) (labelX + labelWidth - area.left));
            for (int px = startPx; px <= endPx; px++) {
                if (!Float.isNaN(curveYAtPixel[px]) && curveYAtPixel[px] >= bboxTop && curveYAtPixel[px] <= bboxBottom) {
                    overlaps = true;
                    break;
                }
            }

            if (!overlaps) break;

            // Nudge further away from curve
            if (preferAbove) {
                labelY -= 3f;
            } else {
                labelY += 3f;
            }

            // If we've gone out of bounds, try the other direction
            if (labelY - textHeight < area.top || labelY + 2f > area.bottom) {
                preferAbove = !preferAbove;
                labelY = preferAbove ? curveY - 4f : curveY + textHeight + 4f;
            }
        }

        // Final clamp
        return Math.max(area.top + textHeight, Math.min(area.bottom - 2f, labelY));
    }

    private void drawEventMarkers(Canvas canvas, RectF area, List<UserEvent> events, long sliceStart, long sliceEnd) {
        final Paint markerPaint = new Paint();
        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setAntiAlias(true);

        final Paint markerTextPaint = new Paint();
        markerTextPaint.setTextSize(5f);
        markerTextPaint.setAntiAlias(true);
        markerTextPaint.setColor(Color.BLACK);

        final float midY = area.centerY();
        int stackIndex = 0;
        long lastTimestamp = 0;
        final long overlapThreshold = 15 * 60 * 1000;
        final float stackStep = 12f;

        for (UserEvent event : events) {
            if (lastTimestamp > 0 && Math.abs(event.timestamp - lastTimestamp) < overlapThreshold) {
                stackIndex++;
            } else {
                stackIndex = 0;
            }
            lastTimestamp = event.timestamp;

            float yOff = (stackIndex == 0) ? 0 : ((stackIndex % 2 == 1) ? 1 : -1) * ((stackIndex + 1) / 2) * stackStep;
            float x = timeToX(event.timestamp, sliceStart, sliceEnd, area);
            float y = midY + yOff;

            markerPaint.setColor(UserEvent.eventTypeColor(event.eventType));
            canvas.drawCircle(x, y, 4f, markerPaint);

            String label = UserEvent.eventTypeName(event.eventType);
            if (label.length() > 6) label = label.substring(0, 6);
            canvas.drawText(label, x - 8f, y - 6f, markerTextPaint);
        }
    }

    private void drawSidePanel(Canvas canvas, RectF area, List<UserEvent> events, List<Treatments> treatments) {
        float y = area.top;

        if (events != null && !events.isEmpty()) {
            Paint sectionPaint = new Paint(headerPaint);
            sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(context.getString(R.string.pdf_events), area.left, y + 10f, sectionPaint);
            y += 16f;

            for (UserEvent event : events) {
                if (y + 10f > area.bottom) break;
                String time = timeFormat.format(new Date(event.timestamp));
                String name = UserEvent.eventTypeName(event.eventType);
                String desc = (event.description != null && !event.description.isEmpty()) ? ": " + event.description : "";
                String line = time + " " + name + desc;
                if (line.length() > 35) line = line.substring(0, 35) + "...";

                Paint dotPaint = new Paint();
                dotPaint.setColor(UserEvent.eventTypeColor(event.eventType));
                dotPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(area.left + 4f, y + 5f, 3f, dotPaint);

                canvas.drawText(line, area.left + 12f, y + 8f, bodyPaint);
                y += 11f;
            }
            y += 5f;
        }

        if (treatments != null && !treatments.isEmpty()) {
            Paint sectionPaint = new Paint(headerPaint);
            sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(context.getString(R.string.pdf_treatments), area.left, y + 10f, sectionPaint);
            y += 16f;

            for (Treatments t : treatments) {
                if (y + 10f > area.bottom) break;
                if (t.insulin == 0 && t.carbs == 0 && (t.notes == null || t.notes.isEmpty())) continue;

                String time = timeFormat.format(new Date(t.timestamp));
                StringBuilder sb = new StringBuilder(time);
                if (t.insulin > 0) sb.append(" ").append(String.format(Locale.US, "%.1fu", t.insulin));
                if (t.carbs > 0) sb.append(" ").append(String.format(Locale.US, "%.0fg", t.carbs));
                if (t.notes != null && !t.notes.isEmpty()) {
                    String notes = t.notes;
                    if (notes.length() > 20) notes = notes.substring(0, 20) + "...";
                    sb.append(" ").append(notes);
                }
                String line = sb.toString();
                if (line.length() > 35) line = line.substring(0, 35) + "...";

                canvas.drawText(line, area.left, y + 8f, bodyPaint);
                y += 11f;
            }
        }
    }

    private void drawFooter(Canvas canvas, float footerY) {
        final String footerText = context.getString(R.string.pdf_generated_by) + "  \u2022  " + dateFormat.format(new Date());
        canvas.drawText(footerText, MARGIN, footerY + 12f, smallPaint);
    }

    private float valueToY(double valueMgdl, double yMin, double yMax, RectF area) {
        double fraction = (valueMgdl - yMin) / (yMax - yMin);
        return (float) (area.bottom - fraction * area.height());
    }

    private float timeToX(long timestamp, long sliceStart, long sliceEnd, RectF area) {
        double fraction = (double) (timestamp - sliceStart) / (sliceEnd - sliceStart);
        return (float) (area.left + fraction * area.width());
    }

    private double unitize(double mgdl) {
        return doMgdl ? mgdl : mgdl * Constants.MGDL_TO_MMOLL;
    }
}
