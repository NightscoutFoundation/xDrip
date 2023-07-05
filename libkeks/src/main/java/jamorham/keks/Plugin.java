package jamorham.keks;


import static java.lang.System.arraycopy;
import static jamorham.keks.Config.Get.ALICE;
import static jamorham.keks.Config.Get.BOB;
import static jamorham.keks.Config.Get.GETDATA;
import static jamorham.keks.Config.Get.GETDATA2;
import static jamorham.keks.Config.Get.KEYCMD;
import static jamorham.keks.Config.Get.SPARAM;
import static jamorham.keks.message.CertInfoTxMessage.expectMyCert1;
import static jamorham.keks.message.CertInfoTxMessage.expectMyCert2;
import static jamorham.keks.util.Util.arrayAppend;
import static jamorham.keks.util.Util.bytesToHex;

import com.eveningoutpost.dexdrip.plugin.IPluginDA;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashSet;

import jamorham.keks.message.AuthChallengeTxMessage;
import jamorham.keks.message.AuthRequestTxMessage2;
import jamorham.keks.message.AuthStatusRxMessage;
import jamorham.keks.message.CertInfoRxMessage;
import jamorham.keks.message.SignChallengeTxMessage;
import jamorham.keks.util.Log;
import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * <p>
 * KEKS Plugin
 */

