package ostrowski.graphics.model;

public class Matrix3x3 {
   public static Matrix3x3 IdentityMatrix() { return new Matrix3x3(new Tuple3(1,0,0),
                                                                   new Tuple3(0,1,0),
                                                                   new Tuple3(0,0,1)); }

   Tuple3[] row = new Tuple3[3];
   public Matrix3x3(Tuple3 row0, Tuple3 row1, Tuple3 row2) {
      row[0] = row0;
      row[1] = row1;
      row[2] = row2;
   }
   public Matrix3x3 transpose() {
      Tuple3 newRow0 = new Tuple3(row[0].x, row[1].x, row[2].x);
      Tuple3 newRow1 = new Tuple3(row[0].y, row[1].y, row[2].y);
      Tuple3 newRow2 = new Tuple3(row[0].z, row[1].z, row[2].z);
      return new Matrix3x3(newRow0, newRow1, newRow2);
   }

   public Matrix3x3 multiply(Matrix3x3 m) {
      Matrix3x3 mT = m.transpose();
      Tuple3 newRow0 = new Tuple3(mT.row[0].dotProduct(row[0]), mT.row[1].dotProduct(row[0]), mT.row[2].dotProduct(row[0]));
      Tuple3 newRow1 = new Tuple3(mT.row[0].dotProduct(row[1]), mT.row[1].dotProduct(row[1]), mT.row[2].dotProduct(row[1]));
      Tuple3 newRow2 = new Tuple3(mT.row[0].dotProduct(row[2]), mT.row[1].dotProduct(row[2]), mT.row[2].dotProduct(row[2]));
      return new Matrix3x3(newRow0, newRow1, newRow2);
   }

   public Tuple3 multiply(Tuple3 t) {
      return new Tuple3(t.dotProduct(row[0]), t.dotProduct(row[1]), t.dotProduct(row[2]));
   }

   /* Computes a rotation matrix to rotates a point in space first about the x axis by the value of the first parameter,
    * then about the Y-axis by the value of the second parameter, and then about the Z-axis by the 3rd.
    * All parameters must be in Degrees.
    */
   public static Matrix3x3 getRotationalTransformation(float xRotation, float yRotation, float zRotation) {
      float cx = (float) Math.cos(Math.toRadians(xRotation));
      float cy = (float) Math.cos(Math.toRadians(yRotation));
      float cz = (float) Math.cos(Math.toRadians(zRotation));
      float sx = (float) Math.sin(Math.toRadians(xRotation));
      float sy = (float) Math.sin(Math.toRadians(yRotation));
      float sz = (float) Math.sin(Math.toRadians(zRotation));
      Matrix3x3 rotX = new Matrix3x3(new Tuple3( 1f, 0f, 0f),
                                     new Tuple3( 0f, cx,-sx),
                                     new Tuple3( 0f, sx, cx));
      Matrix3x3 rotY = new Matrix3x3(new Tuple3( cy, 0f, sy),
                                     new Tuple3( 0f, 1f, 0f),
                                     new Tuple3(-sy, 0f, cy));
      Matrix3x3 rotZ = new Matrix3x3(new Tuple3( cz,-sz, 0f),
                                     new Tuple3( sz, cz, 0f),
                                     new Tuple3( 0f, 0f, 1f));
      return rotX.multiply(rotY).multiply(rotZ);
   }

   public static Matrix3x3 getRotationalTransformationAboutVector(Tuple3 vector, double angleInDegrees) {
      float angleInRadians = (float) Math.toRadians(angleInDegrees);
      double cos = Math.cos(angleInRadians);
      double sin = Math.sin(angleInRadians);
      Tuple3 uv = vector.unitVector();
      float Ux = uv.getX();
      float Uy = uv.getY();
      float Uz = uv.getZ();

      //The math for this come from: https://en.wikipedia.org/wiki/Rotation_matrix
      return new Matrix3x3(new Tuple3((float)((Ux*Ux*(1-cos)) + cos),      (float)((Ux*Uy*(1-cos)) - (Uz*sin)), (float)((Ux*Uz*(1-cos)) + (Uy*sin))),
                           new Tuple3((float)((Uy*Ux*(1-cos)) + (Uz*sin)), (float)((Uy*Uy*(1-cos)) + cos),      (float)((Uy*Uz*(1-cos)) - (Ux*sin))),
                           new Tuple3((float)((Uz*Ux*(1-cos)) - (Uy*sin)), (float)((Uz*Uy*(1-cos)) + (Ux*sin)), (float)((Uz*Uz*(1-cos)) + cos))
                           );
   }
}

