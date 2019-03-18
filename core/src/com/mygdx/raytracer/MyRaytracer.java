package com.mygdx.raytracer;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.environment.PointLight;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.TimeUtils;


public class MyRaytracer extends ApplicationAdapter {
	SpriteBatch batch;
	//Texture img;
	FPSLogger fpslogger;
	Pixmap image;
	Texture tex;
	PerspectiveCamera cam;
	CamController camController;
	ViewMode viewMode = ViewMode.FINAL;
	
	List<Model> models = new ArrayList<Model>();
	List<Model> emitters = new ArrayList<Model>();
	//boolean lightMode = false;
	
	//Global Illumination settings
	int totalSamples=300;
	int currSample = 0;
	int minBounces = 2;
	int maxBounces = 4;
	private boolean previewMode = false;
	
	//Sky settings
	Vector3d skyColor = new Vector3d(0.4f, 0.8f, 1f);
	Vector3d horizonColor = new Vector3d(0.8f, 0.7f, 0.5f);
	float horizonPower = 0.5f;
	float skyIndirectStrength = 8f;
	
	float skyIndirectDiffuseGamma = 2f;
	float skyDirectStrength = 1f;
	float skyIndirectGlossyStrength = 1f;
	float skyIndirectDiffuseStrength = 8f;
	TextureR skyMap;
	
	Vector3d[][] pixFinal;
	Vector3d[][] pixAlbedo;
	Vector3d[][] pixDiffDirectLight;
	Vector3d[][] pixDiffIndirectLight;
	Vector3d[][] pixGlossy;
	//float[][] depthMap;
	
	//Post processing settings
	Model DOFtarget;
	
	float clampDiffuseIndirect = 6f;
	float clampGlossyRefractIndirect = 5f;
	float clampDiffuseDirect = 9f;
	
	float clampGlossyDirect = 5.5f;
	float clampDirect = 5.5f;
	float clampIndirect = 9f;
	
	int maxViewDist = 200;
	float viewPortWidth = 12;
	float duration;
	
	
	TracerThread[][] threads;
	PostThread postThread;
	int threadsHor = 4, threadsVert = 4;
	boolean postDone = false;
	
	@Override
	public void create () {
		batch = new SpriteBatch();

		fpslogger = new FPSLogger();
		
		image = new Pixmap(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Pixmap.Format.RGB888);
		
		tex = new Texture(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), Format.RGB888);
		
		pixDiffDirectLight = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		pixDiffIndirectLight = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		pixGlossy = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		pixAlbedo = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		pixFinal = new Vector3d[Gdx.graphics.getWidth()][Gdx.graphics.getHeight()];
		for (int x =0; x<Gdx.graphics.getWidth();x++){
			for (int y =0; y<Gdx.graphics.getHeight();y++){
					pixGlossy[x][y] = new Vector3d();
					pixDiffDirectLight[x][y] = new Vector3d();
					pixDiffIndirectLight[x][y] = new Vector3d();
					pixAlbedo[x][y] = new Vector3d();
					pixFinal[x][y] = new Vector3d();
			}
		}

		float aspectRatio = (float)Gdx.graphics.getWidth()/(float)Gdx.graphics.getHeight();
		cam = new PerspectiveCamera(45, viewPortWidth, viewPortWidth / aspectRatio);
		cam.position.set(0, 3, -18);
		cam.direction.set(0,0,1);
		camController = new CamController(cam, this);
		
		setupScene();
		skyMap = new TextureR("sky2.jpg", true);
		
