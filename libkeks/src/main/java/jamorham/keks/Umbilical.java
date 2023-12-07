package jamorham.keks;

import androidx.annotation.Keep;

import lombok.val;

/**
 * JamOrHam
 *
 * Coverage test harness
 */

@Keep
public class Umbilical {

    public void unitTest() {
        val s = Plugin.getInstance("");
        s.amConnected();
        s.receivedResponse(null);
        s.receivedResponse2(null);
        s.receivedResponse3(null);
        s.bondNow(null);
        s.receivedData(null);
        s.receivedData2(null);
        s.receivedData3(null);
        s.aNext();
        s.bNext();
        s.cNext();
        s.getStatus();
        s.getName();
        s.getPersistence(0);
        s.setPersistence(0, null);
    }

}
