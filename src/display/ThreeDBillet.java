
/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2005
 * Company:
 * @author
 * @version 1.0
 */
package display;

import basic.TemperatureColorServer;
import basic.TemperatureStats;
import basic.ThreeDCharge;
import basic.TimeServer;
import com.sun.j3d.utils.behaviors.vp.*;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.*;

import javax.media.j3d.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ThreeDBillet extends JFrame implements ChangeListener {
  ThreeDCharge theObject;
  TemperatureStats stats;
  SimpleUniverse u = null;
  ChargeSurfaceDisply billet = null;
//  Selector timeSelector;
  TemperatureColorServer colorServer;
  TimeServer timeServer;


  public ThreeDBillet(String name,ThreeDCharge theObject, TemperatureStats tempStats,
                      TemperatureColorServer colServer, TimeServer tServer) {
    super(name);
    this.theObject = theObject;
    stats = tempStats;
    colorServer = colServer;
    timeServer = tServer;
    try  {
      jbInit();
      setSize(200, 150);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  double xSize;
  double ySize;
  double zSize;

  Canvas3D c3d;

  TempColorSelector colSel;
  ViewingPlatform viewPlatform;

  boolean jbInit() throws Exception {
    JMenuBar menuBar1 = new JMenuBar();
    JButton menuReset = new JButton("Reset View");
    menuReset.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        resetView();
      }
    });
//    timeSelector = new Selector("Time", false, false);
//    timeSelector.setValue((int)(100));
//    timeSelector.addChangeListener(new TimeChangeListener());
    menuBar1.add(menuReset);
//    menuBar1.add(timeSelector);
    setJMenuBar(menuBar1);
    xSize = theObject.getXsize();
    ySize = theObject.getYsize();
    zSize = theObject.getZsize();
    JPanel mainP = new JPanel(new BorderLayout()); //(new GridLayout(1, 2));
    getContentPane().add(mainP, BorderLayout.CENTER);

// 3D display
    GraphicsConfiguration config =
          SimpleUniverse.getPreferredConfiguration();

    c3d = new Canvas3D(config);
    c3d.setSize(700, 300);
    mainP.add(c3d, BorderLayout.CENTER);
    BranchGroup scene = createSceneGraph();
    u = new SimpleUniverse(c3d);

    viewPlatform = u.getViewingPlatform();

    viewPlatform.setNominalViewingTransform(); // stand back to view
    Transform3D tempTrans = new Transform3D();
    double diagonal = Math.sqrt(xSize * xSize + ySize * ySize + zSize * zSize);
    tempTrans.setTranslation(new Vector3d(0, 0, diagonal * 0.05));
//    tempTrans.setTranslation(new Vector3d(0, 0, (Math.max(xSize, zSize) + ySize) * 0.03));
    viewPlatform.getViewPlatformTransform().setTransform(tempTrans);

    OrbitBehavior orbit = new OrbitBehavior(c3d, OrbitBehavior.REVERSE_ALL |
                  OrbitBehavior.DISABLE_TRANSLATE);
    BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
    orbit.setSchedulingBounds(bounds);
    viewPlatform.setViewPlatformBehavior(orbit);

    u.addBranchGraph(scene);

    colSel = new TempColorSelector();
    mainP.add(colSel, BorderLayout.EAST);
    return true;
  }

  TransformGroup objScale;
  Transform3D t3d;
  TransformGroup objTrans;

  public BranchGroup createSceneGraph() {
    BranchGroup objRoot = new BranchGroup();
    // Create a Transformgroup to scale all objects so they
    // appear in the scene.
    objScale = new TransformGroup();
    t3d = new Transform3D();
    Transform3D tempRotate = new Transform3D();
    t3d.rotX(Math.PI / 10.0d);
    tempRotate.rotY(Math.PI / 6.0d);
    t3d.mul(tempRotate);
    objScale.setTransform(t3d);
    objRoot.addChild(objScale);

    objTrans = new TransformGroup();
    //write-enable for behaviors
    objTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    objTrans.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
    objTrans.setCapability(TransformGroup.ENABLE_PICK_REPORTING);

    objScale.addChild(objTrans);

// create the charge display group
    billet = new ChargeSurfaceDisply(theObject, stats, colorServer);
    // add it to the scene graph.

    objTrans.addChild(billet);
    Transform3D trnew = new Transform3D();
//    trnew.setTranslation(new Vector3d(0.0, 0.0, -0.5));
//    objTrans.setTransform(trnew);

    BoundingSphere bounds =
      new BoundingSphere(new Point3d(0.0,0.0,0.0), 100);

    // Let Java 3D perform optimizations on this scene graph.
    objRoot.compile();
    return objRoot;

  }

  void resetView() {
    Transform3D tempTrans = new Transform3D();
    tempTrans.setTranslation(new Vector3d(0, 0, (Math.max(xSize, zSize) + ySize) * 0.03));
    viewPlatform.getViewPlatformTransform().setTransform(tempTrans);
  }

  public void stateChanged(ChangeEvent e) {
    billet.setTime(timeServer.getTime() * stats.lastTimePoint());
    update();
  }

//  void noteTimeChange() {
//    billet.setTimeFraction((double)timeSelector.getValue()/ 100);
//    update();
//  }
//
  public void update() {
    billet.update();
  }

//-------------------------
//   class TimeChangeListener implements ChangeListener {
//     public void stateChanged(ChangeEvent ce) {
//       noteTimeChange();
//     }
//
//     public void addFollowers(Selector sel) {
//
//     }
//   }

// --------------------------------------
  void errMessage(String msg) {
    System.err.println("ThreeDBillet: ERROR: " + msg);
  }

  void debug(String msg) {
    System.out.println("ThreeDBillet: " + msg);
  }
}

