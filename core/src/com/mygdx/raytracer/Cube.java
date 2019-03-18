package com.mygdx.raytracer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class Cube extends Model{

	private Vector3[] vertices = new Vector3[]{
		new Vector3(-0.5f, 1f, 0.5f),	
		new Vector3(0.5f, 1f, 0.5f),	
		new Vector3(-0.5f, 1f, -0.5f),	
		new Vector3(0.5f, 1f, -0.5f),	
		new Vector3(-0.5f, 0f, 0.5f),	
		new Vector3(0.5f, 0f, 0.5f),	
		new Vector3(-0.5f, 0f, -0.5f),	
		new Vector3(0.5f, 0f, -0.5f)	
	};
	
	private Vector3[] transVerts;
	
	private Quad[] quads;
	
	Vector3 position = new Vector3(0,4,-5);
	
	public Cube(float x, float y, float z, float width, float height, float depth){
		transVerts = new Vector3[8];
		
		Matrix4 m = new Matrix4();
		m.scl(width, height, depth);
		
		position.set(x, y, z);
		
		for (int i = 0; i < 8; i++){
			transVerts[i] = new Vector3(vertices[i]);
			transVerts[i].mul(m);
			transVerts[i].add(position);
		}
		
		quads = new Quad[6];
		quads[0] = new Quad(transVerts[0], transVerts[2], transVerts[1]);
		quads[1] = new Quad(transVerts[2], transVerts[6], transVerts[3]);
		quads[2] = new Quad(transVerts[1], transVerts[5], transVerts[0]);
		quads[3] = new Quad(transVerts[0], transVerts[4], transVerts[2]);
		quads[4] = new Quad(transVerts[3], transVerts[7], transVerts[1]);
		quads[5] = new Quad(transVerts[6], transVerts[4], transVerts[7]);
	}
	
	int lastQuadIntersected;
	
	public float intersectRay(Ray ray) {
		float mindist = 9999;
		for (int i = 0; i < 6; i++){
			
			float dist = quads[i].intersectRay(ray);
			//System.out.println("distL:"+dist);
			if (dist > 0 && dist < mindist){
				lastQuadIntersected = i;
				mindist = dist;
			}
		}
		//System.out.println(mindist);
		return mindist;
	}

	@Override
	public Vector3 getNormal(Vector3 intersection) {
		return quads[lastQuadIntersected].getNormal(intersection);
	}

	@Override
	public Vector2 getUV(Vector3 intersection) {
		return quads[lastQuadIntersected].getUV(intersection);
	}

	@Override
	public Vector3 getRandomSurfacePoint() {
		int rand = MathUtils.random(0, 5);
		return quads[rand].getRandomSurfacePoint();
	}

	@Override
	public float getSurfaceArea() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Vector3 getPosition() {
		
		return position;
	}

}
