package ostrowski.graphics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewSequenceDialog
{
   protected final Combo   _sequenceStart;
   protected final Combo   _sequenceEnd;
   public String  _sequenceStartName;
   public String  _sequenceEndName;
   public String _name = "";
   public String _raceName = "human";

   static boolean createOK = false;
   protected Shell dialog;

   public NewSequenceDialog(Shell parent, String racename) {
      _raceName = racename;
      dialog = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
      dialog.setText("Create new sequence");
      dialog.setSize(350, 175);

      final Button buttonOK = new Button(dialog, SWT.PUSH);
      buttonOK.setText("Create");
      buttonOK.setBounds(80, 105, 80, 25);

      Button buttonCancel = new Button(dialog, SWT.PUSH);
      buttonCancel.setText("Cancel");
      buttonCancel.setBounds(190, 105, 80, 25);

      final Label label = new Label(dialog, SWT.None);
      label.setText("Sequence name:");
      label.setBounds(20, 15, 150, 20);

      final Text name = new Text(dialog, SWT.BORDER);
      name.setText(_name);
      name.setBounds(170, 15, 150, 20);
      name.setFocus();

      final Label labelSeqStart = new Label(dialog, SWT.None);
      labelSeqStart.setText("Sequence Key Frame Start:");
      labelSeqStart.setBounds(20, 40, 150, 20);
      _sequenceStart = new Combo(dialog, SWT.NONE);
      _sequenceStart.setBounds(170, 40, 150, 20);

      final Label labelSeqEnd = new Label(dialog, SWT.None);
      labelSeqEnd.setText("Sequence Key Frame End:");
      labelSeqEnd.setBounds(20, 65, 150, 20);
      _sequenceEnd = new Combo(dialog, SWT.NONE);
      _sequenceEnd.setBounds(170, 65, 150, 20);

      int i = 0;
      for (String keyFrame : SequenceLibrary._keyFrames.keySet()) {
         _sequenceStart.add(keyFrame);
         _sequenceEnd.add(keyFrame);
         if (i==0) {
            _sequenceStart.setText(keyFrame);
         }
         if (i==1) {
            _sequenceEnd.setText(keyFrame);
         }
         i++;
      }


      Listener listener = new Listener() {
         @Override
         public void handleEvent(Event event) {
            if (event.widget == buttonOK) {
               createOK = true;
            } else {
               createOK = false;
            }
            dialog.close();
         }
      };

      name.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            _name = name.getText();
         }
      });
      buttonOK.addListener(SWT.Selection, listener);
      buttonCancel.addListener(SWT.Selection, listener);

      _sequenceStart.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            _sequenceStartName = _sequenceStart.getText();
         }
      });
      _sequenceEnd.addModifyListener(new ModifyListener() {
         @Override
         public void modifyText(ModifyEvent e) {
            _sequenceEndName = _sequenceEnd.getText();
         }
      });
   }

   public String open() {
      dialog.open();
      Display display = dialog.getParent().getShell().getDisplay();
      while (!dialog.isDisposed()) {
         if (!display.readAndDispatch()) {
            display.sleep();
         }
       }
      if (createOK) {
         return _name;
      }
      return null;
   }

}
