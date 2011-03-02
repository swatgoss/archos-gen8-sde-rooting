package chrulri.gen8.AppDataResizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.os.IBinder;
import android.util.AndroidRuntimeException;
import android.util.Log;

final class Utils {
	static final String TAG = Utils.class.getName();

	static final int BUFFER_SIZE = 10240;
	static final String SERVICE_MOUNT = "mount";

	public static final String NEWLINE = System.getProperty("line.separator");

	private static Method METHOD_FileUtils_copyFile;
	private static Method METHOD_FileUtils_copyToFile;
	private static Method METHOD_FileUtils_setPermissions;
	private static Method METHOD_ServiceManager_getService;
	private static Method METHOD_IMountService_asInterface;
	private static Method METHOD_IMountService_getVolumeState;
	private static Method METHOD_IMountService_mountVolume;
	private static Method METHOD_IMountService_unmountVolume;
	private static Method METHOD_IMountService_getStorageUsers;
	private static Method METHOD_IMountService_isUsbMassStorageEnabled;

	static {
		try {
			Class<?> clazz;
			clazz = Class.forName("android.os.FileUtils");
			METHOD_FileUtils_copyFile = clazz.getMethod("copyFile", File.class,
					File.class);
			METHOD_FileUtils_copyToFile = clazz.getMethod("copyToFile",
					InputStream.class, File.class);
			METHOD_FileUtils_setPermissions = clazz.getMethod("setPermissions",
					String.class, int.class, int.class, int.class);

			clazz = Class.forName("android.os.ServiceManager");
			METHOD_ServiceManager_getService = clazz.getMethod("getService",
					String.class);

			clazz = Class.forName("android.os.storage.IMountService$Stub");
			METHOD_IMountService_asInterface = clazz.getMethod("asInterface",
					IBinder.class);

			clazz = Class.forName("android.os.storage.IMountService");
			METHOD_IMountService_getVolumeState = clazz.getMethod("getVolumeState",
					String.class);
			METHOD_IMountService_mountVolume = clazz.getMethod("mountVolume",
					String.class);
			METHOD_IMountService_unmountVolume = clazz.getMethod("unmountVolume",
					String.class, boolean.class);
			METHOD_IMountService_getStorageUsers = clazz.getMethod("getStorageUsers",
					String.class);
			METHOD_IMountService_isUsbMassStorageEnabled = clazz
					.getMethod("isUsbMassStorageEnabled");
		} catch (Exception e) {
			Log.wtf(TAG, e);
		}
	}

	private static Object invoke(Method method, Object receiver, Object... args) {
		try {
			return method.invoke(receiver, args);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, null, e);
			throw new AndroidRuntimeException(e);
		} catch (IllegalAccessException e) {
			Log.e(TAG, null, e);
			throw new AndroidRuntimeException(e);
		} catch (InvocationTargetException e) {
			Log.e(TAG, null, e);
			throw new AndroidRuntimeException(e);
		}
	}

	public static String getStackTrace(final Throwable t) {
		if (t == null)
			return "null";
		StringWriter buf = new StringWriter();
		PrintWriter writer = new PrintWriter(buf);
		t.printStackTrace(writer);
		return buf.toString();
	}

	public static class FileUtils {

		public static boolean copyFile(File srcFile, File destFile) {
			return (Boolean) invoke(METHOD_FileUtils_copyFile, null, srcFile,
					destFile);
		}

		public static boolean copyToFile(InputStream inputStream, File destFile) {
			return (Boolean) invoke(METHOD_FileUtils_copyToFile, null, inputStream,
					destFile);
		}

		public static int setPermissions(String file, int mode, int uid, int gid) {
			return (Integer) invoke(METHOD_FileUtils_setPermissions, null, file,
					mode, uid, gid);
		}
	}

	public static class ProcessUtils {

		public static Process run(String... args) throws IOException {
			return Runtime.getRuntime().exec(args);
		}

		public static Process runAsRoot(String... args) throws IOException {
			if (args == null || args.length == 0)
				return null;
			Process su = run("su");
			DataOutputStream out = new DataOutputStream(su.getOutputStream());
			for (String arg : args) {
				out.writeBytes(" ");
				out.writeBytes(arg);
			}
			out.writeBytes("\n");
			out.writeBytes("exit\n");
			out.flush();
			return su;
		}

		public static String readStdOut(Process proc) throws IOException {
			InputStream pin = proc.getInputStream();
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
			Writer out = new StringWriter();
			char[] buf = new char[BUFFER_SIZE];
			Reader in = new InputStreamReader(pin);
			int len;
			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
			}
			return out.toString();
		}

		public static void printLine(Process proc, String line) throws IOException {
			OutputStream pout = proc.getOutputStream();
			Writer out = new OutputStreamWriter(pout);
			out.write(line);
			out.write(NEWLINE);
			out.flush();
		}
	}

	public static class MountService {

		public static Object getService() {
			Object service = invoke(METHOD_ServiceManager_getService, null,
					SERVICE_MOUNT);
			if (service != null) {
				return invoke(METHOD_IMountService_asInterface, null, service);
			} else {
				Log.e(TAG, "Can't get mount service");
			}
			return null;
		}

		public static String getVolumeState(Object mountService, String mountPoint) {
			return (String) invoke(METHOD_IMountService_getVolumeState, mountService,
					mountPoint);
		}

		public static int mountVolume(Object mountService, String mountPoint) {
			return (Integer) invoke(METHOD_IMountService_mountVolume, mountService,
					mountPoint);
		}

		public static void unmountVolume(Object mountService, String mountPoint,
				boolean force) {
			invoke(METHOD_IMountService_unmountVolume, mountService, mountPoint,
					force);
		}

		public static int[] getStorageUsers(Object mountService, String path) {
			return (int[]) invoke(METHOD_IMountService_getStorageUsers, mountService,
					path);
		}

		public static boolean isUsbMassStorageEnabled(Object mountService) {
			return (Boolean) invoke(METHOD_IMountService_isUsbMassStorageEnabled,
					mountService);
		}
	}
}
