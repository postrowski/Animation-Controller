package ostrowski.graphics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.lwjgl.opengl.GL11;

import ostrowski.graphics.objects3d.Helper;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.graphics.objects3d.Thing;
import ostrowski.graphics.objects3d.Thing.Armor;
import ostrowski.graphics.objects3d.Thing.Shield;
import ostrowski.graphics.objects3d.Thing.Weapon;
import ostrowski.graphics.texture.Texture;

public class AnimationController implements SelectionListener, ModifyListener
{
   private final GLScene   scene;
   private final GLView    view;
   private       HumanBody humanModel;
   //private Message       message;
   private       Table     table;
   //private Button    openButton;
   private       Button    saveButton;
   private       Button    newSeqButton;
   private       Button    deleteSeqButton;
   private       Combo     sequence;
   private       Combo     race;
   private       Button    deleteFrameButton;
   private       Button    copyFrameButton;
   private       Button    insertMedianFrameButton;
   private       Combo     leftHeldThingComboBox;
   private       Combo     rightHeldThingComboBox;
   private       Combo     armorComboBox;
   private       Text      positiveAssociations;
   private       Text      repeatCount;

   public AnimationController(Composite parent, GLScene scene) {
      view = new GLView(parent, true/*withControls*/);
      view.addDefaultMap();
      this.scene = scene;
   }
   public void loadRace(String race) {
      Texture texture = null;
      try {
         texture = view.getTextureLoader().getTexture("res/bodyparts/texture_" + race + "male.png");
      } catch (IOException e) {
      }
      if (texture == null) {
         try {
            texture = view.getTextureLoader().getTexture("res/bodyparts/texture_humanmale.png");
         } catch (IOException e) {
         }
      }
      if (humanModel != null) {
         view.removeObject(humanModel);
      }
      humanModel = new HumanBody(texture, view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, race/*raceName*/, true/*isMale*/);
//      Message message = new Message();
//      message.text = "Head";
//      message.colorRGB = new RGB(255, 0, 0);
//      message.visible = true;
//      humanModel.messages.add(message );
      view.addModel(humanModel);

      scene.frameController.loadRace(race, texture);

      boolean first = true;
      sequence.removeAll();
      List<AnimationSequence> sequences = SequenceLibrary.sequences;
      for (AnimationSequence seq : sequences) {
         sequence.add(seq.name);
         if (first) {
            sequence.setText(seq.name);
         }
         first = false;
      }

   }

