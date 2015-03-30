package linkTFM;

import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import mvXML.DoubleWithErrStat;
import mvXML.ValAndPos;
import mvXML.XMLgroupStat;
import mvXML.XMLmv;
import mvmath.FramedPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/25/13
 * Time: 2:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class FceProfTFM {
    DFHeating dfHeating;
    int nTopSec, nBotSec;
    Vector<OneTFMsec> topTFMsecs, botTFMsecs;
    DFHFurnace furnace;
    boolean bTopBot = false;
    public LossParamsTFM lossParamsTFM;
    Vector<BeamParams> fixedBeams, movingBeams;
    int beamSections = 0;
    public FceProfTFM(DFHeating dfHeating) {
        this.dfHeating = dfHeating;
        topTFMsecs = new Vector<OneTFMsec>();
        botTFMsecs = new Vector<OneTFMsec>();
        nTopSec = 0;
        nBotSec = 0;
    }

/*
    public XMLgroupStat fceProfFromTFMOLD(String xmlStr, XMLgroupStat grpStat, DFHFurnace furnace) {
        this.furnace = furnace;
        this.bTopBot = furnace.bTopBot;
        lossParamsTFM = new LossParamsTFM(dfHeating);
        lossParamsTFM.paramsFromXML(xmlStr, grpStat);
        getBeamsData(xmlStr, grpStat);
//        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "topSections", 0);
        String xmlTop = vp.val;
        if (xmlTop.length() > 20)
            getTopOrBotSecs(xmlTop, grpStat, false);
        else
            grpStat.addStat(false, "   Top Section Profile NOT found");
        if (bTopBot) {
            vp = XMLmv.getTag(xmlStr, "botSections", 0);
            String xmlbot = vp.val;
            if (xmlTop.length() > 20)
                getTopOrBotSecs(xmlbot, grpStat, true);
            else
                grpStat.addStat(false, "   Bottom Section Profile NOT found\n");

        }
        grpStat.addStat(true, "Building DFHFurnace Profile\n");
        setFceProfile(grpStat);
        lossParamsTFM.mapTFMlosses(furnace, fixedBeams, movingBeams);
        return grpStat;
    }
*/

    public XMLgroupStat fceProfFromTFM(String xmlStr, XMLgroupStat grpStat, DFHFurnace furnace) {
        this.furnace = furnace;
        this.bTopBot = furnace.bTopBot;
        lossParamsTFM = new LossParamsTFM(dfHeating);
        lossParamsTFM.paramsFromXML(xmlStr, grpStat);
        getBeamsData(xmlStr, grpStat);
//        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        if (bTopBot) {
            vp = XMLmv.getTag(xmlStr, "botSections", 0);
            String xmlBot = vp.val;
            if (xmlBot.length() > 20)
                getTopOrBotSecs(xmlBot, grpStat, true);
            else
                grpStat.addStat(false, "   Bottom Section Profile NOT found");
        }
        vp = XMLmv.getTag(xmlStr, "topSections", 0);
        String xmlTop = vp.val;
        if (xmlTop.length() > 20)
            getTopOrBotSecs(xmlTop, grpStat, false);
        else
            grpStat.addStat(false, "   Top Section Profile NOT found\n");
        if (nBotSec < 2)  {
            bTopBot = false;
            furnace.changeFiringMode(bTopBot, false);
            dfHeating.setHeatingMode("TOP FIRED");
        }

        grpStat.addStat(true, "Building DFHFurnace Profile\n");
        setFceProfile(grpStat);
        lossParamsTFM.mapTFMlosses(furnace, fixedBeams, movingBeams);
        return grpStat;
    }

    void getBeamsData(String xmlStr, XMLgroupStat grpStat)  {
//        fixedBeams = new Vector<BeamParams>();
//        movingBeams = new Vector<BeamParams>();
        ValAndPos vp;
        DoubleWithErrStat dblWithStat;
        vp = XMLmv.getTag(xmlStr, "nLossSec", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "nLossSec", grpStat)).allOK)
            beamSections = (int)dblWithStat.val;
        if (beamSections > 0) {
            fixedBeams = new Vector<BeamParams>();
            movingBeams = new Vector<BeamParams>();
            String secName;
            ValAndPos oneSec;
            for (int sec = 0; sec < beamSections; sec++) {
                secName = "lossSec" + ("" + sec).trim();
                oneSec = XMLmv.getTag(xmlStr, secName, 0);
                if (oneSec.val.length() > 100) {
                    fixedBeams.add(new BeamParams(oneSec.val, sec, BeamParams.BeamType.FIXED, grpStat, dfHeating));
                    movingBeams.add(new BeamParams(oneSec.val, sec, BeamParams.BeamType.MOVING, grpStat, dfHeating));
                }
            }
        }
    }

    void getTopOrBotSecs(String xmlStr, XMLgroupStat grpStat, boolean bBot) {
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        String secLevel = (bBot) ? "BottomSections." : "TopSections.";
        String secPos;
        Vector<OneTFMsec> allSecs = getTFMSec(bBot);
        int nActiveSec = 0;
        String onSecStr;
        vp = XMLmv.getTag(xmlStr, "nActiveSec", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, secLevel + "nActiveSec", grpStat)).allOK)
            nActiveSec = (int)dblWithStat.val;
        for (int i = 0; i < nActiveSec; i++) {
            String subName = "s" + ("" + i).trim();
            secPos = secLevel + subName + ".";
            vp = XMLmv.getTag(xmlStr, subName, 0);
            onSecStr = vp.val;
            if (vp.val.length() > 20)
                allSecs.add(new OneTFMsec(onSecStr, i, grpStat, secPos, bBot));
            else
                grpStat.addStat(false, "Profile Data NOT found for " + secPos + "\n");
        }
        if (bBot)
            nBotSec = nActiveSec;
        else
            nTopSec = nActiveSec;
    }

    Vector <OneTFMsec> getTFMSec(boolean bBot) {
        return (bBot)? botTFMsecs : topTFMsecs;
    }

    boolean setFceProfile(XMLgroupStat grpStat)  {
        boolean bRetVal = true;
        bRetVal &= setTopOrBotProfile(topTFMsecs, false, grpStat);
        if (bTopBot)
            bRetVal &= setTopOrBotProfile(botTFMsecs, true, grpStat);
        dfHeating.adjustForLengthChange();
        furnace.evalEndLen();
        return bRetVal;
    }

    boolean setTopOrBotProfile(Vector<OneTFMsec> vSec, boolean bBot, XMLgroupStat grpStat) {
        boolean bRetVal = true;
        int nSec = vSec.size();
        String profLevel = (bBot) ? "BotProfile." : "TopProfile.";
        if (nSec > 0) {
            if (nSec <= DFHFurnace.MAXSECTIONS) {
                for (OneTFMsec sec:vSec)
                    bRetVal &= sec.setOneSec(furnace, profLevel, grpStat);
            }
            else {
                grpStat.addStat(false, "   " + profLevel + "Too Many Sections [" + nSec + ">" + DFHFurnace.MAXSECTIONS + "]");
                bRetVal = false;
            }
        }
        else {
            grpStat.addStat(false, "   " + profLevel + "No Data");
            bRetVal = false;
        }
        furnace.evalActiveSecs(bBot);
        return bRetVal;
    }

    public JPanel lossPramsPanel() {
        return lossParamsTFM.getLossParamsP();
    }

    public JPanel beamsDataPanel() {
        JPanel outerP = new FramedPanel(new GridBagLayout()) ;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        outerP.add(BeamParams.colHeader(), gbc);
        gbc.gridx++;
        BeamParams one;
        for (int sec = 0; sec < beamSections; sec++) {
            FramedPanel jp = new FramedPanel(new GridBagLayout());
            GridBagConstraints gbcD = new GridBagConstraints();
            gbcD.gridx = 0;
            gbcD.gridy = 0;
            one = fixedBeams.get(sec);
            jp.add(one.commDataP(), gbcD);
            gbcD.gridy++;
            jp.add(one.getBeamsDataP(), gbcD);
            gbcD.gridy++;
            jp.add(one.getPostDataP(), gbcD);
            one = movingBeams.get(sec);
            gbcD.gridy++;
            jp.add(one.getBeamsDataP(), gbcD);
            gbcD.gridy++;
            jp.add(one.getPostDataP(), gbcD);
            outerP.add(jp, gbc);
            gbc.gridx++;
        }
        return outerP;
    }

  }
