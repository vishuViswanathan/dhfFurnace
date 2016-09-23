package directFiredHeating.billetWH;

/**
 * User: M Viswanathan
 * Date: 09-Sep-16
 * Time: 11:35 AM
 * To change this template use File | Settings | File Templates.
 */
public class SampleWHFurnace {
    public static String xmlStr = "<profileCode>000000</profileCode>\n" +
            "<DataTitle>DFHeating Version 001</DataTitle>\n" +
            "<DFHeating><reference>------ SAMPLE REFERENCE ------</reference>\n" +
            "\n" +
            "<fceTitle>----- SAMPLE WH FURNACE -----</fceTitle>\n" +
            "\n" +
            "<customer------ UNKNOWN -----</customer>\n" +
            "\n" +
            "<cbFceFor>Billet/Slab Heating</cbFceFor>\n" +
            "<cbHeatingType>TOP FIRED</cbHeatingType>\n" +
            "\n" +
            "<width>12.8</width>\n" +
            "<cbFuel>FURNACE OIL [9,675 kcal/kg]</cbFuel>\n" +
            "<excessAir>0.1</excessAir>\n" +
            "\n" +
            "<chargeData><chWidth>0.16</chWidth>\n" +
            "<chThickness>0.16</chThickness>\n" +
            "<chLength>12.0</chLength>\n" +
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
            "<productionData><tph>40.0</tph>\n" +
            "<entryTemp>30.0</entryTemp>\n" +
            "<exitTemp>1162.0</exitTemp>\n" +
            "<processName></processName>\n" +
            "<chPitch>0.26</chPitch>\n" +
            "<nChargeRows>1</nChargeRows>\n" +
            "<deltaTemp>20.0</deltaTemp>\n" +
            "<exitZfceTemp>0.0</exitZfceTemp>\n" +
            "<minExitZoneFceTemp>0.0</minExitZoneFceTemp>\n" +
            "<bottShadow>0.0</bottShadow>\n" +
            "</productionData>\n" +
            "\n" +
            "<calculData><ambTemp>30.0</ambTemp>\n" +
            "<airTemp>420.0</airTemp>\n" +
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
            "<bAllowSecFuel>1</bAllowSecFuel>\n" +
            "<bAllowRegenAirTemp>1</bAllowRegenAirTemp>\n" +
            "<bConsiderChTempProfile>1</bConsiderChTempProfile>\n" +
            "<bOnProductionLine>0</bOnProductionLine>\n" +
            "\n" +
            "</tuning>\n" +
            "<furnace><bTopBot>0</bTopBot>\n" +
            "<lossTypeList><lt1001><lossName>Wall Losses</lossName>\n" +
            "<cbBasis>Lateral Wall Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.55</factor>\n" +
            "</lt1001>\n" +
            "<lt1002><lossName>Roof Losses</lossName>\n" +
            "<cbBasis>Roof Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>2.0</factor>\n" +
            "</lt1002>\n" +
            "<lt1003><lossName>Hearth Losses</lossName>\n" +
            "<cbBasis>Hearth Area</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.2</factor>\n" +
            "</lt1003>\n" +
            "<lt1004><lossName>Charging End Loss</lossName>\n" +
            "<cbBasis>Charging End Wall</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.2</factor>\n" +
            "</lt1004>\n" +
            "<lt1005><lossName>Discharge End Loss</lossName>\n" +
            "<cbBasis>Discharging End Wall</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>1.6</factor>\n" +
            "</lt1005>\n" +
            "<lt1006><lossName>Charging Door Loss</lossName>\n" +
            "<cbBasis>Production - Pieces/h</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>100.0</factor>\n" +
            "</lt1006>\n" +
            "<lt1007><lossName>Discharging Door Loss</lossName>\n" +
            "<cbBasis>Production - Pieces/h</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>1186.0</factor>\n" +
            "</lt1007>\n" +
            "<lt1008><lossName>Discharging Roller Losses</lossName>\n" +
            "<cbBasis>Fixed</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>250000.0</factor>\n" +
            "</lt1008>\n" +
            "<lt1009><lossName>Slot Losses</lossName>\n" +
            "<cbBasis>Section Length</cbBasis>\n" +
            "<cbTempAct>Linear to Temperature DegC</cbTempAct>\n" +
            "<factor>22.9</factor>\n" +
            "</lt1009>\n" +
            "<lt1010><lossName>Charging Roller Losses</lossName>\n" +
            "<cbBasis>Fixed</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>135000.0</factor>\n" +
            "</lt1010>\n" +
            "<lt1011><lossName>Kick Off Arm Losses</lossName>\n" +
            "<cbBasis>Production - Pieces/h</cbBasis>\n" +
            "<cbTempAct>Not Related</cbTempAct>\n" +
            "<factor>7000.0</factor>\n" +
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
            "<topSections><nActiveSec>3</nActiveSec>\n" +
            "<s0><cbSecType>Recuperative</cbSecType>\n" +
            "<cbFuelChoice>Individual Fuel</cbFuelChoice>\n" +
            "<cbFuels>null</cbFuels>\n" +
            "<excessAir>0.1</excessAir>\n" +
            "<fuelTemp>0.0</fuelTemp>\n" +
            "<cbBurnerType>Normal</cbBurnerType>\n" +
            "<bAPHCommonRecu>0</bAPHCommonRecu>\n" +
            "<flueExhFract>0.85</flueExhFract>\n" +
            "<regenPHTemp>890.0</regenPHTemp>\n" +
            "<tcLocation>2.28</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>3</nActive>\n" +
            "<ss0><length>2.27</length>\n" +
            "<stHeight>1.265</stHeight>\n" +
            "<endHeight>1.265</endHeight>\n" +
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
            "<l1010><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>1</s>\n" +
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
            "<ss1><length>0.6</length>\n" +
            "<stHeight>1.265</stHeight>\n" +
            "<endHeight>0.9</endHeight>\n" +
            "<temperature>976.0</temperature>\n" +
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
            "<l1009><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>0</s>\n" +
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
            "<ss2><length>0.4</length>\n" +
            "<stHeight>0.9</stHeight>\n" +
            "<endHeight>0.9</endHeight>\n" +
            "<temperature>1067.0</temperature>\n" +
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
            "<l1009><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>0</s>\n" +
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
            "</ss2>\n" +
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
            "<tcLocation>1.44</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>2</nActive>\n" +
            "<ss0><length>1.8</length>\n" +
            "<stHeight>0.9</stHeight>\n" +
            "<endHeight>2.0</endHeight>\n" +
            "<temperature>1121.0</temperature>\n" +
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
            "<l1009><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>0</s>\n" +
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
            "<ss1><length>3.6</length>\n" +
            "<stHeight>2.0</stHeight>\n" +
            "<endHeight>2.0</endHeight>\n" +
            "<temperature>1148.0</temperature>\n" +
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
            "<l1009><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>0</s>\n" +
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
            "<tcLocation>1.8</tcLocation>\n" +
            "\n" +
            "<subsections><nActive>1</nActive>\n" +
            "<ss0><length>2.4</length>\n" +
            "<stHeight>2.0</stHeight>\n" +
            "<endHeight>2.0</endHeight>\n" +
            "<temperature>1170.0</temperature>\n" +
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
            "<l1011><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1011>\n" +
            "<l1010><s>0</s>\n" +
            "<f>1.0</f>\n" +
            "</l1010>\n" +
            "<l1009><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1009>\n" +
            "<l1008><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1008>\n" +
            "<l1007><s>1</s>\n" +
            "<f>1.0</f>\n" +
            "</l1007>\n" +
            "<l1006><s>0</s>\n" +
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
            "</ss0>\n" +
            "</subsections>\n" +
            "</s2>\n" +
            "</topSections>\n" +
            "<ExistingAirRecu><bCounter>1</bCounter>\n" +
            "<heatingFlowBase>13793.85173972258</heatingFlowBase>\n" +
            "<heatedFlowBase>13082.095267911276</heatedFlowBase>\n" +
            "<heatingTinBase>684.3252864682808</heatingTinBase>\n" +
            "<heatingToutBase>330.1248617092372</heatingToutBase>\n" +
            "<heatedTinBase>30.0</heatedTinBase>\n" +
            "<heatedToutBase>460.0</heatedToutBase>\n" +
            "<heatExchBase>1806369.173545555</heatExchBase>\n" +
            "<fFBase>2269.5634239166907</fFBase>\n" +
            "<hTaBase>6937.197685048698</hTaBase>\n" +
            "</ExistingAirRecu>\n" +
            "</furnace>\n" +
            "</DFHeating>\n";
}
