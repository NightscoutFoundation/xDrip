package com.eveningoutpost.dexdrip.insulin.opennov;

import static com.eveningoutpost.dexdrip.HexTestTools.tolerantHexStringToByteArray;
import static com.eveningoutpost.dexdrip.insulin.opennov.mt.ApoepElement.SYS_TYPE_AGENT;
import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.HexTestTools;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import lombok.val;

public class MessageTest extends RobolectricTestWithConfig {

    private final boolean D = false;

    private static final String A_INFO =
            "e7 00 00 0e 00 0c 00 00 01 03 00 06 00 00 00 00" +
                    "00 00";

    private static final String T_XFER =
            "e7 00 00 10 00 0e 00 02 01 07 00 08 01 00 0c 1c " +
                    "00 02 00 10";

    private static final String D_INFO =
            "e7 00 00 d3 00 d1 00 00 02 03 00 cb 00 00 00 08 " +
                    "00 c5 09 84 00 0a 00 08 00 14 65 00 45 07 d7 21 " +
                    "09 8f 00 04 00 01 c6 13 0a 45 00 10 20 00 1f 00 " +
                    "ff ff ff ff 00 00 1f 40 00 00 00 00 09 2d 00 4b " +
                    "00 04 00 47 00 01 00 01 00 06 45 47 44 4f 32 55 " +
                    "00 02 00 01 00 20 44 32 30 31 33 30 32 36 33 32 " +
                    "30 30 30 30 30 20 44 32 30 31 33 30 32 36 33 32 " +
                    "30 30 30 30 30 20 00 03 00 01 00 01 00 00 04 00 " +
                    "01 00 08 30 31 2e 30 38 2e 30 30 0a 5a 00 08 00 " +
                    "01 00 04 10 48 00 01 09 28 00 1c 00 10 4e 6f 76 " +
                    "6f 20 4e 6f 72 64 69 73 6b 20 41 2f 53 00 08 4e " +
                    "6f 76 6f 50 65 6e 00 0a 44 00 02 40 0a 0a 4b 00 " +
                    "16 00 02 00 12 02 01 00 08 04 00 00 01 00 02 a0 " +
                    "48 02 02 00 02 00 00 ";

    private static final String S_INFO =
            "e7 00 00 7c 00 7a 00 01 02 07 00 74 01 00 0c 0d " +
                    "00 6e 00 01 00 6a 00 10 00 06 00 64 09 22 00 02 " +
                    "00 10 0a 4e 00 36 40 00 00 03 00 30 00 06 00 82 " +
                    "34 01 00 02 00 01 00 04 0a 56 00 04 00 05 00 82 " +
                    "34 02 00 03 00 01 00 04 0a 66 00 02 00 06 00 82 " +
                    "f0 00 00 04 00 01 00 04 0a 66 00 02 09 53 00 02 " +
                    "00 00 0a 58 00 0a 00 08 44 6f 73 65 20 4c 6f 67 " +
                    "09 7b 00 04 00 00 00 03 0a 64 00 04 00 02 71 00 ";

    private static final String A_REQUEST =
            "e2 00 00 32 80 00 00 00 00 01 00 2a 50 79 00 26 " +
                    "80 00 00 00 80 00 80 00 00 00 00 00 00 00 00 80 " +
                    "00 00 00 08 00 14 65 00 45 07 b9 51 40 0a 00 01 " +
                    "01 00 00 00 00 00 ";

    private static final String A_RESPONSE =
            "e3 00 00 2c 00 03 50 79 00 26 80 00 00 00 80 00 " +
                    "80 00 00 00 00 00 00 00 80 00 00 00 00 08 00 14 " +
                    "65 00 45 07 b9 51 00 00 00 00 00 00 00 00 00 00 ";

    private static final String CA_SEG =
            "e7 00 00 14 00 12 00 01 01 07 00 0c 01 00 0c 0d " +
                    "00 06 00 01 00 02 00 00  ";