		//Init tracer threads
		threads = new TracerThread[threadsHor][threadsVert];
		for (int tX = 0; tX < threadsHor; tX++){
			for (int tY = 0; tY < threadsVert; tY++){
				threads[tX][tY] = new TracerThread(this, tX, tY);
				threads[tX][tY].start();
			}
		}
		
		
	}

	public void setupScene(){
		TextureR woodTex = new TextureR("wood.jpg"); 
		TextureR stoneTex = new TextureR("stone.jpg"); 
		TextureR concreteTex = new TextureR("concrete.jpg"); 
		
		models.add(new Sphere(new Vector3(-4f,1.5f,-6f), 1.5f).setDiffuse(0.9f,0.2f,0.2f));
		models.add(new Sphere(new Vector3(4f,1.5f,-6f), 1.5f).setDiffuse(0.1f,0.4f,0.8f).setGlossy(0.9f,0.9f,0.9f).setGlossyFactor(.15f));
		models.add(DOFtarget = new Sphere(new Vector3(0f,1.5f,-6f), 1.5f).setDiffuse(0.9f,0.95f,0.85f).setGlossy(0.9f,0.9f,0.9f).setGlossyFactor(.15f).setGlass(true));
		models.add(new Sphere(new Vector3(-2f, 1.5f, 0f), 1.5f).setDiffuse(0.1f,0.5f,0.7f).setGlossy(0.7f,0.2f,0.9f).setGlossyFactor(1f));
		models.add(new Sphere(new Vector3(2f, 1.5f, 0f), 1.5f).setGlossy(0.2f,0.9f,0.6f).setGlossyFactor(1f).setGlossyRoughness(0.5f));
		
		//models.add(new Cube(0, 0, -10, 2, 2, 2).setDiffuse(0.3f,0.4f,0.7f).setGlossyFactor(.8f));
		//PLANES
		//bottom
		//models.add(new Plane(new Vector3(0,1,0), 0).setDiffuse(0.8f,0.8f,0.8f));
		models.add(new Quad(new Vector3(-30, 0, 30), new Vector3(-30, 0, -30), new Vector3(30, 0, 30)).setDiffuseTexture(woodTex).setTiling(10f, 10f));
		//models.add(new Quad(new Vector3(-6, 0, 6), new Vector3(-6, 0, -10), new Vector3(6, 0, 6)).setDiffuse(0.7f,0.7f,0.7f));
		//top
		//models.add(new Plane(new Vector3(0,-1,0), 10).setDiffuse(0.8f,0.8f,0.8f));
		models.add(new Quad(new Vector3(-6, 10, -8), new Vector3(-6, 10, 6), new Vector3(6, 10, -8)).setDiffuse(0.7f,0.8f,1f).setDiffuseTexture(concreteTex).setTiling(5f, 5f));
		//left
		//models.add(new Plane(new Vector3(1,0,0), 6).setDiffuse(0.0f,0.8f,0.0f));
		models.add(new Quad(new Vector3(-6, 10, -8), new Vector3(-6, 0, -8), new Vector3(-6, 10, 6)).setDiffuseTexture(stoneTex).setTiling(2f, 2f));
		//right
		//models.add(new Plane(new Vector3(-1,0,0), 6).setDiffuse(0.0f,0.0f,0.8f));
		models.add(new Quad(new Vector3(6, 10, 6), new Vector3(6, 0, 6), new Vector3(6, 10, -8)).setDiffuseTexture(stoneTex).setTiling(2f, 2f));
		//front
		//models.add(new Plane(new Vector3(0,0,-1), 6).setDiffuse(0.6f,0.6f,0.1f));
		models.add(new Quad(new Vector3(-6, 10, 6), new Vector3(-6, 0, 6), new Vector3(6, 10, 6)).setDiffuse(0.7f,1f,0.4f).setDiffuseTexture(concreteTex).setTiling(5f, 5f));
		//back
		//models.add(new Plane(new Vector3(0,0,1), 16).setDiffuse(0.6f,0.6f,0.6f));
		//mirror
		models.add(new Quad(new Vector3(-3, 7, 5.9f), new Vector3(-3, 1, 5.9f), new Vector3(3, 7, 5.9f)).setGlossy(0.9f,0.9f,0.9f).setGlossyFactor(1f).setGlossyRoughness(0.02f));
		
		//glass
		//models.add(new Quad(new Vector3(-3, 7, -9f), new Vector3(-3, 0, -9f), new Vector3(3, 7, -9f)).setGlossy(0.9f,0.9f,0.9f).setGlossyFactor(1f).setGlass(true));
		//models.add(new Plane(new Vector3(0,-1,0), 10).setDiffuse(0.8f,0.8f,0.8f).setEmission(0.4f,0.4f,0.4f, 15f));
		//Lights
		models.add(new Sphere(new Vector3(60f, 30f,-100f), 18f).setDiffuse(0.2f,0.2f,0.2f).setEmission(1f,1f,0.4f, 1f, 100f).setCameraInvisible(false));
		models.add(new Quad(new Vector3(-2,9.95f,-4),new Vector3(-2,9.95f,0),new Vector3(2,9.95f,-4)).setEmission(1f,1f,1f, 15f, 8f).setCameraInvisible(false));
		//models.add(new Sphere(new Vector3(-6f,6f,-2f), 2f).setDiffuse(0.2f,0.2f,0.2f).setEmission(22f,22f,22f, 15f));
		//models.add(new Sphere(new Vector3(4f,6f,2f), 1.5f).setDiffuse(0.2f,0.2f,0.2f).setEmission(22f,22f,22f, 15f));
		
		//pointLights.add(new PointLight().setPosition(-5, 5, -1).setColor(0.8f, 0.8f, 0.8f, 1f).setIntensity(20f));
		//pointLights.add(new PointLight().setPosition(5, 5, -5).setColor(Color.BLUE).setIntensity(15f));
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		camController.update();
		cam.update();
		
		//sample has been reset due to camera movement {
		if (previewMode){
			duration = 0;
		}
		
		if (allThreadsFinished() && !postDone){
			//begin post processing
			//postProcessing();
			Gdx.app.log("weg","reherh");
			postDone = true;
			postThread = new PostThread(this);
			postThread.start();
		}
		
		tex.draw(image, 0, 0);
		batch.begin();
		batch.draw(tex, 0, 0);
		batch.end();
		
		updateTitle();

	}
	
	public void setViewMode(ViewMode mode) {
		viewMode = mode;
		for (int x =0; x<Gdx.graphics.getWidth();x++){
			for (int y =0; y<Gdx.graphics.getHeight();y++){
					Vector3d processedColor = null;
					
					//clamp01(processedColor);
					if (viewMode == ViewMode.FINAL)
						processedColor = new Vector3d(pixFinal[x][y]);
					else if (viewMode == ViewMode.ALBEDO)
						processedColor = new Vector3d(pixAlbedo[x][y]);
					else if (viewMode == ViewMode.GLOSSY)
						processedColor = new Vector3d(pixGlossy[x][y]);
					else if (viewMode == ViewMode.DIRECTLIGHT)
						processedColor = new Vector3d(pixDiffDirectLight[x][y]);
					else if (viewMode == ViewMode.INDIRECTLIGHT)
						processedColor = new Vector3d(pixDiffIndirectLight[x][y]);
					
					processedColor.set(Math.pow(processedColor.x, 1f/2.2f), Math.pow(processedColor.y, 1f/2.2f), Math.pow(processedColor.z, 1f/2.2f));
					clamp01(processedColor);
					image.drawPixel(x, y, Color.rgba8888((float)processedColor.x, (float)processedColor.y, (float)processedColor.z, 1));
					
			}
		}
	}
	
	
	private void updateTitle() {
		if (!allThreadsFinished()) duration += Gdx.graphics.getRawDeltaTime();
		//int elapsedTime = (int) ((TimeUtils.nanoTime() - startTime) / 1000000000);
		int hrs = (int)duration / 3600;
		int mins = ((int)duration % 3600)/60;
		int secs = (int)duration % 60;
		String timestring = (hrs > 0 ? hrs+":" : "") + (mins>9 ? mins+":" : "0"+mins+":") + (secs>9 ? secs : "0"+secs);
		String titleStr = "Pathtracer - " +"Time: "+timestring;
		float progress = 0;
		for (int tX = 0; tX < threadsHor; tX++){
			for (int tY = 0; tY < threadsVert; tY++){
				progress += threads[tX][tY].currSample;
			}
		}
		progress /= threadsHor * threadsVert;
		titleStr += " Samples: "+(int)(progress)+"/"+totalSamples+" ";
		progress /= totalSamples;
		progress = (int)(progress * 1000f)/10f;
		titleStr += "Progress: "+progress+"%"; 
		Gdx.graphics.setTitle(titleStr);
		fpslogger.log();
	}
	
	public void setPreviewMode(boolean b) {
		//if (previewMode != b) {
			previewMode = b;
			if (previewMode) {
				for (int tX = 0; tX < threadsHor; tX++){
					for (int tY = 0; tY < threadsVert; tY++){
						threads[tX][tY].resetRender();
					}
				}
			}
			
		//}
	}
	
	public boolean isPreviewEnabled() {
		return previewMode;
	}
	
	private boolean allThreadsFinished() {
		boolean b = true;
		for (int tX = 0; tX < threadsHor; tX++){
			for (int tY = 0; tY < threadsVert; tY++){
				if (threads[tX][tY].currSample < totalSamples){
					b = false;
					return b;
				}
			}
		}
		return b;
	}
	
	private void setThreadsFinished(boolean b) {
		for (int tX = 0; tX < threadsHor; tX++){
			for (int tY = 0; tY < threadsVert; tY++){
				threads[tX][tY].threadFinished = b;
			}
		}
	}
	
	
	void forceFinishThreads() {
		for (int tX = 0; tX < threadsHor; tX++){
			for (int tY = 0; tY < threadsVert; tY++){
				threads[tX][tY].currSample = totalSamples-1;
			}
		}
	}
	public Vector3d clamp01(Vector3d vec){
		vec.x = MathUtils.clamp(vec.x, 0, 1);
		vec.y = MathUtils.clamp(vec.y, 0, 1);
		vec.z = MathUtils.clamp(vec.z, 0, 1);
		return vec;
	}
	
	boolean shutDown = false;
	@Override
	public void dispose () {
		shutDown = true;
		batch.dispose();
		//ppimg.dispose();
		image.dispose();
		tex.dispose();
	}
}
