package com.mygdx.raytracer;

import com.badlogic.gdx.graphics.Color;

public class AssembleThread implements Runnable{

	MyRaytracer mainThread;
	public AssembleThread(MyRaytracer tracer) {
		mainThread = tracer;
	}
	@Override
	public void run() {
		//while (!mainThread.shutDown){
        	
			assembleRender();
        	//} else {
        		/*try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
        	//}
        //}
		
	}
	
	private void assembleRender() {
		TracerThread[][] threads = mainThread.threads;
		ViewMode viewMode = mainThread.viewMode;
		//take the rendered parts from all threads
		for (int tX = 0; tX < mainThread.threadsHor; tX++){
			for (int tY = 0; tY < mainThread.threadsVert; tY++){
				int xStart = threads[tX][tY].xStart;
				int yStart = threads[tX][tY].yStart;
				
				for (int x = 0; x < threads[tX][tY].tW; x++){
					for (int y = 0; y < threads[tX][tY].tH; y++){
						TracerThread thr = threads[tX][tY];
						
						Vector3d processedColor = null;
						if (viewMode == ViewMode.FINAL)
							processedColor = new Vector3d(thr.pixDirectLight[x][y]).add(thr.pixDiffLight[x][y])
							.scl(thr.pixDiff[x][y]).add(thr.pixGlossy[x][y]);
						else if (viewMode == ViewMode.ALBEDO)
							processedColor = new Vector3d(thr.pixDiff[x][y]);
						else if (viewMode == ViewMode.DIRECTLIGHT)
							processedColor = new Vector3d(thr.pixDirectLight[x][y]);
						else if (viewMode == ViewMode.INDIRECTLIGHT)
							processedColor = new Vector3d(thr.pixDiffLight[x][y]);
						else processedColor = new Vector3d(thr.pixGlossy[x][y]);
						
						//Vector3d processedColor = new Vector3d(previousLight).scl(previousCol).add(previousGlossy);
						//Vector3d processedColor = new Vector3d(previousLight);
						if (true) processedColor.set(Math.pow(processedColor.x, 1f/2.2f), Math.pow(processedColor.y, 1f/2.2f), Math.pow(processedColor.z, 1f/2.2f));
						//mainThread.clamp01(processedColor);
						
						mainThread.image.drawPixel(x+xStart, y+yStart, Color.rgba8888((float)processedColor.x, (float)processedColor.y, (float)processedColor.z, 1));
						
						
					}
					
				}
			}
		}
		
	}

}
