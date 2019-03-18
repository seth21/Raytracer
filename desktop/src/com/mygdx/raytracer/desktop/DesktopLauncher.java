package com.mygdx.raytracer.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.mygdx.raytracer.MyRaytracer;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.vSyncEnabled = false;
		config.foregroundFPS = 10;
		config.backgroundFPS = 10;
		config.width = 1280;
		config.height = 720;
		
		new LwjglApplication(new MyRaytracer(), config);
	}
}
