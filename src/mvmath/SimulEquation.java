package mvmath;
import java.io.*;

public final class SimulEquation implements Serializable {
	public static final int MAXUNKNOWNS = Determinant.MAXDETSIZE;
	int variables;
	double[][] leftSide;
	double[] rightSide;

	public SimulEquation(int unknowns) {
		if (unknowns < 1) {
			System.err.println(
			  "SimulEquation:No unknowns !");
		}

		else if (unknowns > MAXUNKNOWNS) {
			System.err.println(
			  "SimulEquation:too many unknowns !");
		}

		variables = unknowns;
		leftSide = new double[variables][variables];
		rightSide = new double[variables];
	}

	public boolean addRow(double[] rowDat, double rightVal, int rowNum) {
		if (rowNum < 0 || rowNum >= variables) {
			System.err.println("SimulEquation :row number out of range !");
			return false;
		}

		if (rowDat.length != variables) {
			System.err.println("SimulEquation :row size mismatch !");
			return false;
		}

		for (int n = 0; n < variables; n++) {
			leftSide[rowNum][n] = rowDat[n];
        }
		rightSide[rowNum] = rightVal;
		return true;
	}

	public double[] evalSimulEquation() {
		if (variables > 0)
			return evalSimulEquation(leftSide, rightSide);
		else
			return null;
	}

	public static double[] evalSimulEquation(double[][] array,
										double[] right) {
		// array[][] holds the left side determinant and right[]
		// holds the righ side values of the equations
		// the evaluated results are send back in a double[][]

		int unknowns = array.length;

		if (unknowns < 1) {
			System.err.println(
			  "SimulEquation:No unknowns !");
			return null;
		}

		if (unknowns > MAXUNKNOWNS) {
			System.err.println(
			  "SimulEquation:too many unknowns !");
			return null;
		}

		if (right.length != unknowns) {
			System.err.println(
			  "SimulEquation:left side and right side size mismatch !");
			return null;
		}

		double[][] arrayCopy = (double[][])array.clone();
		for (int row = 0; row < unknowns; row++)
				arrayCopy[row] = (double[])array[row].clone();

		double[] retval = new double[unknowns];
		if (unknowns == 1)
			retval[0] = right[0] / array[0][0];
		else {
			double det = Determinant.getDeterminant(array);
			for (int col = 0; col < unknowns; col++) {
				// copy right[] to the particular column
				for (int row = 0; row < unknowns; row++)
					arrayCopy[row][col] = right[row];
				// find and save unknown for the column
				retval[col] = Determinant.getDeterminant(arrayCopy) / det;

				// reset the last column with original
				for (int row = 0; row < unknowns; row++)
					arrayCopy[row][col] = array[row][col];
			}
		}

		return  retval;
	}

    void debug(String msg) {
      System.out.println(getClass().getName() + ": " + msg);
    }
}