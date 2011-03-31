package chrulri.gen8.AppDataResizer;

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import chrulri.gen8.AppDataResizer.Utils.FileUtils;
import chrulri.gen8.AppDataResizer.Utils.LogUtils;
import chrulri.gen8.AppDataResizer.Utils.MountUtils;
import chrulri.gen8.AppDataResizer.Utils.ProcessUtils;

public class MainActivity extends Activity {

	static final String TAG = MainActivity.class.getName();
	static final String PARTED_BIN = "parted.bin";
	static final String INTERNAL_MMC = "/dev/block/mmcblk1";
	static final String INTERNAL_HDD = "/dev/sda"; // TODO to be verified!

	private File _partedBinary;
	private Object _mountService;
	private File _sdcard;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// layout
		setContentView(R.layout.main);
		Button btnMount = (Button) findViewById(R.id.btnMount);
		Button btnUnmount = (Button) findViewById(R.id.btnUnmount);
		Button btnCheck = (Button) findViewById(R.id.btnCheck);
		// setup parted binary
		_partedBinary = getFileStreamPath(PARTED_BIN);
		_partedBinary.delete();
		if (!FileUtils.copyToFile(getResources().openRawResource(R.raw.parted),
				_partedBinary)) {
			LogUtils.showError(this, R.string.err_extract);
			return;
		}
		// prepare mount service
		_mountService = MountUtils.getService();
		_sdcard = Environment.getExternalStorageDirectory();
		// register button click handler
		btnMount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mount();
			}
		});
		btnUnmount.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				unmount();
			}
		});
		btnCheck.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				new AsyncTask<Void, Integer, Boolean>() {

					@Override
					protected Boolean doInBackground(Void... params) {
						try {
							Process p;
							int i = 0;
							// check if mounted
							publishProgress(i++, R.string.progress_checkMounted);
							if ("mounted".equals(MountUtils.getVolumeState(_mountService,
									_sdcard.toString()))) {
								return false;
							}
							// check usb mass storage
							publishProgress(i++, R.string.progress_checkUsbMassStorage);
							if (MountUtils.isUsbMassStorageEnabled(_mountService)) {
								return false;
							}
							// setup parted binary
							publishProgress(i++, R.string.progress_setupPartedBinary);
							int ret = ProcessUtils.runAsRoot("chmod", "755",
									_partedBinary.getAbsolutePath()).waitFor();
							if (ret != 0) {
								LogUtils.showError(MainActivity.this, R.string.err_chmod);
								return false;
							}
							// parse and verify parted print
							publishProgress(i++, R.string.progress_verifyPartedPrint);
							p = ProcessUtils.runAsRoot(_partedBinary.getAbsolutePath(),
									"-ms", getInternalDevice(), "print");
							String partedPrint = ProcessUtils.readStdOut(p);
							p.waitFor();
							// parse and verify output
							if (partedPrint == null || partedPrint.trim().length() == 0) {
								LogUtils.showError(MainActivity.this,
										"Failed to get Parted Output!");
								return false;
							}
							String[] printLines = partedPrint.replace("\n", "").split(";");
							if (printLines.length < 3) {
								LogUtils.showError(MainActivity.this,
										"Parted returned no existing partitions!", new IOException(
												partedPrint));
								return false;
							}
							String partedFormat = printLines[0];
							if (!"BYT".equals(partedFormat)) {
								LogUtils.showError(MainActivity.this, "Unknown Parted Format ("
										+ partedFormat + ")");
								return false;
							}
							// be very sure what you do: verify device and partitions before changing anything at
							// all!
							String devNodes[] = printLines[1].split(":");
							if (devNodes.length != 7) {
								LogUtils.showError(MainActivity.this, "Invalid Device Line",
										new IOException(printLines[1]));
								return false;
							}
							String devSize = devNodes[1];
							// "path":"size":"transport-type":"logical-sector-size":"physical-sector-size":"partition-table-type":"model-name";
							if (!getInternalDevice().equals(devNodes[0])) {
								LogUtils.showError(MainActivity.this,
										"Unexpected Device Name (" + devNodes[0] + ")");
								return false;
							}
							String fatNodes[] = printLines[2].split(":");
							// "number":"begin":"end":"size":"filesystem-type":"partition-name":"flags-set";
							if (printLines.length == 3) {
								// only one partition, verify it anyway!
								if (fatNodes.length != 7 || !fatNodes[0].equals("1")
										|| !fatNodes[1].equals("8192B")
										|| !fatNodes[2].equals(fatNodes[3])
										|| !fatNodes[2].equals(devSize)
										|| !fatNodes[4].equals("fat32")) {
									LogUtils.showError(MainActivity.this,
											"Unexpected first partition", new IOException(
													printLines[2]));
									return false;
								}
							} else {
								// already customized? forget it!
								return false;
							}
							// check first partition
							publishProgress(i++, R.string.progress_checkPartition);
							p = ProcessUtils.runAsRoot(_partedBinary.getAbsolutePath(),
									"-ms", getInternalDevice(), "check", "1");
							String out = ProcessUtils.readStdOut(p);
							ret = p.waitFor();
							if (ret != 0) {
								LogUtils.showError(MainActivity.this, "Partition Check failed",
										new IOException(out));
								return false;
							}

							// BYT;
							// /dev/block/mmcblk1:7466MB:sd/mmc:512:512:msdos:MMC MMC08G;
							// 1:8192B:7466MB:7466MB:fat32::lba;

							// TODO check partitions:
							// parted.bin -ms /dev/block/... check 1
							// TODO resize:
							// parted.bin -ms /dev/block/... resize 1 START_OF_PART_ONE END_OF_DEVICE-1000
							// TODO mkpart:
							// parted.bin -ms /dev/block/... mkpart primary END_OF_PART_ONE END_OF_DEVICE
							// TODO mkfs:
							// mke2fs -Text3 /dev/block/...p2
							// tune2fs -c-1 -i0 /dev/block/...p2

							return true;
						} catch (Exception e) {
							LogUtils.showError(MainActivity.this, R.string.err_parted, e);
							return false;
						}
					}

					protected void onProgressUpdate(Integer... values) {
						// TODO update progress dialog
					};

					protected void onPostExecute(Boolean result) {
						// TODO show nice result
						LogUtils.toast(MainActivity.this, result.toString());
					};
				}.execute();
			}
		});
	}

	private String getInternalDevice() {
		// TODO determine if A70H or not
		return INTERNAL_MMC; // INTERNAL_HDD
	}

	private void mount() {
		MountUtils.mountVolume(_mountService, _sdcard.toString());
	}

	private void unmount() {
		MountUtils.unmountVolume(_mountService, _sdcard.toString(), false);
	}
}