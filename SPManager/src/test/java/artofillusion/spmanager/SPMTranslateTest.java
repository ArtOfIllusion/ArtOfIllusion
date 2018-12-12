/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package artofillusion.spmanager;

import artofillusion.ui.Translate;
import org.junit.Test;

/**
 *
 * @author maksim.khramov
 */
public class SPMTranslateTest {
    
    public SPMTranslateTest() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void hello() {
        System.out.println(Translate.label("spmanager:label.okMsg").getText());
//        ApplicationPreferences ap = artofillusion.ArtOfIllusion.getPreferences();
//        SPMTranslate.setLocale(ap.getLocale());
//        System.out.println(SPMTranslate.bLabel("errMsg").getText());
    }
}
