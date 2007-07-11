/* Copyright (C) 2007 by Peter Eastman

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion;

import junit.framework.*;

import java.util.*;

import artofillusion.raytracer.*;
import artofillusion.object.*;
import artofillusion.math.*;
import artofillusion.texture.*;

public class TestOctreeNode extends TestCase
{
  private OctreeNode rootNode;

  protected void setUp() throws Exception
  {
    // Create a scene for testing.

    Texture tex = new UniformTexture();
    ArrayList objectList = new ArrayList();
    for (int i = 0; i < 100; i++)
    {
      Vec3 pos = new Vec3(Math.random()*10, Math.random()*10, 0.0);
      objectList.add(createSphere(pos, tex));
    }
    for (int i = 0; i < 100; i++)
    {
      Vec3 pos = new Vec3(Math.random()*10, 0.0, Math.random()*10);
      objectList.add(createSphere(pos, tex));
    }
    for (int i = 0; i < 500; i++)
    {
      Vec3 pos = new Vec3(Math.random()*10, Math.random()*10, Math.random()*10);
      objectList.add(createSphere(pos, tex));
    }
    RTObject objects[] = (RTObject []) objectList.toArray(new RTObject[objectList.size()]);

    // Create an octree for it.

    BoundingBox objBounds[] = new BoundingBox [objects.length];
    double minx, maxx, miny, maxy, minz, maxz;
    minx = miny = minz = Double.MAX_VALUE;
    maxx = maxy = maxz = -Double.MAX_VALUE;
    for (int i = 0; i < objects.length; i++)
      {
        objBounds[i] = objects[i].getBounds();
        if (objBounds[i].minx < minx)
          minx = objBounds[i].minx;
        if (objBounds[i].maxx > maxx)
          maxx = objBounds[i].maxx;
        if (objBounds[i].miny < miny)
          miny = objBounds[i].miny;
        if (objBounds[i].maxy > maxy)
          maxy = objBounds[i].maxy;
        if (objBounds[i].minz < minz)
          minz = objBounds[i].minz;
        if (objBounds[i].maxz > maxz)
          maxz = objBounds[i].maxz;
      }
    rootNode = new OctreeNode(minx, maxx, miny, maxy, minz, maxz, objects, objBounds, null);
  }

  private RTSphere createSphere(Vec3 pos, Texture tex)
  {
    Sphere sphere = new Sphere(0.5, 0.5, 0.5);
    sphere.setTexture(tex, tex.getDefaultMapping(sphere));
    return new RTSphere(sphere, Mat4.translation(pos.x, pos.y, pos.z), Mat4.translation(-pos.x, -pos.y, -pos.z), new double[0]);
  }

  /**
   * Test the findNode() method.
   */

  public void testFindNode()
  {
    for (int i = 0; i < 1000; i++)
    {
      Vec3 pos = new Vec3(Math.random()*(rootNode.maxx-rootNode.minx)+rootNode.minx,
                          Math.random()*(rootNode.maxy-rootNode.miny)+rootNode.miny,
                          Math.random()*(rootNode.maxz-rootNode.minz)+rootNode.minz);
      OctreeNode node = rootNode.findNode(pos);
      assertTrue(node.contains(pos));
      assertNotNull(node.getObjects()); // Make tsure it's a terminal node.
    }
    for (int i = 0; i < 1000; i++)
    {
      Vec3 pos = new Vec3(20.0*Math.random()-5.0, 20.0*Math.random()-5.0, 20.0*Math.random()-5.0);
      assertEquals(rootNode.contains(pos), rootNode.findNode(pos) != null);
    }
  }

  /**
   * Test tracing a ray through the octree, and make sure it hits all the right nodes.
   */

  public void testTraceRay()
  {
    Ray r = new Ray(null);
    for (int i = 0; i < 1000; i++)
    {
      // Select a random ray direction and origin outside the scene.

      r.getOrigin().set(Math.random(), Math.random(), Math.random());
      r.getOrigin().normalize();
      r.getOrigin().scale(10.0);
      r.getOrigin().add(new Vec3(5.0, 5.0, 5.0));
      r.getDirection().set(Math.random()-0.5, Math.random()-0.5, Math.random()-0.5);
      r.getDirection().normalize();

      // Find every leaf node that the ray passes through.

      HashSet intersections = new HashSet();
      findIntersectingNodes(rootNode, r, intersections);

      // Now trace the ray through the tree and see if it hits all of the correct nodes.

      OctreeNode node = rootNode.findFirstNode(r);
      while (node != null)
      {
        assertTrue(intersections.contains(node));
        intersections.remove(node);
        OctreeNode nextNode = node.findNextNode(r);
        assertNotSame(nextNode, node);
        node = nextNode;
      }
      assertEquals(0, intersections.size()); // Make sure we hit them all.
    }
  }

  private void findIntersectingNodes(OctreeNode node, Ray ray, Set intersections)
  {
    if (!ray.intersects(node))
      return;
    if (node.getObjects() == null)
    {
      OctreeNode children[] = node.findChildNodes();
      for (int i = 0; i < children.length; i++)
        findIntersectingNodes(children[i], ray, intersections);
    }
    else
      intersections.add(node);
  }
}