   public void createUI(Composite parent) {
      Helper helper = new Helper();

      Composite block = Helper.createComposite(parent, 1, GridData.FILL_HORIZONTAL);
      block.setLayout(new GridLayout(1 /*columns*/, false/*sameWidth*/));
      GridData data = new GridData(SWT.FILL, SWT.FILL, false/*grabExcessHorizontalSpace*/, true/*grabExcessVerticalSpace*/);
      data.verticalAlignment = SWT.BEGINNING;
      data.horizontalAlignment = SWT.CENTER;
      block.setLayoutData(data);

      Group groupTop = helper.createGroup(block, "Data File", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      Group groupUpper = helper.createGroup(block, "Sequence", 5, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      Group groupCenter = helper.createGroup(block, "Frames", 3, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      Group groupObjects = helper.createGroup(block, "Objects", 6, true/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
      Group groupAssociations = helper.createGroup(block, "Association", 6, true/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);

      GridData layoutData = new GridData();
      layoutData.grabExcessVerticalSpace = true;
      groupCenter.setLayoutData(layoutData);

//      openButton = new Button(groupTop, SWT.FLAT);
//      openButton.setText("open");
//      openButton.addSelectionListener(this);
      saveButton = new Button(groupTop, SWT.FLAT);
      saveButton.setText("save");
      saveButton.addSelectionListener(this);
      race = new Combo(groupTop, SWT.NONE);
      race.addModifyListener(this);

      sequence = new Combo(groupUpper, SWT.NONE);
      sequence.addSelectionListener(this);
      newSeqButton = new Button(groupUpper, SWT.PUSH);
      newSeqButton.addSelectionListener(this);
      newSeqButton.setText("new");
      deleteSeqButton = new Button(groupUpper, SWT.PUSH);
      deleteSeqButton.setText("delete");
      deleteSeqButton.addSelectionListener(this);
      Text repeatCountLabel = new Text(groupUpper, 0);
      repeatCountLabel.setText("Repeat Count");
      repeatCountLabel.setEditable(false);
      repeatCount = new Text(groupUpper, SWT.BORDER);
      repeatCount.setEditable(true);
      repeatCount.addModifyListener(this);
      createTable(groupCenter, 3);

      deleteFrameButton = new Button(groupCenter, SWT.PUSH);
      deleteFrameButton.setText("delete frame");
      deleteFrameButton.addSelectionListener(this);
      insertMedianFrameButton = new Button(groupCenter, SWT.PUSH);
      insertMedianFrameButton.setText("insert median frame");
      insertMedianFrameButton.addSelectionListener(this);
      copyFrameButton = new Button(groupCenter, SWT.PUSH);
      copyFrameButton.setText("copy frame");
      copyFrameButton.addSelectionListener(this);

      helper.createLabel(groupObjects, "Left Hand:", 0, 1, null);
      leftHeldThingComboBox = createLeftHandCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupObjects, "Right Hand:", 0, 1, null);
      rightHeldThingComboBox = createRightHandCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupObjects, "Armor:", 0, 1, null);
      armorComboBox = createArmorCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupAssociations, "Positive", 0, 1, null);
      positiveAssociations = helper.createText(groupAssociations, "", true/*editable*/, 3/*hSpan*/);
      helper.createLabel(groupAssociations, "", 0, 2, null);
      positiveAssociations.addModifyListener(this);
      init();
      loadRace("human");
   }

   private Combo createLeftHandCombo(Group parent, Helper helper) {
      List<String> entries = new ArrayList<>();
      entries.add("");
      for (Shield shield : Shield.values()) {
         entries.add(shield.toString());
      }
      // add the bows into this combobox
      for (Weapon weapon : Weapon.values()) {
         if (weapon.toString().contains("bow_")) {
            entries.add(weapon.toString());
         }
      }
      Combo combo = helper.createCombo(parent, 0, 1/*hSpan*/, entries);
      combo.addSelectionListener(this);
      return combo;
   }

   private Combo createRightHandCombo(Group parent, Helper helper) {
      List<String> entries = new ArrayList<>();
      entries.add("");
      for (Weapon weapon : Weapon.values()) {
         // don't put the bows into this combobox
         if (weapon.toString().contains("bow_")) {
            continue;
         }
         entries.add(weapon.toString());
      }
      Combo combo = helper.createCombo(parent, 0, 1/*hSpan*/, entries);
      combo.addSelectionListener(this);
      return combo;
   }

   private Combo createArmorCombo(Group parent, Helper helper) {
      List<String> entries = new ArrayList<>();
      for (Armor armor : Armor.values()) {
         StringBuilder sb = new StringBuilder();
         String name = armor.toString();
         boolean currentCharUpperCase = true;
         for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean upperCase = Character.isUpperCase(ch);
            if (upperCase && currentCharUpperCase) {
               sb.append(' ');
            }
            sb.append(ch);
            currentCharUpperCase = upperCase;
         }
         entries.add(sb.toString());
      }
      Combo combo = helper.createCombo(parent, 0, 1/*hSpan*/, entries);
      combo.addSelectionListener(this);
      return combo;
   }

   public void createTable(Composite parent, int colSpan) {
      table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER);
      GridData layoutData = new GridData();
      layoutData.horizontalSpan = colSpan;
      layoutData.grabExcessHorizontalSpace = true;
      layoutData.grabExcessVerticalSpace = true;
      table.setLayoutData(layoutData);
      table.setHeaderVisible(true);
      table.setLinesVisible(true);
      table.setBounds(new org.eclipse.swt.graphics.Rectangle(47, 67, 190, 70));

      String[] columnNames = new String[] { "Index", "Time", "Data"};
      for (String columnName : columnNames) {
         TableColumn tableColumn = new TableColumn(table, SWT.NONE);
         tableColumn.setText(columnName);
      }

