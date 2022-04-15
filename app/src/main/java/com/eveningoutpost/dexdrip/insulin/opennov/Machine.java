package com.eveningoutpost.dexdrip.insulin.opennov;

import static com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage.d;
import static com.eveningoutpost.dexdrip.insulin.opennov.FSA.Action.WRITE_READ;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.insulin.opennov.data.ICompleted;

import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov state machine
 */

@RequiredArgsConstructor
public class Machine {

    private static final String TAG = "OpenNov";
    private static final int MAX_REQUESTS = 100;
    private final boolean loadEverything = Options.loadEverything();

    private int requestCounter = 0;
    private long currentSegmentCount = -1;
    private int currentSegment = -1;
    private State state = State.AWAIT_ASSOCIATION_REQ;
    private final OpContext context = new OpContext();
    private boolean lastSuccessCache = false;

    private final ICompleted iCompleted;

    public enum State {
        AWAIT_ASSOCIATION_REQ,
        AWAIT_CONFIGURATION,
        ASK_INFORMATION,
        AWAIT_INFORMATION,
        AWAIT_STORAGE_INFO,
        AWAIT_XFER_CONFIRM,
        AWAIT_LOG_DATA,
        AWAIT_CLOSE_DOWN,
        PROFIT;

        private static final State[] values = values();

        public State next() {
            val newState = values[(this.ordinal() + 1) % values.length];
            UserError.Log.d(TAG, "Asking for next state " + this + " -> " + newState);
            return newState;
        }
    }

    FSA processPayload(final byte[] payload) {
        if (d)
            UserError.Log.d(TAG, "state: " + state + " processing payload: " + HexDump.dumpHexString(payload));
        if (payload != null) {
            val msg = Message.parse(context, payload);
            if (!msg.getContext().isError()) {
                return processState(msg);
            } else {
                UserError.Log.e(TAG, "Got error response parsing message");
                return FSA.empty();
            }
        }
        return FSA.empty();
    }

    FSA processState(final Message msg) {
        UserError.Log.d(TAG, "Processing state: " + state);

        if (msg.getContext().wantsRelease()) {
            UserError.Log.d(TAG, "Remote end requests release");
            return doCloseDown(msg);
        }

        switch (state) {

            case AWAIT_ASSOCIATION_REQ:
                if (context.aRequest != null && context.aRequest.valid()) {
                    state = state.next();
                    return new FSA(WRITE_READ, msg.getAResponse());
                }
                break;

            case AWAIT_CONFIGURATION:
                if (context.getConfiguration() != null) {
                    if (context.configuration.isAsExpected()) {
                        if (context.configuration.getNumberOfSegments() > 1) {
                            UserError.Log.e(TAG, "Multiple segments @ " + context.configuration.getNumberOfSegments());
                        }
                        state = state.next();
                        return new FSA(WRITE_READ, msg.getAcceptConfig());
                    } else {
                        UserError.Log.d(TAG, "Configuration not as expected");
                    }

                } else {
                    UserError.Log.d(TAG, "Configuration not found");
                }
                break;

            case ASK_INFORMATION:
                UserError.Log.d(TAG, "Ask information");
                state = state.next();
                return new FSA(WRITE_READ, msg.getAskInformation());

            case AWAIT_INFORMATION:
                UserError.Log.d(TAG, "Await information");
                if (context.specification == null) {
                    UserError.Log.d(TAG, "Failed to acquire specification - trying again");
                    return new FSA(WRITE_READ, msg.getAskInformation());
                }
                lastSuccessCache = wasLastReadSuccess();
                state = state.next();
                return new FSA(WRITE_READ, msg.getConfirmedAction());

            case AWAIT_STORAGE_INFO:
                UserError.Log.d(TAG, "Await storage information");
                return handleNextSegment(msg);

            case AWAIT_XFER_CONFIRM:
                UserError.Log.d(TAG, "Await transfer information");
                if (msg.getLength() != 0) {
                    if (context.trigSegmDataXfer != null
                            && context.trigSegmDataXfer.isOkay()) {
                        context.trigSegmDataXfer = null; // clear for next
                        state = state.next();
                    } else {
                        UserError.Log.d(TAG, "Transfer information not right - trying again");
                        return handleNextSegment(msg);
                    }
                } else {
                    UserError.Log.d(TAG, "Didn't get anything - trying again in state: " + state);
                }
                return FSA.writeNull();

            case AWAIT_LOG_DATA:
                UserError.Log.d(TAG, "Await Log data: msize:" + msg.getLength());
                if (msg.getLength() == 0) return FSA.writeNull();
                if (context.eventReport.doses.size() > 0) {
                    setLastReadSuccess(false); // started receiving data
                }
                val count = iCompleted.receiveFinalData(msg);
                if (count == 0 && !loadEverything && lastSuccessCache) {
                    UserError.Log.d(TAG, "No new data so requesting close");
                    setLastReadSuccess(true); // completed receiving data
                    return doCloseDown(msg);
                }
                val er = msg.getContext().eventReport;
                val tsil = msg.getContext().segmentInfoList;
                if (currentSegmentCount == (er.index + er.count)) {
                    UserError.Log.d(TAG, "Segment " + er.instance + " complete @ " + currentSegmentCount);
                    tsil.markProcessed(er.instance);
                    if (!tsil.hasUnprocessed()) {
                        UserError.Log.d(TAG, "All segments processed");
                        setLastReadSuccess(true); // completed receiving data
                    } else {
                        UserError.Log.e(TAG, "Segments remain unprocessed");
                        return handleNextSegment(msg);
                    }
                }
                return FSA.writeRead(msg.getConfirmedXfer(er.instance, (int) er.index, (int) er.count, false, false));

            case AWAIT_CLOSE_DOWN:
                if (msg.isClosed()) {
                    UserError.Log.d(TAG, "Closed down successfully");
                    return FSA.empty();
                } else {
                    UserError.Log.e(TAG, "Didn't close down this time");
                    return FSA.empty();
                }

            default:
                UserError.Log.e(TAG, "Unknown state: " + state);
        }

        return FSA.empty();
    }

