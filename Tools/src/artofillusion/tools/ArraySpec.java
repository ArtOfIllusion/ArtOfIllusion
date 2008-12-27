/* The ArrayParam class defines parameters for the array tool.
   This also sets the default values in the dialog.*/

/* Copyright 2001 Rick van der Meiden

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. 
*/


package artofillusion.tools;

import artofillusion.*;
import artofillusion.math.*;
import artofillusion.object.*;

import java.util.*;

/** Parameters for creating arrays are stored in the ArraySpec class.
    From these parameters, an array can be created.
    @author Rick van der Meiden
*/
  
public class ArraySpec
{
        // --- constants

        /** defines that a linear array is to be created */
        public static final int METHOD_LINEAR = 0;
        /** defines that an array along a curve is to be created */
        public static final int METHOD_CURVE = 1;
        /** defines that the stepsize determines the number of copies along a curve */
        public static final int MODE_STEP = 0;
        /** defines that the number of copying along a curve is explicitly set by the user */
        public static final int MODE_COPIES = 1;

        private static final int SD_LEVEL = 5;

        // --- private data
        
        // where to create this array
        private LayoutWindow window;
        // undo record
        private UndoRecord undo;
        // froot object for grouping
        private ObjectInfo arrayRoot;

        // --- public data
        
        // general paramters

        /** Set to METHOD_LINEAR to create a liniar array.
            Set to METHOD_CURVE to create an array along a curve. */
        public int method;
        /** the objects that should be copied to create an array */
        public Vector objectList;         // list of objectInfo's

        // linear paramters
        /** Number of copies to make for a linar array. */
        public int linearCopies;                        
        /** step size in X direction for a linear array. */
        public double stepX;
        /** step size in Y direction for a linear array. */
        public double stepY;
        /** step size in Z direction for a linear array. */
        public double stepZ;
        /** when set, stepX is multiplied by the objects boudingbox x size */
        public boolean intervalX;
        /** when set, stepY is multiplied by the objects boudingbox y size */
        public boolean intervalY;
        /** when set, stepZ is multiplied by the objects boudingbox z size */
        public boolean intervalZ;

        // spline parameters
        /** the curve along which to create an array */
        public ObjectInfo curve;
        /** Set to MODE_STEP to determine the number of copies from the step size.
            Set to MODE_COPIES to use the curveCopies field explicitly */
        public int curveMode;
        /** The number of copies to make along a curve.
                Only used when curveMode = CURVE_COPIES */
        public int curveCopies;
        /**  the stepsize between copies.
                Only used when curveMode = CURVE_COPIES */
        public double curveStep;
        /** When set, curveStep is multiplied by the size of the object. Not Used. */
        public boolean curveInterval;
        /** When set, the orientationof the object(s) follow the curve.*/
        public boolean orientation;
        /** When set, the initial position of the object is ignored.
                The object will pivot abouts its origin instead of about a point on the path curve */
        public boolean ignoreOrigin;
        /** When set, the initial orientation of the object is ignored.
                The object will have a default orientation at the beginning of the path curve.*/
        public boolean ignoreOrientation;

        // general options

        /** If set, an instance of the object(s) will be created at
                first position of the array (linear displacement 0 or the
                beginning of the splien curve. */
        public boolean dupFirst;
        /** If set, group all the objects together as children of a null object. */
        public boolean group;
        /** If set, create live copies instead of regular copies) */
        public boolean live;
        /** If set, create copies of children too, positioned relative to the parent object(s) */
        public boolean deep;

        // public methods

        /** create an instance with default settings
        @param win window in which the array is to be created.
        */
        
        public ArraySpec (LayoutWindow win)
        {
                this.window = win;

                // set list of object to copy (all selected objects)
                Scene scene = window.getScene();
                int selection[] = window.getSelectedIndices();
                objectList = new Vector();         // list of objectInfo's
                for (int sel=0; sel<selection.length;sel++)
                {
                        ObjectInfo info = scene.getObject(selection[sel]);
                        objectList.addElement(info);
                }

                // set paramters
                method = METHOD_LINEAR;
                linearCopies = 10;
                stepX = 1;
                stepY = 0;
                stepZ = 0;
                intervalX = true;
                intervalY = false;
                intervalZ = false;
        
                curve = null;
                curveMode=MODE_COPIES; 
                curveCopies = 10;
                curveStep = 0.1;
                orientation = true;
                ignoreOrigin = true;
                ignoreOrientation = true;
        
                dupFirst = true;
                group = true;
                deep = true;
                live = false;

        }
        
