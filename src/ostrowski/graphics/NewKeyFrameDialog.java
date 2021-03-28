package ostrowski.graphics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class NewKeyFrameDialog
{
   static boolean        createOK = false;
   protected String      name     = "New Sequence";
   protected final Shell dialog;

   public NewKeyFrameDialog(Shell parent) {
      dialog = new Shell(parent, SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM);
      dialog.setText("Create Key Frame");
      dialog.setSize(250, 150);

      final Button buttonOK = new Button(dialog, SWT.PUSH);
      buttonOK.setText("Create");
      buttonOK.setBounds(20, 55, 80, 25);

      Button buttonCancel = new Button(dialog, SWT.PUSH);
      buttonCancel.setText("Cancel");
      buttonCancel.setBounds(120, 55, 80, 25);

      final Label label = new Label(dialog, SWT.None);
      label.setText("Key Frame name:");
      label.setBounds(20, 15, 100, 20);

      final Text name = new Text(dialog, SWT.BORDER);
      name.setText(this.name);
      name.setBounds(120, 15, 100, 20);

      Listener listener = event -> {
         if (event.widget == buttonOK) {
            createOK = true;
         } else {
            createOK = false;
         }
         dialog.close();
      };

      ModifyListener modifyListener = e -> this.name = name.getText();
      name.addModifyListener(modifyListener );
      buttonOK.addListener(SWT.Selection, listener);
      buttonCancel.addListener(SWT.Selection, listener);
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
