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

   public FrameController     _frameController;
   public AnimationController _animationController;

   static Display              _display;

   private static Shell        SHELL;

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

      _frameController = new FrameController(topSash, this);

      SashForm bottomSash = new SashForm(mainSash, SWT.HORIZONTAL);
      data = new GridData(SWT.FILL, SWT.FILL, true/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      bottomSash.setLayoutData(data);
      bottomSash.SASH_WIDTH = 4;
      bottomSash.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));

      _animationController = new AnimationController(bottomSash, this);

      Composite topComp = new Composite(topSash, SWT.NONE);
      topComp.setLayout(new FillLayout());

      _frameController.createUI(topComp);
      _animationController.createUI(bottomSash);

      _frameController.setCameraPosition(0f, _frameController.getCameraPosition().getY(), -120f);
      _animationController.setCameraPosition(0f, _frameController.getCameraPosition().getY(), -250f);

      _frameController.incrementCameraAngle(0f, -15f);
      _animationController.incrementCameraAngle(0f, -10f);
   }

   public static void main(String[] args) {
      _display = new Display();
      SHELL = new Shell(_display);
      GLScene scene = new GLScene(SHELL);

      SHELL.setText("SWT/LWJGL Human modeler");
      SHELL.setSize(600, 1000);
      SHELL.setLocation(1200, 100);
      SHELL.open();

      _display.asyncExec(scene);

      while (!SHELL.isDisposed()) {
         if (!_display.readAndDispatch()) _display.sleep();
      }
      _display.dispose();
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

         _frameController.drawScene(_display);
         _animationController.drawScene(_display);
      }
      if (!SHELL.isDisposed()) _display.asyncExec(this);
   }

   public void setStaticModelData(AnimationFrame frame) {
      _frameController.setModelData(frame);
   }

   public void setCurrentFrameModelData(AnimationFrame frame) {
      _animationController.setCurrentFrameModelData(frame);
   }

}