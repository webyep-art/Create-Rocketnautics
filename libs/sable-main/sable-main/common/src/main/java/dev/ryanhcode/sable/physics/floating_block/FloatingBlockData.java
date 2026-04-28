package dev.ryanhcode.sable.physics.floating_block;

import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableMathUtils;
import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FloatingBlockData {
    private static final Matrix3d tempMassMatrix = new Matrix3d();
    private static final Vector3d tempPosOffset = new Vector3d();
    protected Matrix3d outerProduct = new Matrix3d().scale(0);
    protected Vector3d weightedPosition = new Vector3d();
    protected double totalScale;
    protected int blockCount = 0;
    protected double latestPressureScale = 1;

    public void addFloatingBlock(final Vector3dc pos, final double scale) {
        this.addData(pos, scale);
        this.blockCount++;
    }

    public void removeFloatingBlock(final Vector3dc pos, final double scale) {
        this.blockCount--;
        this.addData(pos, -scale);
    }

    private void addData(final Vector3dc pos, final double scale) {
        this.weightedPosition.fma(scale, pos);
        this.totalScale += scale;


        pos.fma(-1 / this.totalScale, this.weightedPosition, tempPosOffset);
        if (this.blockCount > 0)
            SableMathUtils.fmaOuterProduct(tempPosOffset, tempPosOffset, scale * this.totalScale / (this.totalScale - scale), this.outerProduct);

        this.outerProduct.add(tempMassMatrix.identity().scale(scale / 6.0));
    }

    public void translateOrigin(final Vector3dc nudge) {
        this.weightedPosition.fma(this.totalScale, nudge);
    }

    public double getPressureScale()
    {
        return latestPressureScale;
    }
    static final Vector3d positionTemp = new Vector3d();
    public void computePressureScale(SubLevel subLevel)
    {
        subLevel.logicalPose().orientation().transform(weightedPosition, positionTemp);
        subLevel.logicalPose().position().fma(1 / totalScale, positionTemp, positionTemp);
        latestPressureScale = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), positionTemp);
    }
}
