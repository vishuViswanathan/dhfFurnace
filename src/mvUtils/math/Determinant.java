package mvUtils.math;

public final class Determinant {
	final static int MAXDETSIZE = 9;
	double[][] array;
	int detSize;

	public Determinant(int size) {
		if (size < 2 || size > Determinant.MAXDETSIZE) {
			System.err.println("Determinant size outof range (" +
					detSize + ") !");
		}
		else {
			detSize = size;
			array = new double[detSize][detSize];
		}
	}

	public boolean addRow(double[] row, int rowNum) {
		int len = row.length;
		if (len != detSize) {
			System.err.println("Determinant: row size error!");
			return false;
		}
		if (rowNum < 0 || rowNum >= detSize) {
			System.err.println("Determinant: row number error!");
			return false;
		}
		for (int n = 0; n < detSize; n++)
			array[rowNum][n] = row[n];
		return true;
	}

	public double getDeterminant() {
		if (detSize > 0)
			return getDeterminant(array);
		else
			return Double.NaN;
	}

	public static double getDeterminant(double[][] array) {
		double result = Double.NaN;
		int size = array.length;
		if (size < 2 || size > Determinant.MAXDETSIZE) {
			System.err.println("Determinant size outof range (" +
					size + ") !");
			return result;
		}


		if (size == 2) {
			result = array[0][0] * array[1][1] -
						array[0][1] * array[1][0];
		}
		else {
			double[][] array2 = new double[size - 1][size -1];
			boolean add = true;
			result = 0;
			for (int col = 0; col < size; col++ ) {
				for (int col1 = 0, col2 = 0; col1 < size;
										col1++) {
					if (col1 != col) {
						for (int row1 = 1, row2 =0;
								row1 < size; row1++, row2++){
							array2[row2][col2] = array[row1][col1];
						}
						col2++;
					}
				}

				if (add)
					result += array[0][col] *
							getDeterminant(array2);
				else
					result -= array[0][col] *
							getDeterminant(array2);
				add = !add;
			}
		}

		return result;
	}
}