/* Copyright (C) 2018 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY 
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */



package artofillusion.animation;

import artofillusion.Scene;
import artofillusion.TextureParameter;
import artofillusion.WireframeMesh;
import artofillusion.math.BoundingBox;
import artofillusion.math.Vec3;
import artofillusion.object.Curve;
import artofillusion.object.Mesh;
import artofillusion.object.Object3D;
import artofillusion.object.Tube;
import artofillusion.texture.ParameterValue;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;


/**
 *
 * @author maksim.khramov
 */

public class ActorTest
{

    @Test(expected = InvalidObjectException.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testLoadActorBadVersion() throws IOException
    {
        ByteBuffer wrap = ByteBuffer.allocate(200);
        wrap.putShort((short)1); // Actor Version 1. Expected exception to be thrown
        
        new Actor(new DataInputStream(new ByteArrayInputStream(wrap.array())), (Scene)null);

    }

    @Test
    public void testCreateActorForObject()
    {
        
        Object3D tube = new Tube(new Curve(new Vec3[] {new Vec3(), new Vec3()}, new float[] {0f, 1f}, Mesh.APPROXIMATING, false), new double[] {0f, 1f}, Tube.CLOSED_ENDS);
        Actor actor = new Actor(tube);
        
        Assert.assertNotNull(actor);
        Assert.assertEquals(1, actor.getNumGestures());
 
    }

    @Test
    public void testGetGestureIndexByIdFromSingleNoMatch()
    {
        Actor actor = new Actor(new Mock3DObject());
        Assert.assertEquals(-1, actor.getGestureIndex(8));
    }

    @Test
    public void testGetGestureIndexByIdFirst()
    {
        Actor actor = new Actor(new Mock3DObject());

        Assert.assertEquals(1, actor.getNumGestures());
        Assert.assertEquals(0, actor.getGestureIndex(0));

    }

    public void testGetGestureIndexByIdSecond() 
    {
        Actor actor = new Actor(new Mock3DObject());
        actor.addGesture(new MockKeyframe(), "Second");
        
        Assert.assertEquals(2, actor.getNumGestures());
        Assert.assertEquals(1, actor.getGestureIndex(1));
    }
    
    
    private class MockKeyframe implements Gesture {

        @Override
        public Gesture blend(Gesture[] p, double[] weight) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Skeleton getSkeleton() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setSkeleton(Skeleton s) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void textureChanged(TextureParameter[] oldParams, TextureParameter[] newParams) {            
        }

        @Override
        public ParameterValue getTextureParameter(TextureParameter param) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setTextureParameter(TextureParameter param, ParameterValue value) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe duplicate(Object owner) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public double[] getGraphValues() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setGraphValues(double[] values) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe blend(Keyframe o2, double weight1, double weight2) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe blend(Keyframe o2, Keyframe o3, double weight1, double weight2, double weight3) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe blend(Keyframe o2, Keyframe o3, Keyframe o4, double weight1, double weight2, double weight3, double weight4) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean equals(Keyframe k) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void writeToStream(DataOutputStream out) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
    private class Mock3DObject extends Object3D {

        @Override
        public Object3D duplicate() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void copyObject(Object3D obj) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public BoundingBox getBounds() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setSize(double xsize, double ysize, double zsize) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public WireframeMesh getWireframeMesh() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Keyframe getPoseKeyframe() {
            return new MockKeyframe();
        }

        @Override
        public void applyPoseKeyframe(Keyframe k) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    
    }

}
