package jamorham.keks;


import static java.lang.System.arraycopy;
import static jamorham.keks.Config.Get.ALICE;
import static jamorham.keks.Config.Get.BOB;
import static jamorham.keks.Config.Get.GETDATA;
import static jamorham.keks.Config.Get.KEY1A;
import static jamorham.keks.Config.Get.KEY1B;
import static jamorham.keks.Config.Get.KEY2A;
import static jamorham.keks.Config.Get.KEY2B;
import static jamorham.keks.Config.Get.KEYCMD;
import static jamorham.keks.Config.Get.SPARAM;
import static jamorham.keks.util.Util.arrayAppend;
import static jamorham.keks.util.Util.bytesToHex;

import com.eveningoutpost.dexdrip.plugin.IPluginDA;

import java.util.Arrays;
import java.util.HashSet;

import jamorham.keks.message.AuthChallengeTxMessage;
import jamorham.keks.message.AuthRequestTxMessage2;
import jamorham.keks.message.AuthStatusRxMessage;
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
    private static final int GetData = 734275;

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
            case ChallengeReply:
                return "Challenge Reply";
            case GetData:
                return "Get Data";
            default:
                return "Error " + state;
        }
    }

    private static volatile Plugin instance = null;
    private volatile int state = 0;
    private volatile byte[] accumulator = new byte[0];
    private volatile byte[] keyToTestAgainst = null;

    public Plugin(String password) {
        context.password = password;
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

    private static final KeyPair key1 = KeyPair.fromBytes(new byte[][]{KEY1A.bytes, KEY1B.bytes});
    private static final KeyPair key2 = KeyPair.fromBytes(new byte[][]{KEY2A.bytes, KEY2B.bytes});

    @Getter
    private final Context context = new Context();

    {
        context.alice = ALICE.bytes;
        context.bob = BOB.bytes;
        context.keyA = key1;
        context.KeyB = key2;
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
                        }
                        return true;
                    } else {
                        expectedSize = 0;
                        presponse = null;
                        if (context.passwordBytes.length > 4) {
                            changeState(Pairing);
                        }
                        return true;
                    }
                }

            case Pairing:
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

    @Override
    public byte[][] aNext() {
        Log.d(TAG, "Processing anext in state: " + stateToName(state));
        switch (state) {
            case RoundStart:
                if (context.getRound3Packet() != null || context.savedKey != null) {
                    state = RequestAuth;
                    return new byte[][]{new AuthRequestTxMessage2(8, alt).byteSequence, null};
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
                return new byte[][]{new AuthRequestTxMessage2(8, alt).byteSequence, Calc.getRound3Packet(context).output()};
            case RequestAuth:
                state = ChallengeReply;
                return new byte[][]{new AuthChallengeTxMessage(Calc.calculateHash(context)).byteSequence, null};
            case ChallengeReply:
                state = Unknown;
                return new byte[][]{TIME_EXTENDED, null};
            case Pairing:
                state = GetData;
                return new byte[][]{TIME_EXTENDED, null};
            case GetData:
                state = Unknown;
                return new byte[][]{GETDATA.bytes};
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

        }
        Log.d(TAG, "Invalid state for validation: " + stateToName(state));
        return false;
    }

    private byte[][] sequencePacket(final byte[] Packet) {
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
