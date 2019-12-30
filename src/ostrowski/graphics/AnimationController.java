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
   private final GLScene   _scene;
   private final GLView    _view;
   private HumanBody _humanModel;
   //private Message                      _message;
   private Table     _table;
   //private Button    _openButton;
   private Button    _saveButton;
   private Button    _newSeqButton;
   private Button    _deleteSeqButton;
   private Combo     _sequence;
   private Combo     _race;
   private Button    _deleteFrameButton;
   private Button    _copyFrameButton;
   private Button    _insertMedianFrameButton;
   private Combo     _leftHeldThingComboBox;
   private Combo     _rightHeldThingComboBox;
   private Combo     _armorComboBox;
   private Text _positiveAssociations;
   private Text _repeatCount;

   public AnimationController(Composite parent, GLScene scene) {
      _view = new GLView(parent, true/*withControls*/);
      _view.addDefaultMap();
      _scene = scene;
   }
   public void loadRace(String race) {
      Texture texture = null;
      try {
         texture = _view.getTextureLoader().getTexture("res/bodyparts/texture_"+race+"male.png");
      } catch (IOException e) {
      }
      if (texture == null) {
         try {
            texture = _view.getTextureLoader().getTexture("res/bodyparts/texture_humanmale.png");
         } catch (IOException e) {
         }
      }
      if (_humanModel != null) {
         _view.removeObject(_humanModel);
      }
      _humanModel = new HumanBody(texture, _view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, race/*raceName*/, true/*isMale*/);
//      Message message = new Message();
//      message._text = "Head";
//      message._colorRGB = new RGB(255, 0, 0);
//      message._visible = true;
//      _humanModel._messages.add(message );
      _view.addModel(_humanModel);

      _scene._frameController.loadRace(race, texture);

      boolean first = true;
      _sequence.removeAll();
      List<AnimationSequence> sequences = SequenceLibrary._sequences;
      for (AnimationSequence seq : sequences) {
         _sequence.add(seq._name);
         if (first) {
            _sequence.setText(seq._name);
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

//      _openButton = new Button(groupTop, SWT.FLAT);
//      _openButton.setText("open");
//      _openButton.addSelectionListener(this);
      _saveButton = new Button(groupTop, SWT.FLAT);
      _saveButton.setText("save");
      _saveButton.addSelectionListener(this);
      _race = new Combo(groupTop, SWT.NONE);
      _race.addModifyListener(this);

      _sequence = new Combo(groupUpper, SWT.NONE);
      _sequence.addSelectionListener(this);
      _newSeqButton = new Button(groupUpper, SWT.PUSH);
      _newSeqButton.addSelectionListener(this);
      _newSeqButton.setText("new");
      _deleteSeqButton = new Button(groupUpper, SWT.PUSH);
      _deleteSeqButton.setText("delete");
      _deleteSeqButton.addSelectionListener(this);
      Text repeatCountLabel = new Text(groupUpper, 0);
      repeatCountLabel.setText("Repeat Count");
      repeatCountLabel.setEditable(false);
      _repeatCount = new Text(groupUpper, SWT.BORDER);
      _repeatCount.setEditable(true);
      _repeatCount.addModifyListener(this);
      createTable(groupCenter, 3);

      _deleteFrameButton = new Button(groupCenter, SWT.PUSH);
      _deleteFrameButton.setText("delete frame");
      _deleteFrameButton.addSelectionListener(this);
      _insertMedianFrameButton = new Button(groupCenter, SWT.PUSH);
      _insertMedianFrameButton.setText("insert median frame");
      _insertMedianFrameButton.addSelectionListener(this);
      _copyFrameButton = new Button(groupCenter, SWT.PUSH);
      _copyFrameButton.setText("copy frame");
      _copyFrameButton.addSelectionListener(this);

      helper.createLabel(groupObjects, "Left Hand:", 0, 1, null);
      _leftHeldThingComboBox = createLeftHandCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupObjects, "Right Hand:", 0, 1, null);
      _rightHeldThingComboBox = createRightHandCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupObjects, "Armor:", 0, 1, null);
      _armorComboBox = createArmorCombo(groupObjects, helper);
      helper.createLabel(groupObjects, "", 0, 4, null);

      helper.createLabel(groupAssociations, "Positive", 0, 1, null);
      _positiveAssociations = helper.createText(groupAssociations, "", true/*editable*/, 3/*hSpan*/);
      helper.createLabel(groupAssociations, "", 0, 2, null);
      _positiveAssociations.addModifyListener(this);
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
      _table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER);
      GridData layoutData = new GridData();
      layoutData.horizontalSpan = colSpan;
      layoutData.grabExcessHorizontalSpace = true;
      layoutData.grabExcessVerticalSpace = true;
      _table.setLayoutData(layoutData);
      _table.setHeaderVisible(true);
      _table.setLinesVisible(true);
      _table.setBounds(new org.eclipse.swt.graphics.Rectangle(47, 67, 190, 70));

      String[] columnNames = new String[] { "Index", "Time", "Data"};
      for (String columnName : columnNames) {
         TableColumn tableColumn = new TableColumn(_table, SWT.NONE);
         tableColumn.setText(columnName);
      }

      int colIndex = 0;
      for (TableColumn col : _table.getColumns()) {
         if (colIndex++ < 2) {
            col.pack();
         }
         else {
            col.setWidth(750);
         }
      }
      _table.addSelectionListener(this);
      for (int i = 0; i < 10; i++) {
         TableItem row = new TableItem(_table, SWT.NONE);
         row.setData(i);
         row.setText(new String[] { "", "", ""});
      }
   }

   public void loadSequenceIntoTable() {
      AnimationSequence seq = getCurrentAnimationSequence();
      if (seq != null) {
         _table.removeAll();
         for (int i = 0; i < seq._frames.size(); i++) {
            AnimationFrame frame = seq._frames.get(i);
            TableItem row = new TableItem(_table, SWT.NONE);
            row.setData(i);
            row.setText(new String[] { String.valueOf(i), String.valueOf(frame._timeInMilliseconds), frame._data});
         }
         _table.select(0);
         loadFrameSet();
      }
   }

   public void init() {
      for (String race : SequenceLibrary._availableRaces) {
         _race.add(race);
      }
      _race.setText("human");
      loadSequenceIntoTable();
   }

   public void loadFrameSet() {
      _humanModel.clearAnimations();
      HumanBody resetHuman = new HumanBody(null, _view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, _race.getText()/*raceName*/, true/*isMale*/);
      _humanModel.setAngles(resetHuman.getAngles());
      _humanModel.setRepeat(true);
      _humanModel.addAnimationSequence(getCurrentAnimationSequence());
   }

   private AnimationSequence getCurrentAnimationSequence() {
      return SequenceLibrary.getAnimationSequenceByName(_race.getText(), _sequence.getText());
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _table) {
         if (e.item instanceof TableItem) {
            TableItem item = (TableItem) e.item;
            Integer row = (Integer) item.getData();
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               AnimationFrame frame = seq._frames.get(row);
               _scene.setStaticModelData(frame);
            }
         }
      }
      else if (e.widget == _sequence) {
         loadSequenceIntoTable();
      }
      else if (e.widget instanceof Button) {
//         if (e.widget == _openButton) {
//            SequenceLibrary.openFile(e.display.getActiveShell());
//            _sequence.removeAll();
//            for (AnimationSequence seq : SequenceLibrary.getSequencesForRace(_race.getText())) {
//               addSequence(seq);
//            }
//            _scene._frameController.updateKeyFrames();
//         }
         if (e.widget == _saveButton) {
            SequenceLibrary.saveFile(null, _race.getText());
            //SequenceLibrary.saveToAnyFile(e.display.getActiveShell());
         }
         else if (e.widget == _newSeqButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            AnimationFrame frame = null;
            if (seq != null) {
               int index = _table.getSelectionIndex();
               frame = new AnimationFrame();
               if (index >= 0) {
                  AnimationFrame sourceFrame = seq._frames.get(index);
                  frame._data = sourceFrame._data;
               }
               else {
                  frame._data = _humanModel.getAngles();
               }
               frame._timeInMilliseconds = 250;
            }
            seq = new AnimationSequence();
            NewSequenceDialog dlg = new NewSequenceDialog(e.display.getActiveShell(), _race.getText());
            String name = dlg.open();
            if (name != null) {
               seq._name = name;
               seq._beginKeyFrame = dlg._sequenceStartName;
               seq._endKeyFrame = dlg._sequenceEndName;
               frame = new AnimationFrame();
               frame._data = SequenceLibrary._keyFrames.get(dlg._sequenceStartName);
               frame._timeInMilliseconds = 250;
               seq._frames.add(frame);
               frame = new AnimationFrame();
               frame._data = SequenceLibrary._keyFrames.get(dlg._sequenceEndName);
               frame._timeInMilliseconds = 250;
               seq._frames.add(frame);
               SequenceLibrary._sequences.add(seq);
               _sequence.add(name);
            }
         }
         else if (e.widget == _deleteSeqButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               SequenceLibrary._sequences.remove(seq);
               _sequence.remove(seq._name);
               if (_sequence.getItemCount() > 0) {
                  _sequence.select(0);
               }
               loadSequenceIntoTable();
            }
         }
         else if (e.widget == _deleteFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               seq._frames.remove(_table.getSelectionIndex());
               loadSequenceIntoTable();
            }
         }
         else if (e.widget == _copyFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               int index = _table.getSelectionIndex();
               AnimationFrame frame = seq._frames.get(index);
               AnimationFrame copy = new AnimationFrame(frame);
               seq._frames.add(index, copy);
               loadSequenceIntoTable();
               _table.select(index + 1); // select the new copy
            }
         }
         else if (e.widget == _insertMedianFrameButton) {
            AnimationSequence seq = getCurrentAnimationSequence();
            if (seq != null) {
               int index = _table.getSelectionIndex();
               if (index > 0) {
                  AnimationFrame frame0 = seq._frames.get(index - 1);
                  AnimationFrame frame1 = seq._frames.get(index);
                  HashMap<String, HashMap<String, Float>> data0 = HumanBody.splitAnglesTextIntoHashMap(frame0._data);
                  HashMap<String, HashMap<String, Float>> data1 = HumanBody.splitAnglesTextIntoHashMap(frame1._data);
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
                  newFrame._data = HumanBody.combineHashMapIntoAnglesText(dataAve);
                  newFrame._timeInMilliseconds = frame1._timeInMilliseconds / 2;
                  frame1._timeInMilliseconds = frame1._timeInMilliseconds / 2;
                  seq._frames.add(index, newFrame);
                  loadSequenceIntoTable();
                  _table.select(index + 1); // select the new copy
               }
            }
         }
      }
      else if (e.widget == _leftHeldThingComboBox) {
         setHeldThing(false/*rightHand*/, _leftHeldThingComboBox.getText());
      }
      else if (e.widget == _rightHeldThingComboBox) {
         setHeldThing(true/*rightHand*/, _rightHeldThingComboBox.getText());
      }
      else if (e.widget == _armorComboBox) {
         _humanModel.setArmor(_armorComboBox.getText());
         _scene._frameController._humanModel.setArmor(_armorComboBox.getText());
      }

   }

   private boolean setHeldThing(boolean rightHand, String thingName) {
      if ((thingName != null) && (thingName.length() > 0)) {
         try {
            Thing.Shield shield = Thing.Shield.valueOf(thingName);
            _humanModel.setHeldThing(rightHand, shield);
            _humanModel.setHeldThing(rightHand, (Weapon)null);
            _scene._frameController._humanModel.setHeldThing(rightHand, shield);
            _scene._frameController._humanModel.setHeldThing(rightHand, (Weapon)null);
            return true;
         }
         catch (Exception ex) {
            try {
               Weapon weapon = Thing.Weapon.valueOf(thingName);
               _humanModel.setHeldThing(rightHand, weapon);
               _humanModel.setHeldThing(rightHand, (Shield)null);
               _scene._frameController._humanModel.setHeldThing(rightHand, (Shield)null);
               _scene._frameController._humanModel.setHeldThing(rightHand, weapon);
               return true;
            }
            catch (Exception exc) {
            }
         }
      }
      _humanModel.setHeldThing(rightHand, (Weapon)null);
      _humanModel.setHeldThing(rightHand, (Shield)null);
      _scene._frameController._humanModel.setHeldThing(rightHand, (Shield)null);
      _scene._frameController._humanModel.setHeldThing(rightHand, (Weapon)null);
      return false;
   }
   void addSequence(AnimationSequence seq) {
      _sequence.add(seq._name);
      _sequence.select(_sequence.getItemCount() - 1);
      loadSequenceIntoTable();
   }

   public HumanBody getModel() {
      return _humanModel;
   }

   public void setCurrentFrameModelData(AnimationFrame frame) {
      AnimationSequence seq = getCurrentAnimationSequence();
      if (seq != null) {
         TableItem[] selectedRows = _table.getSelection();
         if (selectedRows.length == 1) {
            TableItem[] allRows = _table.getItems();
            for (int i = 0; i < allRows.length; i++) {
               if (selectedRows[0] == allRows[i]) {
                  selectedRows[0].setText(new String[] { String.valueOf(i), String.valueOf(frame._timeInMilliseconds), frame._data});
                  seq._frames.get(i)._data = frame._data;
                  seq._frames.get(i)._timeInMilliseconds = frame._timeInMilliseconds;
                  seq._frames.get(i)._leftFootPlanted = frame._leftFootPlanted;
                  seq._frames.get(i)._rightFootPlanted = frame._rightFootPlanted;
                  loadFrameSet();
                  return;
               }
            }
         }
      }
   }

   public void clearSequences() {
      SequenceLibrary._sequences.clear();
      _sequence.removeAll();
   }

   public void incrementCameraAngle(float x, float y) {
      _view.incrementCameraAngle(x, y);
   }

   public void drawScene(Display display) {
      _humanModel.advanceAnimation();

      // Save matrix state
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      //      GL11.glPushMatrix(); // (top) matrix is equivalent to identity matrix now.
      //      {
      //         _view.useAsCurrentCanvas();
      //         _view.setupView();
      //         FloatBuffer headLoc = _humanModel.getHeadLocationInWindowReferenceFrame();
      //         int x = (int) headLoc.get(0);
      //         int y = (int) headLoc.get(1);
      //         //int z = (int) headLoc.get(2);
      //         _message._xLoc = x - 64;
      //         _message._yLoc = y + 30;
      //         GL11.glPopMatrix();
      //      }

      _view.drawScene(/*_humanModel, */display);

   }

   public void setCameraPosition(float x, float y, float z) {
      _view.setCameraPosition(x, y, z);
   }

   @Override
   public void modifyText(ModifyEvent e) {
      if (e.widget == _positiveAssociations) {
         AnimationSequence seq = getCurrentAnimationSequence();
         if (seq != null) {
            seq._positiveAttributes.clear();
            seq._positiveAttributes.add(_positiveAssociations.getText());
         }
      }
      else if (e.widget == _repeatCount) {
         String value = _repeatCount.getText();
         try {
            _humanModel._repeatCount = Integer.parseInt(value);
         }
         catch(Exception ex) {
         }
      }
      else if (e.widget == _race) {
         loadRace(_race.getText());
         loadSequenceIntoTable();
      }
   }
}
