/*
 * Copyright 2018 Veeam Software.
 * 
 * Created by Maksim Khramov
 * Date: Jul 4, 2018.
 */
package artofillusion.object;

import artofillusion.Scene;
import artofillusion.animation.Keyframe;
import java.util.Enumeration;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class ObjectCollectionTest {
    @Test
    public void testObjectCollectionIsClosed() {
        ObjectCollection oc = new ObjectCollection() {
            @Override
            protected Enumeration<ObjectInfo> enumerateObjects(ObjectInfo info, boolean interactive, Scene scene) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Object3D duplicate() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void copyObject(Object3D obj) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void setSize(double xsize, double ysize, double zsize) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public Keyframe getPoseKeyframe() {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void applyPoseKeyframe(Keyframe k) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
        
        System.out.println(oc.isClosed());
    }
}
