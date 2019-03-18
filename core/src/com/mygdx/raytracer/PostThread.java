package com.mygdx.raytracer;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class PostThread extends Thread{

	MyRaytracer main;
	
	//Post processing settings
	boolean enablePostProcessing = true;
	int bloomRadius = 10;
	float bloomStrength = 0.2f;
	float bloomThreshold = 0.8f;
	int denoiseRadiusLight = 3;
	int denoiseRadiusGlossy = 1;
	int denoiseCenter = 0;
	float[] denSmartThreshLight = {110f, .5f, .4f};
	float[] denSmartThreshGlossy = {40f, .25f, .3f};
	float[] denClampThresh = {90f, .5f, .5f};
	float denoiseThreshDepth = 0.1f;
	
	Model DOFtarget;
	float DOFfocusWidth = 6;
	float DOFmaxBlurDist = 30;
	int DOFmaxBlur = 10;
	
	boolean enableTonemapping = true;
	
	public PostThread(MyRaytracer mainThread){
		main = mainThread;
	}
	
	public void run() {
		postProcessing();
		
	}
	
	private void postProcessing() {

		if (!enablePostProcessing) return;
		Gdx.app.log("ih", "start");
		//take the rendered parts from all threads
		for (int tX = 0; tX < main.threadsHor; tX++){
			for (int tY = 0; tY < main.threadsVert; tY++){
				int xStart = main.threads[tX][tY].xStart;
				int yStart = main.threads[tX][tY].yStart;
				
				for (int x = 0; x < main.threads[tX][tY].tW; x++){
					for (int y = 0; y < main.threads[tX][tY].tH; y++){
						
						main.pixAlbedo[x+xStart][y+yStart].set(main.threads[tX][tY].pixDiff[x][y]);
						main.pixDiffIndirectLight[x+xStart][y+yStart].set(main.threads[tX][tY].pixDiffLight[x][y]);
						main.pixDiffDirectLight[x+xStart][y+yStart].set(main.threads[tX][tY].pixDirectLight[x][y]);
						main.pixGlossy[x+xStart][y+yStart].set(main.threads[tX][tY].pixGlossy[x][y]);
					}
					
				}
			}
		}
		
			
		float[][] depths = new float[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		Vector3[][] normals = new Vector3[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		
		raytraceDepthNormals(normals, depths);
		//Denoising on the separate channels
		smartGaussianDenoise(main.pixDiffDirectLight, normals, depths, denSmartThreshLight, denoiseRadiusLight);
		smartGaussianDenoise(main.pixDiffIndirectLight, normals, depths, denSmartThreshLight, denoiseRadiusLight);
		smartGaussianDenoise(main.pixGlossy, normals, depths, denSmartThreshGlossy, denoiseRadiusGlossy);
		//Merge channels to pixAlbedo for filters that operate on the final image
		mergeChannels();
		//Depth of Field
		depthOfField(depths);
		//Bloom
		bloom();	
		
		//refresh image
		main.setViewMode(main.viewMode);
		Gdx.app.log("PostProcessing", "End");
	}
	
public void raytraceDepthNormals(Vector3[][] normals, float[][] depths){
		
		Ray ray = new Ray();
		Vector3 dir = new Vector3(0,0,0);
		
		Vector3[] planePoints = main.cam.frustum.planePoints;
		Vector3 halfWidthDir = new Vector3(planePoints[0]).sub(planePoints[1]).scl(0.5f);
		Vector3 halfHeightDir = new Vector3(planePoints[2]).sub(planePoints[1]).scl(0.5f);
		int samples = 10;
		
		for (int x = 0; x < Gdx.graphics.getWidth(); x++){
			for (int y = 0; y < Gdx.graphics.getHeight(); y++){
				
				normals[x][y] = new Vector3();
				for (int s = 0; s < samples; s++){
					float jitter = 0.4f;
					float xx = 2f* (x + MathUtils.random(-jitter, jitter)) / Gdx.graphics.getWidth() - 1f;
					float yy = (Gdx.graphics.getHeight() - 2f* (y + MathUtils.random(-jitter, jitter))) / Gdx.graphics.getHeight();
					dir.set(main.cam.direction).mulAdd(halfWidthDir, xx).mulAdd(halfHeightDir, yy).nor();
					ray.set(main.cam.position, dir);
					
					//raytrace for pixel
					Vector3 modelNormal = new Vector3();
					
					int closestHit = -1;
					float minHitDist = 50;
					Vector2 uv = new Vector2();
					
					for (int i = 0; i < main.models.size(); i++){
						Model model = main.models.get(i);
						
						float hitDist = model.intersectRay(ray);
						if (hitDist > 0 & hitDist < minHitDist){
								minHitDist = hitDist;
								closestHit = i;
						}
						
					}
					
					if (closestHit > -1) {
						Vector3 closestIntersection = new Vector3().set(ray.origin).mulAdd(ray.direction, minHitDist);
						Model hitModel = main.models.get(closestHit);
						modelNormal.set(hitModel.getNormal(closestIntersection));

						depths[x][y] += minHitDist / (float)samples;
						normals[x][y].mulAdd(modelNormal, 1.0f/(float)samples);
					}
				}

			}
		}
	}

Color hsvCol = new Color();

public float[] getHSV(Vector3d rgbCol){
	hsvCol.set((float)rgbCol.x, (float)rgbCol.y, (float)rgbCol.z, 1);
	float[] hsv = hsvCol.toHsv(new float[3]);
	//Gdx.app.log("RGB:", rgbCol.toString()+" HSV:"+hsv[0]+","+hsv[1]+","+hsv[2]);
	return hsv;
}

public void clamp01(Vector3 vec){
	vec.x = MathUtils.clamp(vec.x, 0, 1);
	vec.y = MathUtils.clamp(vec.y, 0, 1);
	vec.z = MathUtils.clamp(vec.z, 0, 1);
}

public Vector3d clamp01(Vector3d vec){
	vec.x = MathUtils.clamp(vec.x, 0, 1);
	vec.y = MathUtils.clamp(vec.y, 0, 1);
	vec.z = MathUtils.clamp(vec.z, 0, 1);
	return vec;
}

public double clamp01(double x){
	return MathUtils.clamp(x, 0.0, 1.0);
}

public float clamp01Float(double x){
	return (float) MathUtils.clamp(x, 0.0, 1.0);
}

public boolean pixelsSimilar(int smartOrClamp, Vector3d col1, Vector3 normal1, float depth1, Vector3d col2, Vector3 normal2, float depth2, float[] threshHSV){
	if (Math.abs(depth1 - depth2) < denoiseThreshDepth 
			&& normal1.dot(normal2) > 0.75f
			&& pixelsSimilarColorOnly(smartOrClamp, col1, col2, threshHSV))
	return true;
	else return false;
}

public boolean pixelsSimilarColorOnly(int smartOrClamp, Vector3d col1, Vector3d col2, float[] threshHSV){
	float[] hsv1 = getHSV(col1);
	float[] hsv2 = getHSV(col2);
	float hTemp = Math.abs(hsv2[0] - hsv1[0]);
	float hDiff = (hTemp > 180f) ? 360f - hTemp : hTemp; 
	float[] thresh = smartOrClamp == 0 ? threshHSV : denClampThresh;
	if (hDiff < thresh[0] 
			& Math.abs(hsv2[1] - hsv1[1]) < thresh[1] 
			& Math.abs(hsv2[2] - hsv1[2]) < thresh[2])
	return true;
	else return false;
}

private void clampDenoise(Vector3[][] normals, float[][] depths, float[] threshHSV) {
	//Setup temp buffer
	Vector3d[][] tempBuf = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			tempBuf[x][y] = new Vector3d();
			//clamp01(pixelCol[x][y]);
		}
	}
	
	Vector3d RAverage = new Vector3d();
	Vector3d LAverage = new Vector3d();
	Vector3 RAverageNormal = new Vector3();
	Vector3 LAverageNormal = new Vector3();
	Vector3d CAverage = new Vector3d();
	int denoiseClampRadius = 1;
	float sideFactor = 1f/(float)denoiseClampRadius;
	float RDepth = 0;
	float LDepth = 0;
	
	//Horizontal blur pass
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			RAverage.setZero();
			LAverage.setZero();
			RAverageNormal.setZero();
			LAverageNormal.setZero();
			CAverage.setZero();
			RDepth = 0;
			LDepth = 0;
			
			//check whether the averages of the 2 sides are similar
			for (int xx = -denoiseClampRadius; xx <= -1; xx++){
				int xTarget = MathUtils.clamp(x+xx,	0, Gdx.graphics.getWidth()-1);
				LAverage.mulAdd(main.pixAlbedo[xTarget][y], sideFactor);
				LDepth += depths[xTarget][y];
				LAverageNormal.mulAdd(normals[xTarget][y], sideFactor);
			}
			for (int xx = 1; xx <= denoiseClampRadius; xx++){
				int xTarget = MathUtils.clamp(x+xx,	0, Gdx.graphics.getWidth()-1);
				RAverage.mulAdd(main.pixAlbedo[xTarget][y], sideFactor);
				RDepth += depths[xTarget][y];
				RAverageNormal.mulAdd(normals[xTarget][y], sideFactor);
			}
			
			//compare surrouding averages
			if (pixelsSimilar(0, LAverage, LAverageNormal, LDepth, RAverage, RAverageNormal, RDepth, threshHSV)){
				//they are similar - so we will get the average of those 2 averages and check that with the original center
				CAverage.mulAdd(RAverage, 0.5f).mulAdd(LAverage, 0.5f);

				//original center not similar to new one - replace with new center CAverage
				if (!pixelsSimilarColorOnly(1, CAverage, main.pixAlbedo[x][y], threshHSV)){
					tempBuf[x][y].set(CAverage);
				}
				else tempBuf[x][y].set(main.pixAlbedo[x][y]);
			}
			//sides are not similar - no point in continuing
			else tempBuf[x][y].set(main.pixAlbedo[x][y]);
			
		}
	}
	
	//Vertical blur pass
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			RAverage.setZero();
			LAverage.setZero();
			CAverage.setZero();
			RAverageNormal.setZero();
			LAverageNormal.setZero();
			RDepth = 0;
			LDepth = 0;
			//check whether the averages of the 2 sides are similar
			for (int yy = -denoiseClampRadius; yy <= -1; yy++){
				int yTarget = MathUtils.clamp(y+yy,	0, Gdx.graphics.getHeight()-1);
				LAverage.mulAdd(tempBuf[x][yTarget], sideFactor);
				LDepth += depths[x][yTarget];
				LAverageNormal.mulAdd(normals[x][yTarget], sideFactor);
			}
			for (int yy = 1; yy <= denoiseClampRadius; yy++){
				int yTarget = MathUtils.clamp(y+yy,	0, Gdx.graphics.getHeight()-1);
				RAverage.mulAdd(tempBuf[x][yTarget], sideFactor);
				RDepth += depths[x][yTarget];
				RAverageNormal.mulAdd(normals[x][yTarget], sideFactor);
			}
			
			//compare surrouding averages' HSVs
			if (pixelsSimilar(0, LAverage, LAverageNormal, LDepth, RAverage, RAverageNormal, RDepth, threshHSV)){
				//they are similar - so we will get the average of those 2 averages and check that with the original center
				CAverage.mulAdd(RAverage, 0.5f).mulAdd(LAverage, 0.5f);
				
				//if not similar replace original center with new center CAverage
				if (!pixelsSimilarColorOnly(1, CAverage, tempBuf[x][y], threshHSV)){
					main.pixAlbedo[x][y].set(CAverage);
				}
				else main.pixAlbedo[x][y].set(tempBuf[x][y]);
			}
			//sides are not similar - no point in continuing
			else main.pixAlbedo[x][y].set(tempBuf[x][y]);
		}
	}
}

