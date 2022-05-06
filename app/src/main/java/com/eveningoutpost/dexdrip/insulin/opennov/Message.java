package com.eveningoutpost.dexdrip.insulin.opennov;


import static com.eveningoutpost.dexdrip.insulin.opennov.mt.Apdu.ApduType.AareApdu;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.Apdu.ApduType.PrstApdu;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.Apdu.ApduType.RlrqApdu;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.EventReport.MDC_NOTI_CONFIG;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.EventReport.MDC_NOTI_SEGMENT_DATA;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.ARequest;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.AResponse;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Apdu;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.ArgumentsSimple;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Attribute;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.ConfirmedAction;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.DataApdu;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.EventReport;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.EventRequest;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.IdModel;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.RelativeTime;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.SegmentInfoList;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.Specification;
import com.eveningoutpost.dexdrip.insulin.opennov.mt.TrigSegmDataXfer;

import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov message processing
 */

@RequiredArgsConstructor
public class Message extends BaseMessage {

    private static final int EVENT_REPORT_CHOSEN = 0x0100;
    private static final int CONFIRMED_EVENT_REPORT_CHOSEN = 0x0101;
    private static final int SCONFIRMED_EVENT_REPORT_CHOSEN = 0x0201;
    private static final int GET_CHOSEN = 0x0203;
    private static final int SGET_CHOSEN = 0x0103;
    private static final int CONFIRMED_ACTION = 0x0107;
    private static final int CONFIRMED_ACTION_CHOSEN = 0x0207;
    private static final int MDC_ACT_SEG_GET_INFO = 0x0C0D;
    private static final int MDC_ACT_SEG_TRIG_XFER = 0x0C1C;

    private static final int STORE_HANDLE = 0x100;

    @Getter
    private boolean valid = false;
    @Getter
    private int invokeId = -1;
    @Getter
    private boolean closed = false;
    @Getter
    private final OpContext context;
    @Getter
    private int length = -1;


    public static Message parse(final OpContext context, final byte[] payload) {
        if (payload == null) {
            return null;
        }

        val m = new Message(context);
        m.length = payload.length;

        if (m.length < 4) {
            return m;
        }

        val buffer = ByteBuffer.wrap(payload);
        val apdu = Apdu.parse(buffer);

        if (apdu == null) return null;
        m.getContext().apdu = apdu;

        if (d) log("Choice length: " + apdu.choiceLength);

        switch (apdu.apduType) {

            case AarqApdu:
                context.aRequest = ARequest.parse(buffer);
                if (d) log(context.aRequest.toJson());
                break;

            case AareApdu:
                log(AResponse.parse(buffer).toJson());
                break;

            case RlrqApdu:
                log("Release request");
                break;

            case RlreApdu:
                m.closed = true;
                break;

            case AbrtApdu:
                error("Received error reply");
                break;

            case PrstApdu:

                val dpdu = DataApdu.parse(buffer);
                m.invokeId = dpdu.invokeId;
                context.invokeId = m.invokeId;

                log("olen: " + dpdu.olen + " invokeid:" + m.invokeId + " dchoice:" + Integer.toHexString(dpdu.dchoice) + " dlen:" + dpdu.dlen);

                switch (dpdu.dchoice) {
                    case CONFIRMED_ACTION_CHOSEN:    // confirmed action

                        val handle = getUnsignedShort(buffer);
                        val actionType = getUnsignedShort(buffer);
                        val actionLen = getUnsignedShort(buffer);
                        log("handle: " + Integer.toHexString(handle) + " atype:" + Integer.toHexString(actionType));

                        switch (actionType) {
                            case MDC_ACT_SEG_GET_INFO:
                                context.segmentInfoList = SegmentInfoList.parse(buffer);;
                                break;

                            case MDC_ACT_SEG_TRIG_XFER:
                                context.trigSegmDataXfer = TrigSegmDataXfer.parse(buffer);
                                break;

                            default:
                                log("Unknown action type: " + actionType);
                        }
                        break;


                    case CONFIRMED_EVENT_REPORT_CHOSEN:
                        val er = EventReport.parse(buffer);
                        if (er != null) {
                            for (val ds : er.doses) {
                                if (d) log("dose: " + ds.toJson() + " " + JoH.dateTimeText(ds.absoluteTime) + " valid: " + ds.isValid());
                            }
                            context.eventReport = er;
                        }
                        break;

                    case SCONFIRMED_EVENT_REPORT_CHOSEN:
                        val e = EventRequest.parse(buffer);
                        log("SConfirmed: " + e.toJson());
                        context.eventRequest = e;
                        break;

                    case GET_CHOSEN:
                    case SGET_CHOSEN:
                        val handlex = getUnsignedShort(buffer);
                        val count = getUnsignedShort(buffer);
                        val len = getUnsignedShort(buffer);
                        if (d) log("gchosen: " + count + " Slen:" + len + " :: handle: " + handlex);
                        for (int i = 0; i < count; i++) {
                            val a = Attribute.parse(buffer);
                            if (d) log(a.toJson());
                            switch (a.atype) {
                                case MDC_ATTR_ID_PROD_SPECN:
                                    context.specification = Specification.parse(a.bytes);
                                    log(context.specification.toJson());
                                    break;
                                case MDC_ATTR_TIME_REL:
                                    context.relativeTime = RelativeTime.parse(a.bytes);
                                    log(context.relativeTime.toJson());
                                    break;
                                case MDC_ATTR_ID_MODEL:
                                    context.model = IdModel.parse(a.bytes);
                                    log(context.model.toJson());
                                    break;
                            }
                        }
                        break;

                    case CONFIRMED_ACTION:
                        val ca = ConfirmedAction.parse(buffer);
                        log("Confirmed action: handle:" + ca.handle + " type:" + ca.type + " bytes:" + HexDump.toHexString(ca.bytes));
                        break;

                    default:
                        log("Unknown dchoice: " + Integer.toHexString(dpdu.dchoice));
                }
                break;

            default:
                error("Unhandled Apdu type: " + apdu.apduType);
        }

        return m;
    }

