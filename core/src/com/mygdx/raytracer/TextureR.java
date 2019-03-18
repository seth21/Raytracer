package com.mygdx.raytracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class TextureR {

	public Vector3[][] pixels;
	public int width, height;
	public boolean clamp = false;
	
	public TextureR(String filename){
		Pixmap map = new Pixmap(Gdx.files.internal(filename));
		width = map.getWidth();
		height = map.getHeight();
		pixels = new Vector3[width][height];
		for (int x = 0; x < width; x++){
			for (int y = 0; y < height; y++){
				Color col = new Color(map.getPixel(x, y));
				pixels[x][y] = new Vector3(col.r, col.g, col.b);
			}
		}
		map.dispose();
	}
	
	public TextureR(String filename, boolean clamp){
		this(filename);
		this.clamp = clamp;
	}
	
	public Vector3 getColor(Vector2 uvs){
		return getColor(uvs.x, uvs.y);
	}
	
	public Vector3 getColor(float u, float v){
		float uu ;
		float vv;
		if (clamp){
			uu = MathUtils.clamp(u, 0, 1);
			vv = MathUtils.clamp(v, 0, 1);
		}
		else{
			uu = u%1;
			vv = v%1;
		}
		int x = (int) (uu * (width-1));
		int y = (int) (vv * (height-1));
		//Gdx.app.log("UV", uu+","+x);
		return pixels[x][y];
		
	}

	
}
