/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.exoplayercompose.player;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.ExoTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;

import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_AUDIO_BUFFER_SIZE;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_CAMERA_MOTION_BUFFER_SIZE;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_METADATA_BUFFER_SIZE;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_MUXED_BUFFER_SIZE;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_TEXT_BUFFER_SIZE;
import static com.google.android.exoplayer2.DefaultLoadControl.DEFAULT_VIDEO_BUFFER_SIZE;
import static com.google.android.exoplayer2.util.Assertions.checkState;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * The default {@link LoadControl} implementation.
 */
public final class CustomLoadControl implements LoadControl {

    /**
     * The default minimum duration of media that the player will attempt to ensure is buffered at all
     * times, in milliseconds. This value is only applied to playbacks without video.
     */
    public static int DEFAULT_MIN_BUFFER_MS = 16000; //4 chunks each of 4s

    /**
     * The default maximum duration of media that the player will attempt to buffer, in milliseconds.
     * For playbacks with video, this is also the default minimum duration of media that the player
     * will attempt to ensure is buffered.
     */
    public static int DEFAULT_MAX_BUFFER_MS = 32000; // 8 chuncks each of 4s

    /**
     * The default duration of media that must be buffered for playback to start or resume following a
     * user action such as a seek, in milliseconds.
     */
    public static int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 1000; //start after downloading 1 chunk

    /**
     * The default duration of media that must be buffered for playback to resume after a rebuffer, in
     * milliseconds. A rebuffer is defined to be caused by buffer depletion rather than a user action.
     */
    public static int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 8000; //2 chuncks each of 4s

    public static void setMaxBufferMs(int maxBufferMs){
        DEFAULT_MAX_BUFFER_MS = maxBufferMs;
    }

    public static void resetToDefaultMaxBufferMs(){
        DEFAULT_MAX_BUFFER_MS = 32000;
    }



    /**
     * To increase buffer time and size.
     * Added by Sri
     */
    public static int VIDEO_BUFFER_SCALE_UP_FACTOR = 4;
    public static final int DEFAULT_TARGET_BUFFER_BYTES = C.LENGTH_UNSET;

    /** The default prioritization of buffer time constraints over size constraints. */
    public static final boolean DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS = false;

    /**
     * Priority for media loading.
     */
    public static final int LOADING_PRIORITY = 0;

    /** The default back buffer duration in milliseconds. */
    public static final int DEFAULT_BACK_BUFFER_DURATION_MS = 0;

    /**
     * A {@link EventListener} instance
     */
    private EventListener bufferedDurationListener;

    /**
     * A {@link Handler}
     */
    private Handler eventHandler;

    private static final int ABOVE_HIGH_WATERMARK = 0;
    private static final int BETWEEN_WATERMARKS = 1;
    private static final int BELOW_LOW_WATERMARK = 2;

    //    private final DefaultAllocator allocator;
//
//    private long minBufferUs;
//    private long maxBufferUs;
//    private long bufferForPlaybackUs;
//    private long bufferForPlaybackAfterRebufferUs;
    private PriorityTaskManager priorityTaskManager;

    private int targetBufferSize;
    private boolean isBuffering;

    private final String TAG = "Logix CustomLoadControl";

    /** The default for whether the back buffer is retained from the previous keyframe. */
    public static final boolean DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME = false;

    /** A default size in bytes for a video buffer. */
    public static final int DEFAULT_VIDEO_BUFFER_SIZE = 2000 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** A default size in bytes for an audio buffer. */
    public static final int DEFAULT_AUDIO_BUFFER_SIZE = 200 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** A default size in bytes for a text buffer. */
    public static final int DEFAULT_TEXT_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** A default size in bytes for a metadata buffer. */
    public static final int DEFAULT_METADATA_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** A default size in bytes for a camera motion buffer. */
    public static final int DEFAULT_CAMERA_MOTION_BUFFER_SIZE = 2 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** A default size in bytes for a muxed buffer (e.g. containing video, audio and text). */
    public static final int DEFAULT_MUXED_BUFFER_SIZE =
            DEFAULT_VIDEO_BUFFER_SIZE + DEFAULT_AUDIO_BUFFER_SIZE + DEFAULT_TEXT_BUFFER_SIZE;

