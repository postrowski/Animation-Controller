package ostrowski.graphics.model;

import org.eclipse.swt.widgets.Event;

import ostrowski.graphics.GLView;

public interface ISelectionWatcher
{
   boolean ObjectSelected(TexturedObject object, GLView view,  boolean selectionOn);
   void onMouseUp(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter);
   void onMouseDown(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter);
   void onMouseMove(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter);
   void onMouseDrag(TexturedObject object, Event event, double angleFromCenter, double normalizedDistFromCenter);
}