private void smartGaussianDenoise(Vector3d[][] srcPixels, Vector3[][] normals, float[][] depths, float[] threshHSV, int denRadius) {
	
	//Setup temp buffer
	Vector3d[][] tempBuf = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			tempBuf[x][y] = new Vector3d();
			//clamp01(pixelCol[x][y]);
		}
	}
	
	Vector3d temp = new Vector3d();
	
	//Horizontal blur pass
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			temp.setZero();
			//calculate center color and depth value
			Vector3d center = new Vector3d();
			float centerDepth = 0f;
			Vector3 centerNormal = new Vector3();
			
			float denCparts = 0;
			for (int denC = 1; denC <= denoiseCenter; denC++) denCparts += 2 * denC;
			denCparts += denoiseCenter + 1;
			for (int cx = -denoiseCenter; cx <= denoiseCenter; cx++){
				float cFactor = ((float)denoiseCenter + 1.0f - (float)Math.abs(cx)) / denCparts;
				int xFinal = MathUtils.clamp(x+cx, 0, Gdx.graphics.getWidth()-1);
				center.mulAdd(srcPixels[xFinal][y], cFactor);
				centerDepth += depths[xFinal][y] * cFactor;
				centerNormal.mulAdd(normals[xFinal][y], cFactor);
			}
			
			//Gdx.app.log("ih", "new center"+center.toString()+" old"+pixelCol[x][y].toString());
			//double totalCenter = center.x + center.y + center.z;
			List<Integer> partList = new ArrayList<Integer>();
			int parts = 0;
			//calculate which pixels will be taken into account
			for (int xx = -denRadius; xx <= denRadius; xx++){
				int xTarget = MathUtils.clamp(x+xx, 0, Gdx.graphics.getWidth()-1);
				Vector3d side = srcPixels[xTarget][y];
				float sideDepth = depths[xTarget][y];
				Vector3 sideNormal = normals[xTarget][y];
				
				if (pixelsSimilar(0, center, centerNormal, centerDepth, side, sideNormal, sideDepth, threshHSV))
				//add to part list
				{
					partList.add(xx);
					parts += denRadius + 1 - Math.abs(xx);
				}
			}
			//no similar pixels were found, use center pixel
			if (partList.size() == 0){
				//partList.add(0);
				//parts += denRadius + 1;
				temp.set(center);
			}
			float partContrib = 1.0f/(float)parts;
			//calculate the final pixel
			for (Integer xP : partList){
				int xTarget = MathUtils.clamp(x+xP, 0, Gdx.graphics.getWidth()-1);
				float finalContrib = (float)(denRadius + 1 - Math.abs(xP)) * partContrib;
				temp.mulAdd(srcPixels[xTarget][y], finalContrib);
			}
			//double pixelsAffected = (float)partList.size()/(float)(2*Math.abs(denoiseRadius)+1);
			//pixelCol[x][y].set(pixelsAffected, pixelsAffected,pixelsAffected);
			tempBuf[x][y].set(temp);
			//tempBuf[x][y].set(pixelCol[x][y]);
			//Gdx.app.log("wehw", pixelCol[x][y].x +","+ pixelCol[x][y].y +","+ pixelCol[x][y].z);
		}
	}
	//Vertical blur pass + Add to original buffer
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			temp.setZero();
			//Vector3d center = pixelCol[x][y];
			Vector3d center = new Vector3d();
			float centerDepth = 0f;
			Vector3 centerNormal = new Vector3();
			
			float denCparts = 0;
			for (int denC = 1; denC <= denoiseCenter; denC++) denCparts += 2 * denC;
			denCparts += denoiseCenter + 1;
			for (int cy = -denoiseCenter; cy <= denoiseCenter; cy++){
				float cFactor = ((float)denoiseCenter + 1.0f - (float)Math.abs(cy)) / denCparts;
				int yFinal = MathUtils.clamp(y+cy, 0, Gdx.graphics.getHeight()-1);
				center.mulAdd(tempBuf[x][yFinal], cFactor);
				centerDepth += depths[x][yFinal] * cFactor;
				centerNormal.mulAdd(normals[x][yFinal], cFactor);
			}

			//double totalCenter = center.x + center.y + center.z;
			List<Integer> partList = new ArrayList<Integer>();
			int parts = 0;
			//calculate which pixels will be taken into account
			for (int yy = -denRadius; yy <= denRadius; yy++){
				int yTarget = MathUtils.clamp(y+yy, 0, Gdx.graphics.getHeight()-1);
				Vector3d side = tempBuf[x][yTarget];
				float sideDepth = depths[x][yTarget];
				Vector3 sideNormal = normals[x][yTarget];
				//Vector3d side = pixelCol[x][yTarget];
				//add to part list
				if (pixelsSimilar(0, center, centerNormal, centerDepth, side, sideNormal, sideDepth, threshHSV))
				{
					partList.add(yy);
					parts += denRadius + 1 - Math.abs(yy);
				}
			}
			//no similar pixels were found, use center pixel
			if (partList.size() == 0){
				//partList.add(0);
				//parts += denRadius + 1;
				temp.set(center);
			}
			
			float partContrib = 1.0f/(float)parts;
			//calculate the final pixel
			for (Integer yP : partList){
				int yTarget = MathUtils.clamp(y+yP, 0, Gdx.graphics.getHeight()-1);
				float finalContrib = (float)(denRadius + 1 - Math.abs(yP)) * partContrib;
				temp.mulAdd(tempBuf[x][yTarget], finalContrib);
				//temp.mulAdd(pixelCol[x][yTarget], finalContrib);
			}
			srcPixels[x][y].set(temp);
			//double pixelsAffected = (float)partList.size()/(float)(2*Math.abs(denoiseRadius)+1);
			//pixelCol[x][y].set(pixelsAffected, pixelsAffected,pixelsAffected);
		}
	}
	
}

