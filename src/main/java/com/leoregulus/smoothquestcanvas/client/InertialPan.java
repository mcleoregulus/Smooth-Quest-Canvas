package com.leoregulus.smoothquestcanvas.client;

/** Frame-rate independent pan dynamics. Coordinates and velocities use GUI pixels and seconds. */
public final class InertialPan {
    private double x, y, targetX, targetY, velocityX, velocityY, pendingX, pendingY;
    private boolean dragged;

    public record Bounds(double minX, double maxX, double minY, double maxY) {
        public static Bounds canvas(double contentWidth, double contentHeight, int width, int height) {
            double dx = contentWidth - width;
            double dy = contentHeight - height;
            return new Bounds(dx < 0 ? dx / 2 : 0, dx < 0 ? dx / 2 : dx,
                    dy < 0 ? dy / 2 : 0, dy < 0 ? dy / 2 : dy);
        }
    }

    public void reset(double x, double y) {
        this.x = targetX = x;
        this.y = targetY = y;
        velocityX = velocityY = pendingX = pendingY = 0;
        dragged = false;
    }

    public void advance(boolean held, double dx, double dy, double dt, double follow, double friction, Bounds bounds) {
        if (!Double.isFinite(dt) || dt <= 0) return;
        dt = Math.min(dt, 0.05);
        double oldTargetX = targetX, oldTargetY = targetY;
        if (held) {
            if (!dragged) {
                pendingX += dx;
                pendingY += dy;
                if (Math.hypot(pendingX, pendingY) >= 3) {
                    dragged = true;
                    dx = pendingX;
                    dy = pendingY;
                } else dx = dy = 0;
            }
            targetX = clamp(targetX + dx, bounds.minX, bounds.maxX);
            targetY = clamp(targetY + dy, bounds.minY, bounds.maxY);
            double alpha = -Math.expm1(-18 * dt);
            velocityX += (clamp((targetX - oldTargetX) / dt, -4000, 4000) - velocityX) * alpha;
            velocityY += (clamp((targetY - oldTargetY) / dt, -4000, 4000) - velocityY) * alpha;
            x += (targetX - x) * -Math.expm1(-follow * dt);
            y += (targetY - y) * -Math.expm1(-follow * dt);
        } else {
            double decay = Math.exp(-friction * dt);
            double followDecay = Math.exp(-follow * dt);
            double integration = -Math.expm1(-friction * dt) / friction;
            targetX += velocityX * integration;
            targetY += velocityY * integration;
            // Exact solution for a decaying target velocity followed by an exponential camera.
            double coupling = Math.abs(follow - friction) < 1e-6
                    ? dt * followDecay : (decay - followDecay) / (follow - friction);
            x = targetX + (x - oldTargetX) * followDecay - velocityX * coupling;
            y = targetY + (y - oldTargetY) * followDecay - velocityY * coupling;
            velocityX *= decay;
            velocityY *= decay;
        }
        if (targetX <= bounds.minX || targetX >= bounds.maxX) velocityX = 0;
        if (targetY <= bounds.minY || targetY >= bounds.maxY) velocityY = 0;
        targetX = clamp(targetX, bounds.minX, bounds.maxX);
        targetY = clamp(targetY, bounds.minY, bounds.maxY);
        x = clamp(x, bounds.minX, bounds.maxX);
        y = clamp(y, bounds.minY, bounds.maxY);
        if (!held && Math.hypot(velocityX, velocityY) < 2 && Math.hypot(x - targetX, y - targetY) < 0.05) {
            x = targetX;
            y = targetY;
            velocityX = velocityY = 0;
        }
    }

    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    public double x() { return x; }
    public double y() { return y; }
    public boolean isSettled() { return x == targetX && y == targetY && velocityX == 0 && velocityY == 0; }
}
