package yinian.draw;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class PacketImage {

	private BufferedImage bufImg;

	public PacketImage(String path) {
		try {
			bufImg = ImageIO.read(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setImage(String path) {
		try {
			bufImg = ImageIO.read(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BufferedImage getCutImage(int x, int y, int w, int h) {
		return bufImg.getSubimage(x, y, w, h);
	}

	public BufferedImage getResult(int xx, int yy, int width, int height) {
		BufferedImage cutImg = getCutImage(xx, yy, width, height);
		int creY = 0;
		boolean havaBlackLine = false;
		for (int y = 0; y < height; y++) {
			if (y < height - 1 && !isBlackLine(cutImg, y)
					&& isBlackLine(cutImg, y + 1)) {
				havaBlackLine = true;
				continue;
			}
			if (havaBlackLine) {
				creY++;
			}
			if (y == height - 1) {
				break;
			}
			if (isBlackLine(cutImg, y) && !isBlackLine(cutImg, y + 1)) {
				if (creY == 0) {
					continue;
				}
				int startY = y - creY;
				int endY = y + 2;
				for (int x = 0; x < width; x++) {
					int startRGB = cutImg.getRGB(x, startY);
					int endRGB = cutImg.getRGB(x, endY);
					int creR = (getR(endRGB) - getR(startRGB)) / creY;
					int creG = (getG(endRGB) - getG(startRGB)) / creY;
					int creB = (getB(endRGB) - getB(startRGB)) / creY;
					for (int i = startY + 1, cr = 0; i < endY; i++, cr++) {
						int subRGB = new Color(getR(startRGB) + cr * creR,
								getG(startRGB) + cr * creG, getB(startRGB) + cr
										* creB).getRGB();
						cutImg.setRGB(x, i, subRGB);
					}
				}
				havaBlackLine = false;
				creY = 0;
			}
		}
		return cutImg;
	}

	public boolean isBlackLine(BufferedImage bi, int y) {
		int min = 1000000000;
		int max = -1000000000;
		for (int x = bi.getMinX(), width = bi.getWidth(); x < width; ++x) {
			int pixel = bi.getRGB(x, y);
			if (pixel > max) {
				max = pixel;
			}
			if (pixel < min) {
				min = pixel;
			}
		}
		return max - min > 9000000 ? false : true;
	}

	private int getR(int rgb) {
		return (rgb & 0xff0000) >> 16;
	}

	private int getG(int rgb) {
		return (rgb & 0xff00) >> 8;
	}

	private int getB(int rgb) {
		return rgb & 0xff;
	}

	public static void main(String[] args) {
		try {
			ImageIO.write(new PacketImage("C:/Users/Zad/Desktop/96907107309456115.jpg").getResult(120, 415,
					380, 380), "jpg", new File("C:/Users/Zad/Desktop/result.jpg"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}