  /** create an array of objects with current paramter settings */ 
     
  public void createArray ()
  {
        undo = new UndoRecord(window, false);

        // create a group object, even when not grouping
        ObjectInfo info = (ObjectInfo)objectList.elementAt(0);
        String name = "Array of " + info.getName();

        CoordinateSystem coords = new CoordinateSystem(new Vec3(0,0,0), Vec3.vz(), Vec3.vy());
        if (method == METHOD_CURVE && curve != null)
                coords = curve.getCoords().duplicate();
        else if (method == METHOD_LINEAR)
        {
                Vec3 displacement = new Vec3(stepX, stepY, stepZ);
                BoundingBox bounds = info.getObject().getBounds();
                if (intervalX) displacement.x *= bounds.getSize().x;
                if (intervalY) displacement.y *= bounds.getSize().y;
                if (intervalZ) displacement.z *= bounds.getSize().z;
                displacement = displacement.times(((double)linearCopies-1.0)/2.0);

                coords = info.getCoords().duplicate();
                Mat4 dis = Mat4.translation(displacement.x,
                                            displacement.y,
                                            displacement.z);
                coords.transformOrigin(dis);
        }

        arrayRoot = new ObjectInfo(new NullObject(), coords, name);

        if (group)
        {
                window.addObject (arrayRoot, undo);
        }

        if (method == METHOD_LINEAR)
                createLinearArray();
        else if (method == METHOD_CURVE)
                createCurveArray();

        window.setUndoRecord(undo);
        window.rebuildItemList();
        window.updateImage();
        
  }

  /** create an array of linearly translated objects */
  private void createLinearArray ()
  {
        if (method != METHOD_LINEAR)
                return;

        for (int i=0; i<objectList.size(); i++)
        {
                // get object
                ObjectInfo info = (ObjectInfo)objectList.elementAt(i);

                // calculate displacement vector
                Vec3 displacement = new Vec3(stepX, stepY, stepZ);
                BoundingBox bounds = info.getObject().getBounds();
                if (intervalX) displacement.x *= bounds.getSize().x;
                if (intervalY) displacement.y *= bounds.getSize().y;
                if (intervalZ) displacement.z *= bounds.getSize().z;

                int start = (dupFirst==true ? 0 : 1);
                for (int n=start; n<linearCopies; n++)
                        createCopy (info, displacement.times(n));
        }

        window.updateImage();
  }

  /** create an array of objects tranlated along a curve */
  private void createCurveArray ()
  {
        if (method != METHOD_CURVE)
                return;

        if (curve == null) return;
        Curve cv = (Curve) curve.getObject();

        // map curve to global coordinate space
        MeshVertex vert[] = cv.getVertices();
        Vec3 v[] = new Vec3 [vert.length];
        Mat4 trans = curve.getCoords().fromLocal();
        for (int i = 0; i < v.length; i++)
          v[i] = trans.times(vert[i].r);

        // subdive curve
        Vec3 subdiv[] = new Curve(v, cv.getSmoothness(), cv.getSmoothingMethod(), cv.isClosed()).subdivideCurve(SD_LEVEL).getVertexPositions();
        // Vec3 startPoint = findPointOnCurve(subdiv, cv.isClosed(), 0);
        // Vec3 startPoint = v[0];
        int startCount = (dupFirst==true ? 0 : 1);
        double curveLength = calcCurveLength(cv);
        if (curveMode == MODE_COPIES)
        {
                if (curveCopies < 1) curveCopies = 1;
                curveStep = curveLength / (curveCopies + (cv.isClosed() ? 1 : 0));
        }
        else
        {
                if (curveStep / curveLength < 0.001) curveStep = 0.001*curveLength;
                curveCopies = (int)(curveLength / curveStep);
        }
        // find orientation for all subdivision vertices
        Vec3 zdir[] = new Vec3 [subdiv.length];
        Vec3 updir[] = new Vec3 [subdiv.length];
        constructMinRotFrame(subdiv, cv.isClosed(), zdir, updir);
        // coordinate system at first point on curve
        CoordinateSystem startCS = new CoordinateSystem(subdiv[0],zdir[0],updir[0]);
        
        // for all objects in selection
        for (int i=0; i<objectList.size(); i++)
        {
                // get object
                ObjectInfo info = (ObjectInfo)objectList.elementAt(i);

                for (int n=startCount; n<curveCopies; n++)
                {
                        // determine displacement
                        Vec3 updir_return = new Vec3(0,0,0), zdir_return = new Vec3(0,0,0);
                        double relativePos = n*curveLength / ((curveCopies-1) + (cv.isClosed() ? 1 : 0));

                        CoordinateSystem curveCS = findCoordinateSystem(subdiv, cv.isClosed(), relativePos, zdir, updir);

                        CoordinateSystem newCS = info.getCoords().duplicate();
                        if (ignoreOrigin)
                                newCS.setOrigin(startCS.getOrigin());
                        if (ignoreOrientation)
                                newCS.setOrientation(startCS.getZDirection(), startCS.getUpDirection());    

                        newCS.transformOrigin(startCS.toLocal());
                        newCS.transformAxes(startCS.toLocal());
                        newCS.transformOrigin(curveCS.fromLocal());
                        if (orientation)
                                newCS.transformAxes(curveCS.fromLocal());

                        createCopy (info, newCS);
                        
                }
        }

        window.updateImage();
        
  }

