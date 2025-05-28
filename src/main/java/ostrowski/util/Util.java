package ostrowski.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Util {

  /**
   * Projects a 3D point to window coordinates.
   * 
   * @param obj        the object coordinate (x,y,z)
   * @param modelView  the model-view matrix
   * @param projection the projection matrix
   * @param viewport   int[4] - {x, y, width, height}
   * @param winCoords  Vector3f to store window coords
   * @return true if successful
   */
  public static boolean project(Vector3f obj, Matrix4f modelView, Matrix4f projection, int[] viewport,
      Vector3f winCoords) {
    Vector4f tmp = new Vector4f(obj, 1.0f);

    // modelview transform
    modelView.transform(tmp);

    // projection transform
    projection.transform(tmp);

    if (tmp.w == 0.0f)
      return false;

    // perspective division
    tmp.div(tmp.w);

    // Map x,y to range 0-1
    tmp.x = tmp.x * 0.5f + 0.5f;
    tmp.y = tmp.y * 0.5f + 0.5f;
    tmp.z = tmp.z * 0.5f + 0.5f;

    // Map x,y to viewport
    winCoords.x = tmp.x * viewport[2] + viewport[0];
    winCoords.y = tmp.y * viewport[3] + viewport[1];
    winCoords.z = tmp.z;

    return true;
  }

  public static Vector3f unproject(Vector3f winCoords, Matrix4f modelView,
                                   Matrix4f projection, int[] viewport) {
    Matrix4f inverse = new Matrix4f();
    projection.mul(modelView, inverse).invert();

    // Normalize window coordinates to [-1, 1]
    float x = (winCoords.x - viewport[0]) / viewport[2] * 2.0f - 1.0f;
    float y = (winCoords.y - viewport[1]) / viewport[3] * 2.0f - 1.0f;
    float z = winCoords.z * 2.0f - 1.0f;

    Vector4f tmp = new Vector4f(x, y, z, 1.0f);
    inverse.transform(tmp);

    if (tmp.w == 0.0f)
      return null;

    return new Vector3f(tmp.x / tmp.w, tmp.y / tmp.w, tmp.z / tmp.w);
  }

}
