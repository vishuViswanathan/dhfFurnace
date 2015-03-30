package basic;

public class ThreeDArrayTest extends Object {

    ThreeDArray nodes;

    public ThreeDArrayTest() {
        int xSize = 10, ySize = 9, zSize = 8;
        double bodyTemp = 40;
        double ambientTemp = 1000;
        double wcTemp = 1000;
        nodes = new ThreeDArray(xSize, ySize, zSize);
        ElementType el = new ElementType("Body", 7800, 30, 0.17);
        double ambientHTr = 200.0;
        double wcHTr = 200.0;
        OneNode aNode;
        // the body
        for (int x = 1; x < xSize - 1; x++) {
            for (int y = 1; y < ySize - 1; y++) {
                for (int z = 1; z < zSize - 1; z++) {
                    aNode = new OneNode(el, 0.01, 0.01, 0.01);
                    aNode.setTemperature(bodyTemp);
                    nodes.add(aNode, x, y, z);
                }
            }
        }
        // the ambient
        // bottom and top faces
        for (int x = 0; x < xSize; x++) {
            for (int y = 0; y < ySize; y++) {
                aNode = new OneNode(ambientHTr);
                aNode.setTemperature(ambientTemp);
                nodes.add(aNode, x, y, 0);
                aNode = new OneNode(ambientHTr);
                aNode.setTemperature(ambientTemp);
                nodes.add(aNode, x, y, zSize - 1);
            }
        }
        // left and right faces
        for (int y = 0; y < ySize; y++) {
            for (int z = 0; z < zSize; z++) {
                aNode = new OneNode(ambientHTr);
                aNode.setTemperature(ambientTemp);
                nodes.add(aNode, 0, y, z);
                aNode = new OneNode(ambientHTr);
                aNode.setTemperature(ambientTemp);
                nodes.add(aNode, xSize - 1, y, z);
            }
        }

        // back and front faces
        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {
                aNode = new OneNode(ambientHTr);
                // water cooled spot on XZ surface (back)
                if (x > 3 && x < 7 && z > 3 && z < 7) {
                    aNode = new OneNode(wcHTr);
                    aNode.setTemperature(wcTemp);
                } else {
                    aNode = new OneNode(ambientHTr);
                    aNode.setTemperature(ambientTemp);
                }
                nodes.add(aNode, x, 0, z);
                aNode = new OneNode(ambientHTr);
                aNode.setTemperature(ambientTemp);
                nodes.add(aNode, x, ySize - 1, z);
            }
        }

/*
    double time = 0;
    double deltaTime = 0.1;
    debug("At time = " + time);
    int loops = 500;
    double increment = deltaTime / loops;
    for (int l = 0; l < loops; l++)
      nodes.update(increment);
    time += deltaTime;
    if (nodes.writeTempProfileFile("c:/java/trials/results", false))
       debug("File saved ...");
    else
       debug("File writing problem");
*/
    }

    public static void main(String[] args) {
        ThreeDArrayTest threeDArrayTest = new ThreeDArrayTest();
    }

    void errMessage(String msg) {
        System.err.println("ThreeDArrayTest: ERROR: " + msg);
    }

    void debug(String msg) {
        System.out.println("ThreeDArrayTest: " + msg);
    }
}