  /** create a copy of an object and displace it. If live = true then make a
     live duplicate instead of a copy */
  private ObjectInfo createCopy (ObjectInfo info, Vec3 displacement)
  {
        Mat4 trans = Mat4.translation(displacement.x, displacement.y, displacement.z);
        return createCopy(info, trans);
  }

  /** create a copy of an object and place it at cs.
     If live = true then make a live duplicate instead of a copy */
  private ObjectInfo createCopy (ObjectInfo info, CoordinateSystem cs)
  {
        Mat4 trans = cs.fromLocal().times(info.getCoords().toLocal());
        return createCopy(info, trans);
  }

  /** create a copy (or live duplicate) of an object,
      tranform its coordinate system and add it to the group object
  */

  private ObjectInfo createCopy (ObjectInfo info, Mat4 trans)
  {
        ObjectInfo newinfo = info.duplicate();
        if (!live)
                newinfo.setObject(info.object.duplicate());
        newinfo.getCoords().transformCoordinates(trans);
        window.addObject(newinfo, undo);
        if (group)
                arrayRoot.addChild(newinfo, 0);

        if (deep)
        {
                for (int i=0; i< info.getChildren().length;i++)
                {
                        newinfo.addChild(createChildCopy(info.getChildren()[i], trans),i);
                }
        }

        return newinfo;
  }

  /** create a copy (or live diplicate) of an object, add it to  and tranform
        its coordinate system. */

  private ObjectInfo createChildCopy (ObjectInfo info, Mat4 trans)
  {
        ObjectInfo newinfo = info.duplicate();
        if (!live)
                newinfo.setObject(info.getObject().duplicate());
        newinfo.getCoords().transformCoordinates(trans);
        window.addObject(newinfo, undo);
        
        for (int i=0; i< info.getChildren().length;i++)
        {
                newinfo.addChild(createChildCopy(info.getChildren()[i], trans),i);
        }
        
        return newinfo;
  }


  /** calculate the length of a curve given some error determined by SD_LEVEL */
  private double calcCurveLength(Curve c)
  {
        Vec3 subdiv[] = c.subdivideCurve(SD_LEVEL).getVertexPositions();
        double sum = 0;

        for (int i=1;i<subdiv.length;i++)
                sum += subdiv[i].distance(subdiv[i-1]);

        if (c.isClosed())
                sum += subdiv[subdiv.length-1].distance(subdiv[0]);

        return sum;
  }

  /** determines a point on the curve for which the distance to
     the first point is given by relativePosition */
  private Vec3 findPointOnCurve(Vec3 subdiv[], boolean isClosed, double relativePosition)
  {
        // find interval around relativePosition
        int i;
        double sum=0, prevsum=0;
        for (i=1;i<subdiv.length;i++)
        {
                prevsum = sum;
                sum += subdiv[i].distance(subdiv[i-1]);
                if (sum >= relativePosition)
                        break;
        }
        int prev_i = i-1;
        if (sum < relativePosition)
        {
                if (isClosed)
                {
                        prevsum = sum;
                        sum += subdiv[subdiv.length-1].distance(subdiv[0]);
                        prev_i = subdiv.length-1;
                        i = 0;
                }
                else
                {
                        i = i-1;
                        prev_i = i-2;
                }
        }

        // find point by linear interpolation in this interval
        double interval = sum-prevsum;
        
        return subdiv[i].times((relativePosition - prevsum)/interval).plus(subdiv[prev_i].times((sum - relativePosition)/interval));
  }

