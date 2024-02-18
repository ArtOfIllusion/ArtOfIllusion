package artofillusion.tools;

import artofillusion.object.Object3D;

import java.util.Optional;

public interface PrimitiveFactory {
    String getCategory();

    String getName();

    default String getObjectName() {
        return getName();
    }
    Optional<Object3D> create();
}
