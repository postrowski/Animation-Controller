package ostrowski.graphics.model;

import java.nio.FloatBuffer;

/**
 * A simple tuple of 3 elements. A tuple is a set of values that relate
 * to each other in some way. In this case its a set of 3 so it might
 * represent a vertex or normal in 3D space.
 *
 */
public class Tuple3 {
    static int NEXT_ID = 0;
    public int objId = NEXT_ID++;
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
   public void move(Tuple3 offset) {
      _x = _x + offset.getX();
      _y = _y + offset.getY();
      _z = _z + offset.getZ();
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
      return this.applyTransformation(Matrix3x3.getRotationalTransformation(x, y, z));
   }

   public Tuple3 applyTransformation(Matrix3x3 matrix) {
      return matrix.multiply(this);
   }
   public void applyTransformationInPlace(Matrix3x3 matrix) {
      float x = dotProduct(matrix._row[0]);
      float y = dotProduct(matrix._row[1]);
      float z = dotProduct(matrix._row[2]);
      _x = x;
      _y = y;
      _z = z;
   }

}