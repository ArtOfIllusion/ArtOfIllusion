/* Copyright (C) 2016 by Petri Ihalainen
   Changes Copyright 2016 by Petri Ihalainen

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.view;

import artofillusion.math.*;
import artofillusion.ui.*;
import artofillusion.object.*;
import artofillusion.*;
import java.awt.*;

public class ClickedPointFinder
{
	private TriangleMath T = new TriangleMath();
	private double[] bary;

	public ClickedPointFinder(){};

	/** 
	 *  Return the closest point on a surface of an object that is found under 
	 *  a given point on the view. If no object surface is found then a point at  
	 *  view.distToPlane is returned. 
	 */
	public Vec3 newPoint(ViewerCanvas v, Point point)
	{
		Vec3 clickedPoint = v.getCamera().convertScreenToWorld(point, v.getDistToPlane());
		Vec3 pointOnTriangle;
		boolean inSpace = true;
		bary = new double[3];

		Vec3 cameraOrigin = v.getCamera().getCameraCoordinates().getOrigin();
		Vec3 cameraZ = v.getCamera().getCameraCoordinates().getZDirection();

		Mat4 objToScreen = v.getCamera().getObjectToScreen();
		Mat4 worldToScreen = v.getCamera().getWorldToScreen();
		Mat4 toScene, toThisObject, fromExtToLocal;
		ObjectInfo[] objList = renderableObjects(v);
		RenderingMesh rMesh;
		Vec3[] corner3D = new Vec3[3];
		Vec2[] corner2D = new Vec2[3];
		Vec2 point2D = new Vec2(point.x, point.y);
		CoordinateSystem localCoords;

		localCoords = getLocalCoords(v);
		toThisObject = localCoords.toLocal();

		for (int i = 0; i< objList.length ; i++)
		{	
			rMesh = objList[i].getPreviewMesh();
			toScene = objList[i].getCoords().fromLocal();
			fromExtToLocal = toThisObject.times(toScene);

			for (int j = 0; j < rMesh.triangle.length; j++)
			{
				corner3D[0] = new Vec3 (fromExtToLocal.times(rMesh.vert[rMesh.triangle[j].v1]));
				corner3D[1] = new Vec3 (fromExtToLocal.times(rMesh.vert[rMesh.triangle[j].v2]));
				corner3D[2] = new Vec3 (fromExtToLocal.times(rMesh.vert[rMesh.triangle[j].v3]));				

				corner2D[0] = worldToScreen.timesXY(corner3D[0]);
				corner2D[1] = worldToScreen.timesXY(corner3D[1]);
				corner2D[2] = worldToScreen.timesXY(corner3D[2]);
				
				if (v instanceof ObjectViewer && ! ((ObjectViewer)v).getUseWorldCoords())
				{
					corner2D[0] = objToScreen.timesXY(corner3D[0]);
					corner2D[1] = objToScreen.timesXY(corner3D[1]);
					corner2D[2] = objToScreen.timesXY(corner3D[2]);
				}
				if (onTriangle(corner2D, point2D))
				{	
					(corner3D[0] = corner3D[0]).scale(bary[0]);
					(corner3D[1] = corner3D[1]).scale(bary[1]);
					(corner3D[2] = corner3D[2]).scale(bary[2]);
				
					pointOnTriangle = new Vec3(corner3D[0].plus(corner3D[1].plus(corner3D[2])));
					if (inSpace)
					{	
						clickedPoint = pointOnTriangle;
						inSpace = false;
					}
					else
						if (closer(pointOnTriangle, clickedPoint, cameraOrigin, cameraZ) && onFrontSide(pointOnTriangle, cameraOrigin, cameraZ))
							clickedPoint = pointOnTriangle;
				}
			}
		}
		return clickedPoint;
	}

	private boolean closer(Vec3 p1, Vec3 p2, Vec3 cameraOrigin, Vec3 cameraZ)
	{
		double distTo1 = p1.minus(cameraOrigin).dot(cameraZ);
		double distTo2 = p2.minus(cameraOrigin).dot(cameraZ);

		System.out.println("distTo1 = " + distTo1);
		System.out.println("distTo2 = " + distTo2);
		
		if (distTo1 < distTo2)
			return true;
		else
			return false;
	}

	private boolean onFrontSide(Vec3 p, Vec3 cameraOrigin, Vec3 cameraZ)
	{
		double distToP = p.minus(cameraOrigin).dot(cameraZ);

		System.out.println("distToP = " + distToP);
		
		if (distToP > 0.0)
			return true;
		else
			return false;
	}

	private boolean onTriangle(Vec2[] corner2D, Vec2 point2D)
	{
		// The sum of barycentric coordinates is always 1.0. 
		// If the point is outside, some of those will be negative. 
		bary = T.bary(corner2D, point2D);		
		if(bary[0] >= 0.0 && bary[1] >= 0.0 && + bary[2] >= 0.0) 
			return true;
		else
			return false;
	}

	private ObjectInfo[] renderableObjects(ViewerCanvas v)
	{
		Scene scene = v.getScene();
		int n = scene.getNumObjects();
		int m = 0;
		ObjectInfo[] rObjI = new ObjectInfo[n];
		
		for(int i = 0; i < n; i++)
		{
			ObjectInfo oi = scene.getObject(i);
			if (oi.isVisible() && oi.getObject().canSetTexture())
			{
				rObjI[m] = oi;
				m++;
			}
		}
		
		ObjectInfo[] vObjI = new ObjectInfo[m];
		
		for(int i = 0; i < m; i++)
		{
			vObjI[i] = rObjI[i];
		}
		System.out.println(vObjI);
		return vObjI;
	}
/*
	// These two were supposed to be a pre-check for each mesh, 
	// whether to check the individual triangles or not
	private boolean boxInView(ViewerCanvas v, ObjectInfo oi)
	{
		return true;
	}
	
	private boolean clickOnBox(ViewerCanvas v, ObjectInfo oi)
	{
		return true;
	}
*/
	CoordinateSystem getLocalCoords(ViewerCanvas v)
	{
		if (v instanceof ObjectViewer && ! ((ObjectViewer)v).getUseWorldCoords())
		{
			((ObjectViewer)v).setUseWorldCoords(true);
			CoordinateSystem c = ((ObjectViewer)v).getDisplayCoordinates().duplicate();
			((ObjectViewer)v).setUseWorldCoords(false);
			return c;
		}
		else
			return new CoordinateSystem();
	}
 }