package com.mygdx.raytracer;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public abstract class Model {

	public Vector3 diffuse;
	public Vector3 glossy;
	public boolean glass = false;
	public Vector3 emissionColor;
	public float emissionRadius;
	public float emissionIntensity = 0;
	public boolean castShadow = true;
	public boolean cameraInvisible = false;
	public float glossyFactor = 0, glossyTint = 0, glossyRoughness = 0, refractionFactor = 0, transparencyFactor = 0, translucencyFactor = 0;
	public TextureR diffTexture;
	public Vector2 tiling;
	
	public abstract float intersectRay(Ray ray);
	public abstract Vector3 getNormal(Vector3 intersection);
	public abstract Vector2 getUV(Vector3 intersection);
	public abstract Vector3 getRandomSurfacePoint();
	public abstract float getSurfaceArea();
	public abstract Vector3 getPosition();
	
	public Model(){
		diffuse = new Vector3(1,1,1);
		glossy = new Vector3(1,1,1);
		emissionColor = new Vector3(0,0,0);
		tiling = new Vector2(1,1);
	}
	
	public Model setDiffuse(Vector3 color){
		diffuse.set(color);
		return this;
	}
	
	public Model setDiffuseTexture(TextureR tex){
		diffTexture = tex;
		return this;
	}
	
	public Model setGlossy(Vector3 color){
		glossy.set(color);
		return this;
	}
	
	public Model setGlass(boolean g){
		glass = g;
		if (glass) refractionFactor = 1;
		return this;
	}
	
	public Model setGlossyRoughness(float gRough){
		glossyRoughness = MathUtils.clamp(gRough, 0, 1);
		return this;
	}
	
	public Model setTransparency(float trans){
		transparencyFactor = MathUtils.clamp(trans, 0, 1);
		return this;
	}
	
	public Model setTranslucency(float trans){
		translucencyFactor = MathUtils.clamp(trans, 0, 1);
		return this;
	}
	
	public Model setTiling(float x, float y){
		tiling.set(x,y);
		return this;
	}
	
	public Model setDiffuse(float r, float g, float b){
		diffuse.set(r,g,b);
		return this;
	}
	
	public Model setGlossy(float r, float g, float b){
		glossy.set(r,g,b);
		return this;
	}
	
	public Model setEmission(float r, float g, float b, float radius, float intensity){
		emissionRadius = radius;
		emissionColor.set(r,g,b);
		cameraInvisible = true;
		emissionIntensity = intensity;
		return this;
	}

	public Model setCastShadow(boolean bool){
		castShadow = bool;
		return this;
	}
	
	public Model setGlossyFactor(float g){
		glossyFactor = MathUtils.clamp(g, 0, 1);
		return this;
	}
	
	public Model setCameraInvisible(boolean bool){
		cameraInvisible = bool;
		return this;
	}
	
	
	
}
