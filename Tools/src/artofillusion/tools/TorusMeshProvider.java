/* Copyright (C) 2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */


package artofillusion.tools;

import artofillusion.math.Vec3;
import artofillusion.object.Mesh;
import artofillusion.object.Object3D;
import artofillusion.object.SplineMesh;


import java.util.Arrays;
import java.util.Optional;

public class TorusMeshProvider implements PrimitiveFactory {

    private static final double thickness = 0.5;
    private int uSize = 5;
    private int vSize = 5;

    @Override
    public String getCategory() {
        return "Geometry";
    }

    @Override
    public String getName() {
        return "Torus";
    }

    @Override
    public Optional<Object3D> create() {
        float[] uSmoothness = new float [uSize];
        float[] vSmoothness = new float [vSize];

        Arrays.fill(uSmoothness, 1.0f);
        Arrays.fill(vSmoothness, 1.0f);

        Vec3[][] v = getMeshPoints(1.0, 1.0);

        SplineMesh torus = new SplineMesh(v, uSmoothness, vSmoothness, Mesh.APPROXIMATING,true, true);

        return Optional.of(torus);
    }

    private Vec3[][] getMeshPoints(double xSize, double ySize) {
        double rad = Math.min(xSize, ySize) * 0.25 * thickness;
        double radX = xSize * 0.5 - rad;
        double radY = ySize * 0.5 - rad;
        double uScale = 2.0 * Math.PI / uSize;
        double vScale = 2.0 * Math.PI / vSize;
        Vec3 vr = new Vec3();
        Vec3 vc = new Vec3();

        Vec3[][] v = new Vec3 [uSize][vSize];

        for (int i = 0; i < uSize; i++) {
            double uScaleI = uScale * i;
            vc.set(radX * Math.cos(uScaleI), radY * Math.sin(uScaleI), 0.0);
            vr.set(rad * Math.cos(uScaleI), rad * Math.sin(uScaleI), 0.0);
            for (int j = 0; j < vSize; j++) {
                double vScaleJ = vScale * j;
                v[i][j] = new Vec3(vc.x + vr.x * Math.cos(vScaleJ), vc.y + vr.y * Math.cos(vScaleJ), rad * Math.sin(vScaleJ));
            }
        }

        return v;
    }
}