      int colIndex = 0;
      for (TableColumn col : table.getColumns()) {
         if (colIndex++ < 2) {
            col.pack();
         }
         else {
            col.setWidth(750);
         }
      }
      table.addSelectionListener(this);
      for (int i = 0; i < 10; i++) {
         TableItem row = new TableItem(table, SWT.NONE);
         row.setData(i);
         row.setText(new String[] { "", "", ""});
      }
   }

   public void loadSequenceIntoTable() {
      AnimationSequence seq = getCurrentAnimationSequence();
      if (seq != null) {
         table.removeAll();
         for (int i = 0; i < seq.frames.size(); i++) {
            AnimationFrame frame = seq.frames.get(i);
            TableItem row = new TableItem(table, SWT.NONE);
            row.setData(i);
            row.setText(new String[] {String.valueOf(i), String.valueOf(frame.timeInMilliseconds), frame.data});
         }
         table.select(0);
         loadFrameSet();
      }
   }

   public void init() {
      for (String race : SequenceLibrary.availableRaces) {
         this.race.add(race);
      }
      race.setText("human");
      loadSequenceIntoTable();
   }

   public void loadFrameSet() {
      humanModel.clearAnimations();
      HumanBody resetHuman = new HumanBody(null, view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, race.getText()/*raceName*/, true/*isMale*/);
      humanModel.setAngles(resetHuman.getAngles());
      humanModel.setRepeat(true);
      humanModel.addAnimationSequence(getCurrentAnimationSequence());
   }

   private AnimationSequence getCurrentAnimationSequence() {
      return SequenceLibrary.getAnimationSequenceByName(race.getText(), sequence.getText());
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == table) {
         if (e.item instanceof TableItem) {
            TableItem item = (TableItem) e.item;
            Integer row = (Integer) item.getData();
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               AnimationFrame frame = seq.frames.get(row);
               scene.setStaticModelData(frame);
            }
         }
      }
      else if (e.widget == sequence) {
         loadSequenceIntoTable();
      }
      else if (e.widget instanceof Button) {
//         if (e.widget == openButton) {
//            SequenceLibrary.openFile(e.display.getActiveShell());
//            sequence.removeAll();
//            for (AnimationSequence seq : SequenceLibrary.getSequencesForRace(race.getText())) {
//               addSequence(seq);
//            }
//            scene.frameController.updateKeyFrames();
//         }
         if (e.widget == saveButton) {
            SequenceLibrary.saveFile(null, race.getText());
            //SequenceLibrary.saveToAnyFile(e.display.getActiveShell());
         }
         else if (e.widget == newSeqButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            AnimationFrame frame = null;
            if (seq != null) {
               int index = table.getSelectionIndex();
               frame = new AnimationFrame();
               if (index >= 0) {
                  AnimationFrame sourceFrame = seq.frames.get(index);
                  frame.data = sourceFrame.data;
               }
               else {
                  frame.data = humanModel.getAngles();
               }
               frame.timeInMilliseconds = 250;
            }
            seq = new AnimationSequence();
            NewSequenceDialog dlg = new NewSequenceDialog(e.display.getActiveShell(), race.getText());
            String name = dlg.open();
            if (name != null) {
               seq.name = name;
               seq.beginKeyFrame = dlg.sequenceStartName;
               seq.endKeyFrame = dlg.sequenceEndName;
               frame = new AnimationFrame();
               frame.data = SequenceLibrary.keyFrames.get(dlg.sequenceStartName);
               frame.timeInMilliseconds = 250;
               seq.frames.add(frame);
               frame = new AnimationFrame();
               frame.data = SequenceLibrary.keyFrames.get(dlg.sequenceEndName);
               frame.timeInMilliseconds = 250;
               seq.frames.add(frame);
               SequenceLibrary.sequences.add(seq);
               sequence.add(name);
            }
         }
         else if (e.widget == deleteSeqButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               SequenceLibrary.sequences.remove(seq);
               sequence.remove(seq.name);
               if (sequence.getItemCount() > 0) {
                  sequence.select(0);
               }
               loadSequenceIntoTable();
            }
         }
         else if (e.widget == deleteFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               seq.frames.remove(table.getSelectionIndex());
               loadSequenceIntoTable();
            }
         }
         else if (e.widget == copyFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               int index = table.getSelectionIndex();
               AnimationFrame frame = seq.frames.get(index);
               AnimationFrame copy = new AnimationFrame(frame);
               seq.frames.add(index, copy);
               loadSequenceIntoTable();
               table.select(index + 1); // select the new copy
            }
         }
         else if (e.widget == insertMedianFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               int index = table.getSelectionIndex();
               if (index > 0) {
                  AnimationFrame frame0 = seq.frames.get(index - 1);
                  AnimationFrame frame1 = seq.frames.get(index);
                  HashMap<String, HashMap<String, Float>> data0 = HumanBody.splitAnglesTextIntoHashMap(frame0.data);
                  HashMap<String, HashMap<String, Float>> data1 = HumanBody.splitAnglesTextIntoHashMap(frame1.data);
                  HashMap<String, HashMap<String, Float>> dataAve = new HashMap<>();
                  for (String key : data0.keySet()) {
                     HashMap<String, Float> value0 = data0.get(key);
                     HashMap<String, Float> value1 = data1.get(key);
                     HashMap<String, Float> valueAve = new HashMap<>();
                     dataAve.put(key, valueAve);
                     for (String subkey : value0.keySet()) {
                        Float subvalue0 = value0.get(subkey);
                        Float subvalue1 = value1.get(subkey);
                        valueAve.put(subkey, (subvalue1 + subvalue0)/2);
                     }
                  }
                  AnimationFrame newFrame = new AnimationFrame();
                  newFrame.data = HumanBody.combineHashMapIntoAnglesText(dataAve);
                  newFrame.timeInMilliseconds = frame1.timeInMilliseconds / 2;
                  frame1.timeInMilliseconds = frame1.timeInMilliseconds / 2;
                  seq.frames.add(index, newFrame);
                  loadSequenceIntoTable();
                  table.select(index + 1); // select the new copy
               }
            }
         }
      }
      else if (e.widget == leftHeldThingComboBox) {
         setHeldThing(false/*rightHand*/, leftHeldThingComboBox.getText());
      }
      else if (e.widget == rightHeldThingComboBox) {
         setHeldThing(true/*rightHand*/, rightHeldThingComboBox.getText());
      }
      else if (e.widget == armorComboBox) {
         humanModel.setArmor(armorComboBox.getText());
         scene.frameController.humanModel.setArmor(armorComboBox.getText());
      }

   }

   private boolean setHeldThing(boolean rightHand, String thingName) {
      if ((thingName != null) && (thingName.length() > 0)) {
         try {
            Thing.Shield shield = Thing.Shield.valueOf(thingName);
            humanModel.setHeldThing(rightHand, shield);
            humanModel.setHeldThing(rightHand, (Weapon)null);
            scene.frameController.humanModel.setHeldThing(rightHand, shield);
            scene.frameController.humanModel.setHeldThing(rightHand, (Weapon)null);
            return true;
         }
         catch (Exception ex) {
            try {
               Weapon weapon = Thing.Weapon.valueOf(thingName);
               humanModel.setHeldThing(rightHand, weapon);
               humanModel.setHeldThing(rightHand, (Shield)null);
               scene.frameController.humanModel.setHeldThing(rightHand, (Shield)null);
               scene.frameController.humanModel.setHeldThing(rightHand, weapon);
               return true;
            }
            catch (Exception exc) {
            }
         }
      }
      humanModel.setHeldThing(rightHand, (Weapon)null);
      humanModel.setHeldThing(rightHand, (Shield)null);
      scene.frameController.humanModel.setHeldThing(rightHand, (Shield)null);
      scene.frameController.humanModel.setHeldThing(rightHand, (Weapon)null);
      return false;
   }
   void addSequence(AnimationSequence seq) {
      sequence.add(seq.name);
      sequence.select(sequence.getItemCount() - 1);
      loadSequenceIntoTable();
   }

   public HumanBody getModel() {
      return humanModel;
   }

   public void setCurrentFrameModelData(AnimationFrame frame) {
      AnimationSequence seq = getCurrentAnimationSequence();
      if (seq != null) {
         TableItem[] selectedRows = table.getSelection();
         if (selectedRows.length == 1) {
            TableItem[] allRows = table.getItems();
            for (int i = 0; i < allRows.length; i++) {
               if (selectedRows[0] == allRows[i]) {
                  selectedRows[0].setText(new String[] {String.valueOf(i), String.valueOf(frame.timeInMilliseconds), frame.data});
                  seq.frames.get(i).data = frame.data;
                  seq.frames.get(i).timeInMilliseconds = frame.timeInMilliseconds;
                  seq.frames.get(i).leftFootPlanted = frame.leftFootPlanted;
                  seq.frames.get(i).rightFootPlanted = frame.rightFootPlanted;
                  loadFrameSet();
                  return;
               }
            }
         }
      }
   }

   public void clearSequences() {
      SequenceLibrary.sequences.clear();
      sequence.removeAll();
   }

   public void incrementCameraAngle(float x, float y) {
      view.incrementCameraAngle(x, y);
   }

   public void drawScene(Display display) {
      humanModel.advanceAnimation();

      // Save matrix state
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      //      GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
      //      {
      //         view.useAsCurrentCanvas();
      //         view.setupView();
      //         FloatBuffer headLoc = humanModel.getHeadLocationInWindowReferenceFrame();
      //         int x = (int) headLoc.get(0);
      //         int y = (int) headLoc.get(1);
      //         //int z = (int) headLoc.get(2);
      //         message.xLoc = x - 64;
      //         message.yLoc = y + 30;
      //         GL11.glPopMatrix();
      //      }

      view.drawScene(/*_humanModel, */display);

   }

   public void setCameraPosition(float x, float y, float z) {
      view.setCameraPosition(x, y, z);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (e.widget == positiveAssociations) {
         AnimationSequence seq = getCurrentAnimationSequence();
         if (seq != null) {
            seq.positiveAttributes.clear();
            seq.positiveAttributes.add(positiveAssociations.getText());
         }
      }
      else if (e.widget == repeatCount) {
         String value = repeatCount.getText();
         try {
            humanModel.repeatCount = Integer.parseInt(value);
         }
         catch(Exception ex) {
         }
      }
      else if (e.widget == race) {
         loadRace(race.getText());
         loadSequenceIntoTable();
      }
   }
}
