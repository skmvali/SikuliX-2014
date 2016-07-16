/*
 * Copyright (c) 2010-2016, Sikuli.org, sikulix.com
 * Released under the MIT License.
 *
 */

package org.sikuli.android;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.sikuli.basics.Debug;
import org.sikuli.basics.FileManager;
import org.sikuli.script.RunTime;
import org.sikuli.script.ScreenImage;
import se.vidstige.jadb.JadbDevice;
import se.vidstige.jadb.JadbException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADBDevice {

  private static int lvl = 3;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, "ADBDevice: " + message, args);
  }

  private JadbDevice device = null;
  private int devW = -1;
  private int devH = -1;
  private ADBRobot robot = null;
  private ADBScreen screen = null;

  private static ADBDevice adbDevice = null;

  public static int KEY_HOME = 3;
  public static int KEY_BACK = 4;
  public static int KEY_MENU = 82;


  private ADBDevice() {
  }

  public static ADBDevice get() {
    if (adbDevice == null) {
      adbDevice = new ADBDevice();
      adbDevice.device = ADBClient.getDevice();
      if (adbDevice.device == null) {
        adbDevice = null;
      }
    }
    return adbDevice;
  }

  public static void reset() {
    adbDevice = null;
    ADBClient.reset();
  }

  public String toString() {
    return String.format("Android device: serial: %s display: %dx%d",
            getDeviceSerial(), getBounds().width, getBounds().height);
  }

  public ADBRobot getRobot(ADBScreen screen) {
    if (robot == null) {
      this.screen = screen;
      robot = new ADBRobot(screen, this);
    }
    return robot;
  }

  public String getDeviceSerial() {
    return device.getSerial();
  }

  public Rectangle getBounds() {
    if (devW < 0) {
      Dimension dim = getDisplayDimension();
      devW = (int) dim.getWidth();
      devH = (int) dim.getHeight();
    }
    return new Rectangle(0, 0, devW, devH);
  }

  public ScreenImage captureScreen() {
    BufferedImage bimg = captureDeviceScreen();
    return new ScreenImage(getBounds(), bimg);
  }

  public ScreenImage captureScreen(Rectangle rect) {
    BufferedImage bimg = captureDeviceScreen(rect.x, rect.y, rect.width, rect.height);
    return new ScreenImage(rect, bimg);
  }

  public BufferedImage captureDeviceScreen() {
    return captureDeviceScreen(0, 0, devW, devH);
  }

  public BufferedImage captureDeviceScreen(int y, int _h) {
    return captureDeviceScreen(0, y, devW, _h);
  }

  public BufferedImage captureDeviceScreen(int x, int y, int w, int h) {
    Mat matImage = captureDeviceScreenMat(x, y, w, h);
    BufferedImage bImage = null;
    if (matImage != null) {
      bImage = new BufferedImage(matImage.width(), matImage.height(), BufferedImage.TYPE_3BYTE_BGR);
      byte[] bImageData = ((DataBufferByte) bImage.getRaster().getDataBuffer()).getData();
      matImage.get(0, 0, bImageData);
    }
    return bImage;
  }

  public Mat captureDeviceScreenMat(int x, int y, int w, int h) {
    byte[] imagePrefix = new byte[12];
    byte[] image = new byte[0];
    int actW = w;
    if (x + w > devW) {
      actW = devW - x;
    }
    int actH = h;
    if (y + h > devH) {
      actH = devH - y;
    }
    Debug timer = Debug.startTimer();
    try {
      InputStream stdout = device.executeShell("screencap");
      stdout.read(imagePrefix);
      if (imagePrefix[8] != 0x01) {
        log(-1, "captureDeviceScreenMat: image type not RGBA");
        return null;
      }
      if (byte2int(imagePrefix, 0, 4) != devW || byte2int(imagePrefix, 4, 4) != devH) {
        log(-1, "captureDeviceScreenMat: width or height differ from device values");
        return null;
      }
      image = new byte[actW * actH * 4];
      int lenRow = devW * 4;
      byte[] row = new byte[lenRow];
      for (int count = 0; count < y; count++) {
        stdout.read(row);
      }
      for (int count = 0; count < actH; count++) {
        if (x > 0) {
          stdout.read(row);
          System.arraycopy(row, x * 4, image, count * actW * 4, actW * 4);
        } else {
          stdout.read(image, count * actW * 4, actW * 4);
        }
      }
      long duration = timer.end();
      log(lvl, "captureDeviceScreenMat:[%d,%d %dx%d] %d", x, y, actW, actH, duration);
    } catch (IOException | JadbException e) {
      log(-1, "captureDeviceScreenMat: [%d,%d %dx%d] %s", x, y, actW, actH, e);
    }
    Mat matOrg = new Mat(actH, actW, CvType.CV_8UC4);
    matOrg.put(0, 0, image);
    Mat matImage = new Mat();
    Imgproc.cvtColor(matOrg, matImage, Imgproc.COLOR_RGBA2BGR, 3);
    return matImage;
  }

  private int byte2int(byte[] bytes, int start, int len) {
    int val = 0;
    int fact = 1;
    for (int i=start; i < start + len; i++) {
      int b = bytes[i] & 0xff;
      val += b * fact;
      fact *= 256;
    }
    return val;
  }

  public Boolean isDisplayOn() {
    String dump = dumpsys("power");
    Pattern displayOn = Pattern.compile("Display Power: state=(..)");
    Matcher match = displayOn.matcher(dump);
    if (match.find()) {
      if (match.group(1).contains("ON")) {
        return true;
      }
      return false;
    } else {
      String token = "Display Power: state=OFF";
      log(-1, "isDisplayOn: dumpsys power: token not found: %s", token);
    }
    return null;
  }

  private Dimension getDisplayDimension() {
    String dump = dumpsys("display");
    String token = "mDefaultViewport= ... deviceWidth=1200, deviceHeight=1920}";
    Dimension dim = null;
    Pattern displayDimension = Pattern.compile("mDefaultViewport=.*?deviceWidth=(\\d*).*?deviceHeight=(\\d*)");
    Matcher match = displayDimension.matcher(dump);
    if (match.find()) {
      int w = Integer.parseInt(match.group(1));
      int h = Integer.parseInt(match.group(2));
      dim = new Dimension(w, h);
    } else {
      log(-1, "getDisplayDimension: dumpsys display: token not found: %s", token);
    }
    return dim;
  }

  public String dumpsys(String component) {
    InputStream stdout = null;
    String out = "";
    try {
      if (component == null || component.isEmpty()) {
        component = "power";
      }
      if (component.toLowerCase().contains("all")) {
        stdout = device.executeShell("dumpsys");
      } else {
        stdout = device.executeShell("dumpsys", component);
      }
      out = inputStreamToString(stdout, "UTF-8");
    } catch (IOException | JadbException e) {
      log(-1, "dumpsys: %s: %s", component, e);
    }
    return out;
  }

  public String printDump(String component) {
    String dump = dumpsys(component);
    if (!dump.isEmpty()) {
      System.out.println("***** Android device dump: " + component);
      System.out.println(dump);
    }
    return dump;
  }

  public String printDump() {
    String dump = dumpsys("all");
    if (!dump.isEmpty()) {
      File out = new File(RunTime.get().fSikulixStore, "android_dump_" + getDeviceSerial() + ".txt");
      System.out.println("***** Android device dump all services");
      System.out.println("written to file: " + out.getAbsolutePath());
      FileManager.writeStringToFile(dump, out);
    }
    return dump;
  }

  private static final int BUFFER_SIZE = 4 * 1024;

  private static String inputStreamToString(InputStream inputStream, String charsetName) {
    StringBuilder builder = new StringBuilder();
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(inputStream, charsetName);
      char[] buffer = new char[BUFFER_SIZE];
      int length;
      while ((length = reader.read(buffer)) != -1) {
        builder.append(buffer, 0, length);
      }
      return builder.toString();
    } catch (Exception e) {
      return "";
    }
  }

  public void wakeUp(int seconds) {
    int times = seconds * 4;
    try {
      device.executeShell("input", "keyevent", "26");
      while (0 < times--) {
        if (isDisplayOn()) {
          return;
        } else {
          RunTime.pause(0.25f);
        }
      }
    } catch (Exception e) {
      log(-1, "wakeUp: did not work: %s", e);
    }
    log(-1, "wakeUp: timeout: %d seconds", seconds);
  }

  public void inputKeyEvent(int key) {
    try {
      device.executeShell("input", "keyevent", Integer.toString(key));
    } catch (Exception e) {
      log(-1, "inputKeyEvent: %d did not work: %s", e.getMessage());
    }
  }

  public void tap(int x, int y) {
    try {
      device.executeShell("input tap", Integer.toString(x), Integer.toString(y));
    } catch (IOException | JadbException e) {
      log(-1, "tap: %s", e);
    }
  }

  public void swipe(int x1, int y1, int x2, int y2) {
    try {
      device.executeShell("input swipe", Integer.toString(x1), Integer.toString(y1),
              Integer.toString(x2), Integer.toString(y2));
    } catch (IOException | JadbException e) {
      log(-1, "swipe: %s", e);
    }
  }
}