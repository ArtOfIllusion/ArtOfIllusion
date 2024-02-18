package artofillusion.tools;

import artofillusion.object.Object3D;
import artofillusion.object.ProceduralPointLight;
import artofillusion.ui.Translate;

import java.util.Optional;

public class ProceduralPointLightFactory implements PrimitiveFactory {

    @Override
    public String getName() {
        return Translate.text("menu.proceduralPointLight");
    }

    @Override
    public String getCategory() {
        return "Lights";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new ProceduralPointLight(0.1));
    }
}