    /**
     * The buffer size in bytes that will be used as a minimum target buffer in all cases. This is
     * also the default target buffer before tracks are selected.
     */
    public static final int DEFAULT_MIN_BUFFER_SIZE = 200 * C.DEFAULT_BUFFER_SEGMENT_SIZE;

    /** Builder for {@link CustomLoadControl}. */
    public static final class Builder {

        @Nullable
        private DefaultAllocator allocator;
        private int minBufferMs;
        private int maxBufferMs;
        private int bufferForPlaybackMs;
        private int bufferForPlaybackAfterRebufferMs;
        private int targetBufferBytes;
        private boolean prioritizeTimeOverSizeThresholds;
        private int backBufferDurationMs;
        private boolean retainBackBufferFromKeyframe;
        private boolean buildCalled;


        private EventListener bufferedDurationListener;

        /** Constructs a new instance. */
        public Builder() {
            minBufferMs = DEFAULT_MIN_BUFFER_MS;
            maxBufferMs = DEFAULT_MAX_BUFFER_MS;
            bufferForPlaybackMs = DEFAULT_BUFFER_FOR_PLAYBACK_MS;
            bufferForPlaybackAfterRebufferMs = DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS;
            targetBufferBytes = DEFAULT_TARGET_BUFFER_BYTES;
            prioritizeTimeOverSizeThresholds = DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS;
            backBufferDurationMs = DEFAULT_BACK_BUFFER_DURATION_MS;
            retainBackBufferFromKeyframe = DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME;
        }

        /**
         * Sets the {@link DefaultAllocator} used by the loader.
         *
         * @param allocator The {@link DefaultAllocator}.
         * @return This builder, for convenience.
         * @throws IllegalStateException If {@link #build()} has already been called.
         */
        public Builder setAllocator(DefaultAllocator allocator) {
            checkState(!buildCalled);
            this.allocator = allocator;
            return this;
        }

        public Builder setBufferedDurationListener(
                EventListener bufferedDurationListener) {
            this.bufferedDurationListener = bufferedDurationListener;
            return this;
        }

        /**
         * Sets the buffer duration parameters.
         *
         * @param minBufferMs The minimum duration of media that the player will attempt to ensure is
         *     buffered at all times, in milliseconds.
         * @param maxBufferMs The maximum duration of media that the player will attempt to buffer, in
         *     milliseconds.
         * @param bufferForPlaybackMs The duration of media that must be buffered for playback to start
         *     or resume following a user action such as a seek, in milliseconds.
         * @param bufferForPlaybackAfterRebufferMs The default duration of media that must be buffered
         *     for playback to resume after a rebuffer, in milliseconds. A rebuffer is defined to be
         *     caused by buffer depletion rather than a user action.
         * @return This builder, for convenience.
         * @throws IllegalStateException If {@link #build()} has already been called.
         */
        public Builder setBufferDurationsMs(
                int minBufferMs,
                int maxBufferMs,
                int bufferForPlaybackMs,
                int bufferForPlaybackAfterRebufferMs) {
            checkState(!buildCalled);
            assertGreaterOrEqual(bufferForPlaybackMs, 0, "bufferForPlaybackMs", "0");
            assertGreaterOrEqual(
                    bufferForPlaybackAfterRebufferMs, 0, "bufferForPlaybackAfterRebufferMs", "0");
            assertGreaterOrEqual(minBufferMs, bufferForPlaybackMs, "minBufferMs", "bufferForPlaybackMs");
            assertGreaterOrEqual(
                    minBufferMs,
                    bufferForPlaybackAfterRebufferMs,
                    "minBufferMs",
                    "bufferForPlaybackAfterRebufferMs");
            assertGreaterOrEqual(maxBufferMs, minBufferMs, "maxBufferMs", "minBufferMs");
            this.minBufferMs = minBufferMs;
            this.maxBufferMs = maxBufferMs;
            this.bufferForPlaybackMs = bufferForPlaybackMs;
            this.bufferForPlaybackAfterRebufferMs = bufferForPlaybackAfterRebufferMs;
            return this;
        }

        /**
         * Sets the target buffer size in bytes. If set to {@link C#LENGTH_UNSET}, the target buffer
         * size will be calculated based on the selected tracks.
         *
         * @param targetBufferBytes The target buffer size in bytes.
         * @return This builder, for convenience.
         * @throws IllegalStateException If {@link #build()} has already been called.
         */
        public Builder setTargetBufferBytes(int targetBufferBytes) {
            checkState(!buildCalled);
            this.targetBufferBytes = targetBufferBytes;
            return this;
        }

