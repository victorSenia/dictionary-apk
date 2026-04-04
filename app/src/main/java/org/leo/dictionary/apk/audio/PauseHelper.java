package org.leo.dictionary.apk.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;

import java.util.logging.Logger;

public class PauseHelper {
    private final static Logger LOGGER = Logger.getLogger(PauseHelper.class.getName());
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final long KEEP_ALIVE_PERIOD_MS = 1500L;
    private static final long SILENT_BURST_MS = 200L;

    private static final Object audioLock = new Object();
    private static AudioTrack audioTrack;
    private static byte[] silentBurst;

    public static void pause(long delay) {
        if (delay <= 0) {
            return;
        }
        long start = SystemClock.elapsedRealtime();
        long deadline = start + delay;
        long nextKick = start + KEEP_ALIVE_PERIOD_MS;
        sleepUntil(Math.min(deadline, nextKick));
        while (nextKick < deadline) {
            playSilentBurst();
            nextKick += KEEP_ALIVE_PERIOD_MS;
            sleepUntil(Math.min(deadline, nextKick));
        }
    }

    private static void ensureAudioTrack() {
        synchronized (audioLock) {
            if (audioTrack != null) {
                return;
            }

            int minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
            if (minBuffer <= 0) {
                throw new IllegalStateException("Invalid min buffer size: " + minBuffer);
            }

            int burstBytes = bytesForDurationMs(SILENT_BURST_MS);
            int bufferSize = Math.max(minBuffer, burstBytes);

            audioTrack = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(ENCODING)
                            .setChannelMask(CHANNEL_MASK)
                            .build(),
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );

            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                AudioTrack bad = audioTrack;
                audioTrack = null;
                bad.release();
                throw new IllegalStateException("AudioTrack initialization failed");
            }

            silentBurst = new byte[burstBytes];
        }
    }

    private static void playSilentBurst() {
        ensureAudioTrack();
        final AudioTrack track;
        final byte[] data;

        synchronized (audioLock) {
            track = audioTrack;
            data = silentBurst;
        }

        if (track == null || data == null) {
            throw new IllegalStateException("AudioTrack not initialized");
        }
        track.play();

        int written = 0;
        while (written < data.length) {
            int n = track.write(data, written, data.length - written);
            if (n < 0) {
                throw new IllegalStateException("AudioTrack write failed: " + n);
            }
            written += n;
        }
        try {
            sleepUntil(SystemClock.elapsedRealtime() + SILENT_BURST_MS);
        } finally {
            track.pause();
            track.flush();
        }
    }

    private static int bytesForDurationMs(long durationMs) {
        long samples = (SAMPLE_RATE * durationMs) / 1000L;
        long bytes = samples * 2L; // 16-bit mono
        if (bytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("duration too large: " + durationMs);
        }
        return (int) bytes;
    }

    private static void sleepUntil(long targetElapsedRealtime) {
        while (true) {
            long remaining = targetElapsedRealtime - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                break;
            }
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
