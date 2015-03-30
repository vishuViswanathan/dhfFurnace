package display;

import javax.media.j3d.*;
import com.sun.j3d.utils.geometry.*;
import basic.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.behaviors.vp.*;
import com.sun.j3d.utils.universe.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.image.BufferedImage;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class ChargeSurfaceDisply
    extends Group {
  ThreeDCharge charge;
  TemperatureStats stats;
  int chargeType;
  double xSize, ySize, zSize;
  SurfaceImage[] surfaces;
  TemperatureColorServer colorServer;
  int surfacesN = 0;
  double timeFraction = 1.0;
  double time = 10000; // set high so that it shows the end-result-so-far
  public ChargeSurfaceDisply() {
  }

  public ChargeSurfaceDisply(ThreeDCharge theCharge, TemperatureStats tempStats,
                             TemperatureColorServer colorServer) {
    charge = theCharge;
    stats = tempStats;
    this.colorServer = colorServer;
    chargeType = charge.chargeDef.chargeType;
    initDisplay();
  }

  void initDisplay() {
    xSize = charge.getXsize();
    ySize = charge.getYsize();
    zSize = charge.getZsize();
    switch (charge.chargeDef.chargeType) {
      case ChargeDef.RECTANGULAR:
        Box billet = new Box( (float) xSize / 100, (float) zSize / 100,
                             (float) ySize / 100, Box.GENERATE_NORMALS |
                             Box.GENERATE_TEXTURE_COORDS |
                             Box.ENABLE_APPEARANCE_MODIFY, null, 1);
        addChild(billet);
        surfacesN = 6;
        surfaces = new SurfaceImage[surfacesN];
        surfaces[0] = new SurfaceImage(3, billet.getShape(Box.TOP));
        surfaces[1] = new SurfaceImage(1, billet.getShape(Box.BOTTOM));
        surfaces[2] = new SurfaceImage(21, billet.getShape(Box.LEFT));
        surfaces[3] = new SurfaceImage(22, billet.getShape(Box.RIGHT));
        surfaces[4] = new SurfaceImage(4, billet.getShape(Box.FRONT));
        surfaces[5] = new SurfaceImage(2, billet.getShape(Box.BACK));

        break;
      case ChargeDef.BEAMBLANK_H:
        double flangeT = charge.flangeTint;
        double webT = charge.webTint;
        Box leftFlange = new Box( (float) xSize / 100, (float) zSize / 100,
                                 (float) flangeT / 100, Box.GENERATE_NORMALS |
                                 Box.GENERATE_TEXTURE_COORDS |
                                 Box.ENABLE_APPEARANCE_MODIFY, null, 1);
        Transform3D leftFlTr = new Transform3D();
        leftFlTr.setTranslation(new Vector3d(0, 0, - (ySize - flangeT) / 100));
        // The trasnfer is NOT CLEAR as to why it works
        TransformGroup trGrLF = new TransformGroup(leftFlTr);
        trGrLF.addChild(leftFlange);
        addChild(trGrLF);
        Box rightFlange = new Box( (float) xSize / 100, (float) zSize / 100,
                                  (float) flangeT / 100, Box.GENERATE_NORMALS |
                                  Box.GENERATE_TEXTURE_COORDS |
                                  Box.ENABLE_APPEARANCE_MODIFY, null, 1);
        Transform3D rightFlTr = new Transform3D();
        rightFlTr.setTranslation(new Vector3d(0, 0, (ySize - flangeT) / 100));
        // The trasnfer is NOT CLEAR as to why it works
        TransformGroup trGrRF = new TransformGroup(rightFlTr);
        trGrRF.addChild(rightFlange);
        addChild(trGrRF);

        Box web = new Box( (float) xSize / 100, (float) webT / 100,
                          (float) (ySize - 2 * flangeT) / 100,
                          Box.GENERATE_NORMALS |
                          Box.GENERATE_TEXTURE_COORDS |
                          Box.ENABLE_APPEARANCE_MODIFY, null, 1);
        Transform3D webTr = new Transform3D();
        webTr.setTranslation(new Vector3d(0, 0, 0));
        TransformGroup trGrWeb = new TransformGroup(webTr);
        trGrWeb.addChild(web);
        addChild(trGrWeb);

        surfacesN = 18;
        surfaces = new SurfaceImage[surfacesN];
        surfaces[0] = new SurfaceImage(7, leftFlange.getShape(Box.TOP));
        surfaces[1] = new SurfaceImage(5, leftFlange.getShape(Box.BOTTOM));
        surfaces[2] = new SurfaceImage(13, leftFlange.getShape(Box.LEFT));
        surfaces[3] = new SurfaceImage(16, leftFlange.getShape(Box.RIGHT));
        surfaces[4] = new SurfaceImage(4, leftFlange.getShape(Box.FRONT));
        surfaces[5] = new SurfaceImage(6, leftFlange.getShape(Box.BACK));

        surfaces[6] = new SurfaceImage(11, rightFlange.getShape(Box.TOP));
        surfaces[7] = new SurfaceImage(1, rightFlange.getShape(Box.BOTTOM));
        surfaces[8] = new SurfaceImage(15, rightFlange.getShape(Box.LEFT));
        surfaces[9] = new SurfaceImage(18, rightFlange.getShape(Box.RIGHT));
        surfaces[10] = new SurfaceImage(12, rightFlange.getShape(Box.FRONT));
        surfaces[11] = new SurfaceImage(2, rightFlange.getShape(Box.BACK));

        surfaces[12] = new SurfaceImage(9, web.getShape(Box.TOP));
        surfaces[13] = new SurfaceImage(3, web.getShape(Box.BOTTOM));
        surfaces[14] = new SurfaceImage(14, web.getShape(Box.LEFT));
        surfaces[15] = new SurfaceImage(17, web.getShape(Box.RIGHT));
        surfaces[16] = new SurfaceImage(0, web.getShape(Box.FRONT));
        surfaces[17] = new SurfaceImage(0, web.getShape(Box.BACK));
        break;
      case ChargeDef.BEAMBLANK_V:
        surfacesN = 12;
        surfaces = new SurfaceImage[surfacesN];
        break;
    }
  }

  public void setTime(double theTime) {
    time = theTime;
//    timeFraction = fraction;
//    time = timeFraction * stats.lastTimePoint();
  }

  public void update() {
    for (int n = 0; n < surfacesN; n++) {
      surfaces[n].setApperance();
    }
  }

  static int imWidthRecom = 256;
  static int imHeightRecom = 256;

  class SurfaceImage {
    BufferedImage buffer;
    Graphics2D g2;
    TemperatureStats data; //ThreeDArray data;
    Shape3D shape;
    int orient;
    int unitw, unith;
    int highWcount, highHcount;
    int xmin = 1, ymin = 1;
    int xmax, ymax;
    int flangeT;
    int webT;
    int layer;
    int imWidth = imWidthRecom;
    int imHeight = imHeightRecom;
    public SurfaceImage(int orient, Shape3D shape) {
      data = stats; // charge;
      flangeT = charge.flangeTint;
      webT = charge.webTint;
      this.orient = orient;
      this.shape = shape;
//      Appearance app = this.shape.getAppearance();
//      app.setCapability(Appearance.ALLOW_TEXTURE_WRITE);
      switch (chargeType) {
        case ChargeDef.RECTANGULAR:
          surfaceOfRect();
          break;
        case ChargeDef.BEAMBLANK_H:
          surfaceOfHBB();
          break;
        case ChargeDef.BEAMBLANK_V:
          surfaceOfVBB();
          break;
      }
      highWcount = imWidth - (xmax - xmin + 1) * unitw - 1; // -1 for border visibility
      highHcount = imHeight - (ymax - ymin + 1) * unith - 1;
      buffer = new BufferedImage(imWidth, imHeight, BufferedImage.TYPE_INT_RGB);
      g2 = buffer.createGraphics();
 // added on 20080616
g2.setComposite(AlphaComposite.Src);
    }

    void surfaceOfRect() {
      switch (orient) {
        case 3:
          xmax = data.getXsize() - 2;
          ymax = data.getYsize() - 2;
          layer = data.getZsize() - 2;
          break;
        case 1:
          xmax = data.getXsize() - 2;
          ymax = data.getYsize() - 2;
          layer = 1;
          break;
        case 21:
          xmax = data.getYsize() - 2;
          ymax = data.getZsize() - 2;
          layer = 1;
          break;
        case 22:
          xmax = data.getYsize() - 2;
          ymax = data.getZsize() - 2;
          layer = data.getXsize() - 2;
          break;
        case 4:
          xmax = data.getXsize() - 2;
          ymax = data.getZsize() - 2;
          layer = 1;
          break;
        case 2:
          xmax = data.getXsize() - 2;
          ymax = data.getZsize() - 2;
          layer = data.getYsize() - 2;
          break;
      }
      while (imWidth < (xmax - xmin + 1) * 2) {
        imWidth *= 2;
      }
      unitw = imWidth / (xmax - xmin + 1);
      while (imHeight < (ymax - ymin + 1) * 2) {
        imHeight *= 2;
      }
      unith = imHeight / (ymax - ymin + 1);
      unith = imHeight / (ymax - ymin + 1);
    }

    void surfaceOfHBB() {
      boolean done = true;
      switch (orient) {
        case 1:
          layer = 1;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = flangeT;
          break;
        case 2: // same for 10
          layer = flangeT;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 3:
          layer = charge.webMinCell;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = flangeT + 1;
          ymax = data.getYsize() - 2 - flangeT;
          break;
        case 4: // same for 8
          layer = data.getYsize() - 1 - flangeT;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 5:
          layer = 1;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = data.getYsize() - 1 - flangeT;
          ymax = data.getYsize() - 2;
          break;
        case 6:
          layer = data.getYsize() - 2;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 7:
          layer = data.getZsize() - 2;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = data.getYsize() - 1 - flangeT;
          ymax = data.getYsize() - 2;
          break;
        case 9:
          layer = charge.webMaxCell;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = flangeT + 1;
          ymax = data.getYsize() - 2 - flangeT;
          break;
        case 11:
          layer = data.getZsize() - 2;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = flangeT;
          break;
        case 12:
          layer = 1;
          xmin = 1;
          xmax = data.getXsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 13:
          layer = 1;
          xmin = data.getYsize() - 1 - flangeT;
          xmax = data.getYsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 14:
          layer = 1;
          xmin = flangeT + 1;
          xmax = data.getYsize() - 2 - flangeT;
          ymin = (data.getZsize() - 2 - webT) / 2 + 1;
          ymax = ymin + webT - 1;
          break;
        case 15:
          layer = 1;
          xmin = 1;
          xmax = flangeT;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 16:
          layer = data.getXsize() - 2;
          xmin = data.getYsize() - 1 - flangeT;
          xmax = data.getYsize() - 2;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        case 17:
          layer = data.getXsize() - 2;
          xmin = flangeT + 1;
          xmax = data.getYsize() - 2 - flangeT;
          ymin = (data.getZsize() - 2 - webT) / 2 + 1;
          ymax = ymin + webT - 1;
          break;
        case 18:
          layer = data.getXsize() - 2;
          xmin = 1;
          xmax = flangeT;
          ymin = 1;
          ymax = data.getZsize() - 2;
          break;
        default:
          done = false;
          break;
      }
      if (!done)
        debug("Strange Orient " + orient + ", xmax = " + xmax + ", xmin = " +
              xmin);
      if (done) {
        while (imWidth < (xmax - xmin + 1) * 2) {
          imWidth *= 2;
        }
        unitw = imWidth / (xmax - xmin + 1);
        while (imHeight < (ymax - ymin + 1) * 2) {
          imHeight *= 2;
        }
        unith = imHeight / (ymax - ymin + 1);
      }
    }

    void surfaceOfVBB() {

    }

    void updateBufferRect() {
      int atX;
      int atY;
      int x, y;
      float temp;
      int w = unitw + 1;
      int h = unith + 1;
      switch (orient) {
        case 3:
          atY = 0;
          y = (ymax - ymin + 1);
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, row, layer);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 1:
          atY = 0;
          y = 1;
          for (int row = ymin; row <= ymax; row++, y++) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, row, layer);
//              temp = (float) data.getTemperatureDataAt(col, row, layer);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 21:
          atY = 0;
          y = (ymax - ymin + 1);
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = xmax - xmin + 1;
            for (int col = xmax; col >= xmin; col--, x--) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, layer, col, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 22:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, layer, col, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 4:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, layer, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          g2.setColor(Color.white);
          g2.fillRect(0, (atY - unith), w, h);

          break;
        case 2:
          atY = 0;
          y = 1;
          for (int row = ymax; row >= ymin; row--, y++) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = (xmax - xmin + 1);
            for (int col = xmax; col >= xmin; col--, x--) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, layer, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
      }
    }

    void updateBufferHBB() {
      int atX;
      int atY;
      float temp;
      int w = unitw + 1;
      int h = unith + 1;
      int x, y;
      switch (orient) {
        case 0:
          break;
        case 12:
        case 4:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, layer, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          if (orient == 12) {
            g2.setColor(Color.white);
            g2.fillRect(0, (atY - unith), w, h);
          }
          break;
        case 2:
        case 6:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = xmax - xmin + 1;
            for (int col = xmax; col >= xmin; col--, x--) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, layer, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 1:
        case 5:
        case 3:
          atY = 0;
          y = 1;
          for (int row = ymin; row <= ymax; row++, y++) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, row, layer);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 11:
        case 7:
        case 9:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, col, row, layer);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 13:
        case 14:
        case 15:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = xmax - xmin + 1;
            for (int col = xmax; col >= xmin; col--, x--) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, layer, col, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
        case 16:
        case 17:
        case 18:
          atY = 0;
          y = ymax - ymin + 1;
          for (int row = ymax; row >= ymin; row--, y--) {
            h = (y > highHcount) ? unith : unith + 1;
            atX = 0;
            x = 1;
            for (int col = xmin; col <= xmax; col++, x++) {
              w = (x > highWcount) ? unitw : unitw + 1;
              temp = (float) data.getTemperatureDataAt(time, layer, col, row);
              g2.setColor(colorServer.getTemperatureColor(temp));
              g2.fillRect(atX, atY, w, h);
              atX += w;
            }
            atY += h;
          }
          break;
      }
    }

    void updateBufferVBB() {

    }

     ImageComponent2D grayImage;


    public void setApperance() {
      switch (chargeType) {
        case ChargeDef.RECTANGULAR:
          updateBufferRect();
          break;
        case ChargeDef.BEAMBLANK_H:
          updateBufferHBB();
          break;
        case ChargeDef.BEAMBLANK_V:
          updateBufferVBB();
          break;
      }
// added on 20091028
        if (grayImage == null) {
            grayImage = new ImageComponent2D(ImageComponent.
                        FORMAT_RGB, buffer);
            grayImage.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
        }
        else     {
            grayImage.set(buffer);
        }
//----

// was             ImageComponent2D grayImage = new ImageComponent2D(ImageComponent.FORMAT_RGB, buffer);

      Texture2D tex = new Texture2D(Texture.BASE_LEVEL, Texture.RGB,
                                    imWidthRecom, imHeightRecom);
      tex.setImage(0, grayImage);
      Appearance ap = new Appearance();
      ap.setTexture(tex);
//      Appearance oldApp = shape.getAppearance();
//      oldApp.setTexture(tex);
      shape.setAppearance(ap);
    }
  }

  void debug(String msg) {
    System.out.print("ChargeSurface Display:  " + msg + "\n");
  }

}