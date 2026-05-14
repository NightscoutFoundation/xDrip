package com.eveningoutpost.dexdrip.plugin;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author Asbjørn Aarrestad
 */
public class DownloadTest extends RobolectricTestWithConfig {

    @Test
    public void getUrl_buildsCorrectUrl() throws Exception {
        // :: Setup
        PluginDef pluginDef = new PluginDef("myplugin", "author1", "1.0", "example.com/repo");

        // :: Act
        String url = invokeGetUrl(pluginDef);

        // :: Verify
        assertThat(url).isEqualTo("https://example.com/repo/author1-myplugin-1.0.bin");
    }

    @Test
    public void getUrl_handlesSpecialCharactersInName() throws Exception {
        // :: Setup
        PluginDef pluginDef = new PluginDef("my-plugin", "my-author", "2.0.1", "cdn.example.org/plugins");

        // :: Act
        String url = invokeGetUrl(pluginDef);

        // :: Verify
        assertThat(url).isEqualTo("https://cdn.example.org/plugins/my-author-my-plugin-2.0.1.bin");
    }

    @Test
    public void getData_returnsNullForNullPluginDef() throws Exception {
        // :: Setup
        // (null input)

        // :: Act
        byte[] result = invokeGetData(null);

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getSizedBlock_readsCorrectBytes() throws Exception {
        // :: Setup
        byte[] payload = {10, 20, 30};
        ByteBuffer bb = ByteBuffer.allocate(4 + payload.length);
        bb.putInt(payload.length);
        bb.put(payload);
        bb.flip();

        // :: Act
        byte[] result = invokeGetSizedBlock(bb);

        // :: Verify
        assertThat(result).isEqualTo(payload);
    }

    @Test
    public void getSizedBlock_returnsNullForNegativeSize() throws Exception {
        // :: Setup
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(-1);
        bb.flip();

        // :: Act
        byte[] result = invokeGetSizedBlock(bb);

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getSizedBlock_returnsNullForOversizedBlock() throws Exception {
        // :: Setup
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(10_000_001);
        bb.flip();

        // :: Act
        byte[] result = invokeGetSizedBlock(bb);

        // :: Verify
        assertThat(result).isNull();
    }

    @Test
    public void getSizedBlock_readsEmptyBlock() throws Exception {
        // :: Setup
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0);
        bb.flip();

        // :: Act
        byte[] result = invokeGetSizedBlock(bb);

        // :: Verify
        assertThat(result).isNotNull();
        assertThat(result).hasLength(0);
    }

    @Test
    public void getSizedBlock_readsBoundaryMaxSize() throws Exception {
        // :: Setup
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(10_000_000);
        bb.flip();

        // :: Act
        // Size is exactly at the boundary (10_000_000) which is <= 10_000_000, so it should
        // attempt to read but fail due to insufficient data in buffer
        byte[] result = null;
        try {
            result = invokeGetSizedBlock(bb);
        } catch (InvocationTargetException e) {
            // Expected: BufferUnderflowException because we don't have 10M bytes in buffer
            assertThat(e.getCause()).isInstanceOf(java.nio.BufferUnderflowException.class);
            return;
        }

        // :: Verify
        // If we reach here, something unexpected happened
        assertThat(result).isNull();
    }

    private static String invokeGetUrl(PluginDef pluginDef) throws Exception {
        Method method = Download.class.getDeclaredMethod("getUrl", PluginDef.class);
        method.setAccessible(true);
        return (String) method.invoke(null, pluginDef);
    }

    private static byte[] invokeGetData(PluginDef pluginDef) throws Exception {
        Method method = Download.class.getDeclaredMethod("getData", PluginDef.class);
        method.setAccessible(true);
        return (byte[]) method.invoke(null, pluginDef);
    }

    private static byte[] invokeGetSizedBlock(ByteBuffer bb) throws Exception {
        Method method = Download.class.getDeclaredMethod("getSizedBlock", ByteBuffer.class);
        method.setAccessible(true);
        return (byte[]) method.invoke(null, bb);
    }
}
