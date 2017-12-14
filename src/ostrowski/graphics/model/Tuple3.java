package ostrowski.graphics.model;

import java.nio.FloatBuffer;

/**
 * A simple tuple of 3 elements. A tuple is a set of values that relate
 * to each other in some way. In this case its a set of 3 so it might
 * represent a vertex or normal in 3D space.
 *
 */
public class Tuple3 {
	/** The x element in this tuple */
	protected float _x;
	/** The y element in this tuple */
	protected float _y;
	/** The z element in this tuple */
	protected float _z;

	/**
	 * Create a new 3 dimensional tuple
	 *
	 * @param x The X element value for the new tuple
	 * @param y The Y element value for the new tuple
	 * @param z The Z element value for the new tuple
	 */
   public Tuple3(float x,float y,float z) {
      this._x = x;
      this._y = y;
      this._z = z;
   }
   public Tuple3(FloatBuffer buffer) {
      this._x = buffer.get(0);
      this._y = buffer.get(1);
      this._z = buffer.get(2);
   }

   public void scale(double xScale, double yScale, double zScale) {
      _x *= xScale;
      _y *= yScale;
      _z *= zScale;
   }
   public void move(double xOffset, double yOffset, double zOffset) {
      _x += xOffset;
      _y += yOffset;
      _z += zOffset;
   }

	/**
	 * Get the X element value from this tuple
	 *
	 * @return The X element value from this tuple
	 */
	public float getX() {
		return _x;
	}

	/**
	 * Get the Y element value from this tuple
	 *
	 * @return The Y element value from this tuple
	 */
	public float getY() {
		return _y;
	}

	/**
	 * Get the Z element value from this tuple
	 *
	 * @return The Z element value from this tuple
	 */
	public float getZ() {
		return _z;
	}

   public Tuple3 add(float dx, float dy, float dz) {
      return new Tuple3(getX() + dx, getY() + dy, getZ() + dz);
   }
   public Tuple3 add(Tuple3 other) {
      return new Tuple3(getX() + other.getX(), getY() + other.getY(), getZ() + other.getZ());
   }
   public Tuple3 subtract(float dx, float dy, float dz) {
      return new Tuple3(getX() - dx, getY() - dy, getZ() - dz);
   }
   public Tuple3 subtract(Tuple3 other) {
      return new Tuple3(getX() - other.getX(), getY() - other.getY(), getZ() - other.getZ());
   }
   public Tuple3 divide(float d) {
      return new Tuple3(getX() / d, getY() / d, getZ() / d);
   }
   public Tuple3 multiply(float d) {
      return new Tuple3(getX() * d, getY() * d, getZ() * d);
   }

   public float dotProduct(Tuple3 other) {
      return (getX() * other.getX()) + (getY() * other.getY()) + (getZ() * other.getZ());
   }

   public Tuple3 crossProduct(Tuple3 other) {
      return new Tuple3((_y*other._z) - (_z*other._y), (_z*other._x) - (_x*other._z), (_x*other._y) - (_y*other._x));
   }
   public double magnitude() {
      return Math.sqrt(this.dotProduct(this));
   }

   public Tuple3 unitVector() {
      return divide((float) magnitude());
   }
   public Tuple3 normalize() {
      return unitVector();
   }

   @Override
   public String toString() {
      return "Tuple3:{" + getX() + ", " + getY() + ", " + getZ() + ")";
   }

   /* Rotates the 3-D point in space first about the x axis by the value of the first parameter,
    * then about the Y-axis by the value of the second parameter, and then about the Z-axis by the 3rd.
    * All parameters must be in Degrees.
    */
   public Tuple3 rotate(float x, float y, float z) {
      float cx = (float) Math.cos(Math.toRadians(x));
      float cy = (float) Math.cos(Math.toRadians(y));
      float cz = (float) Math.cos(Math.toRadians(z));
      float sx = (float) Math.sin(Math.toRadians(x));
      float sy = (float) Math.sin(Math.toRadians(y));
      float sz = (float) Math.sin(Math.toRadians(z));
      Matrix3x3 rotX = new Matrix3x3(new Tuple3( 1f, 0f, 0f),
                                     new Tuple3( 0f, cx,-sx),
                                     new Tuple3( 0f, sx, cx));
      Matrix3x3 rotY = new Matrix3x3(new Tuple3( cy, 0f, sy),
                                     new Tuple3( 0f, 1f, 0f),
                                     new Tuple3(-sy, 0f, cy));
      Matrix3x3 rotZ = new Matrix3x3(new Tuple3( cz,-sz, 0f),
                                     new Tuple3( sz, cz, 0f),
                                     new Tuple3( 0f, 0f, 1f));
      Matrix3x3 result = rotX.multiply(rotY).multiply(rotZ);
      return result.multiply(this);
   }

   class Matrix3x3 {
      Tuple3[] _row = new Tuple3[3];
      public Matrix3x3(Tuple3 row0, Tuple3 row1, Tuple3 row2) {
         _row[0] = row0;
         _row[1] = row1;
         _row[2] = row2;
      }
      public Matrix3x3 transpose() {
         Tuple3 newRow0 = new Tuple3(_row[0]._x, _row[1]._x, _row[2]._x);
         Tuple3 newRow1 = new Tuple3(_row[0]._y, _row[1]._y, _row[2]._y);
         Tuple3 newRow2 = new Tuple3(_row[0]._z, _row[1]._z, _row[2]._z);
         return new Matrix3x3(newRow0, newRow1, newRow2);
      }

      public Matrix3x3 multiply(Matrix3x3 m) {
         Matrix3x3 mT = m.transpose();
         Tuple3 newRow0 = new Tuple3(mT._row[0].dotProduct(_row[0]), mT._row[1].dotProduct(_row[0]), mT._row[2].dotProduct(_row[0]));
         Tuple3 newRow1 = new Tuple3(mT._row[0].dotProduct(_row[1]), mT._row[1].dotProduct(_row[1]), mT._row[2].dotProduct(_row[1]));
         Tuple3 newRow2 = new Tuple3(mT._row[0].dotProduct(_row[2]), mT._row[1].dotProduct(_row[2]), mT._row[2].dotProduct(_row[2]));
         return new Matrix3x3(newRow0, newRow1, newRow2);
      }

      public Tuple3 multiply(Tuple3 t) {
         return new Tuple3(t.dotProduct(_row[0]), t.dotProduct(_row[1]), t.dotProduct(_row[2]));
      }
   }
}