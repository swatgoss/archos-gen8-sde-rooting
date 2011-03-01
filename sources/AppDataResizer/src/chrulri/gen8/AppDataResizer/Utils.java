package chrulri.gen8.AppDataResizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;

import android.util.Log;

final class Utils {
	static final String TAG = Utils.class.getName();
	static final int BUFFER_SIZE = 10240;

	public static final String NEWLINE = System.getProperty("line.separator");
	private static Method METHOD_COPYFILE;
	private static Method METHOD_COPYTOFILE;
	private static Method METHOD_SETPERMISSIONS;

	static {
		try {
			Class<?> clazz = Class.forName("android.os.FileUtils");
			METHOD_COPYFILE = clazz.getMethod("copyFile", File.class, File.class);
			METHOD_COPYTOFILE = clazz.getMethod("copyToFile", InputStream.class,
					File.class);
			METHOD_SETPERMISSIONS = clazz.getMethod("setPermissions", String.class,
					int.class, int.class, int.class);
		} catch (Throwable t) {
			Log.wtf(TAG, t);
		}
	}

	public static String getStackTrace(final Throwable t) {
		if (t == null)
			return "null";
		final StringWriter buf = new StringWriter();
		final PrintWriter writer = new PrintWriter(buf);
		t.printStackTrace(writer);
		return buf.toString();
	}

	public static boolean copyFile(File srcFile, File destFile) {
		try {
			return (Boolean) METHOD_COPYFILE.invoke(null, srcFile, destFile);
		} catch (Exception e) {
			Log.e(TAG, null, e);
			return false;
		}
	}

	public static boolean copyToFile(InputStream inputStream, File destFile) {
		try {
			return (Boolean) METHOD_COPYTOFILE.invoke(null, inputStream, destFile);
		} catch (Exception e) {
			Log.e(TAG, null, e);
			return false;
		}
	}

	public static int setPermissions(String file, int mode, int uid, int gid) {
		try {
			return (Integer) METHOD_SETPERMISSIONS.invoke(null, file, mode, uid, gid);
		} catch (Exception e) {
			Log.e(TAG, null, e);
			return -1;
		}
	}

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

	public static String readProcess(Process p1) throws InterruptedException,
			IOException {
		InputStream pin = p1.getInputStream();
		p1.waitFor();
		Writer out = new StringWriter();
		char[] buf = new char[BUFFER_SIZE];
		Reader in = new InputStreamReader(pin);
		int len;
		while ((len = in.read(buf)) != -1) {
			out.write(buf, 0, len);
		}
		return out.toString();
	}
}
