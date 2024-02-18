package artofillusion.tools;

import artofillusion.object.NullObject;
import artofillusion.object.Object3D;
import artofillusion.ui.Translate;

import java.util.Optional;

public class NullFactory implements PrimitiveFactory {

    @Override
    public String getName() {
        return Translate.text("menu.null");
    }
    @Override
    public String getCategory() {
        return "Other";
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new NullObject());
    }

}
