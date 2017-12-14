package ostrowski.graphics.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * The data that has been read from the Wavefront .obj file. This is
 * kept separate from the actual rendering with the hope the data
 * might be used for some other rendering engine in the future.
 *
 */
public class ObjData
{
   /** The verticies that have been read from the file */
   protected ArrayList<Tuple3> _verts     = new ArrayList<>();
   /** The faces data read from the file */
   protected ArrayList<Face>   _faces     = new ArrayList<>();

   /** The normals that have been read from the file */
   ArrayList<Tuple3> _normals = new ArrayList<>();

   public ObjData() {
   }

   Tuple3 _avePoint = null;
   public Tuple3 getAveragePoint() {
      if (_avePoint == null) {
         _avePoint = new Tuple3(0,0,0);
         int totalCount = 0;
         for (Face face : _faces) {
            for (int v=0 ; v< face._vertexCount ; v++) {
               _avePoint = _avePoint.add(face.getVertex(v));
               totalCount++;
            }
         }
         _avePoint = _avePoint.divide(totalCount);
      }
      return _avePoint;
   }
   /**
    * Create a new set of OBJ data by reading it in from the specified
    * input stream.
    *
    * @param in The input stream from which to read the OBJ data
    * @throws IOException Indicates a failure to read from the stream
    */
   public ObjData(InputStream in) throws IOException {
      // read the file line by line adding the data to the appropriate
      // list held locally
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(in)))
      {
         /** The texture coordinates that have been read from the file */
         ArrayList<Tuple2> texCoords = new ArrayList<>();

         while (reader.ready()) {
            String line = reader.readLine();

            // if we read a null line thats means on some systems
            // we've reached the end of the file, hence we want to
            // to jump out of the loo
            if (line == null) {
               break;
            }

            line = line.trim();
            // "vn" indicates normal data
            if (line.startsWith("vn")) {
               Tuple3 normal = readTuple3(line);
               _normals.add(normal);
               // "vt" indicates texture coordinate data
            }
            else if (line.startsWith("vt")) {
               Tuple2 tex = readTuple2(line);
               texCoords.add(tex);
               // "v" indicates vertex data
            }
            else if (line.startsWith("v")) {
               Tuple3 vert = readTuple3(line);
               _verts.add(vert);
               // "f" indicates a face
            }
            else if (line.startsWith("f")) {
               // readFace(...) assumes that the vertex data is already loaded into _verts
               Face face = readFace(line, texCoords);
               _faces.add(face);
            }
         }
      }
      // Print some diagnositics data so we can see whats happening
      // while testing
      //System.out.println("Read " + _verts.size() + " verticies");
      //System.out.println("Read " + _faces.size() + " faces");
   }

   /**
    * Get the number of faces found in the model file
    *
    * @return The number of faces found in the model file
    */
   public int getFaceCount() {
      return _faces.size();
   }

   /**
    * Get the data for specific face
    *
    * @param index The index of the face whose data should be retrieved
    * @return The face data requested
    */
   public Face getFace(int index) {
      return _faces.get(index);
   }

   public void scale(double xScale, double yScale, double zScale) {
      for (Tuple3 vert : _verts) {
         vert.scale(xScale, yScale, zScale);
      }
   }

   public void move(double xOffset, double yOffset, double zOffset) {
      for (Tuple3 vert : _verts) {
         vert.move(xOffset, yOffset, zOffset);
      }
   }

   /**
    * Read a set of 3 float values from a line assuming the first token
    * on the line is the identifier
    *
    * @param line The line from which to read the 3 values
    * @return The set of 3 floating point values read
    * @throws IOException Indicates a failure to process the line
    */
   protected Tuple3 readTuple3(String line) throws IOException {
      StringTokenizer tokens = new StringTokenizer(line, " ");

      tokens.nextToken();

      try {
         float x = Float.parseFloat(tokens.nextToken());
         float y = Float.parseFloat(tokens.nextToken());
         float z = Float.parseFloat(tokens.nextToken());

         return new Tuple3(x, y, z);
      } catch (NumberFormatException e) {
         throw new IOException(e.getMessage());
      }
   }

   /**
    * Read a set of 2 float values from a line assuming the first token
    * on the line is the identifier
    *
    * @param line The line from which to read the 3 values
    * @return The set of 2 floating point values read
    * @throws IOException Indicates a failure to process the line
    */
   protected Tuple2 readTuple2(String line) throws IOException {
      StringTokenizer tokens = new StringTokenizer(line, " ");

      tokens.nextToken();

      try {
         float x = Float.parseFloat(tokens.nextToken());
         float y = Float.parseFloat(tokens.nextToken());

         return new Tuple2(x, y);
      } catch (NumberFormatException e) {
         throw new IOException(e.getMessage());
      }
   }

   /**
    * Read a set of face data from the line
    *
    * @param line The line which to interpret as face data
    * @return The face data extracted from the line
    * @throws IOException Indicates a failure to process the line
    */
   protected Face readFace(String line, ArrayList<Tuple2> texCoords) throws IOException {
      StringTokenizer points = new StringTokenizer(line, " ");

      points.nextToken();
      int faceCount = points.countTokens();

      // currently we only support triangles so anything other than
      // 3 verticies is invalid
      if ((faceCount != 3) && (faceCount != 4)) {
         throw new RuntimeException("Only triangles and quads are supported");
      }

      // create a new face data to populate with the values from the line
      Face face = new Face(faceCount);

      try {
         // for each line we're going to read 3 bits of data, the index
         // of the vertex, the index of the texture coordinate and the
         // normal.
         for (int i = 0; i < faceCount; i++) {
            String faceToken = points.nextToken();
            StringTokenizer parts = new StringTokenizer(faceToken, "/");

            int v = Integer.parseInt(parts.nextToken());
            int t = Integer.parseInt(parts.nextToken());
            if (parts.hasMoreElements()) {
               int n = Integer.parseInt(parts.nextToken());
               // we have the indicies we can now just add the point
               // data to the face.
               face.addPoint(_verts.get(v - 1), texCoords.get(t - 1), _normals.get(n - 1));
            }
            else {
               face.addPoint(_verts.get(v - 1), new Tuple2(0, 0), _normals.get(t - 1));
            }
         }
      } catch (NumberFormatException e) {
         throw new IOException(e.getMessage());
      } catch (Exception e) {
         throw new IOException(e.getMessage());
      }
      Tuple3 line1 = face.getVertex(1).subtract(face.getVertex(0));
      Tuple3 line2 = face.getVertex(2).subtract(face.getVertex(1));
      Tuple3 compNorm = line1.crossProduct(line2);
      compNorm = compNorm.normalize();
      Tuple3 reportedNorm = face.getNormal(0).add(face.getNormal(1).add(face.getNormal(2)));
      if (face._vertexCount == 4) {
         reportedNorm = reportedNorm.add(face.getNormal(3));
      }
      reportedNorm = reportedNorm.divide(face._vertexCount);

      float dotProduct = compNorm.dotProduct(reportedNorm );
      if ((dotProduct < 0.5) || (dotProduct > 1.5)) {
         return face;
      }
      return face;
   }

   public void scaleNormals(double x, double y, double z) {
      for (Tuple3 normal : _normals) {
         normal.scale(x, y, z);
      }
   }

}
