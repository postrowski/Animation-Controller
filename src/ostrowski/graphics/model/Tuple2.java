package ostrowski.graphics.model;

/**
 * A simple tuple of 2 elements. A tuple is a set of values that relate
 * to each other in some way. In this case its a set of 2 so it might
 * represent a vertex or normal in 2D space or a texture coordinate
 * 
 */
public class Tuple2 {
	/** The x element in this tuple */
	private float x;
	/** The y element in this tuple */
	private float y;
	
	/**
	 * Create a new Tuple of 2 elements
	 * 
	 * @param x The X element value
	 * @param y The Y element value
	 */
	public Tuple2(float x,float y) {
		this.x = x;
		this.y = y;
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

   public Tuple2 add(float dx, float dy) {
      return new Tuple2(getX() + dx, getY() + dy);
   }
   public Tuple2 add(Tuple2 other) {
      return new Tuple2(getX() + other.getX(), getY() + other.getY());
   }
   public Tuple2 subtract(Tuple2 other) {
      return new Tuple2(getX() - other.getX(), getY() - other.getY());
   }
   public Tuple2 divide(float d) {
      return new Tuple2(getX() / d, getY() / d);
   }
   public Tuple2 multiply(float d) {
      return new Tuple2(getX() * d, getY() * d);
   }
   
   public float dotProduct(Tuple2 other) {
      return getX() * other.getX() + getY() * other.getY();
   }
   public double magnitude() {
      return Math.sqrt(this.dotProduct(this));
   }
   
   public Tuple2 unitVector() {
      return divide((float) magnitude());
   }

   @Override
   public String toString() {
      return "Tuple2:{" + getX() + ", " + getY() + ")";
   }
}