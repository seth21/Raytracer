package com.mygdx.raytracer;

import java.util.List;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Octree {

	float boxSize;
	Vector3 maxWorld, minWorld;
	List<BoundingBox> boxes;
	
	public Octree(){
		BoundingBox b = new BoundingBox();
	}
	
}