private void bloom(){
	//Setup temp buffer, copy from normal buffer and apply threshold filter
	Vector3d[][] tempBuf = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
	Vector3d[][] tempBuf2 = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			Vector3d origPix = main.pixFinal[x][y];
			double luma = 0.2126*origPix.x + 0.7152*origPix.y + 0.0722*origPix.z;
			if (luma < bloomThreshold) tempBuf[x][y] = new Vector3d();
			else tempBuf[x][y] = new Vector3d(origPix);
		}
	}
	
	Vector3d temp = new Vector3d();
	float part;
	float tempPart = 0;
	for (int i = bloomRadius; i >= 1; i--){
		tempPart += i * 2;
	}
	tempPart += bloomRadius + 1;
	part = 1f/tempPart;
	
	//Horizontal blur pass
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			temp.setZero();
			for (int xx = -bloomRadius; xx <= bloomRadius; xx++){
				int xTarget = MathUtils.clamp(x+xx, 0, Gdx.graphics.getWidth()-1);
				float contrib = (bloomRadius + 1 - Math.abs(xx)) * part;
				temp.mulAdd(tempBuf[xTarget][y], contrib);
			}
			tempBuf2[x][y] = new Vector3d(temp);
		}
	}
	//Vertical blur pass + Add to original buffer
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			temp.setZero();
			for (int yy = -bloomRadius; yy <= bloomRadius; yy++){
				int yTarget = MathUtils.clamp(y+yy, 0, Gdx.graphics.getHeight()-1);
				float contrib = (bloomRadius + 1 - Math.abs(yy)) * part;
				temp.mulAdd(tempBuf2[x][yTarget], contrib);
			}
			//tempBuf[x][y].set(temp);
			//Add to original buffer
			main.pixFinal[x][y].mulAdd(temp, bloomStrength);
		}
	}
}

