package com.mygdx.raytracer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class Quad extends Model{

	//topleft, botleft, topright
	public Vector3[] vertices;
	public Vector3 normal;
	Vector3 dX,dY;
	Vector3 dX2,dY2;
	Vector2 lastUV;
	double eps=1e-6;
	float xLength = -1;
	float yLength = -1;
	
	public Quad (Vector3 topLeft, Vector3 botLeft, Vector3 topRight) {
		super();
		vertices = new Vector3[3];
		vertices[0] = topLeft;
		vertices[1] = botLeft;
		vertices[2] = topRight;
		lastUV = new Vector2();
	}
	
	/** @return The normal */
	public Vector3 getNormal (Vector3 intersection) {
		if (normal==null){
			Vector3 topL = vertices[0];
			Vector3 botL = vertices[1];
			Vector3 topR = vertices[2];
			
			dX = new Vector3(topR).sub(topL);
	        dY = new Vector3(botL).sub(topL);
	        normal = new Vector3(dX).crs(dY);
	        normal.nor();
		}
		return normal;
	}

	/** Intersects a {@link Ray} and a {@link Plane}. The intersection point is stored in intersection in case an intersection is
	 * present.
	 * 
	 * @param ray The ray
	 * @param uv UV coordinates of the point hit
	 * @return Distance the ray traveled. */
	public float intersectRay(Ray ray) {
		float denom = ray.direction.dot(getNormal(null));
		if (denom != 0) {
			Vector3 rOriginV1 = new Vector3(ray.origin).sub(vertices[0]);
			float t = -(rOriginV1.dot(normal)) / denom;
			if (t <= eps) return 0;
			Vector3 intersection = new Vector3(ray.origin).mulAdd(ray.direction, t);
			
			Vector3 dV1Inters = new Vector3(intersection).sub(vertices[0]);
			float u = dV1Inters.dot(dX);
	        float v = dV1Inters.dot(dY);
	        if (xLength <= 0) xLength = dX.dot(dX);
	        if (yLength <= 0) yLength = dY.dot(dY);
	        
	        boolean intersected = (u >= 0.0f && u <= xLength && v >= 0.0f && v <= yLength);
			
	        if (intersected) {
	        	lastUV.set((tiling.x * u)/xLength, (tiling.y * v)/yLength);
	        	return t;
	        }
	        else return 0;

		} else
			return 0;
	}

	@Override
	public Vector2 getUV(Vector3 intersection) {
		return lastUV;
	}

	@Override
	public Vector3 getRandomSurfacePoint() {
		if (dX == null || dY == null){
			Vector3 topL = vertices[0];
			Vector3 botL = vertices[1];
			Vector3 topR = vertices[2];
			
			dX = new Vector3(topR).sub(topL);
	        dY = new Vector3(botL).sub(topL);
		}
		
		float u = MathUtils.random();
		float v = MathUtils.random();
		
		Vector3 point = new Vector3(vertices[0]).mulAdd(dX, u).mulAdd(dY, v);
		return point;
	}

	public float findTriangleArea(float sideA, float sideB, float sideC)
    { 
		float s = 0.5f * (sideA + sideB + sideC);
        float area = (float) Math.sqrt(s*(s-sideA)*(s-sideB)*(s-sideC));

        return area;
    }

	float width, height;
	public float getSurfaceArea() {
		if (width == 0 || height == 0){
			width = dX.len();
			height = dY.len();
		}
		
		return width*height;
	}

	@Override
	public Vector3 getPosition() {
		// TODO Auto-generated method stub
		return new Vector3(vertices[1]).lerp(vertices[2], 0.5f);
	}
}
