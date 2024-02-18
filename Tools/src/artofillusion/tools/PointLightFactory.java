package artofillusion.tools;

import artofillusion.math.RGBColor;
import artofillusion.object.Object3D;
import artofillusion.object.PointLight;
import artofillusion.ui.Translate;

import java.util.Optional;

public class PointLightFactory implements PrimitiveFactory {
    @Override
    public String getName() {
        return Translate.text("menu.pointLight");
    }

    @Override
    public String getCategory() {
        return "Lights";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new PointLight(new RGBColor(1.0f, 1.0f, 1.0f), 1.0f, 0.1));
    }
}
