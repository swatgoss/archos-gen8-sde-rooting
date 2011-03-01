package chrulri.gen8.AppDataResizer;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	static final String TAG = MainActivity.class.getName();
	static final String PARTED_BIN = "parted.bin";
	static final String INTERNAL_DEVICE = "/dev/block/mmcblk1";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Button btnTest = (Button) findViewById(R.id.btnTest);
		btnTest.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				testParted();
			}
		});
	}

	private void toast(CharSequence msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	private void toast(int resId) {
		toast(getText(resId));
	}

	private void testParted() {
		File partedBinary = getFileStreamPath(PARTED_BIN);
		partedBinary.delete();
		if (!Utils.copyToFile(getResources().openRawResource(R.raw.parted),
				partedBinary)) {
			toast(R.string.err_extract);
			return;
		}
		try {
			int ret = Utils.runAsRoot("chmod", "755", partedBinary.getAbsolutePath())
					.waitFor();
			if (ret != 0) {
				Log.e(TAG, "chmod (" + ret + ")");
				toast(R.string.err_chmod);
			}
			Process p1 = Utils.runAsRoot(partedBinary.getAbsolutePath(),
					INTERNAL_DEVICE, "print", "list");
			String partedOutput = Utils.readProcess(p1);
			toast(partedOutput);
		} catch (Exception e) {
			Log.e(TAG, "parted", e);
			toast(R.string.err_parted);
			return;
		}
	}
}