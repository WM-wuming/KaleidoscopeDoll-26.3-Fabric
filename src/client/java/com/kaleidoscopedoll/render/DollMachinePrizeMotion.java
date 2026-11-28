package com.kaleidoscopedoll.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Intersectiond;
import org.joml.Quaterniond;
import org.joml.Vector3d;

final class DollMachinePrizeMotion {
    private static final int UPDATES_PER_SECOND = 240;
    private static final double DURATION = 2.85;
    private static final double SETTLED = 2.56;
    private static final int LAST_FRAME = (int) Math.round(DURATION * UPDATES_PER_SECOND);
    private static final double STEP = 1.0 / UPDATES_PER_SECOND;
    private static final float ACTIVE_SCALE = 0.79F;
    private static final double HALF_SIZE = 2 * ACTIVE_SCALE;
    private static final Vector3d HALF_EXTENTS = new Vector3d(HALF_SIZE);
    private static final double MAX_CONTACT_DISTANCE = 2 * Math.sqrt(3) * HALF_SIZE;
    private static final double TURN = Math.PI * 2;
    private static final Vec3[] HOME = {
            new Vec3(5.5 + Math.sqrt(0.5), 14, 5.5), new Vec3(4.5, 14.5, 11),
            new Vec3(11, 14, 10), new Vec3(9, 18.75, 7), new Vec3(11, 14, 4)
    };
    static final int PRIZE_COUNT = HOME.length;
    private static final PrizePose[][] FRAMES = simulate();

    private DollMachinePrizeMotion() {}

    static Vec3 home(int prize) {
        return HOME[prize];
    }

    static PrizePose sample(int prize, double seconds) {
        double frame = Mth.clamp(seconds * UPDATES_PER_SECOND, 0, LAST_FRAME);
        int index = (int) frame;
        PrizePose from = FRAMES[index][prize];
        if (index == LAST_FRAME) {
            return from;
        }
        PrizePose to = FRAMES[index + 1][prize];
        double blend = frame - index;
        return new PrizePose(from.center.lerp(to.center, blend), Mth.lerp(blend, from.spin, to.spin),
                Mth.lerp((float) blend, from.scale, to.scale));
    }

    // Bake the cosmetic collisions once; GeckoLib's clock samples the same motion at any frame rate.
    private static PrizePose[][] simulate() {
        Prize[] prizes = {
                new Prize(0, 16, 25, 10, 22, new Quaterniond().rotationY(Math.PI / 4)),
                new Prize(1, 26, 7, -18, -17, new Quaterniond().rotationX(Math.PI / 8)),
                new Prize(2, -23, 18, 11, 18, new Quaterniond().rotationY(-Math.PI / 8)),
                new Prize(3, -15, -25, -8, -21, new Quaterniond().rotationZ(-Math.PI / 8)),
                new Prize(4, -23, 20, 15, 25, new Quaterniond())
        };
        PrizePose[][] frames = new PrizePose[LAST_FRAME + 1][PRIZE_COUNT];
        for (int frame = 0; frame <= LAST_FRAME; frame++) {
            double time = frame * STEP;
            if (time > 0.10) {
                double settling = smoothstep(1.02, 1.75, time);
                double drag = Math.exp(-(0.3 + 11.5 * settling) * STEP);
                for (int i = 0; i < prizes.length; i++) {
                    Prize prize = prizes[i];
                    Vec3 returnForce = HOME[i].subtract(prize.center).scale(48 * settling);
                    prize.velocity = prize.velocity.add(returnForce.add(0, -14 * (1 - settling), 0).scale(STEP)).scale(drag);
                    prize.center = prize.center.add(prize.velocity.scale(STEP));
                    prize.angularVelocity *= drag;
                    prize.angle += prize.angularVelocity * STEP;
                    prize.updateAxes();
                }
                for (int iteration = 0; iteration < 6; iteration++) {
                    for (int i = 0; i < prizes.length; i++) {
                        for (int j = i + 1; j < prizes.length; j++) {
                            collide(prizes[i], prizes[j]);
                        }
                    }
                    for (Prize prize : prizes) {
                        contain(prize);
                    }
                }
            }

            float scale = (float) (1 - (1 - ACTIVE_SCALE) * smoothstep(0, 0.10, time) * (1 - smoothstep(SETTLED, DURATION, time)));
            for (int i = 0; i < prizes.length; i++) {
                frames[frame][i] = new PrizePose(prizes[i].center, prizes[i].angle, scale);
            }
        }

        for (int i = 0; i < prizes.length; i++) {
            double restingAngle = Math.rint(prizes[i].angle / TURN) * TURN;
            for (int frame = 0; frame <= LAST_FRAME; frame++) {
                double settle = smoothstep(1.98, SETTLED, frame * STEP);
                PrizePose pose = frames[frame][i];
                frames[frame][i] = new PrizePose(pose.center.lerp(HOME[i], settle),
                        Mth.lerp(settle, pose.spin, restingAngle), pose.scale);
            }
        }
        return frames;
    }

