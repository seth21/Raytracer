package com.mygdx.raytracer;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.Plane.PlaneSide;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.FloatArray;

public class Mesh extends Model{
	
	public Vector3[] vertices;
	public short[] indices;
	

	@Override
	public float intersectRay(Ray ray) {
		// TODO Auto-generated method stub
		//Intersector.intersectRayTriangle(ray, t1, t2, t3, intersection)
		return 0;
	}

	@Override
	public Vector3 getNormal(Vector3 intersection) {
		// TODO Auto-generated method stub
		return null;
	}
	
	Vector3 best = new Vector3();
	Vector3 tmp = new Vector3();
	Vector3 tmp1 = new Vector3();
	Vector3 tmp2 = new Vector3();
	Vector3 tmp3 = new Vector3();
	Vector2 v2tmp = new Vector2();
	
	/** Intersects the given ray with list of triangles. Returns the nearest intersection point in intersection
	 * 
	 * @param ray The ray
	 * @param vertices the vertices
	 * @param indices the indices, each successive 3 shorts index the 3 vertices of a triangle
	 * @param vertexSize the size of a vertex in floats
	 * @param intersection The nearest intersection point (optional)
	 * @return Whether the ray and the triangles intersect. */
	public boolean intersectRayTriangles (Ray ray, float[] vertices, short[] indices, int vertexSize,
		Vector3 intersection) {
		float min_dist = Float.MAX_VALUE;
		boolean hit = false;

		if (indices.length % 3 != 0) throw new RuntimeException("triangle list size is not a multiple of 3");

		for (int i = 0; i < indices.length; i += 3) {
			int i1 = indices[i] * vertexSize;
			int i2 = indices[i + 1] * vertexSize;
			int i3 = indices[i + 2] * vertexSize;

			boolean result = intersectRayTriangle(ray, tmp1.set(vertices[i1], vertices[i1 + 1], vertices[i1 + 2]),
				tmp2.set(vertices[i2], vertices[i2 + 1], vertices[i2 + 2]),
				tmp3.set(vertices[i3], vertices[i3 + 1], vertices[i3 + 2]), tmp);

			if (result == true) {
				float dist = ray.origin.dst2(tmp);
				if (dist < min_dist) {
					min_dist = dist;
					best.set(tmp);
					hit = true;
				}
			}
		}

		if (hit == false)
			return false;
		else {
			if (intersection != null) intersection.set(best);
			return true;
		}
	}
	
	private final Plane p = new Plane(new Vector3(), 0);
	private final Vector3 i = new Vector3();
	private final Vector3 v0 = new Vector3();
	private final Vector3 v1 = new Vector3();
	private final Vector3 v2 = new Vector3();
	
	/** Intersect a {@link Ray} and a triangle, returning the intersection point in intersection.
	 * 
	 * @param ray The ray
	 * @param t1 The first vertex of the triangle
	 * @param t2 The second vertex of the triangle
	 * @param t3 The third vertex of the triangle
	 * @param intersection The intersection point (optional)
	 * @return True in case an intersection is present. */
	public boolean intersectRayTriangle (Ray ray, Vector3 t1, Vector3 t2, Vector3 t3, Vector3 intersection) {
		Vector3 edge1 = v0.set(t2).sub(t1);
		Vector3 edge2 = v1.set(t3).sub(t1);

		Vector3 pvec = v2.set(ray.direction).crs(edge2);
		float det = edge1.dot(pvec);
		if (MathUtils.isZero(det)) {
			p.set(t1, t2, t3);
			if (p.testPoint(ray.origin) == PlaneSide.OnPlane && Intersector.isPointInTriangle(ray.origin, t1, t2, t3)) {
				if (intersection != null) intersection.set(ray.origin);
				return true;
			}
			return false;
		}

		det = 1.0f / det;

		Vector3 tvec = i.set(ray.origin).sub(t1);
		float u = tvec.dot(pvec) * det;
		if (u < 0.0f || u > 1.0f) return false;

		Vector3 qvec = tvec.crs(edge1);
		float v = ray.direction.dot(qvec) * det;
		if (v < 0.0f || u + v > 1.0f) return false;

		float t = edge2.dot(qvec) * det;
		if (t < 0) return false;

		if (intersection != null) {
			if (t <= MathUtils.FLOAT_ROUNDING_ERROR) {
				intersection.set(ray.origin);
			} else {
				ray.getEndPoint(intersection, t);
			}
		}

		return true;
	}

	@Override
	public Vector2 getUV(Vector3 intersection) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector3 getRandomSurfacePoint() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getSurfaceArea() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector3 getPosition() {
		// TODO Auto-generated method stub
		return null;
	}


}