    public static Message parse(final byte[] payload) {
        return parse(new OpContext(), payload);

    }

    public byte[] getAResponse() {
        val apdu = Apdu.builder()
                .at(AareApdu.getValue())
                .choicePayload(new AResponse(context.aRequest)
                        .encode())
                .build();
        return apdu.encode();
    }


    public byte[] getAcceptConfig() {
        if (context.hasConfiguration()) {
            return Apdu.builder()
                    .at(PrstApdu.getValue())
                    .choicePayload(DataApdu.builder()
                            .invokeId(context.invokeId)
                            .dataPayload(EventRequest.builder()
                                    .currentTime(0)
                                    .type(MDC_NOTI_CONFIG)
                                    .replyLen(4)
                                    .reportId(context
                                            .getConfiguration()
                                            .getId())
                                    .reportResult(0)
                                    .build().encode())
                            .dchoice(SCONFIRMED_EVENT_REPORT_CHOSEN)
                            .build().encode())
                    .build().encode();
        } else {
            return null;
        }
    }

    public byte[] getAskInformation() {
        if (context.hasConfiguration()) {
            return Apdu.builder()
                    .at(PrstApdu.getValue())
                    .choicePayload(DataApdu.builder()
                            .invokeId(context.invokeId)
                            .dchoice(SGET_CHOSEN)
                            .dataPayload(ArgumentsSimple.builder()
                                    .handle(context.eventReport.handle)
                                    .build().encode())
                            .build().encode())
                    .build().encode();
        } else {
            return null;
        }
    }

    public byte[] getConfirmedAction() {
        if (context.hasConfiguration()) {
            return Apdu.builder()
                    .at(PrstApdu.getValue())
                    .choicePayload(DataApdu.builder()
                            .invokeId(context.invokeId)
                            .dchoice(CONFIRMED_ACTION)
                            .dataPayload(ConfirmedAction.builder()
                                    .handle(STORE_HANDLE)
                                    .type(MDC_ACT_SEG_GET_INFO)
                                    .build()
                                    .allSegments()
                                    .encode())
                            .build().encode())
                    .build().encode();
        } else {
            return null;
        }
    }

    public byte[] getXferAction(final int segment) {
        if (context.hasConfiguration()) {
            return Apdu.builder()
                    .at(PrstApdu.getValue())
                    .choicePayload(DataApdu.builder()
                            .invokeId(context.invokeId)
                            .dchoice(CONFIRMED_ACTION)
                            .dataPayload(ConfirmedAction.builder()
                                    .handle(STORE_HANDLE)
                                    .type(MDC_ACT_SEG_TRIG_XFER)
                                    .build()
                                    .segment(segment)
                                    .encode())
                            .build().encode())
                    .build().encode();
        } else {
            return null;
        }
    }

    public byte[] getConfirmedXfer(final int instance, final int index, final int count, final boolean lastBlock, final boolean specifyBlock) {
        if (d) log("getConfirmedXfer: i:" + index + " c:" + count + " last:" + lastBlock);
        if (context.hasConfiguration()) {
            val er = EventRequest.builder()
                    .handle(STORE_HANDLE)
                    .currentTime(-1)
                    .replyLen(12)
                    .type(MDC_NOTI_SEGMENT_DATA)
                    .instance(instance)
                    .index(index)
                    .count(count)
                    .confirmed(true)
                    .build();

            if (specifyBlock) {
                if (!lastBlock) {
                    if (index == 0) {
                        er.firstBlock();
                    } else {
                        er.middleBlock();
                    }
                } else {
                    er.lastBlock();
                }
            } else {
                er.middleBlock();
            }
            return Apdu.builder()
                    .at(PrstApdu.getValue())
                    .choicePayload(DataApdu.builder()
                            .invokeId(invokeId)
                            .dataPayload(er.encode())
                            .dchoice(SCONFIRMED_EVENT_REPORT_CHOSEN)
                            .build().encode())
                    .build().encode();
        } else {
            return null;
        }
    }

    public byte[] getCloseDown() {
        return Apdu.builder()
                .at(RlrqApdu.getValue())
                .choicePayload(new byte[2])
                .build().encode();
    }

}
