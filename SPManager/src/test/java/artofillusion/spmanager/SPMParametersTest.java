/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.spmanager;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class SPMParametersTest {
    
    @Test
    public void testGetSPMParametersGetRepositories() {
        SPMParameters spp  = new SPMParameters();
        String[] rp = spp.getRepositories();
        Assert.assertNotNull(rp);
        Assert.assertEquals(1, rp.length);
        String r1 = rp[0];
        System.out.println(r1);
        Assert.assertEquals("http://aoisp.sourceforge.net/AoIRepository", r1);
    }
}
