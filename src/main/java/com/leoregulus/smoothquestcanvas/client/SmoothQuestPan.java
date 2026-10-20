package com.leoregulus.smoothquestcanvas.client;

/** Local control used by the client input bridge; no quest data or network messages are changed. */
public interface SmoothQuestPan {
    void sqc$stopPan();
    boolean sqc$subpixelRendering();
}
