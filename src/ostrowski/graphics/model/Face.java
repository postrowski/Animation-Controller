package ostrowski.graphics.model;


/**
 * A single face defined on a model. This is a series of points
 * each with a position, normal and texture coordinate
 */
public class Face implements Cloneable {
   /** The vertices making up this face */
   private final Tuple3[] _verts;
   /** The normals making up this face */
   private final Tuple3[] _norms;
   /** The texture coordinates making up this face */
   private final Tuple2[] _texs;
   /** The number of points */
   private int      _points;
	public int _vertexCount;

	/**
	 * Create a new face
	 *
	 * @param points The number of points building up this face
	 */
	public Face(int points) {
	   _vertexCount = points;
		_verts = new Tuple3[points];
		_norms = new Tuple3[points];
		_texs  = new Tuple2[points];
	}

	/**
	 * Add a single point to this face
	 *
	 * @param vert The vertex location information for the point
	 * @param tex The texture coordinate information for the point
	 * @param norm the normal information for the point
	 */
	public void addPoint(Tuple3 vert, Tuple2 tex, Tuple3 norm) {
		_verts[_points] = vert;
		_texs[_points]  = tex;
		_norms[_points] = norm;

		_points++;
	}

	public Tuple3 getCommonNormal() {
      Tuple3 s01 = _verts[0].subtract(_verts[1]);
      Tuple3 s12 = _verts[1].subtract(_verts[2]);
      return s01.crossProduct(s12).normalize();
	}

	/**
	 * Get the vertex information for a specified point within this face.
	 *
	 * @param p The index of the vertex information to retrieve
	 * @return The vertex information from this face
	 */
	public Tuple3 getVertex(int p) {
		return _verts[p];
	}

	/**
	 * Get the texture information for a specified point within this face.
	 *
	 * @param p The index of the texture information to retrieve
	 * @return The texture information from this face
	 */
	public Tuple2 getTexCoord(int p) {
		return _texs[p];
	}

	/**
	 * Get the normal information for a specified point within this face.
	 *
	 * @param p The index of the normal information to retrieve
	 * @return The normal information from this face
	 */
	public Tuple3 getNormal(int p) {
		return _norms[p];
	}

   /**
    * change the order of the points on the triangle from A->B->C    to A->C->B
    *                           or the quadralateral from A->B->C->D to A->D->C->B
    * so if a normal is recomputed it will be reversed 180 degrees:
    *     A   =>   A       |       A D  =>   A B
    *    B C  =>  C B      |       B C  =>   D C
    */
   public void invertNormal() {
      if ((_verts.length == 3) || (_verts.length == 4)) {
         int sourceIndex = 1;
         int destIndex = (_verts.length -1);
         Tuple3 tempT3 = _verts[sourceIndex];
         _verts[sourceIndex] = _verts[destIndex];
         _verts[destIndex] = tempT3;

         tempT3 = _norms[sourceIndex];
         _norms[sourceIndex] = _norms[destIndex];
         _norms[destIndex] = tempT3;

         Tuple2 tempT2 = _texs[sourceIndex];
         _texs[sourceIndex] = _texs[destIndex];
         _texs[destIndex] = tempT2;
         return;
      }
      throw new UnsupportedOperationException();
   }

   public void applyTransformation(Matrix3x3 matrix) {
      getCommonNormal().applyTransformationInPlace(matrix);
   }

   public void rotate(float x, float y, float z) {
      Matrix3x3 transform = Matrix3x3.getRotationalTransformation(x, y, z);
      applyTransformation(transform);
   }

   public void move(Tuple3 offset) {
      for (Tuple3 _vert : _verts) {
         _vert.move(offset);
      }
   }

   public Tuple3 getVertexCenter() {
      Tuple3 vertexCenter = new Tuple3(0,0,0);
      for (int v=0 ; v<_vertexCount ; v++ ) {
         vertexCenter.move(getVertex(v));
      }
      return vertexCenter.divide(_vertexCount);
   }

   @Override
   public Face clone() {
      Face dup = new Face(_points);
      for (int i=0 ; i<_points ; i++) {
         dup.addPoint(_verts[i], _texs[i], _norms[i]);
      }
      return dup;
   }
}