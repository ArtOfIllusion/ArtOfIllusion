package artofillusion.spmanager;


import artofillusion.spmanager.LocalSPMFileSystem;
import org.junit.Test;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author maksim.khramov
 */
public class TestLocalSPMFileSystem {
    
    public TestLocalSPMFileSystem() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void hello() {
        LocalSPMFileSystem lfs = new LocalSPMFileSystem();
        lfs.getStartupScripts();
    }
}
