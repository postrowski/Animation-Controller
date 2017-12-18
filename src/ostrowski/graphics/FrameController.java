package ostrowski.graphics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import ostrowski.graphics.model.Tuple3;
import ostrowski.graphics.objects3d.BodyPart;
import ostrowski.graphics.objects3d.Helper;
import ostrowski.graphics.objects3d.HumanBody;
import ostrowski.graphics.texture.Texture;

public class FrameController implements ModifyListener, FocusListener, SelectionListener
{

   private final GLScene _scene;
   private final GLView  _view;
   HumanBody       _humanModel;
   List<Text>      _textFields = new ArrayList<>();
   private Button  _confirmButton;
   private Button  _restoreButton;
   private Text    _timeText;
   private Button  _makeKeyFrameButton;
   private Button  _updateKeyFrameButton;
   private Combo   _keyFramesCombo;
   private Button  _rightFootPlanted;
   private Button  _leftFootPlanted;

   public FrameController(Composite parent, GLScene scene) {
      _view = new GLView(parent, true/*withControls*/);
      _view.addDefaultMap();
      _view.setElevationPower(2);
      _scene = scene;
      Texture humanTexture = null;
      try {
         humanTexture = _view.getTextureLoader().getTexture("res/bodyparts/texture_humanmale.png");
      } catch (IOException e) {
      }

      loadRace("human", humanTexture);
   }

   public void loadRace(String race, Texture texture) {
      if (_humanModel != null) {
         _view.removeObject(_humanModel);
      }
      _humanModel = new HumanBody(texture, _view, 1.0f/*lengthFactor*/, 1.0f/*widthFactor*/, race/*raceName*/, true/*isMale*/);
      _view.addModel(_humanModel);
   }