    private static final String C_REPORT =
            "e7 00 00 c4 00 c2 00 00 01 01 00 bc 00 00 00 00  " +
                    "00 00 0d 1c 00 b2 40 0a 00 04 00 ac 00 3d 01 00  " +
                    "00 08 00 38 0a 4d 00 02 08 00 09 43 00 02 00 00 " +
                    "09 41 00 04 00 00 03 20 09 53 00 02 00 00 0a 57  " +
                    "00 04 00 02 50 4d 09 51 00 02 00 01 0a 63 00 04  " +
                    "00 00 00 00 09 44 00 04 00 00 00 02 00 06 00 02  " +
                    "00 04 00 20 09 2f 00 04 00 82 34 01 0a 46 00 02  " +
                    "f0 40 09 96 00 02 15 60 0a 55 00 08 00 01 00 04  " +
                    "0a 56 00 04 00 05 00 03 00 03 00 1a 09 2f 00 04  " +
                    "00 82 34 02 0a 46 00 02 f0 40 0a 55 00 08 00 01  " +
                    "00 04 0a 66 00 02 00 06 00 04 00 03 00 1a 09 2f  " +
                    "00 04 00 82 f0 00 0a 46 00 02 f0 40 0a 55 00 08  " +
                    "00 01 00 04 0a 66 00 02  ";

    private static final String C_ACCEPT =
            "e7 00 00 16 00 14 00 00 02 01 00 0e 00 00 00 00 " +
                    "00 00 0d 1c 00 04 40 0a 00 00  ";

    private Message parseFromHexString(final String hex) {
        return Message.parse(tolerantHexStringToByteArray(hex));
    }

    private Message parseFromHexString(final Message msg, final String hex) {
        return Message.parse(msg.getContext(), tolerantHexStringToByteArray(hex));
    }

    @Test
    public void parseTest1() {
        assertWithMessage("null should be null").that(Message.parse(null)).isNull(); // should be null
        assertWithMessage("invalid apdut should be null").that(Message.parse(new byte[4])).isNull(); // should be null
    }

    @Test
    public void testSegmentInfo() {  // segment info

        val msg = parseFromHexString(S_INFO);
        val sil = msg.getContext().segmentInfoList;

        assertWithMessage("sil list item not null").that(sil.getItems().get(0)).isNotNull(); // should not be null
        assertWithMessage("sil list scount valid").that(sil.getScount()).isEqualTo(1);
        assertWithMessage("sil list size also valid").that(sil.getItems().size()).isEqualTo(1);
        assertWithMessage("sil list item inst valid").that(sil.getItems().get(0).getInstnum()).isEqualTo(16); // should be 16
        assertWithMessage("sil is typical").that(sil.isTypical()).isTrue();

        if (D) System.out.println("DEBUG: " + msg.getContext().segmentInfoList.toJson());
    }

    @Test
    public void trigSegTest() {
        val y = parseFromHexString(
                "e7 00 00 12 00 10 00 02 02 07 00 0a 01 00 0c 1c " +
                        "00 04 00 10 00 00 ");
        if (D) System.out.println(y);
    }

    @Test
    public void segmentXferTest() {  // segment xfer
        val y = parseFromHexString(
                "e7 00 00 44 00 42 80 01 01 01 00 3c 01 00 00 01 " +
                        "c6 13 0d 21 00 32 00 10 00 00 00 00 00 00 00 03 " +
                        "c0 00 00 24 00 01 c5 d1 ff 00 00 1e 08 00 00 00 " +
                        "00 00 01 4b ff 00 00 14 08 00 00 00 00 00 00 00 " +
                        "00 7f ff ff 04 00 00 08 ");

        assertWithMessage("unit check").that(y.getContext().eventReport.doses.get(0).units).isEqualTo(3.0d);
    }

    @Test
    public void ackTest1() {
        val m = parseFromHexString(
                "e7 00 00 1e 00 1c 80 01 02 01 00 16 01 00 ff ff " +
                        "ff ff 0d 21 00 0c 00 10 00 00 00 00 00 00 00 02 " +
                        "00 80 ");
    }

    @Test
    public void ackTest2() {
        val m = parseFromHexString(
                "e7 00 00 1e 00 1c 80 01 02 01 00 16 01 00 ff ff" +
                        " ff ff 0d 21 00 0c 00 10 00 00 00 00 00 00 00 12 " +
                        "00 80 ");
    }

    @Test
    public void deviceParseTest() {          // device info
        val result = parseFromHexString(D_INFO);
        assertWithMessage("Invoke id").that(result.getInvokeId()).isEqualTo(0);
        assertWithMessage("Serial parse").that(result.getContext().specification.getSerial()).isEqualTo("EGDO2U");
        assertWithMessage("Software parse").that(result.getContext().specification.getSoftWareRevision()).isEqualTo("01.08.00");
        assertWithMessage("Part parse").that(result.getContext().specification.getPartNumber()).isEqualTo("D20130263200000 D20130263200000 ");
        assertWithMessage("Clock parse").that(result.getContext().relativeTime.getRelativeTime()).isEqualTo(116243L);
        assertWithMessage("Model parse").that(result.getContext().model.getModel()).isEqualTo("Novo Nordisk A/S NovoPen");
    }