    private static void collide(Prize a, Prize b) {
        Vec3 difference = b.center.subtract(a.center);
        double distance = difference.length();
        if (distance >= MAX_CONTACT_DISTANCE) {
            return;
        }
        Vec3 normal = distance > 1e-5 ? difference.scale(1 / distance) : new Vec3(1, 0, 0);
        var first = new Vector3d(a.center.x, a.center.y, a.center.z);
        var second = new Vector3d(b.center.x, b.center.y, b.center.z);
        if (!overlaps(a, first, b, second)) {
            return;
        }

        // Find the first non-overlapping position along the line between the two centers.
        double low = 0;
        double high = MAX_CONTACT_DISTANCE - distance + 0.001;
        var offset = new Vector3d();
        for (int iteration = 0; iteration < 9; iteration++) {
            double shift = (low + high) * 0.5;
            offset.set(second.x + normal.x * shift, second.y + normal.y * shift, second.z + normal.z * shift);
            if (overlaps(a, first, b, offset)) {
                low = shift;
            } else {
                high = shift;
            }
        }
        Vec3 separation = normal.scale(high * 0.5);
        a.center = a.center.subtract(separation);
        b.center = b.center.add(separation);
        Vec3 relative = b.velocity.subtract(a.velocity);
        double closingSpeed = relative.dot(normal);
        if (closingSpeed < 0) {
            Vec3 bounce = normal.scale(-closingSpeed * 0.925);
            a.velocity = a.velocity.subtract(bounce);
            b.velocity = b.velocity.add(bounce);
            double twist = (relative.x * normal.z - relative.z * normal.x) * 0.14;
            a.angularVelocity = Mth.clamp(a.angularVelocity + twist, -32, 32);
            b.angularVelocity = Mth.clamp(b.angularVelocity - twist, -32, 32);
        }
    }

    private static boolean overlaps(Prize a, Vector3d aCenter, Prize b, Vector3d bCenter) {
        return Intersectiond.testObOb(aCenter, a.xAxis, a.yAxis, a.zAxis, HALF_EXTENTS,
                bCenter, b.xAxis, b.yAxis, b.zAxis, HALF_EXTENTS);
    }

    private static void contain(Prize prize) {
        Vec3 pos = prize.center;
        double x = HALF_SIZE * (Math.abs(prize.xAxis.x) + Math.abs(prize.yAxis.x) + Math.abs(prize.zAxis.x));
        double y = HALF_SIZE * (Math.abs(prize.xAxis.y) + Math.abs(prize.yAxis.y) + Math.abs(prize.zAxis.y));
        double z = HALF_SIZE * (Math.abs(prize.xAxis.z) + Math.abs(prize.yAxis.z) + Math.abs(prize.zAxis.z));
        Vec3 bounds = new Vec3(Mth.clamp(pos.x, 1.02 + x, 14.98 - x), Mth.clamp(pos.y, 11.02 + y, 23.98 - y),
                Mth.clamp(pos.z, 1.02 + z, 14.98 - z));
        Vec3 velocity = prize.velocity;
        prize.velocity = new Vec3(reflect(pos.x, bounds.x, velocity.x), reflect(pos.y, bounds.y, velocity.y),
                reflect(pos.z, bounds.z, velocity.z));
        prize.center = bounds;
    }

    private static double reflect(double coordinate, double boundary, double velocity) {
        return (coordinate - boundary) * velocity > 0 ? -velocity * 0.83 : velocity;
    }

    private static double smoothstep(double start, double end, double value) {
        double t = Mth.clamp((value - start) / (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }

    record PrizePose(Vec3 center, double spin, float scale) {}

    private static final class Prize {
        private final Quaterniond rest;
        private final Quaterniond orientation = new Quaterniond();
        private final Vector3d xAxis = new Vector3d();
        private final Vector3d yAxis = new Vector3d();
        private final Vector3d zAxis = new Vector3d();
        private Vec3 center;
        private Vec3 velocity;
        private double angle;
        private double angularVelocity;

        private Prize(int index, double xVelocity, double yVelocity, double zVelocity, double spin, Quaterniond rest) {
            this.center = HOME[index];
            this.velocity = new Vec3(xVelocity, yVelocity, zVelocity);
            this.angularVelocity = spin;
            this.rest = rest;
        }

        private void updateAxes() {
            orientation.rotationY(angle).mul(rest);
            orientation.transformPositiveX(xAxis);
            orientation.transformPositiveY(yAxis);
            orientation.transformPositiveZ(zAxis);
        }
    }
}