   public void createUI(Composite parent) {
      Helper helper = new Helper();
      Group center = helper.createGroup(parent, "Center", 1, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      Group left = helper.createGroup(parent, "Left", 1, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      Group right = helper.createGroup(parent, "Right", 1, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
      {
         Group groupHead = helper.createGroup(center, "Head", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupHead, "Head"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupHead, "Head"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupHead, "Head"/*part*/, "t"/*angleName*/, helper, _textFields);
      }
      {
         Group groupTorso = helper.createGroup(center, "Torso", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupTorso, "Torso"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupTorso, "Torso"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupTorso, "Torso"/*part*/, "h"/*angleName*/, helper, _textFields);
         createBlock(groupTorso, "Torso"/*part*/, "t"/*angleName*/, helper, _textFields);
      }
      {
         Group groupCore = helper.createGroup(center, "Pelvis", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupCore, "Pelvis"/*part*/, "h"/*angleName*/, helper, _textFields);
         createBlock(groupCore, "Pelvis"/*part*/, "t"/*angleName*/, helper, _textFields);
      }
      {
         Group groupCore = helper.createGroup(center, "Body", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupCore, "Body"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupCore, "Body"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupCore, "Body"/*part*/, "t"/*angleName*/, helper, _textFields);
      }
      {
         Group groupCore = helper.createGroup(center, "Weapon", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupCore, "rightWeapon"/*part*/, "l"/*angleName*/, helper, _textFields);
         createBlock(groupCore, "rightWeapon"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupCore, "rightWeapon"/*part*/, "t"/*angleName*/, helper, _textFields);
      }
      {
         Group groupLeftArm = helper.createGroup(left, "Arm", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupLeftArm, "LeftArm"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupLeftArm, "LeftArm"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupLeftArm, "LeftArm"/*part*/, "t"/*angleName*/, helper, _textFields);
         {
            Group groupLeftForeArm = helper.createGroup(groupLeftArm, "ForeArm", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
            createBlock(groupLeftForeArm, "LeftForeArm"/*part*/, "f"/*angleName*/, helper, _textFields);
            createBlock(groupLeftForeArm, "LeftForeArm"/*part*/, "t"/*angleName*/, helper, _textFields);
            {
               Group groupLeftHand = helper.createGroup(groupLeftForeArm, "Hand", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
               createBlock(groupLeftHand, "LeftHand"/*part*/, "f"/*angleName*/, helper, _textFields);
               createBlock(groupLeftHand, "LeftHand"/*part*/, "s"/*angleName*/, helper, _textFields);
               {
                  Group groupLeftFingers = helper.createGroup(groupLeftForeArm, "Fingers", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
                  createBlock(groupLeftFingers, "LeftFingers"/*part*/, "f"/*angleName*/, helper, _textFields);
               }
            }
         }
      }
      {
         Group groupLeftLeg = helper.createGroup(left, "Leg", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupLeftLeg, "LeftLeg"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupLeftLeg, "LeftLeg"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupLeftLeg, "LeftLeg"/*part*/, "t"/*angleName*/, helper, _textFields);
         {
            Group groupLeftLowerLeg = helper.createGroup(groupLeftLeg, "LowerLeg", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
            createBlock(groupLeftLowerLeg, "LeftLowerLeg"/*part*/, "f"/*angleName*/, helper, _textFields);
            createBlock(groupLeftLowerLeg, "LeftLowerLeg"/*part*/, "t"/*angleName*/, helper, _textFields);
            {
               Group groupLeftFoot = helper.createGroup(groupLeftLowerLeg, "Foot", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
               createBlock(groupLeftFoot, "LeftFoot"/*part*/, "f"/*angleName*/, helper, _textFields);
               {
                  Group groupLeftToes = helper.createGroup(groupLeftFoot, "Toes", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
                  createBlock(groupLeftToes, "LeftToes"/*part*/, "f"/*angleName*/, helper, _textFields);
               }
            }
         }
      }
      {
         Group groupRightArm = helper.createGroup(right, "Arm", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupRightArm, "RightArm"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupRightArm, "RightArm"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupRightArm, "RightArm"/*part*/, "t"/*angleName*/, helper, _textFields);
         {
            Group groupRightForeArm = helper.createGroup(groupRightArm, "ForeArm", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
            createBlock(groupRightForeArm, "RightForeArm"/*part*/, "f"/*angleName*/, helper, _textFields);
            createBlock(groupRightForeArm, "RightForeArm"/*part*/, "t"/*angleName*/, helper, _textFields);
            {
               Group groupRightHand = helper.createGroup(groupRightForeArm, "Hand", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
               createBlock(groupRightHand, "RightHand"/*part*/, "f"/*angleName*/, helper, _textFields);
               createBlock(groupRightHand, "RightHand"/*part*/, "s"/*angleName*/, helper, _textFields);
               {
                  Group groupRightFingers = helper.createGroup(groupRightForeArm, "Fingers", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
                  createBlock(groupRightFingers, "RightFingers"/*part*/, "f"/*angleName*/, helper, _textFields);
               }
            }
         }
      }
      {
         Group groupRightLeg = helper.createGroup(right, "Leg", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/);
         createBlock(groupRightLeg, "RightLeg"/*part*/, "f"/*angleName*/, helper, _textFields);
         createBlock(groupRightLeg, "RightLeg"/*part*/, "s"/*angleName*/, helper, _textFields);
         createBlock(groupRightLeg, "RightLeg"/*part*/, "t"/*angleName*/, helper, _textFields);
         {
            Group groupRightLowerLeg = helper.createGroup(groupRightLeg, "LowerLeg", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
            createBlock(groupRightLowerLeg, "RightLowerLeg"/*part*/, "f"/*angleName*/, helper, _textFields);
            createBlock(groupRightLowerLeg, "RightLowerLeg"/*part*/, "t"/*angleName*/, helper, _textFields);
            {
               Group groupRightFoot = helper.createGroup(groupRightLowerLeg, "Foot", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
               createBlock(groupRightFoot, "RightFoot"/*part*/, "f"/*angleName*/, helper, _textFields);
               {
                  Group groupRightToes = helper.createGroup(groupRightFoot, "Toes", 2, false/*sameSize*/, 1/*hSpacing*/, 1/*vSpacing*/, 2/*hSpan*/);
                  createBlock(groupRightToes, "RightToes"/*part*/, "f"/*angleName*/, helper, _textFields);
               }
            }
         }
      }

      helper.createLabel(center, "", 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);

      _rightFootPlanted = new Button(right, SWT.CHECK);
      _rightFootPlanted.addSelectionListener(this);
      _rightFootPlanted.setText("right foot planted");
      GridData data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      _rightFootPlanted.setLayoutData(data);

      _leftFootPlanted = new Button(left, SWT.CHECK);
      _leftFootPlanted.addSelectionListener(this);
      _leftFootPlanted.setText("left foot planted");
      data = new GridData(GridData.FILL_HORIZONTAL);
      data.horizontalSpan = 2;
      _leftFootPlanted.setLayoutData(data);

      helper.createLabel(center, "", 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);
      helper.createLabel(center, "time in milliseconds:", 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);
      _timeText = helper.createText(center, "250", true/*editable*/, 1/*hSpan*/);
      //_timeText.addModifyListener(this);
      _timeText.addFocusListener(this);

      helper.createLabel(center, "", 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);
      _confirmButton = helper.createButton(center, "commit", 1/*hSpan*/, null/*fontData*/, this);
      _restoreButton = helper.createButton(center, "restore", 1/*hSpan*/, null/*fontData*/, this);

      helper.createLabel(center, "", 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);
      _makeKeyFrameButton = helper.createButton(center, "Make KeyFrame", 1/*hSpan*/, null/*fontData*/, this);

      _keyFramesCombo = helper.createCombo(center, 0/*style*/, 1/*hspan*/, null);
      _updateKeyFrameButton = helper.createButton(center, "Update KeyFrame", 1/*hSpan*/, null/*fontData*/, this);
      _keyFramesCombo.addSelectionListener(this);
      updateKeyFrames();
      readTextValuesFromModel();
   }

   private void readTextValuesFromModel() {
      for (Text text : _textFields) {
         modifyText(text, false/*fromWidget*/);
      }
      _dirty = false;
   }

   private Text createBlock(Group parent, String partName, String angleName, Helper helper, List<Text> textFields) {
      String name = "Front";
      if (angleName.equalsIgnoreCase("f")) {
         name = "Front";
      }
      else if (angleName.equalsIgnoreCase("t")) {
         name = "Twist";
      }
      else if (angleName.equalsIgnoreCase("s")) {
         name = "Side";
      }
      else if (angleName.equalsIgnoreCase("h")) {
         if (partName.equalsIgnoreCase("torso")) {
            name = "RghtShldrUp";
         }
         else {
            name = "LeftHipUp";
         }
      }
      else if (angleName.equalsIgnoreCase("l")) {
         name = "len.offset";
      }

      helper.createLabel(parent, name, 0/*hAlign*/, 1/*hSpan*/, null/*fontData*/);
      Text text = helper.createText(parent, "0", true/*editable*/, 1/*hSpan*/);
      text.addModifyListener(this);
      text.addFocusListener(this);
      _textWidgetToPartName.put(text, partName);
      _textWidgetToAngleName.put(text, angleName);
      textFields.add(text);

      return text;
   }

   HashMap<Text, String> _textWidgetToPartName  = new HashMap<>();
   HashMap<Text, String> _textWidgetToAngleName = new HashMap<>();

   @Override
   public void modifyText(ModifyEvent e) {
      modifyText((Text) (e.widget), true /*fromWidget*/);
   }

   public void modifyText(Text widget, boolean fromWidget) {
      String partName = _textWidgetToPartName.get(widget);
      String angleName = _textWidgetToAngleName.get(widget);

      if (fromWidget) {
         try {
            float value = Float.parseFloat(widget.getText());
            _dirty = true;
            BodyPart part = _humanModel.getBodyPart(partName);
            if (part != null) {
               HashMap<String, Float> map = part.getAnglesMap();
               map.put(angleName, value);
               part.setAnglesFromMap(map);
               _humanModel.setHeightBasedOnLowestLimbPoint();
            }
         } catch (NumberFormatException ex) {
         }
      }
      else {
         BodyPart part = _humanModel.getBodyPart(partName);
         if (part != null) {
            HashMap<String, Float> map = part.getAnglesMap();
            Float value = map.get(angleName);
            if (value == null) {
               value = 0f;
            }
            widget.setText(String.valueOf(value));
         }
      }
   }

   private boolean _dirty = false;
   private String  _originalData;

   @Override
   public void focusGained(FocusEvent e) {
   }

   @Override
   public void focusLost(FocusEvent e) {
      if (_dirty) {
         readTextValuesFromModel();
      }
   }

   public void setModelData(AnimationFrame frame) {
      _originalData = frame._data;
      _timeText.setText(String.valueOf(frame._timeInMilliseconds));
      _humanModel.setAngles(frame._data);
      _humanModel.setHeightBasedOnLowestLimbPoint();
      _leftFootPlanted.setSelection(frame._leftFootPlanted);
      _rightFootPlanted.setSelection(frame._rightFootPlanted);
      readTextValuesFromModel();
   }

   public HumanBody getModel() {
      return _humanModel;
   }

   @Override
   public void widgetDefaultSelected(SelectionEvent e) {
   }

   @Override
   public void widgetSelected(SelectionEvent e) {
      if (e.widget == _confirmButton) {
         long timeInMilliseconds = 0;
         timeInMilliseconds = Long.parseLong(_timeText.getText());
         AnimationFrame frame = new AnimationFrame();
         frame._data = _humanModel.getAngles();
         frame._timeInMilliseconds = timeInMilliseconds;
         frame._leftFootPlanted = _leftFootPlanted.getSelection();
         frame._rightFootPlanted = _rightFootPlanted.getSelection();
         _scene.setCurrentFrameModelData(frame);
      }
      else if (e.widget == _restoreButton) {
         _humanModel.setAngles(_originalData);
         _humanModel.setHeightBasedOnLowestLimbPoint();
         readTextValuesFromModel();
      }
      else if (e.widget == _makeKeyFrameButton) {
         NewKeyFrameDialog keyDlg = new NewKeyFrameDialog(e.display.getActiveShell());
         String keyFrameName = keyDlg.open();
         if (keyFrameName != null) {
            SequenceLibrary._keyFrames.put(keyFrameName, _humanModel.getAngles());
            _keyFramesCombo.add(keyFrameName);
            _keyFramesCombo.select(_keyFramesCombo.getItemCount() - 1);
         }
      }
      else if (e.widget == _updateKeyFrameButton) {
         SequenceLibrary._keyFrames.put(_keyFramesCombo.getText(), _humanModel.getAngles());
      }
      else if (e.widget == _keyFramesCombo) {
         String selectedText = _keyFramesCombo.getText();
         String angles = SequenceLibrary._keyFrames.get(selectedText);
         if (angles != null) {
            _humanModel.setAngles(angles);
            _humanModel.setHeightBasedOnLowestLimbPoint();
            readTextValuesFromModel();
         }
      }
   }

   public void updateKeyFrames() {
      _keyFramesCombo.removeAll();
      for (String key : SequenceLibrary._keyFrames.keySet()) {
         _keyFramesCombo.add(key);
      }
   }

   public Set<String> getKeyFrameNames() {
      return SequenceLibrary._keyFrames.keySet();
   }

   public String getKeyFrame(String key) {
      return SequenceLibrary._keyFrames.get(key);
   }

   public void addKeyFrame(String name, String data) {
      SequenceLibrary._keyFrames.put(name, data);
      _keyFramesCombo.add(name);
   }

   public void incrementCameraAngle(float x, float y) {
      _view.incrementCameraAngle(x, y);
   }

   public void drawScene(Display display) {
      _view.drawScene(/*_humanModel, */display);
   }

   public void setCameraPosition(float x, float y, float z) {
      _view.setCameraPosition(x, y, z);
   }

   public Tuple3 getCameraPosition() {
      return _view._cameraPosition;
   }
}
