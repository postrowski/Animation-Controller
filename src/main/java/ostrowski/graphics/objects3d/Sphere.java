
package ostrowski.graphics.objects3d;

import static org.lwjgl.opengl.GL11.*;

public class Sphere {
    private float radius;
    private int slices;
    private int stacks;

    public Sphere(float radius, int slices, int stacks) {
        this.radius = radius;
        this.slices = slices;
        this.stacks = stacks;
    }

    public void draw() {
        for (int i = 0; i < stacks; i++) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks);
            double z0  = Math.sin(lat0);
            double zr0 =  Math.cos(lat0);

            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            double z1 = Math.sin(lat1);
            double zr1 = Math.cos(lat1);

            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double lng = 2 * Math.PI * (double) (j - 1) / slices;
                double x = Math.cos(lng);
                double y = Math.sin(lng);

                glNormal3d(x * zr0, y * zr0, z0);
                glVertex3d(radius * x * zr0, radius * y * zr0, radius * z0);
                glNormal3d(x * zr1, y * zr1, z1);
                glVertex3d(radius * x * zr1, radius * y * zr1, radius * z1);
            }
            glEnd();
        }
    }
}