        /**
         * Sets whether the load control prioritizes buffer time constraints over buffer size
         * constraints.
         *
         * @param prioritizeTimeOverSizeThresholds Whether the load control prioritizes buffer time
         *     constraints over buffer size constraints.
         * @return This builder, for convenience.
         * @throws IllegalStateException If {@link #build()} has already been called.
         */
        public Builder setPrioritizeTimeOverSizeThresholds(boolean prioritizeTimeOverSizeThresholds) {
            checkState(!buildCalled);
            this.prioritizeTimeOverSizeThresholds = prioritizeTimeOverSizeThresholds;
            return this;
        }

        /**
         * Sets the back buffer duration, and whether the back buffer is retained from the previous
         * keyframe.
         *
         * @param backBufferDurationMs The back buffer duration in milliseconds.
         * @param retainBackBufferFromKeyframe Whether the back buffer is retained from the previous
         *     keyframe.
         * @return This builder, for convenience.
         * @throws IllegalStateException If {@link #build()} has already been called.
         */
        public Builder setBackBuffer(int backBufferDurationMs, boolean retainBackBufferFromKeyframe) {
            checkState(!buildCalled);
            assertGreaterOrEqual(backBufferDurationMs, 0, "backBufferDurationMs", "0");
            this.backBufferDurationMs = backBufferDurationMs;
            this.retainBackBufferFromKeyframe = retainBackBufferFromKeyframe;
            return this;
        }

        /** @deprecated use {@link #build} instead. */
        @Deprecated
        public CustomLoadControl createDefaultLoadControl() {
            return build();
        }

        /** Creates a {@link CustomLoadControl}. */
        public CustomLoadControl build() {
            checkState(!buildCalled);
            buildCalled = true;
            if (allocator == null) {
                allocator = new DefaultAllocator(/* trimOnReset= */ true, C.DEFAULT_BUFFER_SEGMENT_SIZE);
            }
            return new CustomLoadControl(
                    allocator,
                    minBufferMs,
                    maxBufferMs,
                    bufferForPlaybackMs,
                    bufferForPlaybackAfterRebufferMs,
                    targetBufferBytes,
                    prioritizeTimeOverSizeThresholds,
                    backBufferDurationMs,
                    retainBackBufferFromKeyframe,
                    bufferedDurationListener);
        }
    }

    private final DefaultAllocator allocator;

    private final long minBufferUs;
    private final long maxBufferUs;
    private final long bufferForPlaybackUs;
    private final long bufferForPlaybackAfterRebufferUs;
    private final int targetBufferBytesOverwrite;
    private final boolean prioritizeTimeOverSizeThresholds;
    private final long backBufferDurationUs;
    private final boolean retainBackBufferFromKeyframe;

    private int targetBufferBytes;
    private boolean isLoading;

