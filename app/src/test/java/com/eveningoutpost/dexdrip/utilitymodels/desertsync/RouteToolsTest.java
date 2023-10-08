package com.eveningoutpost.dexdrip.utilitymodels.desertsync;

import org.junit.Test;

import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.RouteTools.inSameNetwork;
import static com.eveningoutpost.dexdrip.utilitymodels.desertsync.RouteTools.ip;
import static com.google.common.truth.Truth.assertWithMessage;

// jamorham

public class RouteToolsTest {

    @Test
    public void sameNetworkTest() {

        // TODO probably a lot more bitwise edge cases we could test

        assertWithMessage("ip test ok 1").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.3.4"), 24)).isTrue();
        assertWithMessage("ip test ok 2").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.3.5"), 24)).isTrue();
        assertWithMessage("ip test ok 3").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.3.4"), 32)).isTrue();
        assertWithMessage("ip test ok 4").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.3.4"), 27)).isTrue();
        assertWithMessage("ip test ok 5").that(inSameNetwork(ip("1.0.0.0"), ip("1.2.3.4"), 4)).isTrue();
        assertWithMessage("ip test ok 6").that(inSameNetwork(ip("10.9.8.7"), ip("10.255.255.255"), 8)).isTrue();
        assertWithMessage("ip test ok 7").that(inSameNetwork(ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 128)).isTrue();
        assertWithMessage("ip test ok 8").that(inSameNetwork(ip("2001:0db8:85a3:0000:0000:8a2e:7777:8888"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 64)).isTrue();
        assertWithMessage("ip test ok 9").that(inSameNetwork(ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 8)).isTrue();


        assertWithMessage("ip test bad 0").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.3.4"), 0)).isFalse();
        assertWithMessage("ip test bad 1").that(inSameNetwork(ip("1.2.3.4"), ip("1.2.9.4"), 24)).isFalse();
        assertWithMessage("ip test bad 1.1").that(inSameNetwork(ip("1.2.3.4"), ip("7.2.3.4"), 24)).isFalse();
        assertWithMessage("ip test bad 2").that(inSameNetwork(ip("1.2.9.4"), ip("1.2.3.4"), 24)).isFalse();
        assertWithMessage("ip test bad 3").that(inSameNetwork(ip("1.2.3.4"), ip("99.88.77.66"), 24)).isFalse();
        assertWithMessage("ip test bad 4").that(inSameNetwork(ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), ip("1.2.3.4"), 24)).isFalse();
        assertWithMessage("ip test bad 5").that(inSameNetwork(ip("1.2.3.4"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 24)).isFalse();
        assertWithMessage("ip test bad 6").that(inSameNetwork(ip("1.9.3.4"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 64)).isFalse();
        assertWithMessage("ip test bad 7").that(inSameNetwork(ip("2001:0db8:85a3:7777:0000:8a2e:0370:7334"), ip("2001:0db8:85a3:0000:0000:8a2e:0370:7334"), 64)).isFalse();

    }

    @Test
    public void ipTest() {
        assertWithMessage("ip process ok 1").that(ip(ip("1.2.3.4"))).isEqualTo("1.2.3.4");
        assertWithMessage("ip process ok 2").that(ip(ip("192.168.10.10"))).isEqualTo("192.168.10.10");
    }


}