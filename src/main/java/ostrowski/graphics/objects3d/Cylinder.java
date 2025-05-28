package ostrowski.graphics.objects3d;

import static org.lwjgl.opengl.GL11.*;

public class Cylinder {
    private int slices;
    private int stacks;
    private float radius;
    private float height;

    public Cylinder(float radius, float height, int slices, int stacks) {
        this.radius = radius;
        this.height = height;
        this.slices = slices;
        this.stacks = stacks;
    }

    public void draw() {
        float stackHeight = height / stacks;
        float stackRadiusStep = radius / stacks;

        // Draw the cylinder body
        for (int i = 0; i < stacks; i++) {
            float currRadius = radius - i * stackRadiusStep;
            float nextRadius = radius - (i + 1) * stackRadiusStep;
            float currHeight = i * stackHeight;
            float nextHeight = (i + 1) * stackHeight;

            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= slices; j++) {
                double angle = 2 * Math.PI * j / slices;
                float x = (float)Math.cos(angle);
                float z = (float)Math.sin(angle);

                // normal for lighting
                glNormal3f(x, 0, z);

                // vertex at current stack level
                glVertex3f(currRadius * x, currHeight, currRadius * z);

                // vertex at next stack level
                glVertex3f(nextRadius * x, nextHeight, nextRadius * z);
            }
            glEnd();
        }

        // Draw top cap
        glBegin(GL_TRIANGLE_FAN);
        glNormal3f(0, 1, 0);
        glVertex3f(0, height, 0); // center
        for (int i = 0; i <= slices; i++) {
            double angle = 2 * Math.PI * i / slices;
            float x = (float)Math.cos(angle);
            float z = (float)Math.sin(angle);
            glVertex3f(radius * x, height, radius * z);
        }
        glEnd();

        // Draw bottom cap
        glBegin(GL_TRIANGLE_FAN);
        glNormal3f(0, -1, 0);
        glVertex3f(0, 0, 0); // center
        for (int i = 0; i <= slices; i++) {
            double angle = 2 * Math.PI * i / slices;
            float x = (float)Math.cos(angle);
            float z = (float)Math.sin(angle);
            glVertex3f(radius * x, 0, radius * z);
        }
        glEnd();
    }
}
