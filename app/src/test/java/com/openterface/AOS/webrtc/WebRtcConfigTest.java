package com.openterface.AOS.webrtc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for WebRtcConfig auto-start functionality.
 */
@RunWith(MockitoJUnitRunner.class)
public class WebRtcConfigTest {

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences mockPrefs;

    @Mock
    private SharedPreferences.Editor mockEditor;

    private WebRtcConfig config;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getSharedPreferences("webrtc_server_config", Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean("webrtc_auto_start", false)).thenReturn(mockEditor);
        when(mockEditor.putBoolean("webrtc_auto_start", true)).thenReturn(mockEditor);

        config = new WebRtcConfig(mockContext);
    }

    @Test
    public void isAutoStart_returnsFalseByDefault() {
        // Given: default value is false
        when(mockPrefs.getBoolean("webrtc_auto_start", false)).thenReturn(false);

        // When/Then
        assertFalse(config.isAutoStart());
    }

    @Test
    public void isAutoStart_returnsTrueWhenEnabled() {
        // Given: auto-start is enabled
        when(mockPrefs.getBoolean("webrtc_auto_start", false)).thenReturn(true);

        // When/Then
        assertTrue(config.isAutoStart());
    }

    @Test
    public void setAutoStart_false_persistsCorrectly() {
        // When: set auto-start to false
        config.setAutoStart(false);

        // Then: verify the preference was saved
        verify(mockEditor).putBoolean("webrtc_auto_start", false);
        verify(mockEditor).apply();
    }

    @Test
    public void setAutoStart_true_persistsCorrectly() {
        // When: set auto-start to true
        config.setAutoStart(true);

        // Then: verify the preference was saved
        verify(mockEditor).putBoolean("webrtc_auto_start", true);
        verify(mockEditor).apply();
    }

    @Test
    public void setAutoStart_canToggleFromTrueToFalse() {
        // Given: auto-start is initially true
        when(mockPrefs.getBoolean("webrtc_auto_start", false)).thenReturn(true);
        assertTrue(config.isAutoStart());

        // When: set to false
        config.setAutoStart(false);

        // Then: verify the change was persisted
        verify(mockEditor).putBoolean("webrtc_auto_start", false);
        verify(mockEditor).apply();
    }

    @Test
    public void setAutoStart_canToggleFromFalseToTrue() {
        // Given: auto-start is initially false
        when(mockPrefs.getBoolean("webrtc_auto_start", false)).thenReturn(false);
        assertFalse(config.isAutoStart());

        // When: set to true
        config.setAutoStart(true);

        // Then: verify the change was persisted
        verify(mockEditor).putBoolean("webrtc_auto_start", true);
        verify(mockEditor).apply();
    }

    @Test
    public void autoStart_isIndependentFromOtherSettings() {
        // Given: other settings exist (use actual default values from WebRtcConfig)
        when(mockPrefs.getInt("webrtc_signalling_port", 8080)).thenReturn(8080);
        when(mockPrefs.getInt("webrtc_video_bitrate", 2_000_000)).thenReturn(4000000);
        when(mockPrefs.getInt("webrtc_video_fps", 30)).thenReturn(30);
        when(mockPrefs.getBoolean("webrtc_auto_start", false)).thenReturn(true);

        // When: read multiple settings
        int port = config.getSignallingPort();
        int bitrate = config.getVideoBitrate();
        int fps = config.getVideoFps();
        boolean autoStart = config.isAutoStart();

        // Then: all values are independent
        assertEquals(8080, port);
        assertEquals(4000000, bitrate);
        assertEquals(30, fps);
        assertTrue(autoStart);
    }
}
