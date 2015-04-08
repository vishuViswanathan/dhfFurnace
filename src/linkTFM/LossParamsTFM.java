package linkTFM;

import basic.LossType;
import basic.LossTypeList;
import directFiredHeating.DFHFurnace;
import directFiredHeating.DFHeating;
import mvUtils.display.MultiPairColPanel;
import mvUtils.display.NumberTextField;
import mvUtils.mvXML.DoubleWithErrStat;
import mvUtils.mvXML.ValAndPos;
import mvUtils.mvXML.XMLgroupStat;
import mvUtils.mvXML.XMLmv;
import mvUtils.display.FramedPanel;
import mvUtils.math.SPECIAL;

import javax.swing.*;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: M Viswanathan
 * Date: 3/24/13
 * Time: 10:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class LossParamsTFM {

    void getBeamPos(String beamType, String xmlStr, Vector<Double> beamPos, XMLgroupStat grpStat) {
        int size = beamPos.size();
        String posID;
        ValAndPos vp;
        DoubleWithErrStat dblWithStat;
        for (int i = 0; i < size; i++) {
            posID = "pos" + ("" + i).trim();
            vp = XMLmv.getTag(xmlStr, posID, 0);
            if ((dblWithStat = new DoubleWithErrStat(vp.val, beamType + posID, grpStat)).allOK)
                beamPos.add(i, dblWithStat.val);
        }
    }

    int nChargingRolls, nDischargingRolls;
    int nFingersKickoff, nKickoff;
    int nPeelbar;
    double porteChargingSurface;
    double porteDischargingSurface;
    int nCooledBeamsBilletExtractor;
    double cooledBeamsScreenWallsSurface;
    double billetTurningDeviceSurface;
    double cooledBeamsDoorFramesChargingSurface;
    double cooledBeamsDoorFramesDischargingSurface;
    double chargingDoorOpenTime = 0, dischargingDoorOpenTime = 0; // im minutes

    DFHeating control;

    public LossParamsTFM(DFHeating control) {
        this.control = control;
        nChargingRolls = 0;
        nDischargingRolls = 0;
        nFingersKickoff = 0;
        nKickoff = 0;
        nPeelbar = 0;
    }

    public XMLgroupStat paramsFromXML(String xmlStr, XMLgroupStat grpStat) {
        DoubleWithErrStat dblWithStat;
        ValAndPos vp;
        vp = XMLmv.getTag(xmlStr, "nChargingRolls", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "nChargingRolls", grpStat)).allOK)
            nChargingRolls = (int)dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "nDischargingRolls", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "nDischargingRolls", grpStat)).allOK)
            nDischargingRolls = (int)dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "nFingersKickoff", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "nFingersKickoff", grpStat)).allOK)
            nFingersKickoff = (int)dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "Kickoff", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "Kickoff", grpStat)).allOK)
            nKickoff = (int)dblWithStat.val;
        vp = XMLmv.getTag(xmlStr, "Peelbar", 0);
        if ((dblWithStat = new DoubleWithErrStat(vp.val, "Peelbar", grpStat)).allOK)
            nPeelbar = (int)dblWithStat.val;

//        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "holesSurface", grpStat)).allOK)
//            holesSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "porteChargingSurface", grpStat)).allOK)
            porteChargingSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "porteDischargingSurface", grpStat)).allOK)
            porteDischargingSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "chargingDoorOpenTime", grpStat)).allOK)
            chargingDoorOpenTime = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "dischargingDoorOpenTime", grpStat)).allOK)
            dischargingDoorOpenTime = dblWithStat.val;
