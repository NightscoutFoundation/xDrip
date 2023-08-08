package com.eveningoutpost.dexdrip.models;

import android.os.Build;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.OrderWith;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Alphanumeric;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
@OrderWith(Alphanumeric.class)
public class DateUtilTest {

    public static Collection<String> testISODateStrings = Arrays.asList("2018-03-03 11:22:48", "2018-03-03T11:22:48", "2018-03-03T11:22:48Z",
            "2018-03-03T11:22:48.384Z", "2018-03-03T12:22:48+01:00", "2018-03-03T05:22:48-06:00", "2018-03-03T14:22:48+0300");
    public static Date dtExpected = Date.from(Instant.parse("2018-03-03T11:22:48.000Z"));

    static void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    @RunWith(Parameterized.class)
    abstract public static class SharedTests {

        @Parameterized.Parameter
        public String strInput;

        @Parameterized.Parameters(name = "{index}: fromISODate({0})={1}")
        public static Collection<String> data() {
            return testISODateStrings;
        }

        @Test
        public void tolerantFromISODateString() {
            try {
                assertEquals(dtExpected, DateUtil.tolerantFromISODateString(strInput));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class LowerSdkVersions extends SharedTests {

        @BeforeClass
        public static void setUp() throws Exception {
            setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.M);
            System.out.println("Testing ISO Date Parsing function using SDK version: " + Build.VERSION.SDK_INT);
        }
    }

    public static class UpperSdkVersions extends SharedTests {

        @BeforeClass
        public static void setUp() throws Exception {
            setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
            System.out.println("Testing ISO Date Parsing function using SDK version: " + Build.VERSION.SDK_INT);
        }
    }

    public static class DateUtilMiscTests {

        @BeforeClass
        public static void setUp() throws Exception {
            setFinalStatic(Build.VERSION.class.getField("SDK_INT"), Build.VERSION_CODES.O);
            System.out.println("Testing ISO Date Parsing function using SDK version: " + Build.VERSION.SDK_INT);
        }

        @Test
        public void tolerantFromISODateStringWithoutSeconds() {
            try {
                Date dtExpectedZeroSeconds = Date.from(dtExpected.toInstant().truncatedTo(ChronoUnit.MINUTES));
                assertEquals(dtExpectedZeroSeconds, DateUtil.tolerantFromISODateString("2018-03-03T05:22-0600"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
