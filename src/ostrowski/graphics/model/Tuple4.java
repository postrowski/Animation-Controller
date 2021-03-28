package ostrowski.graphics.model;

import java.nio.FloatBuffer;

/**
 * A simple tuple of 3 elements. A tuple is a set of values that relate
 * to each other in some way. In this case its a set of 3 so it might
 * represent a vertex or normal in 3D space.
 *
 */
public class Tuple4 {
   static int NEXT_ID = 0;
   public int      objId = NEXT_ID++;
	/** The x element in this tuple */
	protected float x;
	/** The y element in this tuple */
	protected float y;
	/** The z element in this tuple */
	protected float z;
	/** The q element in this tuple */
	protected float q;

	/**
	 * Create a new 3 dimensional tuple
	 *
	 * @param x The X element value for the new tuple
	 * @param y The Y element value for the new tuple
	 * @param z The Z element value for the new tuple
	 */
   public Tuple4(float x,float y,float z,float q) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.q = q;
   }
   public Tuple4(FloatBuffer buffer) {
      this.x = buffer.get(0);
      this.y = buffer.get(1);
      this.z = buffer.get(2);
      this.q = buffer.get(3);
   }

   public void scale(double xScale, double yScale, double zScale, double qScale) {
      x *= xScale;
      y *= yScale;
      z *= zScale;
      q *= qScale;
   }
   public void move(Tuple4 offset) {
      x = x + offset.getX();
      y = y + offset.getY();
      z = z + offset.getZ();
      q = q + offset.getQ();
   }

	/**
	 * Get the X element value from this tuple
	 *
	 * @return The X element value from this tuple
	 */
	public float getX() {
		return x;
	}

	/**
	 * Get the Y element value from this tuple
	 *
	 * @return The Y element value from this tuple
	 */
	public float getY() {
		return y;
	}

	/**
	 * Get the Z element value from this tuple
	 *
	 * @return The Z element value from this tuple
	 */
	public float getZ() {
		return z;
	}

	/**
	 * Get the Q element value from this tuple
	 *
	 * @return The Q element value from this tuple
	 */
	public float getQ() {
	   return q;
	}

   public Tuple4 add(float dx, float dy, float dz, float dq) {
      return new Tuple4(getX() + dx, getY() + dy, getZ() + dz, getQ() + dq);
   }
   public Tuple4 add(Tuple4 other) {
      return new Tuple4(getX() + other.getX(), getY() + other.getY(), getZ() + other.getZ(), getQ() + other.getQ());
   }
   public Tuple4 subtract(float dx, float dy, float dz, float dq) {
      return new Tuple4(getX() - dx, getY() - dy, getZ() - dz, getQ() - dq);
   }
   public Tuple4 subtract(Tuple4 other) {
      return new Tuple4(getX() - other.getX(), getY() - other.getY(), getZ() - other.getZ(), getQ() - other.getQ());
   }
   public Tuple4 divide(float d) {
      return new Tuple4(getX() / d, getY() / d, getZ() / d, getQ() / d);
   }
   public Tuple4 multiply(float d) {
      return new Tuple4(getX() * d, getY() * d, getZ() * d, getQ() * d);
   }

   public float dotProduct(Tuple4 other) {
      return (getX() * other.getX()) + (getY() * other.getY()) + (getZ() * other.getZ()) + (getQ() * other.getQ());
   }

//   public Tuple4 crossProduct(Tuple4 other) {
//      return new Tuple4((_y*other._z) - (_z*other._y), (_z*other._x) - (_x*other._z), (_x*other._y) - (_y*other._x));
//   }
   public double magnitude() {
      return Math.sqrt(this.dotProduct(this));
   }

   public Tuple4 unitVector() {
      return divide((float) magnitude());
   }
   public Tuple4 normalize() {
      return unitVector();
   }

   @Override
   public String toString() {
      return "Tuple4:{" + getX() + ", " + getY() + ", " + getZ() + ", " + getQ() + ")";
   }


//   /* Rotates the 3-D point in space first about the x axis by the value of the first parameter,
//    * then about the Y-axis by the value of the second parameter, and then about the Z-axis by the 3rd.
//    * All parameters must be in Degrees.
//    */
//   public Tuple4 rotate(float x, float y, float z) {
//      return this.applyTransformation(Matrix3x3.getRotationalTransformation(x, y, z));
//   }
//
//   public Tuple4 applyTransformation(Matrix3x3 matrix) {
//      return matrix.multiply(this);
//   }
//   public void applyTransformationInPlace(Matrix3x3 matrix) {
//      float x1 = dotProduct(matrix._row[0]);
//      float y1 = dotProduct(matrix._row[1]);
//      float z1 = dotProduct(matrix._row[2]);
//      x = x1;
//      y = y1;
//      z = z1;
//   }

}