//        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "CooledBeamsBilletExtractorNumero", grpStat)).allOK)
//            nCooledBeamsBilletExtractor = (int)dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "CooledBeamsScreenWallsSurface", grpStat)).allOK)
            cooledBeamsScreenWallsSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "billetTurningDeviceSurface", grpStat)).allOK)
            billetTurningDeviceSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "cooledBeamsDoorFramesChargingSurface", grpStat)).allOK)
            cooledBeamsDoorFramesChargingSurface = dblWithStat.val;
        if ((dblWithStat = XMLmv.getDoubleWithErrStat(xmlStr, "cooledBeamsDoorFramesDischargingSurface", grpStat)).allOK)
            cooledBeamsDoorFramesDischargingSurface = dblWithStat.val;
        return grpStat;
    }

    MultiPairColPanel lossParamsP;
    NumberTextField ntNChargingRolls, ntNDischargingRolls, ntNFingersKickoff;
    NumberTextField ntNKickoff, ntNPeelbar, ntHolesSurface;
    NumberTextField ntPorteChargingSurface, ntPorteDischargingSurface;
    NumberTextField ntchargingDoorOpenTime, ntdischargingDoorOpenTime;
    NumberTextField ntCooledBeamsScreenWallsSurface;
    NumberTextField ntBilletTurningDeviceSurface;
    NumberTextField ntCooledBeamsDoorFramesChargingSurface, ntCooledBeamsDoorFramesDischargingSurface;
    int datW = 80;
    int labW = 300;
    public JPanel getLossParamsP() {
        FramedPanel outerP = new FramedPanel();
        lossParamsP = new MultiPairColPanel("Loss Parameters from TFM", labW, datW);
        ntNChargingRolls = new NumberTextField(control, nChargingRolls, 6,false, 0, 100, "#,###", "Number of Charging Rolls" );
        lossParamsP.addItemPair(ntNChargingRolls, false, false);
        ntNDischargingRolls = new NumberTextField(control, nDischargingRolls, 6,false, 0, 100, "#,###", "Number of Discharging Rolls" );
        lossParamsP.addItemPair(ntNDischargingRolls, false, false);
        ntNFingersKickoff = new NumberTextField(control, nFingersKickoff, 6,false, 0, 100, "#,###", "Number of Kickoff Fingers" );
        lossParamsP.addItemPair(ntNFingersKickoff, false, false);
        ntNKickoff = new NumberTextField(control, nKickoff, 6,false, 0, 100, "#,###", "Kickoff" );
        lossParamsP.addItemPair(ntNKickoff, false, false);
        ntNPeelbar = new NumberTextField(control, nPeelbar, 6,false, 0, 100, "#,###", "Number of Peel Bars" );
        lossParamsP.addItemPair(ntNPeelbar, false, false);
        ntPorteChargingSurface = new NumberTextField(control, porteChargingSurface, 6,false, 0, 100, "#,##0.00", "Charging Door Area (m2)" );
        lossParamsP.addItemPair(ntPorteChargingSurface, false, false);
        ntPorteDischargingSurface = new NumberTextField(control, porteDischargingSurface, 6,false, 0, 100, "#,##0.00", "Discharging Door Area (m2)" );
        lossParamsP.addItemPair(ntPorteDischargingSurface, false, false);

        ntchargingDoorOpenTime = new NumberTextField(control, chargingDoorOpenTime, 6,false, 0, 60, "#,##0.00", "Charging Door openTime (minutes/h)" );
        lossParamsP.addItemPair(ntchargingDoorOpenTime, false, false);
        ntdischargingDoorOpenTime = new NumberTextField(control, dischargingDoorOpenTime, 6,false, 0, 60, "#,##0.00", "Discharging Door openTime (minutes/h)" );
        lossParamsP.addItemPair(ntdischargingDoorOpenTime, false, false);

        ntCooledBeamsScreenWallsSurface = new NumberTextField(control, cooledBeamsScreenWallsSurface, 6,false, 0, 100, "#,##0.00", "Area of Cooled Screen Wall (m2)" );
        lossParamsP.addItemPair(ntCooledBeamsScreenWallsSurface, false, false);
        ntBilletTurningDeviceSurface = new NumberTextField(control, billetTurningDeviceSurface, 6,false, 0, 100, "#,##0.00", "Area of Billet Turning device(m2)" );
        lossParamsP.addItemPair(ntBilletTurningDeviceSurface, false, false);
        ntCooledBeamsDoorFramesChargingSurface = new NumberTextField(control, cooledBeamsDoorFramesChargingSurface, 6,false, 0, 100, "#,##0.00", "Area of Cooled Charging Door Frame (m2)" );
        lossParamsP.addItemPair(ntCooledBeamsDoorFramesChargingSurface, false, false);
        ntCooledBeamsDoorFramesDischargingSurface = new NumberTextField(control, cooledBeamsDoorFramesDischargingSurface, 6,false, 0, 100, "#,##0.00", "Area of Cooled Discharging Door Frame (m2)" );
        lossParamsP.addItemPair(ntCooledBeamsDoorFramesDischargingSurface, false, false);
        outerP.add(lossParamsP);
        return outerP;
    }

    DFHFurnace furnace;

    boolean mapTFMlosses(DFHFurnace furnace, Vector<BeamParams> fixedBeams, Vector<BeamParams> movingBeams) {
        this.furnace = furnace;
        LossTypeList lossTypeList = furnace.lossTypeList;
        lossTypeList.resetList();
        Iterator<Integer> allKeys = lossTypeList.keysIter();
        setWallRoofLosses(allKeys);
        if (furnace.bTopBot)
            setSkidLosses(fixedBeams, movingBeams, allKeys);
        setSlotLosses(movingBeams, allKeys);
        setChRollLosses(allKeys);
//        setDischRollLosses(allKeys);
        setDischRollLosses(allKeys);
        setDoorLosses(allKeys);
        setKickOffLosses(allKeys);
        setBilletExtractorLosses(allKeys);
        lossTypeList.informListeners();
        return true;
    }

    boolean setWallRoofLosses(Iterator<Integer> lossTypeKeys)  {
        LossType.LossBasis basis;
        LossType.TempAction tempAct;
        double factor = 0;
        int lNum;
        // Wall losses
        basis = LossType.LossBasis.WALL;
        tempAct = LossType.TempAction.LINEAR;
        factor = 1.4;
        lNum = addToLossType("Lateral Walls", factor, basis, tempAct,  lossTypeKeys);
        if (lNum > 0) {
            furnace.assignLoss(false, lNum, 1.0);
            furnace.assignLoss(true, lNum, 1.0);
        }
        // Charging  End Wall losses
        basis = LossType.LossBasis.CHENDWALL;
        tempAct = LossType.TempAction.LINEAR;
        factor = 1.4;
        lNum = addToLossType("Charging End Wall", factor, basis, tempAct,  lossTypeKeys);
        if (lNum > 0) {
            furnace.assignLoss(true, false,false, lNum, 1.0);
            furnace.assignLoss(true, false,true, lNum, 1.0);
        }
        // Disch End Wall losses
        basis = LossType.LossBasis.DISCHENDWALL;
        tempAct = LossType.TempAction.LINEAR;
        factor = 1.4;
        lNum = addToLossType("Discharging End Wall", factor, basis, tempAct,  lossTypeKeys);
        if (lNum > 0) {
            furnace.assignLoss(false, true,false, lNum, 1.0);
            furnace.assignLoss(false, true,true, lNum, 1.0);
        }
        // Roof losses
        basis = LossType.LossBasis.ROOF;
        tempAct = LossType.TempAction.LINEAR;
        factor = 2.2;
        lNum = addToLossType("Roof", factor, basis, tempAct,  lossTypeKeys);
        if (lNum > 0)
            furnace.assignLoss(false, lNum, 1.0); //only for top Sections

        // Hearth losses
        basis = LossType.LossBasis.HEARTH;
        tempAct = LossType.TempAction.LINEAR;
        factor = 1.15;
        lNum = addToLossType("Hearth", factor, basis, tempAct,  lossTypeKeys);
        if (lNum > 0)  {
            if (furnace.bTopBot)
                furnace.assignLoss(true, lNum, 1.0); // for bottom sections
            else
                furnace.assignLoss(false, lNum, 1.0); // for top sections
        }
        return true;
    }

    boolean setSlotLosses(Vector<BeamParams> movingBeams, Iterator<Integer> lossTypeKeys) {
        boolean retVal = false;
        LossType.LossBasis basis = LossType.LossBasis.LENGTH;
        LossType.TempAction tempAct = LossType.TempAction.POW4;
        int movingBeamSecs = movingBeams.size();
        double lossPerT4, factor;
        BeamParams oneParam;
        double stPos = 0, endPos = 0, length;
        int lNum;
        if (movingBeamSecs > 0) {
            for (int s = 0; s < movingBeamSecs; s++) {
                oneParam = movingBeams.get(s);
                lossPerT4 = oneParam.totalSlotsLossPerT4();
                if (lossPerT4 > 0) {
                    length = oneParam.secLength;
                    factor = lossPerT4 / length;
                    endPos = stPos + length;
                    lNum = addToLossType("Moving Posts Slots Sec#" + (s + 1), factor, basis, tempAct, lossTypeKeys);
                    if (lNum > 0)
                        furnace.assignLoss(true, stPos, endPos, lNum, 1.0);
                }
                stPos = endPos;
            }
            retVal = true;
        }
        else  {
            for (int s = 0; s < movingBeamSecs; s++) {
                oneParam = movingBeams.get(s);
                lossPerT4 = oneParam.totalWHSlotsLossPerT4();
                if (lossPerT4 > 0) {
                    length = oneParam.secLength;
                    factor = lossPerT4 / length;
                    endPos = stPos + length;
                    lNum = addToLossType("Walking Hearth Slots Sec#" + (s + 1), factor, basis, tempAct, lossTypeKeys);
                    if (lNum > 0)
                        furnace.assignLoss(false, stPos, endPos, lNum, 1.0);
                }
                stPos = endPos;
            }
            retVal = true;
        }
        return retVal;
    }

    boolean setSkidLosses(Vector<BeamParams> fixedBeams, Vector<BeamParams> movingBeams, Iterator<Integer> lossTypeKeys)  {
        boolean retVal = false;
        LossType.LossBasis basis = LossType.LossBasis.LENGTH;
        LossType.TempAction tempAct = LossType.TempAction.LINEAR;
        int fixedBeamSecs = fixedBeams.size();
        int movingBeamSecs = movingBeams.size();
        double postsLoss, skidLoss, factor;
        BeamParams oneParam;
        double stPos = 0, endPos, length;
        int lNum;
        // it is assumed that fixedBeamSecs are always >= movingBeamSecs
        if (fixedBeamSecs > 0) {
            for (int s = 0; s < fixedBeamSecs; s++) {
                oneParam = fixedBeams.get(s);
                postsLoss = oneParam.totalPostsLoss();
                skidLoss = oneParam.totalSkidLoss();
                length = oneParam.secLength; // it is assumed that the sections lengths are the same for fixed and moving
                endPos = stPos + length;
                if (s < movingBeamSecs) {
                    oneParam = movingBeams.get(s);
                    postsLoss += oneParam.totalPostsLoss();
                    skidLoss += oneParam.totalSkidLoss();
                }
                factor = postsLoss / length;
                if (length > 0)   {
                    factor = postsLoss / length;
                    if (factor > 0) {
                        lNum = addToLossType("Water Cooled Beams Posts Sec#" + (s + 1), factor, basis, tempAct, lossTypeKeys);
                        if (lNum > 0)  {
                            furnace.assignLoss(true, stPos, endPos, lNum, 1.0);
                        }
                    }
                    factor = skidLoss / length;
                    if (factor > 0) {
                        lNum = addToLossType("Water Cooled Skids Sec#" + (s + 1), factor, basis, tempAct, lossTypeKeys);
                        if (lNum > 0) {
                            furnace.assignLoss(true, stPos, endPos, lNum, 0.9);   // bottopm zones
                            furnace.assignLoss(false, stPos, endPos, lNum, 0.1);  // top zones
                        }
                    }
                }
                stPos = endPos;
            }
            retVal = true;
        }
        return retVal;
    }

    boolean setDoorLosses(Iterator<Integer> lossTypeKeys) {
        LossType.LossBasis basis = LossType.LossBasis.PIECES;
        LossType.TempAction tempAct = LossType.TempAction.POW4;
        double factor;
        int lNum;

        if (chargingDoorOpenTime > 0 && porteChargingSurface > 0) {
            factor = porteChargingSurface * 0.8 * SPECIAL.stefenBoltz * chargingDoorOpenTime/60;
            lNum = addToLossType("Charging Door", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0) {
                if (furnace.bTopBot) {
                    furnace.assignLoss(true, false, false, lNum, 1.0);
//                    furnace.assignLoss(true, false, true, lNum, 0.1);
                }
                else
                    furnace.assignLoss(true, false, false, lNum, 1.0);
            }
        }
        basis = LossType.LossBasis.FIXED;
        tempAct = LossType.TempAction.NONE;

        if (cooledBeamsDoorFramesChargingSurface > 0) {
            factor = cooledBeamsDoorFramesChargingSurface * 5000;
            lNum = addToLossType("Charging Door Frame", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0) {
                furnace.assignLoss(true, false, false, lNum, 1.0);
            }
        }

        basis = LossType.LossBasis.PIECES;
        tempAct = LossType.TempAction.POW4;
        if (dischargingDoorOpenTime > 0 && porteDischargingSurface > 0) {
            factor = porteDischargingSurface * 1.0 * SPECIAL.stefenBoltz * dischargingDoorOpenTime / 60;
            lNum = addToLossType("Discharging Door", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0) {
                if (furnace.bTopBot) {
                    furnace.assignLoss(false, true, false, lNum, 1.0);
//                    furnace.assignLoss(false, true, true, lNum, 0.1);
                }
                else
                    furnace.assignLoss(false, true, false, lNum, 1.0);
            }
        }

        basis = LossType.LossBasis.FIXED;
        tempAct = LossType.TempAction.NONE;
        if (cooledBeamsDoorFramesDischargingSurface > 0) {
            factor = cooledBeamsDoorFramesDischargingSurface * 7000;
            lNum = addToLossType("Discharging Door Frame", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0) {
                furnace.assignLoss(false, true, false, lNum, 1.0);
            }
        }

        return true;
    }

    boolean setScreenWallLosses(Iterator<Integer> lossTypeKeys) {
        if (cooledBeamsDoorFramesDischargingSurface > 0) {
            LossType.LossBasis basis = LossType.LossBasis.FIXED;
            LossType.TempAction tempAct = LossType.TempAction.NONE;
            double factor = cooledBeamsDoorFramesDischargingSurface * 11000 * Math.PI * 0.088;
            int lNum = addToLossType("Screen Wall ", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0)
                furnace.assignLoss(false, true, false, lNum, 1.0);
            return true;
        }
        else
            return false;
    }

    boolean setChRollLosses(Iterator<Integer> lossTypeKeys)  {
        if (nChargingRolls > 0) {
            LossType.LossBasis basis = LossType.LossBasis.FIXED;
            LossType.TempAction tempAct = LossType.TempAction.NONE;
            double factor = nChargingRolls * 15000;
            int lNum = addToLossType("Loss due to Charging Rollers ", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0)
                furnace.assignLoss(true, false, false, lNum, 1.0);
            return true;
        }
        else
            return false;
    }

    boolean setDischRollLosses(Iterator<Integer> lossTypeKeys)  {
        if (nDischargingRolls > 0) {
            LossType.LossBasis basis = LossType.LossBasis.FIXED;
            LossType.TempAction tempAct = LossType.TempAction.LINEAR;
            double factor = 24 * nDischargingRolls;
            int lNum = addToLossType("Loss due to Discharging Rollers ", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0)
                furnace.assignLoss(false, true, false, lNum, 1.0);
            return true;
        }
        else
            return false;
    }

    boolean setKickOffLosses(Iterator<Integer> lossTypeKeys)  {
        if (nKickoff > 0) {
            LossType.LossBasis basis = LossType.LossBasis.FIXED;
            LossType.TempAction tempAct = LossType.TempAction.NONE;
            double factor = 30000 * nFingersKickoff;
            int lNum = addToLossType("Kick-off Fingers ", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0) {
                if (furnace.bTopBot) {
                    furnace.assignLoss(false, true, false, lNum, 0.8);
                    furnace.assignLoss(false, true, true, lNum, 0.2);
                }
                else
                    furnace.assignLoss(false, true, false, lNum, 1.0);
            }
            return true;
        }
        return false;
    }

    boolean setBilletExtractorLosses(Iterator<Integer> lossTypeKeys)   {
        if (nPeelbar > 0) {
            LossType.LossBasis basis = LossType.LossBasis.FIXED;
            LossType.TempAction tempAct = LossType.TempAction.LINEAR;
            double factor = 60 * nPeelbar;
            int lNum = addToLossType("Peel Bar at Discharge ", factor, basis, tempAct, lossTypeKeys);
            if (lNum > 0)
                furnace.assignLoss(false, true, false, lNum, 1.0);
            return true;
        }
        else
            return false;

    }

    int addToLossType(String lossName, double factor, LossType.LossBasis basis, LossType.TempAction tempAct, Iterator<Integer> lossTypeKeys ) {
        int lNum = -1;
        if (lossTypeKeys.hasNext()) {
            lNum = lossTypeKeys.next();
            furnace.lossTypeList.changeLossItemVal(lNum,  lossName, factor, basis, tempAct, true);
        }
        return lNum;
    }
}
