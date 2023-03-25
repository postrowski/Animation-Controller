package ostrowski.graphics.objects3d;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.objects3d.Torso.Hand;
import ostrowski.graphics.texture.Texture;
import ostrowski.util.Semaphore;
import ostrowski.util.SemaphoreAutoLocker;

public abstract class BallSocketJoint extends BodyPart
{
   RangedValue frontRot;
   RangedValue sideRot;
   RangedValue twistCW;

   float   length;        //in inches
   boolean leftSideOfBody;
   private Thing armor;

   @Override
   protected abstract Semaphore getChildLock();
   public BallSocketJoint(String name, String modelResourceName, boolean leftSideOfBody,
                          Texture texture, Texture selectedTexture, GLView glView,
                          float lengthFactor, float widthFactor, String raceName, boolean isMale) {
      super(name, modelResourceName, leftSideOfBody, texture, selectedTexture, glView, lengthFactor, widthFactor, raceName, isMale);
      this.leftSideOfBody = leftSideOfBody;
      frontRot = getValueByNameAsRangedValue("frontRot");
      sideRot = getValueByNameAsRangedValue("sideRot");
      twistCW = getValueByNameAsRangedValue("twistCW");
      length = getValueByNameAsFloat("_length");

      length *= lengthFactor;
   }

   public FloatBuffer getEndpointLocationInWindowReferenceFrame(int childCount) {
      rotateJoint();
      translateLength();
      if (childCount == 0) {
         return projectToWindowLocation();
      }

      synchronized(children) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
            for (TexturedObject child : children) {
               if (child instanceof BallSocketJoint) {
                  return ((BallSocketJoint)child).getEndpointLocationInWindowReferenceFrame(childCount - 1);
               }
            }
         }
      }
      return null;
   }

   @Override
   public void validateRanges() {
      frontRot.validateRange();
      sideRot.validateRange();
      twistCW.validateRange();
   }

   @Override
   public void render(GLView glView, List<Message> messages) {
      GL11.glPushMatrix();
      {
         bindToTexture();
         //GL11.glColor3f(1.0f, 1.0f, 1.0f);
         rotateJoint();
//         GL11.glPushMatrix();
//         {
            for (ObjModel model : models) {
               model.render(glView, messages);
            }
            if (armor != null)
             {
               armor.render(glView, messages);
//         }
//         GL11.glPopMatrix();
            }

         translateLength();
         if (children.size() > 0) {
            //if ((this instanceof Hand) && !this._leftSideOfBody) {
            if (this instanceof Hand) {
               List<Thing> weapons = new ArrayList<>();
               List<TexturedObject> objects = new ArrayList<>();
               WeaponPart weaponPart = null;
               synchronized(children) {
                  try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
                     for (TexturedObject child : children) {
                        if (child instanceof WeaponPart) {
                           weaponPart = (WeaponPart) child;
                        }
                        else if ((child instanceof Thing) && (((Thing)child).weapon != null)) {
                           weapons.add((Thing)child);
                        }
                        else {
                           objects.add(child);
                        }
                     }
                  }
               }
               for (TexturedObject child : objects) {
                  child.render(glView, messages);
               }
               float weaponHoldOffsetY = 1.25f * widthFactor;
               float weaponHoldOffsetZ = -1.5f * widthFactor;
               GL11.glTranslatef(0f, weaponHoldOffsetY, weaponHoldOffsetZ);
               if (weaponPart != null) {
                  if (weaponPart.lengthOffset != null) {
                     GL11.glTranslatef(weaponPart.lengthOffset, 0f, 0f);
                  }
                  if (weaponPart.twistRot != null) {
                     GL11.glRotatef(weaponPart.twistRot, 1f, 0f, 0f);
                  }
               }
               for (TexturedObject child : weapons) {
                  child.render(glView, messages);
               }
            }
            else {
               synchronized(children) {
                  try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
                     for (TexturedObject child : children) {
                        child.render(glView, messages);
                     }
                  }
               }
            }
         }
      }
      GL11.glPopMatrix();
   }

   public Tuple3 getEndPoint(int childDepth) {
      Tuple3 endpoint = new Tuple3(0f, 0f, length);
      if (childDepth > 0) {
         synchronized(children) {
            try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
               for (TexturedObject child : children) {
                  if (child instanceof BallSocketJoint) {
                     endpoint = ((BallSocketJoint)child).getEndPoint(childDepth-1).add(endpoint);
                     break;
                  }
               }
            }
         }
      }
      endpoint = endpoint.rotate(frontRot.getValue() - 90f, -sideRot.getValue(), -twistCW.getValue());
      return endpoint;
   }

   private void rotateJoint() {
      //GL11.glRotatef(90f, -1.0f, 0.0f, 0.0f);
      //GL11.glRotatef(_frontRot.getValue(), 1.0f, 0.0f, 0.0f);
      GL11.glRotatef(frontRot.getValue() - 90f, 1.0f, 0.0f, 0.0f);
      //if (GLScene.USING_SCALING_FOR_LEFT_SIDE) {
         GL11.glRotatef(-sideRot.getValue(), 0.0f, 1.0f, 0.0f);
         GL11.glRotatef(-twistCW.getValue(), 0.0f, 0.0f, 1.0f);
      //}
      //else {
      //   GL11.glRotatef((_leftSideOfBody ? sideRot.getValue() : -sideRot.getValue()), 0.0f, 1.0f, 0.0f);
      //   GL11.glRotatef((_leftSideOfBody ? twistCW.getValue() : -twistCW.getValue()), 0.0f, 0.0f, 1.0f);
      //}
   }
   private void translateLength() {
      GL11.glTranslatef(0.0f, 0.0f, length);
   }

   public void removeHeldThings() {
      List<TexturedObject> itemsToRemove = new ArrayList<>();
      synchronized(children) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
            for (TexturedObject child : children) {
               if (child instanceof BodyPart) {
                  continue;
               }
               itemsToRemove.add(child);
            }
            children.removeAll(itemsToRemove);
         }
      }
   }

   public void setArmor(Armor armor) {
      try {
         this.armor = new Thing(armor, this.getClass().getSimpleName(), glView, invertNormals, lengthFactor, widthFactor);
      } catch (IOException e) {
         // armor of this type for this body party doesn't exist, use nothing
         this.armor = null;
      }
      synchronized(children) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
            for (TexturedObject child : children) {
               if (child instanceof BallSocketJoint) {
                  ((BallSocketJoint) child).setArmor(armor);
               }
            }
         }
      }
   }

   @Override
   public void setAnglesFromMap(HashMap<String, Float> angleMap) {
      if (angleMap.containsKey("f")) {
         frontRot.setValue(angleMap.get("f"));
      }
      if (angleMap.containsKey("s")) {
         sideRot.setValue(angleMap.get("s"));
      }
      if (angleMap.containsKey("t")) {
         twistCW.setValue(angleMap.get("t"));
      }
      validateRanges();
   }

   @Override
   public HashMap<String, Float> getAnglesMap() {
      HashMap<String, Float> anglesMap = new HashMap<>();
      anglesMap.put("f", frontRot.getValue());
      anglesMap.put("s", sideRot.getValue());
      anglesMap.put("t", twistCW.getValue());
      return anglesMap;
   }

   @Override
   public void getParts(List<TexturedObject> parts) {
      parts.add(this);
      synchronized(children) {
         try (SemaphoreAutoLocker sal = new SemaphoreAutoLocker(lock_children)) {
            for (TexturedObject child : children) {
               if (child instanceof BodyPart) {
                  ((BodyPart) child).getParts(parts);
               }
            }
         }
      }
   }
}