@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class Plugin implements IPluginDA {

    private static final HashSet<Integer> dontClearAccumulator = new HashSet<>();
    private static final String TAG = "KEKS-Plugin";
    private static final byte[] EMPTY = new byte[0];
    private static final byte[] TIME_EXTENDED = Config.Get.TIME_EXTENDED.bytes;
    private static final byte[] TIME_EXTENDED2 = Config.Get.TIME_EXTENDED2.bytes;
    private static final byte[] TIME_EXTENDED3 = Config.Get.TIME_EXTENDED3.bytes;
    private static final byte[] CHALLENGE_OUT = Config.Get.CHALLENGE_OUT.bytes;

    private static final int Init = 0;
    private static final int Unknown = 370018;
    private static final int Scanning = 448130;
    private static final int Connecting = 432753;
    private static final int RoundStart = 789035;
    private static final int Pairing = 804167;
    private static final int Round1 = 674667;
    private static final int Round2 = 189857;
    private static final int Round3 = 588648;
    private static final int BondFailure = 162087;
    private static final int RequestAuth = 125320;
    private static final int ChallengeReply = 766662;
    private static final int SendCertificate0 = 975913;
    private static final int SendCertificate1 = 694702;
    private static final int SendCertificate2 = 230995;
    private static final int SendCertificate1out = 842681;
    private static final int SendCertificate2out = 558830;
    private static final int SendKeyChallenge = 486262;
    private static final int SendKeyChallengeOut = 327604;
    private static final int GetData = 734275;
    private static final int GetData2 = 199434;

    private static String stateToName(final int state) {
        switch (state) {
            case Init:
                return "Init";
            case Unknown:
                return "Unknown";
            case Scanning:
                return "Scanning";
            case Connecting:
                return "Connecting";
            case RoundStart:
                return "Round Start";
            case Round1:
                return "Round 1";
            case Round2:
                return "Round 2";
            case Round3:
                return "Round 3";
            case BondFailure:
                return "Bond Failure";
            case Pairing:
                return "Pairing";
            case RequestAuth:
                return "Request Auth";
            case SendCertificate0:
                return "Send Certificate 0";
            case SendCertificate1:
                return "Send Certificate 1";
            case SendCertificate2:
                return "Send Certificate 2";
            case SendCertificate1out:
                return "Send Certificate 1 Out";
            case SendCertificate2out:
                return "Send Certificate 2 Out";
            case SendKeyChallenge:
                return "Send Key Challenge";
            case SendKeyChallengeOut:
                return "Send Key Challenge Out";
            case ChallengeReply:
                return "Challenge Reply";
            case GetData:
                return "Get Data";
            case GetData2:
                return "Get Data 2";

            default:
                return "Error " + state;
        }
    }

    private static volatile Plugin instance = null;
    private volatile AuthRequestTxMessage2 lastAuthTx2;
    private volatile int state = 0;
    private volatile byte[] accumulator = new byte[0];
    private volatile byte[] keyToTestAgainst = null;

    static {
        dontClearAccumulator.add(SendCertificate1);
        dontClearAccumulator.add(SendCertificate1out);
        dontClearAccumulator.add(SendCertificate2);
        dontClearAccumulator.add(SendCertificate2out);
    }

    public Plugin(String password) {
        context.password = password;
        context.getPasswordBytes();
    }

    public Plugin() {
    }

    public synchronized static Plugin getInstance(String password) {
        if (instance == null || !instance.context.password.equals(password)) {
            Log.d(TAG, "Creating new instance");
            instance = new Plugin(password);
        }
        return instance;
    }

    @Getter
    private final Context context = new Context();

    {
        context.alice = ALICE.bytes;
        context.bob = BOB.bytes;
        context.keyA = new KeyPair();
        context.KeyB = new KeyPair();
    }

    private synchronized void changeState(int newState) {
        Log.d(TAG, "Changing state: " + stateToName(state) + " - > " + stateToName(newState));
        if (!dontClearAccumulator.contains(newState)) {
            accumulator = EMPTY;
        }
        state = newState;
    }

    @Override
    public void amConnected() {
        context.resetIfNotReady();
        changeState(RoundStart);
    }

    @Override
    public boolean bondNow(final byte[] packet) {
        return Arrays.equals(packet, TIME_EXTENDED)
                || Arrays.equals(packet, TIME_EXTENDED2)
                || Arrays.equals(packet, TIME_EXTENDED3);
    }

    private int positionFromState() {
        switch (state) {
            case Round1:
                return 1;
            case Round2:
                return 2;
            case Round3:
                return 3;
        }
        return 0;
    }

    private byte parameterFromState() {
        switch (state) {
            case Round1:
                return 0;
            case Round2:
                return 1;
            case Round3:
                return 2;
        }
        return -1;
    }

    private int expectedSize;
    private byte[] presponse = null;

    private int expectedBytesForState() {

        switch (state) {
            case Round1:
            case Round2:
            case Round3:
                return Curve.PACKET_SIZE;
            case SendCertificate1:
            case SendCertificate1out:
            case SendCertificate2:
            case SendCertificate2out:
                return expectedSize > 0 ? expectedSize : 0x179005;
            case SendKeyChallenge:
                return 64;
            default:
                return 0x602910;
        }
    }

    private boolean fill(byte[] data) {
        accumulator = arrayAppend(accumulator, data);
        val expected = expectedBytesForState();
        Log.d(TAG, "Expected byte size for state: " + stateToName(state) + " -> " + expected + " so far " + accumulator.length);
        return accumulator.length >= expectedBytesForState();
    }

    // Return true if we want to emit based on our state
    @Override
    public boolean receivedResponse(final byte[] data) {
        Log.d(TAG, "Received remote response: " + bytesToHex(data) + " when in " + stateToName(state));
        switch (state) {
            case RequestAuth:
                if (!verifyChallenge(data)) {
                    context.reset();
                    if (context.sequence > 1) {
                        throw new SecurityException("Mismatch - wait");
                    } else {
                        return false;
                    }
                }
                context.challenge = new byte[8];
                arraycopy(data, 9, context.challenge, 0, context.challenge.length);
                return true;
            case ChallengeReply:
                val status = new AuthStatusRxMessage(data);
                if (status.needsRefresh()) {
                    context.reset();
                }
                if (!status.isAuthenticated()) {
                    Log.d(TAG, "Could not authenticate!");
                    context.reset();
                    if (!status.isBonded()) {
                        changeState(BondFailure);
                    } else {
                        changeState(Unknown);
                    }
                    return true;
                } else {
                    if (status.isBonded()) {
                        Log.d(TAG, "Full success");
                        if (context.passwordBytes.length > 4) {
                            changeState(GetData);
                        } else {
                            changeState(GetData2);
                        }
                        return true;
                    } else {
                        expectedSize = 0;
                        presponse = null;
                        if (context.passwordBytes.length > 4) {
                            changeState(Pairing);
                        } else {
                            if (context.validateParts()) {
                                changeState(SendCertificate0);
                            } else {
                               throw new InvalidParameterException("Missing QR code");
                            }
                        }
                        return true;
                    }
                }

            case Pairing:
                return true;

            case SendCertificate1:
                val rep = new CertInfoRxMessage(data);
                if (rep.valid()) {
                    expectedSize = rep.getSize();
                    changeState(SendCertificate1);
                    return true;
                } else {
                    throw new InvalidParameterException("Invalid QR code 1");
                }
            case SendCertificate2:
                val rep2 = new CertInfoRxMessage(data);
                if (rep2.valid()) {
                    expectedSize = rep2.getSize();
                    changeState(SendCertificate2);
                    return true;
                } else {
                    throw new InvalidParameterException("Invalid QR code 2");
                }

            case SendKeyChallenge:
                if (data.length > 2 && data[1] != 0) {
                    throw new InvalidParameterException("Invalid QR code 3");
                }
                presponse = Calc.challenger(context.getPartC(), data);
                return true;

            case SendKeyChallengeOut:
                return true;

        }

        return false;
    }

    @Override
    public boolean receivedResponse2(byte[] data) {
        return false;
    }

    @Override
    public boolean receivedResponse3(byte[] data) {
        return false;
    }

    private boolean alt = false;

    private AuthRequestTxMessage2 getAuthRequestTx2() {
        lastAuthTx2 = new AuthRequestTxMessage2(8, alt);
        return lastAuthTx2;
    }

    @Override
    public byte[][] aNext() {
        Log.d(TAG, "Processing anext in state: " + stateToName(state));
        switch (state) {
            case RoundStart:
                if (context.getRound3Packet() != null || context.savedKey != null) {
                    state = RequestAuth;
                    return new byte[][]{getAuthRequestTx2().byteSequence, null};
                } else {
                    state = Round1;
                    return sequencePacket(null);
                }
            case Round1:
                state = Round2;
                return sequencePacket(Calc.getRound1Packet(context).output());
            case Round2:
                state = Round3;
                return sequencePacket(Calc.getRound2Packet(context).output());
            case Round3:
                state = RequestAuth;
                return new byte[][]{getAuthRequestTx2().byteSequence, Calc.getRound3Packet(context).output()};
            case RequestAuth:
                state = ChallengeReply;
                return new byte[][]{new AuthChallengeTxMessage(Calc.calculateHash(context)).byteSequence, null};
            case ChallengeReply:
                state = Unknown;
                return new byte[][]{TIME_EXTENDED, null};
            case SendCertificate0:
                state = SendCertificate1;
                return new byte[][]{expectMyCert1(this), null};
            case SendCertificate1:
                state = SendCertificate1out;
                return new byte[][]{null, context.getPartA()};
            case SendCertificate1out:
                state = SendCertificate2;
                return new byte[][]{expectMyCert2(this), null};
            case SendCertificate2:
                state = SendCertificate2out;
                return new byte[][]{null, context.getPartB()};
            case SendCertificate2out:
                state = SendKeyChallenge;
                return new byte[][]{new SignChallengeTxMessage().byteSequence, null};
            case SendKeyChallenge:
                state = SendKeyChallengeOut;
                return new byte[][]{CHALLENGE_OUT, presponse};
            case SendKeyChallengeOut:
                state = GetData;
                return new byte[][]{TIME_EXTENDED, null};
            case Pairing:
                state = GetData;
                return new byte[][]{TIME_EXTENDED, null};
            case GetData:
                state = Unknown;
                return new byte[][]{GETDATA.bytes};
            case GetData2:
                state = Unknown;
                return new byte[][]{GETDATA2.bytes};
            case BondFailure:
                state = Unknown;
                return new byte[][]{GETDATA.bytes, null, null};
        }
        return null;
    }

    @Override
    public byte[][] bNext() {
        return new byte[0][];
    }

    @Override
    public byte[][] cNext() {
        return new byte[0][];
    }

    @Override
    public boolean receivedData(final byte[] data) {
        Log.d(TAG, "Received data stream: " + bytesToHex(data));
        val full = fill(data);
        if (full) {
            context.packet[positionFromState()] = Packet.parse(accumulator);
            accumulator = EMPTY;
            if (!validate()) {
                Log.d(TAG, "Failed validation " + stateToName(state));
                context.packet[positionFromState()] = null;
                return false;
            }
        }
        return full;
    }

    @Override
    public boolean receivedData2(byte[] data) {
        return false;
    }

    @Override
    public boolean receivedData3(byte[] data) {
        return false;
    }

    @Override
    public byte[] getPersistence(int channel) {
        if (channel == 1) {
            val key = getSharedKey();
            return key != null ? key : new byte[0];
        } else if (channel == 3) {
            if (keyToTestAgainst != null && getSharedKey() != null
                    && Arrays.equals(getSharedKey(), keyToTestAgainst)) {
                return new byte[0];
            } else {
                return null;
            }
        } else {
            return new byte[0];
        }
    }

    @Override
    public boolean setPersistence(int channel, byte[] data) {
        if (channel == 1) {
            keyToTestAgainst = data;
            return true;
        }
        if (channel == 2) {
            if (data != null && data.length != 16) {
                return false;
            }
            if (context.savedKey == null) {
                Log.d(TAG, "Updating saved key from loaded persistence data");
                context.savedKey = data;
                return true;
            } else {
                return false;
            }
        }
        if (channel == 3 || channel == 4) {
            instance = null;
        }
        if (channel == 6) {
            alt = Arrays.equals(data, SPARAM.bytes);
        }
        if (channel == 7) {
            dontClearAccumulator.clear();
        }
        if (channel == 8) {
            context.setPartA(data);
        }
        if (channel == 9) {
            context.setPartB(data);
        }
        if (channel == 10) {
            context.setPartC(data);
        }
        return false;
    }

    @Override
    public String getStatus() {
        return "";
    }

    @Override
    public String getName() {
        return "keks";
    }

    private boolean validate() {
        val p = context.packet[positionFromState()];
        if (p == null) {
            Log.d(TAG, "Packet is null");
            return false;
        }
        switch (state) {
            case Round1:
                return Calc.validateRound1Packet(context);
            case Round2:
                return Calc.validateRound2Packet(context);
            case Round3:
                return Calc.validateRound3Packet(context);
            case SendCertificate1out:
            case SendCertificate2out:
                return true;
        }
        Log.d(TAG, "Invalid state for validation: " + stateToName(state));
        return false;
    }

    private boolean verifyChallenge(byte[] data) {
        if (context.savedKey != null) return true;
        context.challenge = lastAuthTx2.singleUseToken;
        val h = Calc.calculateHash(context);
        if (h == null) return false;
        for (int i = 0; i < 8; i++) {
            if (h[i] != data[i + 1]) return false;
        }
        return true;
    }

    private byte[][] sequencePacket(final byte[] Packet) {
        context.sequence++;
        val param = parameterFromState();
        val command = (param < 0) ? null : new byte[]{KEYCMD.bytes[0], param};
        return new byte[][]{command, Packet};
    }

    private byte[] getSharedKey() {
        if (context.savedKey == null) {
            context.savedKey = Calc.getShortSharedKey(context);
        }
        return context.savedKey;
    }

}