  /** Construct the Minimally Rotating Frame at every point along the path.
       sets values in zdir[] and updir[]
       reused code from Peter Eastman's Extrude Tool */
  private void constructMinRotFrame(Vec3 subdiv[], boolean isClosed, Vec3 zdir[], Vec3 updir[])
  {
    Vec3 t[];
    int i, j;

    // subdivide the path and determine its direction at the starting point.
    
    t = new Vec3 [subdiv.length];
    t[0] = subdiv[1].minus(subdiv[0]);
    t[0].normalize();
    zdir[0] = Vec3.vz();
    updir[0] = Vec3.vy();
    
    // Now find two vectors perpendicular to the path, and determine how much they
    // contribute to the z and up directions.
    
    Vec3 dir1, dir2;
    double zfrac1, zfrac2, upfrac1, upfrac2;
    zfrac1 = t[0].dot(zdir[0]);
    zfrac2 = Math.sqrt(1.0-zfrac1*zfrac1);
    dir1 = zdir[0].minus(t[0].times(zfrac1));
    dir1.normalize();
    upfrac1 = t[0].dot(updir[0]);
    upfrac2 = Math.sqrt(1.0-upfrac1*upfrac1);
    dir2 = updir[0].minus(t[0].times(upfrac1));
    dir2.normalize();
    
    // Propagate the vectors along the path.
    
    for (i = 1; i < subdiv.length; i++)
      {
	if (i == subdiv.length-1)
	  {
            if (isClosed)
	      t[i] = subdiv[0].minus(subdiv[subdiv.length-2]);
	    else
	      t[i] = subdiv[subdiv.length-1].minus(subdiv[subdiv.length-2]);
	  }
	else
	  t[i] = subdiv[i+1].minus(subdiv[i-1]);
	t[i].normalize();

        dir1 = dir1.minus(t[i].times(t[i].dot(dir1)));
        dir1.normalize();
        dir2 = dir2.minus(t[i].times(t[i].dot(dir2)));
        dir2.normalize();
        zdir[i] = t[i].times(zfrac1).plus(dir1.times(zfrac2));
        updir[i] = t[i].times(upfrac1).plus(dir2.times(upfrac2));

      }


  }

  /** determines the coordinatesystem that specifies position and
     orienation vectors at the point on the curve for which the distance to
     the first point is given by relativePosition */

  private CoordinateSystem findCoordinateSystem(Vec3 subdiv[], boolean isClosed, double relativePosition, Vec3 zdirs[], Vec3 updirs[])
  {
        // find interval around relativePosition
        int i, prev_i;
        double sum=0, prevsum=0;
        for (i=1;i<subdiv.length;i++)
        {
                prevsum = sum;
                sum += subdiv[i].distance(subdiv[i-1]);
                if (sum >= relativePosition)
                        break;
        }
        prev_i = i-1;
        if (sum < relativePosition)
        {
                if (isClosed)
                {
                        prevsum = sum;
                        sum += subdiv[subdiv.length-1].distance(subdiv[0]);
                        prev_i = subdiv.length-1;
                        i = 0;
                }
                else
                {
                        i = i-1;
                        prev_i = i-2;
                }
        }

        // find directions by linear interpolation in this interval
        double interval = sum-prevsum;
        Vec3 pos = subdiv[i].times((relativePosition - prevsum)/interval).plus(subdiv[prev_i].times((sum - relativePosition)/interval));
        Vec3 zdir = zdirs[i].times((relativePosition - prevsum)/interval).plus(zdirs[prev_i].times((sum - relativePosition)/interval));
        Vec3 updir = updirs[i].times((relativePosition - prevsum)/interval).plus(updirs[prev_i].times((sum - relativePosition)/interval));

        return new CoordinateSystem(pos,zdir,updir);
      
  }

}
