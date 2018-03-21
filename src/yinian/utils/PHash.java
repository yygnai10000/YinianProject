package yinian.utils;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;


public class PHash {

	public static String getFeatureValue(BufferedImage image) {
		// 缩小尺寸，简化色彩
		int[][] grayMatrix = getGrayPixel(image, 32, 32);
		// 计算DCT
		grayMatrix = DCT(grayMatrix, 32);
		// 缩小DCT，计算平均值
		int[][] newMatrix = new int[8][8];
		double average = 0;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				newMatrix[i][j] = grayMatrix[i][j];
				average += grayMatrix[i][j];
			}
		}
		average /= 64.0;
		// 计算hash值
		String hash = "";
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (newMatrix[i][j] < average) {
					hash += '0';
				} else {
					hash += '1';
				}
			}
		}
		return hash;
	}

	public static int[][] getGrayPixel(BufferedImage image, int width, int height) {
		BufferedImage bi = null;
		try {
			bi = resizeImage(image, width, height,
					BufferedImage.TYPE_INT_RGB);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		int minx = bi.getMinX();
		int miny = bi.getMinY();
		int[][] matrix = new int[width - minx][height - miny];
		for (int i = minx; i < width; i++) {
			for (int j = miny; j < height; j++) {
				int pixel = bi.getRGB(i, j);
				int red = (pixel & 0xff0000) >> 16;
				int green = (pixel & 0xff00) >> 8;
				int blue = (pixel & 0xff);
				int gray = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
				matrix[i][j] = gray;
			}
		}
		return matrix;
	}

	public static BufferedImage resizeImage(BufferedImage image, int width,
			int height, int imageType) throws IOException {
		BufferedImage srcImg = image;
		BufferedImage buffImg = null;
		buffImg = new BufferedImage(width, height, imageType);
		buffImg.getGraphics().drawImage(
				srcImg.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0,
				0, null);
		return buffImg;
	}

	/**
	 * 用于计算pHash的相似度<br>
	 * 相似度为1时，图片最相似
	 * 
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static double calculateSimilarity(String str1, String str2) {
		int num = 0;
		for (int i = 0; i < 64; i++) {
			if (str1.charAt(i) == str2.charAt(i)) {
				num++;
			}
		}
		return ((double) num) / 64.0;
	}

	/**
	 * 离散余弦变换
	 * 
	 * 
	 * @param pix
	 *            原图像的数据矩阵
	 * @param n
	 *            原图像(n*n)的高或宽
	 * @return 变换后的矩阵数组
	 */
	public static int[][] DCT(int[][] pix, int n) {
		double[][] iMatrix = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				iMatrix[i][j] = (double) (pix[i][j]);
			}
		}
		double[][] quotient = coefficient(n); // 求系数矩阵
		double[][] quotientT = transposingMatrix(quotient, n); // 转置系数矩阵

		double[][] temp = new double[n][n];
		temp = matrixMultiply(quotient, iMatrix, n);
		iMatrix = matrixMultiply(temp, quotientT, n);

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				pix[i][j] = (int) (iMatrix[i][j]);
			}
		}
		return pix;
	}

	/**
	 * 求离散余弦变换的系数矩阵
	 * 
	 * 
	 * @param n
	 *            n*n矩阵的大小
	 * @return 系数矩阵
	 */
	private static double[][] coefficient(int n) {
		double[][] coeff = new double[n][n];
		double sqrt = 1.0 / Math.sqrt(n);
		for (int i = 0; i < n; i++) {
			coeff[0][i] = sqrt;
		}
		for (int i = 1; i < n; i++) {
			for (int j = 0; j < n; j++) {
				coeff[i][j] = Math.sqrt(2.0 / n)
						* Math.cos(i * Math.PI * (j + 0.5) / (double) n);
			}
		}
		return coeff;
	}

	/**
	 * 矩阵转置
	 * 
	 * 
	 * @param matrix
	 *            原矩阵
	 * @param n
	 *            矩阵(n*n)的高或宽
	 * @return 转置后的矩阵
	 */
	private static double[][] transposingMatrix(double[][] matrix, int n) {
		double nMatrix[][] = new double[n][n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				nMatrix[i][j] = matrix[j][i];
			}
		}
		return nMatrix;
	}

	/**
	 * 矩阵相乘
	 * 
	 * 
	 * @param A
	 *            矩阵A
	 * @param B
	 *            矩阵B
	 * @param n
	 *            矩阵的大小n*n
	 * @return 结果矩阵
	 */
	private static double[][] matrixMultiply(double[][] A, double[][] B, int n) {
		double nMatrix[][] = new double[n][n];
		int t = 0;
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				t = 0;
				for (int k = 0; k < n; k++) {
					t += A[i][k] * B[k][j];
				}
				nMatrix[i][j] = t;
			}
		}
		return nMatrix;
	}
}