    @Test
    public void aRequestTest() { //a request
        val result = parseFromHexString(A_REQUEST);
        assertWithMessage("check type").that(result.getContext().aRequest.getApoep().systemType).isEqualTo(SYS_TYPE_AGENT);
        assertWithMessage("check version").that(result.getContext().aRequest.getApoep().version).isEqualTo(2147483648L);
        assertWithMessage("check nomenclature").that(result.getContext().aRequest.getApoep().nomenclature).isEqualTo(2147483648L);
        assertWithMessage("check encoding").that(result.getContext().aRequest.getApoep().encoding).isEqualTo(32768);
        assertWithMessage("check olist count").that(result.getContext().aRequest.getApoep().olistCount).isEqualTo(0);
        assertWithMessage("check olist len").that(result.getContext().aRequest.getApoep().olistLen).isEqualTo(0);
    }

    @Test
    public void aResponseTest() { //a response
        val result = parseFromHexString(A_REQUEST);
        val response = result.getAResponse();
        if (D) System.out.println(HexDump.dumpHexString(response));
        assertWithMessage("A response vs specimen").that(response).isEqualTo(tolerantHexStringToByteArray(A_RESPONSE));
    }

    @Test
    public void testConfigReport() {      // Configuration report
        val result = parseFromHexString(C_REPORT);
        val configuration = result.getContext().getConfiguration();
        assertWithMessage("Configuration available").that(configuration).isNotNull();
        assertWithMessage("As expected").that(configuration.isAsExpected()).isTrue();
        assertWithMessage("Total capacity").that(configuration.getTotalStorageCapacity()).isEqualTo(800);
    }

    @Test
    public void testAcceptConfigEncoding() {
        val reply = parseFromHexString(C_REPORT).getAcceptConfig();
        assertWithMessage("C confirm vs specimen").that(reply).isEqualTo(tolerantHexStringToByteArray(C_ACCEPT));
    }

    @Test
    public void testAcceptConfigDecoding() {  // accept config
        val result = parseFromHexString(C_ACCEPT);
        val er = result.getContext().eventRequest;
        assertWithMessage("current time valid").that(er.currentTime).isEqualTo(0);
        assertWithMessage("handle valid").that(er.handle).isEqualTo(0);
        assertWithMessage("type valid").that(er.type).isEqualTo(3356);
        assertWithMessage("reply len valid").that(er.replyLen).isEqualTo(4);
        assertWithMessage("report id valid").that(er.reportId).isEqualTo(16394);
        assertWithMessage("report result valid").that(er.reportResult).isEqualTo(0);
    }

    @Test
    public void testAskInformation() {
        val result = parseFromHexString(C_REPORT);
        assertWithMessage("").that(result.getAskInformation()).isEqualTo(tolerantHexStringToByteArray(A_INFO));
    }

    @Test
    public void testAskInformation2() {
        val result = parseFromHexString(C_REPORT);
        if (D) System.out.println(HexDump.dumpHexString(result.getAskInformation()));
    }

    @Test
    public void parseSegmentInfo() {
        val res = parseFromHexString(C_REPORT);
        val msg = parseFromHexString(res, CA_SEG);
        assertWithMessage("").that(msg.getConfirmedAction()).isEqualTo(tolerantHexStringToByteArray(CA_SEG));
    }

    @Test
    public void segTest() {
        val result = parseFromHexString(C_REPORT);
        result.getContext().invokeId = 2;
        val b = result.getXferAction(16);
        assertWithMessage("matches").that(b).isEqualTo(HexTestTools.tolerantHexStringToByteArray(T_XFER));
    }

    @Test
    public void parseTest11() {
        val result = parseFromHexString(T_XFER);
    }

    @Test
    public void parseTest12() {
        val result = parseFromHexString(
                "e7 00 00 12 00 10 00 02 02 07 00 0a 01 00 0c 1c " +
                        "00 04 00 10 00 00"
        );
    }

    @Test
    public void parseTest14() { // aresponse
        val r = parseFromHexString(A_RESPONSE);
    }

}