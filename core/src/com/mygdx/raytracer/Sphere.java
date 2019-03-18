package com.mygdx.raytracer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.NumberUtils;

public class Sphere extends Model{

	/** the radius of the sphere **/
	public float radius;
	/** the center of the sphere **/
	public final Vector3 center;
	Plane plane;

	private static final float PI_4_3 = MathUtils.PI * 4f / 3f;

	/** Constructs a sphere with the given center and radius
	 * @param center The center
	 * @param radius The radius */
	public Sphere (Vector3 center, float radius) {
		super();
		this.center = new Vector3(center);
		this.radius = radius;
	}

	/** @param sphere the other sphere
	 * @return whether this and the other sphere overlap */
	public boolean overlaps (Sphere sphere) {
		return center.dst2(sphere.center) < (radius + sphere.radius) * (radius + sphere.radius);
	}

	@Override
	public int hashCode () {
		final int prime = 71;
		int result = 1;
		result = prime * result + this.center.hashCode();
		result = prime * result + NumberUtils.floatToRawIntBits(this.radius);
		return result;
	}

	@Override
	public boolean equals (Object o) {
		if (this == o) return true;
		if (o == null || o.getClass() != this.getClass()) return false;
		Sphere s = (Sphere)o;
		return this.radius == s.radius && this.center.equals(s.center);
	}

	public float volume () {
		return PI_4_3 * this.radius * this.radius * this.radius;
	}

	public float surfaceArea () {
		return 4 * MathUtils.PI * this.radius * this.radius;
	}
	
	
	@Override
	/** Intersects a {@link Ray} and a sphere, returning the intersection point in intersection.
	 * 
	 * @param ray The ray, the direction component must be normalized before calling this method
	 * @return Distance the ray traveled. */
	public float intersectRay (Ray ray) {
		final float len = ray.direction.dot(center.x - ray.origin.x, center.y - ray.origin.y, center.z - ray.origin.z);
		if (len < 0.f) // behind the ray
			return -1f;
		final float dst2 = center.dst2(ray.origin.x + ray.direction.x * len, ray.origin.y + ray.direction.y * len,
			ray.origin.z + ray.direction.z * len);
		final float r2 = radius * radius;
		if (dst2 > r2) return -1f;
		return len - (float)Math.sqrt(r2 - dst2);
		
	}

	@Override
	public Vector3 getNormal(Vector3 intersection) {
		return new Vector3(intersection).sub(center).nor();
		// TODO Auto-generated method stub
		
	}
	private Vector2 uv = new Vector2();
	@Override
	public Vector2 getUV(Vector3 intersection) {
		
		return uv;
	}

	@Override
	public Vector3 getRandomSurfacePoint() {
		Vector3 temp = new Vector3().setToRandomDirection();
		temp.scl(radius).add(center);
		return temp;
	}

	@Override
	public float getSurfaceArea() {
		return 4f * MathUtils.PI * radius * radius;
	}

	@Override
	public Vector3 getPosition() {
		return center;
	}
	
	


}
