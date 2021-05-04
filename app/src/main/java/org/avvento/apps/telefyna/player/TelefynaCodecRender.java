package org.avvento.apps.telefyna.player;

import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import androidx.annotation.Nullable;

public class TelefynaCodecRender extends MediaCodecVideoRenderer {

    public TelefynaCodecRender(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, boolean enableDecoderFallback, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify) {
        super(context, mediaCodecSelector, allowedJoiningTimeMs, enableDecoderFallback, eventHandler, eventListener, maxDroppedFramesToNotify);
    }

    @Override
    protected boolean codecNeedsSetOutputSurfaceWorkaround(String name) {
        // https://github.com/google/ExoPlayer/issues/3939
        return false;
    }
}