    /** Constructs a new instance, using the {@code DEFAULT_*} constants defined in this class. */
    @SuppressWarnings("deprecation")
    public CustomLoadControl(EventListener listener, Handler handler) {
        this(
                new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE),
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                DEFAULT_TARGET_BUFFER_BYTES,
                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS,
                DEFAULT_BACK_BUFFER_DURATION_MS,
                DEFAULT_RETAIN_BACK_BUFFER_FROM_KEYFRAME);
        bufferedDurationListener = listener;
        eventHandler = handler;
    }

    protected CustomLoadControl(DefaultAllocator allocator,
                                int minBufferMs,
                                int maxBufferMs,
                                int bufferForPlaybackMs,
                                int bufferForPlaybackAfterRebufferMs,
                                int targetBufferBytes,
                                boolean prioritizeTimeOverSizeThresholds,
                                int backBufferDurationMs,
                                boolean retainBackBufferFromKeyframe,EventListener bufferedDurationListener){

        this(allocator,minBufferMs,maxBufferMs,bufferForPlaybackMs,bufferForPlaybackAfterRebufferMs,targetBufferBytes,prioritizeTimeOverSizeThresholds,backBufferDurationMs,retainBackBufferFromKeyframe);
        this.bufferedDurationListener = bufferedDurationListener;
    }

    protected CustomLoadControl(
            DefaultAllocator allocator,
            int minBufferMs,
            int maxBufferMs,
            int bufferForPlaybackMs,
            int bufferForPlaybackAfterRebufferMs,
            int targetBufferBytes,
            boolean prioritizeTimeOverSizeThresholds,
            int backBufferDurationMs,
            boolean retainBackBufferFromKeyframe) {
        assertGreaterOrEqual(bufferForPlaybackMs, 0, "bufferForPlaybackMs", "0");
        assertGreaterOrEqual(
                bufferForPlaybackAfterRebufferMs, 0, "bufferForPlaybackAfterRebufferMs", "0");
        assertGreaterOrEqual(minBufferMs, bufferForPlaybackMs, "minBufferMs", "bufferForPlaybackMs");
        assertGreaterOrEqual(
                minBufferMs,
                bufferForPlaybackAfterRebufferMs,
                "minBufferMs",
                "bufferForPlaybackAfterRebufferMs");
        assertGreaterOrEqual(maxBufferMs, minBufferMs, "maxBufferMs", "minBufferMs");
        assertGreaterOrEqual(backBufferDurationMs, 0, "backBufferDurationMs", "0");

        this.allocator = allocator;
        this.minBufferUs = C.msToUs(minBufferMs);
        this.maxBufferUs = C.msToUs(maxBufferMs);
        this.bufferForPlaybackUs = C.msToUs(bufferForPlaybackMs);
        this.bufferForPlaybackAfterRebufferUs = C.msToUs(bufferForPlaybackAfterRebufferMs);
        this.targetBufferBytesOverwrite = targetBufferBytes;
        this.targetBufferBytes =
                targetBufferBytesOverwrite != C.LENGTH_UNSET
                        ? targetBufferBytesOverwrite
                        : DEFAULT_MIN_BUFFER_SIZE;
        this.prioritizeTimeOverSizeThresholds = prioritizeTimeOverSizeThresholds;
        this.backBufferDurationUs = C.msToUs(backBufferDurationMs);
        this.retainBackBufferFromKeyframe = retainBackBufferFromKeyframe;
    }

    @Override
    public void onPrepared() {
        reset(false);
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups,
                                 ExoTrackSelection[] trackSelections) {
        targetBufferBytes =
                targetBufferBytesOverwrite == C.LENGTH_UNSET
                        ? calculateTargetBufferBytes(renderers, trackSelections)
                        : targetBufferBytesOverwrite;
        allocator.setTargetBufferSize(targetBufferBytes);
    }

    @Override
    public void onStopped() {
        reset(true);
    }

    @Override
    public void onReleased() {
        reset(true);
    }

    @Override
    public Allocator getAllocator() {
        return allocator;
    }

    @Override
    public long getBackBufferDurationUs() {
        return backBufferDurationUs;
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return retainBackBufferFromKeyframe;
    }

    @Override
    public boolean shouldContinueLoading(long playbackPositionUs, long bufferedDurationUs, float playbackSpeed) {
        boolean targetBufferSizeReached = allocator.getTotalBytesAllocated() >= targetBufferBytes;
        long minBufferUs = this.minBufferUs;
        if (playbackSpeed > 1) {
            // The playback speed is faster than real time, so scale up the minimum required media
            // duration to keep enough media buffered for a playout duration of minBufferUs.
            long mediaDurationMinBufferUs =
                    Util.getMediaDurationForPlayoutDuration(minBufferUs, playbackSpeed);
            minBufferUs = min(mediaDurationMinBufferUs, maxBufferUs);
        }
        // Prevent playback from getting stuck if minBufferUs is too small.
        minBufferUs = max(minBufferUs, 500_000);
        if (bufferedDurationUs < minBufferUs) {
            isLoading = prioritizeTimeOverSizeThresholds || !targetBufferSizeReached;
            if (!isLoading && bufferedDurationUs < 500_000) {
                Log.w(
                        "DefaultLoadControl",
                        "Target buffer size reached with less than 500ms of buffered media data.");
            }
        } else if (bufferedDurationUs >= maxBufferUs || targetBufferSizeReached) {
            isLoading = false;
        } // Else don't change the loading state.
        if (bufferedDurationListener != null) {
            bufferedDurationListener.onBufferedDurationSample(bufferedDurationUs);
        }
//        Log.e("ExoplayerCompose","shouldContinueLoading "+" bufferedDurationUs "+bufferedDurationUs);
        return isLoading;
    }

    public long getMaxBufferUs(){
//        return ((DEFAULT_MAX_BUFFER_MS*VIDEO_BUFFER_SCALE_UP_FACTOR)/1000);
        return ((DEFAULT_MAX_BUFFER_MS)/1000);
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering, long targetLiveOffsetUs) {
        bufferedDurationUs = Util.getPlayoutDurationForMediaDuration(bufferedDurationUs, playbackSpeed);
        long minBufferDurationUs = rebuffering ? bufferForPlaybackAfterRebufferUs : bufferForPlaybackUs;
        if (targetLiveOffsetUs != C.TIME_UNSET) {
            minBufferDurationUs = min(targetLiveOffsetUs / 2, minBufferDurationUs);
        }
        if (bufferedDurationListener != null) {
            bufferedDurationListener.onPercentageUpdate(
                    (int) ((bufferedDurationUs * 100) / minBufferDurationUs), rebuffering);
        }
//        Log.e("ExoplayerCompose","shouldStartPlayback "+(minBufferDurationUs <= 0
//                || bufferedDurationUs >= minBufferDurationUs
//                || (!prioritizeTimeOverSizeThresholds
//                && allocator.getTotalBytesAllocated() >= targetBufferBytes))+" bufferedDurationUs "+bufferedDurationUs);
        return minBufferDurationUs <= 0
                || bufferedDurationUs >= minBufferDurationUs
                || (!prioritizeTimeOverSizeThresholds
                && allocator.getTotalBytesAllocated() >= targetBufferBytes);
    }

    /**
     * Calculate target buffer size in bytes based on the selected tracks. The player will try not to
     * exceed this target buffer. Only used when {@code targetBufferBytes} is {@link C#LENGTH_UNSET}.
     *
     * @param renderers The renderers for which the track were selected.
     * @param trackSelectionArray The selected tracks.
     * @return The target buffer size in bytes.
     */
    protected int calculateTargetBufferBytes(
            Renderer[] renderers, ExoTrackSelection[] trackSelectionArray) {
        int targetBufferSize = 0;
        for (int i = 0; i < renderers.length; i++) {
            if (trackSelectionArray[i] != null) {
                targetBufferSize += getDefaultBufferSize(renderers[i].getTrackType());
            }
//            if(renderers[i].getTrackType() == C.TRACK_TYPE_VIDEO)
//                targetBufferSize *= VIDEO_BUFFER_SCALE_UP_FACTOR; /*Added by Sri to control buffer size */
        }
        return max(DEFAULT_MIN_BUFFER_SIZE, targetBufferSize);
    }

    private void reset(boolean resetAllocator) {
        targetBufferBytes =
                targetBufferBytesOverwrite == C.LENGTH_UNSET
                        ? DEFAULT_MIN_BUFFER_SIZE
                        : targetBufferBytesOverwrite;
        isLoading = false;
        if (resetAllocator) {
            allocator.reset();
        }
    }

    private static int getDefaultBufferSize(int trackType) {
        switch (trackType) {
            case C.TRACK_TYPE_DEFAULT:
                return DEFAULT_MUXED_BUFFER_SIZE;
            case C.TRACK_TYPE_AUDIO:
                return DEFAULT_AUDIO_BUFFER_SIZE;
            case C.TRACK_TYPE_VIDEO:
                return DEFAULT_VIDEO_BUFFER_SIZE;
            case C.TRACK_TYPE_TEXT:
                return DEFAULT_TEXT_BUFFER_SIZE;
            case C.TRACK_TYPE_METADATA:
                return DEFAULT_METADATA_BUFFER_SIZE;
            case C.TRACK_TYPE_CAMERA_MOTION:
                return DEFAULT_CAMERA_MOTION_BUFFER_SIZE;
            case C.TRACK_TYPE_NONE:
                return 0;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void assertGreaterOrEqual(int value1, int value2, String name1, String name2) {
        Assertions.checkArgument(value1 >= value2, name1 + " cannot be less than " + name2);
    }

    /**
     * An interface for Event listener
     */
    public interface EventListener {
        void onBufferedDurationSample(long bufferedDurationUs);
        default void onPercentageUpdate(int percentage,boolean rebuffering){}
    }
}