private void depthOfField(float[][] depths) {
	if (DOFtarget == null || DOFmaxBlur < 1) return;
	
	float focusDist = main.cam.position.dst(DOFtarget.getPosition());
	//Setup temp buffer
	Vector3d[][] tempBuf = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
	Vector3d temp = new Vector3d();

	//Horizontal blur pass
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			float blurFac = 0;
			float distDiff = Math.abs(depths[x][y] - focusDist);
			if (distDiff >= DOFfocusWidth) 
				blurFac = MathUtils.clamp((distDiff - DOFfocusWidth) / (DOFmaxBlurDist - DOFfocusWidth), 0f, 1f);
			int dofRadius = (int) (blurFac * DOFmaxBlur);
			
			float part;
			float tempPart = 0;
			for (int i = dofRadius; i >= 1; i--){
				tempPart += i * 2;
			}
			tempPart += dofRadius + 1;
			part = 1f/tempPart;
			
			temp.setZero();
			for (int xx = -dofRadius; xx <= dofRadius; xx++){
				int xTarget = MathUtils.clamp(x+xx, 0, Gdx.graphics.getWidth()-1);
				float contrib = (dofRadius + 1 - Math.abs(xx)) * part;
				temp.mulAdd(main.pixFinal[xTarget][y], contrib);
			}
			tempBuf[x][y] = new Vector3d(temp);
		}
	}
	//Vertical blur pass + Add to original buffer
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			
			float blurFac = 0;
			float distDiff = Math.abs(depths[x][y] - focusDist);
			if (distDiff >= DOFfocusWidth) 
				blurFac = MathUtils.clamp((distDiff - DOFfocusWidth) / (DOFmaxBlurDist - DOFfocusWidth), 0f, 1f);
			int dofRadius = (int) (blurFac * DOFmaxBlur);
			
			float part;
			float tempPart = 0;
			for (int i = dofRadius; i >= 1; i--){
				tempPart += i * 2;
			}
			tempPart += dofRadius + 1;
			part = 1f/tempPart;
			
			temp.setZero();
			for (int yy = -dofRadius; yy <= dofRadius; yy++){
				int yTarget = MathUtils.clamp(y+yy, 0, Gdx.graphics.getHeight()-1);
				float contrib = (dofRadius + 1 - Math.abs(yy)) * part;
				temp.mulAdd(tempBuf[x][yTarget], contrib);
			}
			//tempBuf[x][y].set(temp);
			//Add to original buffer
			main.pixFinal[x][y].set(temp);
		}
	}
	
}

private void mergeChannels() {
	for (int x =0; x<Gdx.graphics.getWidth();x++){
		for (int y =0; y<Gdx.graphics.getHeight();y++){
			Vector3d finalColor = new Vector3d().add(main.pixDiffDirectLight[x][y]).add(main.pixDiffIndirectLight[x][y]).scl(main.pixAlbedo[x][y]).add(main.pixGlossy[x][y]);
			main.pixFinal[x][y] = finalColor;
		}
	}
}
	
}
