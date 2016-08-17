package directFiredHeating.stripDFH;

/**
 * User: M Viswanathan
 * Date: 29-Jul-16
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class SampleStripFurnace {
    public static String xmlStr = "<profileCode>000000</profileCode>\n" +
            "<DataTitle>DFHeating Version 001</DataTitle>\n" +
            "<DFHeating><reference>------ SAMPLE REFERENCE ------</reference>\n" +
            "\n" +
            "<fceTitle>------ SAMPLE FURNACE ------</fceTitle>\n" +
            "\n" +
            "<customer>------ UNKNOWN ------</customer>\n" +
            "\n" +
            "<cbFceFor>Strip Heating</cbFceFor>\n" +
            "<cbHeatingType>STRIP - TOP and BOTTOM</cbHeatingType>\n" +
            "\n" +
            "<width>1.8</width>\n" +
            "<cbFuel>LPG 27000 [27,004 kcal/m3N]</cbFuel>\n" +
            "<excessAir>0.05</excessAir>\n" +
            "\n" +
            "<chargeData><chWidth>1.0</chWidth>\n" +
            "<chThickness>3.0E-4</chThickness>\n" +
            "<chLength>1.25</chLength>\n" +
            "<chDiameter>0.2</chDiameter>\n" +
            "<chType>Rectangular solid</chType>\n" +
            "</chargeData>\n" +
            "\n" +
            "<recuData><deltaTflue>50.0</deltaTflue>\n" +
            "<deltaTAir>50.0</deltaTAir>\n" +
            "<maxFlueAtRecu>800.0</maxFlueAtRecu>\n" +
            "<bAirHeatedByRecu>1</bAirHeatedByRecu>\n" +
            "<bFuelHeatedByRecu>0</bFuelHeatedByRecu>\n" +
            "<bAirAfterFuel>0</bAirAfterFuel>\n" +
            "<deltaTFuelFromRecu>30.0</deltaTFuelFromRecu>\n" +
            "</recuData>\n" +
            "\n" +
            "<productionData><tph>25.0</tph>\n" +
            "<entryTemp>30.0</entryTemp>\n" +
            "<exitTemp>480.0</exitTemp>\n" +
            "<processName></processName>\n" +
            "<chPitch>1.0</chPitch>\n" +
            "<nChargeRows>1</nChargeRows>\n" +
            "<deltaTemp>0.2</deltaTemp>\n" +
            "<exitZfceTemp>1050.0</exitZfceTemp>\n" +
            "<minExitZoneFceTemp>900.0</minExitZoneFceTemp>\n" +
            "<bottShadow>0.0</bottShadow>\n" +
            "</productionData>\n" +
            "\n" +
            "<calculData><ambTemp>30.0</ambTemp>\n" +
            "<airTemp>451.0</airTemp>\n" +
            "<fuelTemp>30.0</fuelTemp>\n" +
            "<calculStep>1.0</calculStep>\n" +
            "</calculData>\n" +
            "\n" +
            "<tuning><epsilonO>1.0</epsilonO>\n" +
            "<gasWallHTMultipler>5.0</gasWallHTMultipler>\n" +
            "<alphaConv>30.0</alphaConv>\n" +
            "<alphaConvRecu>30.0</alphaConvRecu>\n" +
            "<emmFactor>1.0</emmFactor>\n" +
            "<radiationMultiplier>1.0</radiationMultiplier>\n" +
            "<errorAllowed>1.0</errorAllowed>\n" +
            "<suggested1stCorrection>0.0</suggested1stCorrection>\n" +
            "<bTakeEndWalls>1</bTakeEndWalls>\n" +
            "<bTakeGasAbsorption>0</bTakeGasAbsorption>\n" +
            "<bEvalBotFirst>0</bEvalBotFirst>\n" +
            "<bSectionalFlueExh>0</bSectionalFlueExh>\n" +
            "<bSlotRadInCalcul>1</bSlotRadInCalcul>\n" +
            "<bSectionProgress>1</bSectionProgress>\n" +
            "<bSlotProgress>1</bSlotProgress>\n" +
            "<bMindChHeight>1</bMindChHeight>\n" +
            "<bAllowSecFuel>0</bAllowSecFuel>\n" +
            "<bAllowRegenAirTemp>1</bAllowRegenAirTemp>\n" +
            "<bConsiderChTempProfile>1</bConsiderChTempProfile>\n" +
            "<bOnProductionLine>0</bOnProductionLine>\n" +
            "\n" +
            "</tuning>\n" +
            "<furnace><bTopBot>0</bTopBot>\n" +
            "<lossTypeList><lt1001><lossName>Wall Losses</lossName>\n" +
            "<cbBasis>Lateral Wall Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.0</factor>\n" +
            "</lt1001>\n" +
            "<lt1002><lossName>Roof Losses</lossName>\n" +
            "<cbBasis>Roof Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.0</factor>\n" +
            "</lt1002>\n" +
            "<lt1003><lossName>Hearth Losses</lossName>\n" +
            "<cbBasis>Hearth Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.0</factor>\n" +
            "</lt1003>\n" +
            "<lt1004><lossName>Charging End Loss</lossName>\n" +
            "<cbBasis>Fixed</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>15000.0</factor>\n" +
            "</lt1004>\n" +
            "<lt1005><lossName>Discharge End Loss</lossName>\n" +
            "<cbBasis>Fixed</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>44000.0</factor>\n" +
            "</lt1005>\n" +
            "<lt1006><lossName>Roller Losses</lossName>\n" +
            "<cbBasis>Section Length</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>1000.0</factor>\n" +
            "</lt1006>\n" +
            "<lt1007><lossName>Loss #7</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1007>\n" +
            "<lt1008><lossName>Loss #8</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1008>\n" +
            "<lt1009><lossName>Loss #9</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1009>\n" +
            "<lt1010><lossName>Loss #10</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1010>\n" +
            "<lt1011><lossName>Loss #11</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1011>\n" +
            "<lt1012><lossName>Loss #12</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1012>\n" +
            "<lt1013><lossName>Loss #13</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1013>\n" +
            "<lt1014><lossName>Loss #14</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1014>\n" +
            "<lt1015><lossName>Loss #15</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1015>\n" +
            "<lt1016><lossName>Loss #16</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1016>\n" +
            "<lt1017><lossName>Loss #17</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1017>\n" +
            "<lt1018><lossName>Loss #18</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1018>\n" +
            "<lt1019><lossName>Loss #19</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1019>\n" +
            "<lt1020><lossName>Loss #20</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1020>\n" +
            "<lt1021><lossName>Loss #21</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1021>\n" +
            "<lt1022><lossName>Loss #22</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1022>\n" +
            "<lt1023><lossName>Loss #23</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1023>\n" +
            "<lt1024><lossName>Loss #24</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1024>\n" +
            "<lt1025><lossName>Loss #25</lossName>\n" +
            "<cbBasis>Disabled</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>0.0</factor>\n" +
            "</lt1025>\n" +
            "</lossTypeList>\n" +
            "<topSections><nActiveSec>5</nActiveSec>\n" +
            "<s0><cbSecType>Recuperative</cbSecType>\n" +
            "<cbFuelChoice>Common Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.0</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>1</bAPHCommonRecu>\n" +
            "<flueExhFract>0.0</flueExhFract>\n" +
            "<regenPHTemp>0.0</regenPHTemp>\n" +
            "<tcLocation>0.908</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>1</nActive>\n" +
            "<ss0><length>2.717</length>\n" +
            "<stHeight>1.222</stHeight>\n" +
            "<endHeight>1.222</endHeight>\n" +
            "<temperature>804.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss0>\n" +
            "</subsections>\n" +
            "</s0>\n" +
            "<s1><cbSecType>With Burners</cbSecType>\n" +
            "<cbFuelChoice>Common Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.0</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>1</bAPHCommonRecu>\n" +
            "<flueExhFract>0.0</flueExhFract>\n" +
            "<regenPHTemp>0.0</regenPHTemp>\n" +
            "<tcLocation>0.892</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>1</nActive>\n" +
            "<ss0><length>2.39</length>\n" +
            "<stHeight>1.522</stHeight>\n" +
            "<endHeight>1.522</endHeight>\n" +
            "<temperature>905.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss0>\n" +
            "</subsections>\n" +
            "</s1>\n" +
            "<s2><cbSecType>With Burners</cbSecType>\n" +
            "<cbFuelChoice>Common Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.0</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>1</bAPHCommonRecu>\n" +
            "<flueExhFract>0.0</flueExhFract>\n" +
            "<regenPHTemp>0.0</regenPHTemp>\n" +
            "<tcLocation>1.4</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>2</nActive>\n" +
            "<ss0><length>0.23</length>\n" +
            "<stHeight>0.6</stHeight>\n" +
            "<endHeight>0.6</endHeight>\n" +
            "<temperature>880.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss0>\n" +
            "<ss1><length>2.61</length>\n" +
            "<stHeight>1.522</stHeight>\n" +
            "<endHeight>1.522</endHeight>\n" +
            "<temperature>900.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss1>\n" +
            "</subsections>\n" +
            "</s2>\n" +
            "<s3><cbSecType>With Burners</cbSecType>\n" +
            "<cbFuelChoice>Common Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.0</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>1</bAPHCommonRecu>\n" +
            "<flueExhFract>0.0</flueExhFract>\n" +
            "<regenPHTemp>0.0</regenPHTemp>\n" +
            "<tcLocation>1.23</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>2</nActive>\n" +
            "<ss0><length>0.23</length>\n" +
            "<stHeight>0.6</stHeight>\n" +
            "<endHeight>0.6</endHeight>\n" +
            "<temperature>906.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss0>\n" +
            "<ss1><length>2.0</length>\n" +
            "<stHeight>1.522</stHeight>\n" +
            "<endHeight>1.522</endHeight>\n" +
            "<temperature>913.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss1>\n" +
            "</subsections>\n" +
            "</s3>\n" +
            "<s4><cbSecType>With Burners</cbSecType>\n" +
            "<cbFuelChoice>Common Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.0</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>1</bAPHCommonRecu>\n" +
            "<flueExhFract>0.0</flueExhFract>\n" +
            "<regenPHTemp>0.0</regenPHTemp>\n" +
            "<tcLocation>2.245</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>2</nActive>\n" +
            "<ss0><length>0.23</length>\n" +
            "<stHeight>0.6</stHeight>\n" +
            "<endHeight>0.6</endHeight>\n" +
            "<temperature>920.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss0>\n" +
            "<ss1><length>2.813</length>\n" +
            "<stHeight>1.522</stHeight>\n" +
            "<endHeight>1.522</endHeight>\n" +
            "<temperature>926.0</temperature>\n" +
            "<fixedLoss>0.0</fixedLoss>\n" +
            "<lossFactor>1.0</lossFactor>\n" +
            "<lossCB><l1025><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1025>\n" +
            "<l1024><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1024>\n" +
            "<l1023><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1023>\n" +
            "<l1022><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1022>\n" +
            "<l1021><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1021>\n" +
            "<l1020><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1020>\n" +
            "<l1019><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1019>\n" +
            "<l1018><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1018>\n" +
            "<l1017><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1017>\n" +
            "<l1016><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1016>\n" +
            "<l1015><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1015>\n" +
            "<l1014><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1014>\n" +
            "<l1013><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1013>\n" +
            "<l1012><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1012>\n" +
            "<l1011><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1006>\n" +
            "<l1005><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1005>\n" +
            "<l1004><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1004>\n" +
            "<l1003><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1003>\n" +
            "<l1002><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1002>\n" +
            "<l1001><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1001>\n" +
            "</lossCB>\n" +
            "</ss1>\n" +
            "</subsections>\n" +
            "</s4>\n" +
            "</topSections>\n" +
            "<ExistingAirRecu><bCounter>1</bCounter>\n" +
            "<heatingFlowBase>2310.0</heatingFlowBase>\n" +
            "<heatedFlowBase>2120.0</heatedFlowBase>\n" +
            "<heatingTinBase>770.0</heatingTinBase>\n" +
            "<heatingToutBase>345.0</heatingToutBase>\n" +
            "<heatedTinBase>30.0</heatedTinBase>\n" +
            "<heatedToutBase>550.0</heatedToutBase>\n" +
            "<heatExchBase>358280.0</heatExchBase>\n" +
            "<fFBase>562.4862291126835</fFBase>\n" +
            "<hTaBase>1353.714186645513</hTaBase>\n" +
            "</ExistingAirRecu>\n" +
            "<PerformanceData><nRefP>0</nRefP>\n" +
            "<RefP0><ProcessNameP>FHGI</ProcessNameP>\n" +
            "</PerformanceData>\n" +
            "</furnace>\n" +
            "</DFHeating>\n" +
            "<FuelSettings><opcIP>opc.tcp://127.0.0.1:49320</opcIP>\n" +
            "<fuelCharSteps>5</fuelCharSteps>\n" +
            "<speedCheckInterval>10</speedCheckInterval>\n" +
            "<considerFieldZoneTempForLossCorrection>0</considerFieldZoneTempForLossCorrection>\n" +
            "<fuelRanges><nFuelRange>5</nFuelRange>\n" +
            "<zfr#1><max>0.0</max>\n" +
            "<td>1.0</td>\n" +
            "</zfr#1>\n" +
            "<zfr#2><max>100</max>\n" +
            "<td>5</td>\n" +
            "</zfr#2>\n" +
            "<zfr#3><max>100</max>\n" +
            "<td>5</td>\n" +
            "</zfr#3>\n" +
            "<zfr#4><max>100</max>\n" +
            "<td>5</td>\n" +
            "</zfr#4>\n" +
            "<zfr#5><max>100</max>\n" +
            "<td>5</td>\n" +
            "</zfr#5>\n" +
            "</fuelRanges>\n" +
            "</FuelSettings>\n" +
            "<dfhProcessList><pNum>1</pNum>\n" +
            "<StripP1><processName>FHGI</processName>\n" +
            "<description>A Test Process</description>\n" +
            "<chMaterialThin>CR Lo-C emiss 0.32</chMaterialThin>\n" +
            "<chMaterialThick>CR Lo-C emiss 0.33</chMaterialThick>\n" +
            "<tempDFHExit>480.0</tempDFHExit>\n" +
            "<maxExitZoneTemp>1050.0</maxExitZoneTemp>\n" +
            "<minExitZoneTemp>850.0</minExitZoneTemp>\n" +
            "<thinUpperLimit>0.2</thinUpperLimit>\n" +
            "<maxUnitOutput>20.42</maxUnitOutput>\n" +
            "<minUnitOutput>10.0</minUnitOutput>\n" +
            "<maxSpeed>150.0</maxSpeed>\n" +
            "<maxThickness>0.8</maxThickness>\n" +
            "<minThickness>0.1</minThickness>\n" +
            "<maxWidth>1250.0</maxWidth>\n" +
            "<minWidth>850.0</minWidth>\n" +
            "</StripP1>\n" +
            "</dfhProcessList>\n ";
}