/* Copyright (C) 2024 by Maksim Khramov

   This program is free software; you can redistribute it and/or modify it under the
   terms of the GNU General Public License as published by the Free Software
   Foundation; either version 2 of the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful, but WITHOUT ANY
   WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
   PARTICULAR PURPOSE.  See the GNU General Public License for more details. */

package artofillusion.tools;

import artofillusion.object.Cylinder;
import artofillusion.object.Object3D;
import artofillusion.ui.Translate;

import java.util.Optional;

public class CylinderFactory implements PrimitiveFactory {
    @Override
    public String getCategory() {
        return "Geometry";
    }

    @Override
    public String getName() {
        return Translate.text("menu.cylinder");
    }

    @Override
    public Optional<Object3D> create() {
        return Optional.of(new Cylinder(1.0, 0.5, 0.5, 1.0));
    }
}