    private FSA doCloseDown(final Message msg) {
        state = State.AWAIT_CLOSE_DOWN;
        return FSA.writeRead(msg.getCloseDown());
    }

    FSA handleNextSegment(final Message msg) {
        requestCounter++;
        if (requestCounter > MAX_REQUESTS) {
            UserError.Log.e(TAG, "Exceeded max requests");
        } else {
            val sil = msg.getContext().segmentInfoList;
            if (sil.isTypical()) {
                currentSegment = sil.getNextUnprocessedId();
                state = State.AWAIT_XFER_CONFIRM;
                if (currentSegment >= 0) {
                    UserError.Log.d(TAG, "Requesting next segment: " + currentSegment);
                    currentSegmentCount = sil.getNextUnprocessedCount();
                    return FSA.writeRead(msg.getXferAction(currentSegment));
                } else {
                    // no more segments
                    UserError.Log.d(TAG, "No more segments");
                    return doCloseDown(msg);
                }
            } else {
                UserError.Log.wtf(TAG, "Non typical segments - please contact developers");
            }
        }
        return FSA.empty();
    }

    private static final String READ_SUCCESS = "READ_SUCCESS_";

    private void setLastReadSuccess(final boolean result) {
        if (context.specification == null) {
            UserError.Log.wtf(TAG, "Specification null when trying to set success");
            return;
        }
        val serial = context.specification.getSerial();
        if (serial == null || serial.length() < 4) {
            UserError.Log.wtf(TAG, "Invalid serial when trying to set success");
            return;
        }
        PersistentStore.setBoolean(READ_SUCCESS + serial, result);
    }

    private boolean wasLastReadSuccess() {
        if (context.specification == null) {
            UserError.Log.wtf(TAG, "Specification null when trying to check success");
            return false;
        }
        val serial = context.specification.getSerial();
        if (serial == null || serial.length() < 4) {
            UserError.Log.wtf(TAG, "Invalid serial when trying to check success");
            return false;
        }
        return PersistentStore.getBoolean(READ_SUCCESS + serial, false);
    }

    public static void deleteSuccessInfo(final String serial) {
        PersistentStore.removeItem(READ_SUCCESS + serial);
    }

}
