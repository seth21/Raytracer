package com.mygdx.raytracer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class CamController {

	PerspectiveCamera cam;
	MyRaytracer tracer;
	Vector3 camLastPos = new Vector3();
	Vector3 camLastDir = new Vector3();
	public CamController(PerspectiveCamera camera, MyRaytracer tracer) {
		cam = camera;
		this.tracer = tracer;
	}
	
	public void update() {
		boolean action = false;
		camLastPos.set(cam.position);
		camLastDir.set(cam.direction);
		
		if (Gdx.input.isKeyPressed(Keys.D)){
			Vector3 camRight = new Vector3(cam.direction).crs(cam.up);
			Vector3 move = new Vector3(camRight).scl(-0.3f);
			cam.translate(move);

			action = true;

		}
		if (Gdx.input.isKeyPressed(Keys.A)){
			Vector3 camRight = new Vector3(cam.direction).crs(cam.up);
			Vector3 move = new Vector3(camRight).scl(0.3f);
			cam.translate(move);

			action = true;

		}
		if (Gdx.input.isKeyPressed(Keys.W)){
			Vector3 move = new Vector3(cam.direction).scl(0.3f);
			cam.translate(move);

			action = true;

		}
		if (Gdx.input.isKeyPressed(Keys.S)){
			Vector3 move = new Vector3(cam.direction).scl(-0.3f);
			cam.translate(move);

			action = true;

		}
		
		if (Gdx.input.isKeyPressed(Keys.Q)){
			cam.translate(0f, .1f, 0f);

			action = true;

		}
		if (Gdx.input.isKeyPressed(Keys.E)){
			cam.translate(0f, -.1f, 0f);

			action = true;

		}
		if (Gdx.input.isKeyJustPressed(Keys.Z)){
			tracer.setViewMode(ViewMode.FINAL);
			//tracer.rese

		}
		if (Gdx.input.isKeyJustPressed(Keys.X)){
			tracer.setViewMode(ViewMode.ALBEDO);
			//tracer.rese

		}
		if (Gdx.input.isKeyJustPressed(Keys.C)){
			tracer.setViewMode(ViewMode.DIRECTLIGHT);
			//tracer.rese

		}
		if (Gdx.input.isKeyJustPressed(Keys.V)){
			tracer.setViewMode(ViewMode.INDIRECTLIGHT);
			//tracer.rese

		}
		if (Gdx.input.isKeyJustPressed(Keys.B)){
			tracer.setViewMode(ViewMode.GLOSSY);
			//tracer.rese

		}
		if (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)){
			Vector3 camRight = new Vector3(cam.direction).crs(cam.up);
			cam.rotate(Vector3.Y, .3f*Gdx.input.getDeltaX());
			cam.rotate(camRight, -.3f*Gdx.input.getDeltaY());

			action = true;
		}
		if (Gdx.input.isKeyJustPressed(Keys.SPACE)){
			tracer.forceFinishThreads();
			tracer.postDone = false;
		}
		
		//camLastPos.set(cam.position);
		tracer.setPreviewMode(action);
		
		if (!cam.position.epsilonEquals(camLastPos) || !cam.direction.epsilonEquals(camLastDir)){
			//tracer.dirtyRenderThreads();
			tracer.setPreviewMode(true);
			tracer.postDone = false;
		}
		
	}
	
	
}
