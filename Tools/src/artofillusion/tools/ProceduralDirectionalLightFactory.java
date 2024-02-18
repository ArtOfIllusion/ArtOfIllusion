package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.object.ProceduralDirectionalLight;
import artofillusion.ui.Translate;

import java.util.Optional;


public class ProceduralDirectionalLightFactory implements PrimitiveFactory {

    @Override
    public String getName() {
        return Translate.text("menu.proceduralDirectionalLight");
    }

    @Override
    public String getCategory() {
        return "Lights";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of( new ProceduralDirectionalLight(1.0));
    }
}
