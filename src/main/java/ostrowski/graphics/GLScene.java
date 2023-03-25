/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package ostrowski.graphics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class GLScene implements Runnable
{

   public static boolean USING_SCALING_FOR_LEFT_SIDE = true;
   static boolean              initialized = false;

   public FrameController     frameController;
   public AnimationController animationController;

   static Display display;

   private static Shell shell;

   public GLScene(Composite parent) {
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.VERTICAL;
      parent.setLayout(fillLayout);

      
      SashForm mainSash = new SashForm(parent, SWT.VERTICAL);
      GridData data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      mainSash.setLayoutData(data);
      mainSash.SASH_WIDTH = 4;
      mainSash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
      
      SashForm topSash = new SashForm(mainSash, SWT.HORIZONTAL);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      topSash.setLayoutData(data);
      topSash.SASH_WIDTH = 4;
      topSash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));

      frameController = new FrameController(topSash, this);

      SashForm bottomSash = new SashForm(mainSash, SWT.HORIZONTAL);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      bottomSash.setLayoutData(data);
      bottomSash.SASH_WIDTH = 4;
      bottomSash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));

      animationController = new AnimationController(bottomSash, this);

      Composite topComp = new Composite(topSash, SWT.NONE);
      topComp.setLayout(new FillLayout());

      frameController.createUI(topComp);
      animationController.createUI(bottomSash);

      frameController.setCameraPosition(0f, frameController.getCameraPosition().getY(), -120f);
      animationController.setCameraPosition(0f, frameController.getCameraPosition().getY(), -250f);

      frameController.incrementCameraAngle(0f, -15f);
      animationController.incrementCameraAngle(0f, -10f);
   }

   public static void main(String[] args) {
      display = new Display();
      shell = new Shell(display);
      GLScene scene = new GLScene(shell);

      shell.setText("SWT/LWJGL Human modeler");
      shell.setSize(600, 1000);
      shell.setLocation(1200, 100);
      shell.open();

      display.asyncExec(scene);

      while (!shell.isDisposed()) {
         if (!display.readAndDispatch()) display.sleep();
      }
      display.dispose();
   }

   static public boolean pauseanimation = false;

   @Override
   public void run() {
      if (!pauseanimation) {
         // Delay so we don't use up 100% cpu:
         try {
            Thread.sleep(20);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }

         frameController.drawScene(display);
         animationController.drawScene(display);
      }
      if (!shell.isDisposed()) display.asyncExec(this);
   }

   public void setStaticModelData(AnimationFrame frame) {
      frameController.setModelData(frame);
   }

   public void setCurrentFrameModelData(AnimationFrame frame) {
      animationController.setCurrentFrameModelData(frame);
   }

}