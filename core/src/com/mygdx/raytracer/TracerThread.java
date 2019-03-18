package com.mygdx.raytracer;

import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class TracerThread extends Thread{

	
	MyRaytracer mainThread;
	
	int xStart, xEnd = 200, yStart, yEnd = 200;
	int screenW, screenH;
	int threadX, threadY;
	int tW, tH;
	
	Vector3d[][] pixDiff;
	Vector3d[][] pixDirectLight;
	Vector3d[][] pixDiffLight;
	Vector3d[][] pixGlossy;
	
	boolean resetPending = false;
	int currSample = 0;
	boolean threadFinished = false;
	
	public TracerThread(MyRaytracer mainThread, int tX, int tY){
		this.mainThread = mainThread;
		threadX = tX;
		threadY = tY;
		tW = Gdx.graphics.getWidth() / mainThread.threadsHor;
		tH = Gdx.graphics.getHeight() / mainThread.threadsVert;
		xStart = tX * tW; 
		xEnd = xStart + tW - 1;
		yStart = tY * tH; 
		yEnd = yStart + tH - 1;
		
		pixDirectLight = new Vector3d[tW][tH];
		pixDiffLight = new Vector3d[tW][tH];
		pixGlossy = new Vector3d[tW][tH];
		pixDiff = new Vector3d[tW][tH];
		for (int x = 0; x<tW; x++){
			for (int y = 0; y<tH; y++){
					pixGlossy[x][y] = new Vector3d();
					pixDirectLight[x][y] = new Vector3d();
					pixDiffLight[x][y] = new Vector3d();
					pixDiff[x][y] = new Vector3d();
			}
		}
		
	}
	
	public void run() {

        while (!mainThread.shutDown){
        	
        	if (currSample < mainThread.totalSamples && !mainThread.isPreviewEnabled() || mainThread.isPreviewEnabled()){
        		renderScreen();
        	} else {
        		try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        
        /*Gdx.app.postRunnable(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
			}
        	
        });*/
    }
	
	public void setPreviewMode(boolean b) {
		
	}
	
	public void resetRender() {
		currSample = 0;
		resetPending = true;
	}
	
	//boolean wasPreviouslyPreview = false;

	private boolean renderScreen() {
		Ray ray = new Ray();
		Vector3 dir = new Vector3(0,0,0);
		
		PerspectiveCamera cam = mainThread.cam;
		Vector3[] planePoints = cam.frustum.planePoints;
		Vector3 halfWidthDir = new Vector3(planePoints[0]).sub(planePoints[1]).scl(0.5f);
		Vector3 halfHeightDir = new Vector3(planePoints[2]).sub(planePoints[1]).scl(0.5f);
		
		float AAjitter = 0.5f;
		for (int x = xStart; x <= xEnd; x++){
			for (int y = yStart; y <= yEnd; y++){
				
				float xx = 2f* (x + MathUtils.random(-AAjitter, AAjitter)) / Gdx.graphics.getWidth() - 1f;
				float yy = (Gdx.graphics.getHeight() - 2f* (y + MathUtils.random(-AAjitter, AAjitter))) / Gdx.graphics.getHeight();
				dir.set(cam.direction).mulAdd(halfWidthDir, xx).mulAdd(halfHeightDir, yy).nor();
				ray.set(cam.position, dir);
				
				/*if (mainThread.previewMode != wasPreviouslyPreview){
					wasPreviouslyPreview = mainThread.previewMode;
					currSample = 0;
					return true;
				}*/
				
				if (!mainThread.isPreviewEnabled()){
					
					Vector3d sampleColor = new Vector3d();
					Vector3d sampleLight = new Vector3d();
					Vector3d sampleGlossy = new Vector3d();
					Vector3d sampleDirectLight = new Vector3d();
					pathTraceNextEvent(ray, 0, sampleColor, sampleLight, sampleGlossy, sampleDirectLight, null);

					double currSampleContrib = 1.0/(double)(currSample+1.0);
					double previousSamplesContrib = 1.0 - currSampleContrib;

					sampleColor.scl(currSampleContrib);
					sampleLight.scl(currSampleContrib);
					sampleGlossy.scl(currSampleContrib);
					sampleDirectLight.scl(currSampleContrib);
					
					Vector3d previousCol = pixDiff[x - xStart][y - yStart];
					Vector3d previousLight = pixDiffLight[x - xStart][y - yStart];
					Vector3d previousDirectLight = pixDirectLight[x - xStart][y - yStart];
					Vector3d previousGlossy = pixGlossy[x - xStart][y - yStart];
					previousCol.scl(previousSamplesContrib).add(sampleColor.x, sampleColor.y, sampleColor.z);
					previousLight.scl(previousSamplesContrib).add(sampleLight.x, sampleLight.y, sampleLight.z);
					previousDirectLight.scl(previousSamplesContrib).add(sampleDirectLight.x, sampleDirectLight.y, sampleDirectLight.z);
					previousGlossy.scl(previousSamplesContrib).add(sampleGlossy.x, sampleGlossy.y, sampleGlossy.z);
					
					Vector3d processedColor = null;
					if (mainThread.viewMode == ViewMode.FINAL)
						processedColor = new Vector3d(previousDirectLight).add(previousLight).scl(previousCol).add(previousGlossy);
					else if (mainThread.viewMode == ViewMode.ALBEDO)
						processedColor = new Vector3d(previousCol);
					else if (mainThread.viewMode == ViewMode.DIRECTLIGHT)
						processedColor = new Vector3d(previousDirectLight);
					else if (mainThread.viewMode == ViewMode.INDIRECTLIGHT)
						processedColor = new Vector3d(previousLight);
					else processedColor = new Vector3d(previousGlossy);
					
					
					if (true) processedColor.set(Math.pow(processedColor.x, 1f/2.2f), Math.pow(processedColor.y, 1f/2.2f), Math.pow(processedColor.z, 1f/2.2f));
					clamp01(processedColor);
					if (mainThread.shutDown) return false;
					
					mainThread.image.drawPixel(x, y, Color.rgba8888((float)processedColor.x, (float)processedColor.y, (float)processedColor.z, 1));
				}
				else{
						Vector3d sampleColor = previewRaytrace(ray, 0);
						
						if (mainThread.shutDown) return false;
						mainThread.image.drawPixel(x, y, Color.rgba8888((float)sampleColor.x, (float)sampleColor.y, (float)sampleColor.z, 1));
					
				}
				
				if (resetPending && !mainThread.isPreviewEnabled()) {resetPending = false; return false;}
				//image.drawPixel(x, y, Color.rgba8888(pixelColor.r, pixelColor.g, pixelColor.b, 1));
			}	
		}
		//if (currSample == totalSamples-1) postProcessing();
		//if (!mainThread.previewMode) currSample++;
		currSample++;
		//if (mainThread.simpleRendering) threadFinished = true;  
		//simpleRenderDirty = false;
		return true;
	}

	
	public void pathTrace(Ray ray, int depth, Vector3d sampleDiff, Vector3d sampleDiffLight, Vector3d sampleGlossy, Vector3d sampleGlass){
		//Gdx.app.log("Ewgw", "W"+depth);
		// Russian roulette: starting at depth 5, each recursive step will stop with a probability of 0.1
		float rrFactor = 1.0f;
		if (depth >= 4) {
			float rrStopProbability = 0.1f;
			if (MathUtils.random() <= rrStopProbability) {
				//sampleColor.set(0f,0f,0f);
				sampleDiffLight.set(0f,0f,0f);
				return;
			}
			rrFactor = 1.0f / (1.0f - rrStopProbability);
		}
		
		int closestHit = -1;
		float minHitDist = mainThread.maxViewDist;
		
		//search for intersections
		for (int i = 0; i < mainThread.models.size(); i++){
			Model model = mainThread.models.get(i);
			
			float hitDist = model.intersectRay(ray);
			if (hitDist > 0f & hitDist < minHitDist){
				//is it a light? Then don't show in the camera rays (depth==0)
				if (!(model.cameraInvisible && depth == 0) && !(model.castShadow == false && depth > 0)){
					minHitDist = hitDist;
					closestHit = i;
				}		
			}
			
		}
		
		//we found one
		if (closestHit > -1) {
			Vector3 closestIntersection = new Vector3().set(ray.origin).mulAdd(ray.direction, minHitDist);
			Model hitModel = mainThread.models.get(closestHit);
			Vector3 modelNormal  = new Vector3(hitModel.getNormal(closestIntersection));
			
			Vector3d finalLight = new Vector3d(0,0,0);
			Vector3d tempLight = new Vector3d(0,0,0);
			Vector3d tempColor = new Vector3d(0,0,0);
			Vector3d tempGlossy = new Vector3d(0,0,0);
			//Vector3d tempGlass = new Vector3d(0,0,0);
			
			//emission
			finalLight.add(hitModel.emissionColor).scl(rrFactor);
			//if we hit a light or reached the max bounces stop bouncing
			if (depth < mainThread.maxBounces && hitModel.emissionIntensity < 0.01f){
				
				boolean glossy = false;
				//bounce new ray
				Ray newRay;
				//reflect ray
				if (!hitModel.glass){
					float glossyRand = MathUtils.random();
					glossy = glossyRand < hitModel.glossyFactor;
					if (glossy) newRay = getBounceRayGlossy(closestIntersection, modelNormal, ray.direction, hitModel.glossyRoughness);
					else newRay = getBounceRayDiffuse(closestIntersection, modelNormal);
				}
				else{
					newRay = new Ray();
					glossy = getBounceRayGlass(newRay, ray.direction, modelNormal, closestIntersection);
				}
				
				//raytrace
				//Vector3d contribCol = new Vector3d();
				Vector3d contribLight = new Vector3d();
				Vector3d contribGlass = new Vector3d();
				pathTrace(newRay, depth+1, null, contribLight, null, contribGlass);
				//clamping
				//if (glossy && depth == 0) contribLight.clamp(0, mainThread.clampGlossyDirect);
				//else if (glossy && depth > 0) contribLight.clamp(0, mainThread.clampGlossyIndirect);

				if (hitModel.glass){
						if (depth > 0) {
							tempLight.set(contribLight).scl(rrFactor*1.15f);
							if (glossy) tempLight.scl(hitModel.glossy);
							else tempLight.scl(hitModel.diffuse);
						}
						else{
							tempGlossy.set(contribLight).scl(rrFactor*1.15f);
							if (glossy) tempGlossy.scl(hitModel.glossy);
							else tempGlossy.scl(hitModel.diffuse);
						}
				}
				else if (glossy) {
					if (depth > 0) {
						tempLight.set(contribLight).scl(rrFactor);
						tempLight.scl(hitModel.glossy);
					}
					else{
						tempGlossy.set(contribLight).scl(rrFactor);
						tempGlossy.scl(hitModel.glossy);
					}
				}
				else {
					float cosT = newRay.direction.dot(modelNormal);
					tempLight.set(contribLight).scl(cosT * rrFactor * 1f);
					//if depth = 0 separate lighting and color for diffuse
					if (depth > 0) tempLight.scl(hitModel.diffuse);
					else tempColor.set(hitModel.diffuse);
					
				}
				//calculate the texture's contribution if there's one
				if (hitModel.diffTexture != null){
					Vector2 uv = new Vector2(hitModel.getUV(closestIntersection));
					Vector3 uvCol = hitModel.diffTexture.getColor(uv);
					if (depth > 0) tempLight.scl(uvCol);
					else {
						tempColor.scl(uvCol);
						tempGlossy.scl(uvCol);
					}
				}
				finalLight.add(tempLight);

			}
			sampleDiffLight.set(finalLight);
			
			if (depth == 0) {
				sampleGlossy.set(tempGlossy);
				sampleDiff.set(tempColor);
				//sampleGlass.set(tempGlass);
			}
		}
		//we did not find one, therefore we hit the sky
		else{
			float skyBlend = Vector3.Y.dot(ray.direction);
			skyBlend = MathUtils.clamp(skyBlend, 0, 1);
			skyBlend = (float) Math.pow(skyBlend, mainThread.horizonPower);
			Vector3d finalSkyColor = new Vector3d(mainThread.horizonColor).lerp(mainThread.skyColor, skyBlend);
			
			//indirect ray color?
			
			if (depth > 0) {
				finalSkyColor.scl(mainThread.skyIndirectStrength);
				sampleDiffLight.set(finalSkyColor);
			}
			//temp FIX
			else sampleGlossy.set(finalSkyColor);
		}
	}
	
	public void pathTraceNextEvent(Ray ray, int depth, Vector3d sampleDiff, Vector3d sampleDiffLight, Vector3d sampleGlossy, Vector3d sampleDirectLight, RayType previousRayType){
		//Gdx.app.log("Ewgw", "W"+depth);
		// Russian roulette: starting at depth 5, each recursive step will stop with a probability of 0.1
		float rrFactor = 1.0f;
		if (depth >= mainThread.minBounces) {
			float rrStopProbability = 0.1f;
			if (MathUtils.random() <= rrStopProbability) {
				//sampleColor.set(0f,0f,0f);
				sampleDiffLight.set(0f,0f,0f);
				return;
			}
			rrFactor = 1.0f / (1.0f - rrStopProbability);
		}
		
		int closestHit = -1;
		float minHitDist = mainThread.maxViewDist;
		
		//search for intersections
		for (int i = 0; i < mainThread.models.size(); i++){
			Model model = mainThread.models.get(i);
			
			float hitDist = model.intersectRay(ray);
			if (hitDist > 0f & hitDist < minHitDist){
				//is it a light? Then don't show in the camera rays (depth==0)
				if (!(model.cameraInvisible && depth == 0) && !(model.castShadow == false && depth > 0)){
					minHitDist = hitDist;
					closestHit = i;
				}		
			}
		}
		
		//we found one
		if (closestHit > -1) {
			Vector3 closestIntersection = new Vector3().set(ray.origin).mulAdd(ray.direction, minHitDist);
			Model hitModel = mainThread.models.get(closestHit);
			Vector3 modelNormal  = new Vector3(hitModel.getNormal(closestIntersection));
			
			Vector3d finalLight = new Vector3d(0,0,0);
			Vector3d tempColor = new Vector3d(0,0,0);
			Vector3d tempGlossy = new Vector3d(0,0,0);

			//Reflection ray casting
			//if we hit a light or reached the max bounces stop bouncing
			if (depth < mainThread.maxBounces){
				//Bounce a new ray
				Ray newRay;
				RayType rayType;
				//reflect ray
				
				if (hitModel.transparencyFactor > 0.01f && MathUtils.random() < hitModel.transparencyFactor){
					rayType = RayType.TRANSPARENT;
					newRay = getBounceRayTransparent(ray.direction, closestIntersection);
				}
				else{
					if (hitModel.glass){
						newRay = new Ray();
						boolean gloss = getBounceRayGlass(newRay, ray.direction, modelNormal, closestIntersection);
						rayType = gloss ? RayType.GLOSSY : RayType.REFRACTION;
					}
					else if (hitModel.glossyFactor > 0.01f && MathUtils.random() < hitModel.glossyFactor){
						rayType = RayType.GLOSSY;
						newRay = getBounceRayGlossy(closestIntersection, modelNormal, ray.direction, hitModel.glossyRoughness);
					}
					else if (hitModel.translucencyFactor > 0.01f && MathUtils.random() < hitModel.translucencyFactor){
						rayType = RayType.TRANSLUCENT;
						newRay =  getBounceRayTranslucent(ray.direction, closestIntersection);
					}
					else{
						rayType = RayType.DIFFUSE;
						newRay = getBounceRayDiffuse(closestIntersection, modelNormal);
					}
				}
				
				//raytrace
				Vector3d contribLight = new Vector3d();
				if (hitModel.emissionIntensity < 0.005f)
				pathTraceNextEvent(newRay, depth+1, null, contribLight, null, null, rayType);
				//else finalLight.add(hitModel.emissionColor).scl(hitModel.emissionIntensity).scl(rrFactor);
				
				//Clamping the new ray's glossy contribution
				//if (rayType == RayType.GLOSSY && depth == 0) contribLight.clamp(0, mainThread.clampGlossyDirect);
				//else if (rayType == RayType.GLOSSY && depth > 0) contribLight.clamp(0, mainThread.clampGlossyIndirect);
				
				//Calculate lighting results from the ray we just sent
				if (rayType == RayType.REFRACTION) getRefractionLighting(contribLight, finalLight, tempGlossy, depth, rrFactor, hitModel);
				else if (rayType == RayType.GLOSSY) getGlossyLighting(contribLight, finalLight, tempGlossy, depth, rrFactor, hitModel);
				else if (rayType == RayType.DIFFUSE) {
					//Shadow ray casting
					Model emitter = getRandomEmitter();
					if (emitter != null){
						//slightly bias the origin
						Vector3 shadowRayOrigin = new Vector3(closestIntersection).mulAdd(modelNormal, 0.01f);
						Vector3 lightPoint = emitter.getRandomSurfacePoint();
						Vector3 lightPointNormal = emitter.getNormal(lightPoint);
						Vector3 shadowRayDir = new Vector3(lightPoint).sub(closestIntersection);
						float r2 = shadowRayDir.len2();
						float area = emitter.getSurfaceArea();
						Ray shadowRay = new Ray(shadowRayOrigin, shadowRayDir);
						
						Model shadowHitModel = getClosestIntersection(shadowRay);
						if (shadowHitModel == emitter){ //shadow ray was succesful in finding the emitter
							float cosModel = MathUtils.clamp(shadowRay.direction.dot(modelNormal), 0, 1);
							Vector3 minusShadowRayDir = new Vector3(shadowRay.direction).scl(-1);
							float cosLight = MathUtils.clamp(minusShadowRayDir.dot(lightPointNormal), 0, 1);
							
							//float fac = (area * cosT * cosT2 * rrFactor) * (1f/(float)mainThread.maxBounces) / r2;
							float fac = (area * cosLight * cosModel * rrFactor)  / (r2 * (float)mainThread.maxBounces);
							Vector3 shadowRayContrib = new Vector3(shadowHitModel.emissionColor).scl(shadowHitModel.emissionIntensity).scl(fac);
							//if (depth == 0) sampleDirectLight.set(shadowHitModel.emissionColor).scl(shadowHitModel.emissionIntensity).scl(fac);
							//else finalLight.add(shadowHitModel.emissionColor).scl(shadowHitModel.emissionIntensity).scl(fac);
							if (depth == 0) {
								//Clamping the direct contribution
								shadowRayContrib.clamp(0, mainThread.clampDiffuseDirect);
								sampleDirectLight.set(shadowRayContrib);
							}
							else finalLight.add(shadowRayContrib);
						}
						
						
					}		
					
					getDiffuseLighting(newRay.direction, modelNormal, contribLight, finalLight, tempColor, depth, rrFactor, hitModel, previousRayType, sampleDirectLight);
				}
				else if (rayType == RayType.TRANSPARENT) getTransparentLighting(contribLight, finalLight, tempColor, depth, rrFactor, hitModel);
				else if (rayType == RayType.TRANSLUCENT) getTransparentLighting(contribLight, finalLight, tempColor, depth, rrFactor, hitModel);
				
				//calculate the texture's contribution if there's one
				if (hitModel.diffTexture != null){
					Vector2 uv = new Vector2(hitModel.getUV(closestIntersection));
					Vector3 uvCol = hitModel.diffTexture.getColor(uv);
					if (depth > 0) finalLight.scl(uvCol);
					else {
						if (rayType == RayType.DIFFUSE || rayType == RayType.TRANSPARENT) tempColor.scl(uvCol);
						else if (rayType == RayType.GLOSSY) tempGlossy.scl(uvCol);
					}
				}
				//Clamping the indirect contribution
				if (depth == 0) {
					if (rayType == RayType.DIFFUSE) finalLight.clamp(0, mainThread.clampDiffuseIndirect);
					//else if (rayType == RayType.GLOSSY) finalLight.clamp(0, mainThread.clampGlossyIndirect);
					//else if (rayType == RayType.REFRACTION) finalLight.clamp(0, mainThread.clampRefractionIndirect);
				}
				sampleDiffLight.set(finalLight);
			}
			
			
			if (depth == 0) {
				tempGlossy.clamp(0, mainThread.clampGlossyRefractIndirect);
				sampleGlossy.set(tempGlossy);
				sampleDiff.set(tempColor);
				//sampleGlass.set(tempGlass);
			}
		}
		//we did not find one, therefore we hit the sky
		else{
			if (mainThread.skyMap == null){
				float skyBlend = Vector3.Y.dot(ray.direction);
				skyBlend = MathUtils.clamp(skyBlend, 0, 1);
				skyBlend = (float) Math.pow(skyBlend, mainThread.horizonPower);
				Vector3d finalSkyColor = new Vector3d(mainThread.horizonColor).lerp(mainThread.skyColor, skyBlend);
				
				//indirect ray color?
				
				if (depth > 0) {
					finalSkyColor.scl(mainThread.skyIndirectStrength);
					//finalSkyColor.clamp(0, mainThread.clampDiffuseIndirect);
					sampleDiffLight.set(finalSkyColor);
				}
				//temp FIX
				else {
					//finalSkyColor.clamp(0, mainThread.clampDiffuseDirect);
					sampleGlossy.set(finalSkyColor);
				}
			}
			else {
				Vector2 skyUv = getSkymapUV(ray.direction);
				Vector3 skyCol = mainThread.skyMap.getColor(skyUv);
				if (depth > 0) {
					Vector3 skyColIndirect = new Vector3(skyCol);
					
					if (previousRayType == RayType.DIFFUSE) {
						skyColIndirect.set((float)Math.pow(skyColIndirect.x, mainThread.skyIndirectDiffuseGamma), 
								(float)Math.pow(skyColIndirect.y, mainThread.skyIndirectDiffuseGamma), 
								(float)Math.pow(skyColIndirect.z, mainThread.skyIndirectDiffuseGamma));
						skyColIndirect.scl(mainThread.skyIndirectDiffuseStrength);
					}
					else if (previousRayType == RayType.GLOSSY) skyColIndirect.scl(mainThread.skyIndirectGlossyStrength);
					//skyColIndirect.clamp(0, mainThread.clampDiffuseIndirect);
					sampleDiffLight.set(skyColIndirect);
				}
				//temp FIX
				else {
					Vector3 skyColDirect = new Vector3(skyCol);
					skyColDirect.scl(mainThread.skyDirectStrength);
					//skyColDirect.clamp(0, mainThread.clampDiffuseDirect);
					sampleGlossy.set(skyColDirect);
				}
			}
		}
	}
	
	public Vector2 getSkymapUV(Vector3 viewDir){
		float zCos = viewDir.dot(Vector3.Z);
		float xCos = viewDir.dot(Vector3.X);

		float angleRads = (float) Math.atan2(xCos, zCos) ;
		angleRads = angleRads < 0 ? angleRads + MathUtils.PI * 2 : angleRads;
		float u = angleRads/(MathUtils.PI * 2f);

		float yCos = viewDir.dot(Vector3.Y);
		float v = Math.abs(yCos - 1) * 0.5f;

		Vector2 uv = new Vector2(u, v);
		return uv;
	}
	
	private Model getClosestIntersection(Ray ray){
		float minHitDist = mainThread.maxViewDist;
		Model closestModel = null;
		for (int i = 0; i < mainThread.models.size(); i++){
			Model model = mainThread.models.get(i);
			
			float hitDist = model.intersectRay(ray);
			if (hitDist > 0f & hitDist < minHitDist){
					minHitDist = hitDist;
					closestModel = model;	
			}
		}	
		return closestModel;
	}
	
	private Model getRandomEmitter(){

		if (mainThread.emitters.size() == 0){
			for (int i = 0; i < mainThread.models.size(); i++){
				Model model = mainThread.models.get(i);
				if (model.emissionIntensity > 0) mainThread.emitters.add(model);
			}
		}
		if (mainThread.emitters.size() > 0){
			int rand = MathUtils.random(0, mainThread.emitters.size()-1);
			Model emitter = mainThread.emitters.get(rand);
			return emitter;	
		}
		
		else return null;
		
	}
	
	private void getTransparentLighting(Vector3d contribLight, Vector3d finalLight, Vector3d tempColor, int depth, float rrFactor, Model hitModel){
		contribLight.scl(rrFactor);
		if (depth > 0) {
			finalLight.set(contribLight).scl(hitModel.diffuse);
		}
		else{
			tempColor.set(contribLight).scl(hitModel.diffuse);
		}
	}
	
	private void getTranslucentLighting(Vector3d contribLight, Vector3d finalLight, Vector3d tempColor, int depth, float rrFactor, Model hitModel){
		contribLight.scl(rrFactor);
		if (depth > 0) {
			finalLight.set(contribLight).scl(hitModel.diffuse);
		}
		else{
			tempColor.set(contribLight).scl(hitModel.diffuse);
		}
	}
	
	private void getRefractionLighting(Vector3d contribLight, Vector3d finalLight, Vector3d tempGlossy, int depth, float rrFactor, Model hitModel){
		contribLight.scl(rrFactor*1.15f);
		if (depth > 0) {
			finalLight.set(contribLight).scl(hitModel.diffuse);
		}
		else{
			tempGlossy.set(contribLight).scl(hitModel.diffuse);
		}
	}
	
	private void getGlossyLighting(Vector3d contribLight, Vector3d finalLight, Vector3d tempGlossy, int depth, float rrFactor, Model hitModel){
		contribLight.scl(rrFactor);
		if (depth > 0) {
			finalLight.set(contribLight).scl(hitModel.glossy);
		}
		else{
			tempGlossy.set(contribLight).scl(hitModel.glossy);
		}
	}
	
	private void getDiffuseLighting(Vector3 newRayDir, Vector3 hitNormal, Vector3d contribLight, Vector3d finalLight, Vector3d tempColor, int depth, float rrFactor, Model hitModel, RayType previousRayType, Vector3d sampleDirectLight){
		float cosT = MathUtils.clamp(newRayDir.dot(hitNormal),0,1);
		contribLight.scl(cosT * rrFactor * 1f);
		//if depth = 0 separate lighting and color for diffuse
		finalLight.add(contribLight);
		Vector3 finalEmission = new Vector3(hitModel.emissionColor).scl(hitModel.emissionIntensity);
		
		if (depth > 0) {
			if (previousRayType != RayType.DIFFUSE) finalLight.add(finalEmission);
			finalLight.scl(hitModel.diffuse);
		}
		else {
			//if (previousRayType != RayType.DIFFUSE) 
			sampleDirectLight.add(finalEmission);
			tempColor.set(hitModel.diffuse);
			//Gdx.app.log("wegw", previousRayType.toString());
		}
	}
	
	private boolean getBounceRayGlass(Ray newRay, Vector3 rayDir, Vector3 N, Vector3 intersection) {
		//refraction Index
		float n = 1.5f;
		float R0 = (1.0f-n)/(1.0f+n);
		R0 = R0*R0;
		Vector3 tempN = new Vector3(N);
		if(tempN.dot(rayDir)>0f) { // we're inside the medium
			tempN.scl(-1f);
			n = 1f/n;
		}
		n=1f/n;
		float cost1 = (tempN.dot(rayDir))*-1f; // cosine of theta_1
		float cost2 = 1.0f - n*n*(1.0f-cost1*cost1); // cosine of theta_2
		float Rprob = R0 + (1.0f-R0) * (float)Math.pow(1.0f - cost1, 5.0f); // Schlick-approximation
		if (cost2 > 0f && MathUtils.random() > Rprob) { // refraction direction
			Vector3 tempRayDir = new Vector3(rayDir);
			Vector3 r1 = tempRayDir.scl(n);
			Vector3 r2 = tempN.scl(n*cost1- (float)Math.sqrt(cost2));
			newRay.set(intersection, r1.add(r2).nor());
			return false;
		}
		else { // reflection direction
			Vector3 tempRayDir = new Vector3(rayDir);
			tempRayDir.add(tempN.scl(cost1*2f)).nor();
			newRay.set(intersection, tempRayDir);
			return true;
		}	
	}
	
	
	private Ray getBounceRayTransparent(Vector3 incomingDir, Vector3 intersection){
		Vector3 newOrigin = new Vector3(intersection).mulAdd(incomingDir, 0.05f);
		return new Ray(newOrigin, incomingDir);
		
	}
	
	private Ray getBounceRayTranslucent(Vector3 incomingDir, Vector3 intersection){
		Vector3 newOrigin = new Vector3(intersection).mulAdd(incomingDir, 0.05f);
		return new Ray(newOrigin, incomingDir);
		
	}

	private Ray getBounceRayGlossy(Vector3 origin, Vector3 surfaceNormal, Vector3 incomingDir, float glossyRough) {
		Vector3 reflectDir = new Vector3();
		//-2*(V dot N)*N + V
		float dot = incomingDir.dot(surfaceNormal);
		reflectDir.set(surfaceNormal).scl(-2f*dot).add(incomingDir);
		//roughness
		if (glossyRough > 0){
			Vector3 randDir = new Vector3().setToRandomDirection();
			float randDeg = MathUtils.random(-glossyRough*45f, glossyRough*45f);
			reflectDir.rotate(randDir, randDeg);
		}
		
		return new Ray(origin, reflectDir);
	}
	
	//Halton hal1 = new Halton();
	//Halton hal2 = new Halton();
	
	public static Vector2 halton(Vector2 into, int index)
    {
        int s = (index+1 & 0x7fffffff),
                numX = s % 2, numY = s % 3, denX = 2, denY = 3;
        while (denX <= s) {
            numX *= 2;
            numX += (s % (denX * 2)) / denX;
            denX *= 2;
        }
        while (denY <= s) {
            numY *= 3;
            numY += (s % (denY * 3)) / denY;
            denY *= 3;
        }
        if(into == null)
            into = new Vector2((float)numX / denX, (float)numY / denY);
        else
            into.set((float)numX / denX, (float)numY / denY);
        return into;
}
	int hal = 20;
	private Ray getBounceRayDiffuse(Vector3 origin, Vector3 originalNormal) {
		/*hal1.next();
		float u1 = (float) hal1.get();
		hal2.next();
		float u2 = (float) hal2.get();*/

		//Vector2 halton = halton(new Vector2(), hal);
		//Vector3 hemiDir = CosineSampleHemisphere(halton.x, halton.y);
		Vector3 hemiDir = CosineSampleHemisphere(MathUtils.random(), MathUtils.random());
		
		Vector3 rotX = new Vector3();
		Vector3 rotY = new Vector3();
		Vector3 rotatedDir = new Vector3();
		ons(originalNormal, rotX, rotY);
		rotatedDir.x = new Vector3(rotX.x, rotY.x, originalNormal.x).dot(hemiDir);
		rotatedDir.y = new Vector3(rotX.y, rotY.y, originalNormal.y).dot(hemiDir);
		rotatedDir.z = new Vector3(rotX.z, rotY.z, originalNormal.z).dot(hemiDir);
		
		return new Ray(origin, rotatedDir);
		//return new Ray(origin, hemiDir.add(originalNormal));
	}
	
	public Vector3d clamp01(Vector3d vec){
		vec.x = MathUtils.clamp(vec.x, 0, 1);
		vec.y = MathUtils.clamp(vec.y, 0, 1);
		vec.z = MathUtils.clamp(vec.z, 0, 1);
		return vec;
	}
	
	// given v1, set v2 and v3 so they form an orthonormal system
	// (we assume v1 is already normalized)
	void ons(Vector3 v1, Vector3 v2, Vector3 v3) {
	    if (Math.abs(v1.x) > Math.abs(v1.y)) {
			// project to the y = 0 plane and construct a normalized orthogonal vector in this plane
			float invLen = (float) (1.f / Math.sqrt(v1.x * v1.x + v1.z * v1.z));
			v2.set(-v1.z * invLen, 0.0f, v1.x * invLen);
	    } else {
			// project to the x = 0 plane and construct a normalized orthogonal vector in this plane
			float invLen = (float) (1.0f / Math.sqrt(v1.y * v1.y + v1.z * v1.z));
			v2.set(0.0f, v1.z * invLen, -v1.y * invLen);
	    }
	    v3.set(v1).crs(v2);
	}
	
	Vector3 CosineSampleHemisphere(float u1, float u2)
	{
	    float r = (float) Math.sqrt(u1);
	    float theta = 2 * MathUtils.PI * u2;
	 
	    float x = r * MathUtils.cos(theta);
	    float y = r * MathUtils.sin(theta);
	 
	    return new Vector3(x, y, (float) Math.sqrt(Math.max(0.0f, 1 - u1)));
	}

public Vector3d previewRaytrace(Ray ray, int depth){
		
		Vector3 modelNormal = new Vector3();

		int closestHit = -1;
		float minHitDist = 50;
		
		for (int i = 0; i < mainThread.models.size(); i++){
			Model model = mainThread.models.get(i);
			
			float hitDist = model.intersectRay(ray);
			if (hitDist > 0 & hitDist < minHitDist){
					minHitDist = hitDist;
					closestHit = i;
			}
			
		}
		Vector3d finalColor = new Vector3d(0,0,0);
		
		if (closestHit > -1) {
			Vector3 closestIntersection = new Vector3().set(ray.origin).mulAdd(ray.direction, minHitDist);
			Model hitModel = mainThread.models.get(closestHit);
			//modelNormal.set(hitModel.getNormal(closestIntersection));

			finalColor.set(hitModel.diffuse);
			finalColor.scl(hitModel.glossy);
			finalColor.add(hitModel.emissionColor);
			finalColor.add(0.05f, 0.05f, 0.05f);
			clamp01(finalColor);
			//calculate the texture's contribution if there's one
			if (hitModel.diffTexture != null){
				Vector2 uv = new Vector2(hitModel.getUV(closestIntersection));
				//Gdx.app.log("orig UV", uv.toString());
				finalColor.scl(hitModel.diffTexture.getColor(uv));
			}
			
			
		}
		else finalColor.set((float)mainThread.skyColor.x, (float)mainThread.skyColor.y, (float)mainThread.skyColor.z);
		
		return finalColor;
	}
}
