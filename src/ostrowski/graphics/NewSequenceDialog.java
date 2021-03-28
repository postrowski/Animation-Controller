package ostrowski.graphics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewSequenceDialog
{
   protected final Combo sequenceStart;
   protected final Combo sequenceEnd;
   public String sequenceStartName;
   public String sequenceEndName;
   public String name     = "";
   public String raceName = "human";

   static boolean createOK = false;
   protected Shell dialog;

   public NewSequenceDialog(Shell parent, String racename) {
      raceName = racename;
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
      name.setText(this.name);
      name.setBounds(170, 15, 150, 20);
      name.setFocus();

      final Label labelSeqStart = new Label(dialog, SWT.None);
      labelSeqStart.setText("Sequence Key Frame Start:");
      labelSeqStart.setBounds(20, 40, 150, 20);
      sequenceStart = new Combo(dialog, SWT.NONE);
      sequenceStart.setBounds(170, 40, 150, 20);

      final Label labelSeqEnd = new Label(dialog, SWT.None);
      labelSeqEnd.setText("Sequence Key Frame End:");
      labelSeqEnd.setBounds(20, 65, 150, 20);
      sequenceEnd = new Combo(dialog, SWT.NONE);
      sequenceEnd.setBounds(170, 65, 150, 20);

      int i = 0;
      for (String keyFrame : SequenceLibrary.keyFrames.keySet()) {
         sequenceStart.add(keyFrame);
         sequenceEnd.add(keyFrame);
         if (i==0) {
            sequenceStart.setText(keyFrame);
         }
         if (i==1) {
            sequenceEnd.setText(keyFrame);
         }
         i++;
      }


      Listener listener = event -> {
         if (event.widget == buttonOK) {
            createOK = true;
         } else {
            createOK = false;
         }
         dialog.close();
      };

      name.addModifyListener(e -> this.name = name.getText());
      buttonOK.addListener(SWT.Selection, listener);
      buttonCancel.addListener(SWT.Selection, listener);

      sequenceStart.addModifyListener(e -> sequenceStartName = sequenceStart.getText());
      sequenceEnd.addModifyListener(e -> sequenceEndName = sequenceEnd.getText());
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
         return name;
      }
      return null;
   }

}
