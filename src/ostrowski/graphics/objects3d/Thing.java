package ostrowski.graphics.objects3d;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.opengl.GL11;

import ostrowski.graphics.GLView;
import ostrowski.graphics.model.Face;
import ostrowski.graphics.model.Message;
import ostrowski.graphics.model.ObjLoader;
import ostrowski.graphics.model.ObjModel;
import ostrowski.graphics.model.TexturedObject;
import ostrowski.graphics.model.Tuple3;

public class Thing extends TexturedObject
{
   public Tuple3   locationOffset = new Tuple3(0, 0, 0);
   public Tuple3   facingOffset   = new Tuple3(0, 0, 0);
   private Message message;
   public Weapon     weapon     = null;
   public WeaponPart weaponPart = null;

   public enum Dice {
      d4, d6, d8, d10, d12, d12_mod, d20
   }
   public enum Armor {
      NoArmor,
      Cloth,
      HeavyCloth,
      Leather,
      HeavyLeather,
      ChainMail,
      HeavyChain,
      ElvenChain,
      ScaleMail,
      BandedMail,
      Samurai,
      LightPlate,
      PlateMail,
      HeavyPlate,
      Mithril
   }

   public enum Shield {
      Buckler,
      SmallShield,
      MediumShield,
      LargeShield,
      TowerShield
   }
   public enum Weapon {
      Arrow,
      Axe,
      ThrowingAxe,
      BastardSword,
      //BastardSword_Fine,
      Club,
      Dagger,
      Flail,
      GreatAxe,
      Halberd,
      Javelin,
      Knife,
      Katana,
      //Katana_Fine,
      Longsword,
      //Longsword_Fine,
      Broadsword,
      Mace,
      Nunchucks,
      Maul,
      MorningStar,
      PickAxe,
      Quarterstaff,
      Rapier,
      Sabre,
      Shortsword,
      Spear,
      //ThrowingStar,
      //ThreePartStaff,
      TwoHandedSword,
      //TwoHandedSword_Fine,
      WarHammer,
      //BlowGun,
      //BowComposite,
      Longbow_ready,
      Bow_ready,
      Shortbow_ready,
      Longbow_idle,
      Bow_idle,
      Shortbow_idle,
      //Crossbow,
      //CrossbowHeavy,
      //CrossbowLight,
      //Sling,
      //StaffSling,
   }

   public Thing(Shield shield, GLView glView, boolean invertNormals, float lengthFactor, float widthFactor) throws IOException {
      super(null/*texture*/, null/*selectedTexture*/, invertNormals);
      loadObject(glView, shield.toString(),
                 "res/shields/" + shield.toString().toLowerCase() + ".obj",
                 "res/shields/texture_shields.png",
                 lengthFactor, widthFactor);
   }
   public Thing(Weapon weapon, WeaponPart weaponPart, GLView glView, boolean invertNormals, float lengthFactor, float widthFactor) throws IOException {
      super(null/*texture*/, null/*selectedTexture*/, invertNormals);
      this.weapon = weapon;
      this.weaponPart = weaponPart;
      loadObject(glView, weapon.toString(),
                 "res/weapons/" + weapon.toString().toLowerCase() + ".obj",
                 "res/weapons/texture_weapons.png",
                 lengthFactor, widthFactor);
   }
   public Thing(Dice dice, GLView glView, float sizeFactor, String colorSet) throws IOException {
      super(null/*texture*/, null/*selectedTexture*/, true/*invertNormals*/);
      loadObject(glView, dice.toString(),
                 "res/dice/" + dice + ".obj",
                 "res/dice/diceMap" + colorSet + ".png",
                 sizeFactor * 50, sizeFactor * 50);
   }

   public Thing(Armor armor, String bodyPartName, GLView glView, boolean invertNormals, float lengthFactor, float widthFactor) throws IOException {
      super(null/*texture*/, null/*selectedTexture*/, invertNormals);
      loadObject(glView, armor.toString(),
                 "res/armor/" + armor.toString().toLowerCase() +"/" + bodyPartName.toLowerCase() + ".obj",
                 "res/armor/" + armor.toString().toLowerCase() +"/" + "texture_armor.png",
                 lengthFactor, widthFactor);
   }

   private void loadObject(GLView glView, String name, String modelResourceName, String textureResourceName, float lengthFactor, float widthFactor) throws IOException {
      glView.useAsCurrentCanvas();
      texture = selectedTexture = glView.getTextureLoader().getTexture(textureResourceName);

      message = new Message();
      message.text = name;
      message.visible = false;

      if ((modelResourceName != null) && (modelResourceName.length() > 0)) {
         if ((weapon == Weapon.MorningStar) || (weapon == Weapon.Flail) || (weapon == Weapon.Nunchucks)) {
            String name0 = modelResourceName.replaceFirst(".obj", "_0.obj");
            ObjModel obj = ObjLoader.loadObj(name0, glView, lengthFactor, widthFactor);
            if (obj != null) {
               addObject(obj);
               String name1 = modelResourceName.replaceFirst(".obj", "_1.obj");
               obj = ObjLoader.loadObj(name1, glView, lengthFactor, widthFactor);
               if (obj != null) {
                  addObject(obj);
               }
            }
         }
         else {
            ObjModel obj = ObjLoader.loadObj(modelResourceName, glView, lengthFactor, widthFactor);
            if (obj != null) {
               addObject(obj);
            }
         }
      }
   }

   @Override
   public void render(GLView glView, List<Message> messages) {
      // must bind the texture BEFORE we call glBegin(...)
      bindToTexture();
      GL11.glPushMatrix();
      {
         //GL11.glColor3f(1.0f, 1.0f, 1.0f);

         GL11.glTranslatef(locationOffset.getX(), locationOffset.getY(), locationOffset.getZ());
         GL11.glRotatef(facingOffset.getX(), 1f, 0f, 0f);
         GL11.glRotatef(facingOffset.getY(), 0f, 1f, 0f);
         GL11.glRotatef(facingOffset.getZ(), 0f, 0f, 1f);

         for (ObjModel model : models) {
            model.render(glView, messages);
            if ((weaponPart != null) && (weaponPart.frontRot != null) && (models.size() > 1)) {
               Float minX = null;
               for (int faceIndex = 0; faceIndex<model.data.getFaceCount() ; faceIndex++) {
                  Face face = model.data.getFace(faceIndex);
                  for (int vertexIndex = 0; vertexIndex<face.vertexCount; vertexIndex++) {
                     Tuple3 point = face.getVertex(vertexIndex);
                     if ((minX == null) || (minX > point.getX())) {
                        minX = point.getX();
                     }
                  }
               }
               GL11.glTranslatef(minX, 0f, 0f);
               GL11.glRotatef(weaponPart.frontRot, 0f, 1f, 0f);
            }
         }
         // set the location of any messages based on the current head position in 2D
         if ((message != null) && (message.text != null) && (message.text.length() > 0) && message.visible) {
            if (glView.font != null) {
               FloatBuffer headLoc = BodyPart.projectToWindowLocation();
               int x = (int) headLoc.get(0);
               int y = (int) headLoc.get(1);
               int z = (int) headLoc.get(2);

               // Window reference frame uses y=0 at the bottom.
               // When we draw text, y=0 is at the top, so subtract y from the window height.
               message.xLoc = x;
               message.yLoc = y;
               message.zLoc = z;

               messages.add(message);
            }
         }
         GL11.glPopMatrix();
      }
   }
}
