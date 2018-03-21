package yinian.draw;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class FontManager {
	private static Log log = LogFactory.getLog(FontManager.class);

	/**
	 * ·½Õý¾²ÀÙ×ÖÌå
	 */
	public Font fzjlFont() {
		Font font = null;
		File file = new File("fzjl.TTF");
		try {
			FileInputStream fi = new FileInputStream(file);
			BufferedInputStream fb = new BufferedInputStream(fi);
			font = Font.createFont(Font.TRUETYPE_FONT, fb);
		} catch (FontFormatException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}

		font = font.deriveFont(Font.BOLD, 50);

		return font;

	}
	
	/**
	 * Î¢ÈíÑÅºÚ×ÖÌå
	 */
	public Font wryhFont() {
		Font font = null;
		File file = new File("MSYHBD.TTF");
		try {
			FileInputStream fi = new FileInputStream(file);
			BufferedInputStream fb = new BufferedInputStream(fi);
			font = Font.createFont(Font.TRUETYPE_FONT, fb);
		} catch (FontFormatException e) {
			System.out.println(e);
		} catch (FileNotFoundException e) {
			System.out.println(e);
		} catch (IOException e) {
			System.out.println(e);
		}

		font = font.deriveFont(Font.BOLD, 40);

		return font;

	}
}
