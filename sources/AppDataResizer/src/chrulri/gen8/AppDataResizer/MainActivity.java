package chrulri.gen8.AppDataResizer;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import chrulri.gen8.AppDataResizer.Utils.FileUtils;
import chrulri.gen8.AppDataResizer.Utils.MountService;
import chrulri.gen8.AppDataResizer.Utils.ProcessUtils;

public class MainActivity extends Activity {

	static final String TAG = MainActivity.class.getName();
	static final String PARTED_BIN = "parted.bin";
	static final String INTERNAL_DEVICE = "/dev/block/mmcblk1";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// register button click handler
		Button btnTest = (Button) findViewById(R.id.btnTest);
		btnTest.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				testParted();
			}
		});
		Button btnUnmount = (Button) findViewById(R.id.btnUnmount);
		btnUnmount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Object msvc = MountService.getService();
				String sdc = Environment.getExternalStorageDirectory().toString();
				MountService.unmountVolume(msvc, sdc, false);
			}
		});
		Button btnMount = (Button) findViewById(R.id.btnMount);
		btnMount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Object msvc = MountService.getService();
				String sdc = Environment.getExternalStorageDirectory().toString();
				MountService.mountVolume(msvc, sdc);
			}
		});
		Button btnCheck = (Button) findViewById(R.id.btnCheck);
		btnCheck.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Object msvc = MountService.getService();
				String sdc = Environment.getExternalStorageDirectory().toString();
				toast(sdc + Utils.NEWLINE + MountService.getVolumeState(msvc, sdc)
						+ Utils.NEWLINE
						+ Boolean.toString(MountService.isUsbMassStorageEnabled(msvc)));
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
		if (!FileUtils.copyToFile(getResources().openRawResource(R.raw.parted),
				partedBinary)) {
			toast(R.string.err_extract);
			return;
		}
		try {
			int ret = ProcessUtils.runAsRoot("chmod", "755",
					partedBinary.getAbsolutePath()).waitFor();
			if (ret != 0) {
				Log.e(TAG, "chmod (" + ret + ")");
				toast(R.string.err_chmod);
			}
			Process p1 = ProcessUtils.runAsRoot(partedBinary.getAbsolutePath(),
					"-ms", INTERNAL_DEVICE, "print");
			String partedOutput = ProcessUtils.readStdOut(p1);
			toast(partedOutput);
			// TODO check partitions:
			// parted.bin -ms /dev/block/... check 1
			// parted.bin -ms /dev/block/... check 2
			// TODO resize:
			// parted.bin -ms /dev/block/... resize 1 START_OF_PART_ONE END_OF_DEVICE-1000
			// TODO mkpart:
			// parted.bin -ms /dev/block/... mkpart primary END_OF_PART_ONE END_OF_DEVICE
			// TODO mkfs:
			// mke2fs -Text3 /dev/block/...p2
			// tune2fs -c-1 -i0 /dev/block/...p2
		} catch (Exception e) {
			Log.e(TAG, "parted", e);
			toast(R.string.err_parted);
			return;
		}
	}
}