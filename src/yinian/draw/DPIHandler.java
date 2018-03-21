package yinian.draw;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;


public class DPIHandler {

	public static final String DENSITY_UNITS_NO_UNITS = "00";
	public static final String DENSITY_UNITS_PIXELS_PER_INCH = "01";
	public static final String DENSITY_UNITS_PIXELS_PER_CM = "02";

	public void saveGridImage(BufferedImage gridImage, File output)
			throws IOException {
		output.delete();

		final String formatName = "jpeg";

		for (Iterator<ImageWriter> iw = ImageIO
				.getImageWritersByFormatName(formatName); iw.hasNext();) {
			ImageWriter writer = iw.next();
			ImageWriteParam writeParam = writer.getDefaultWriteParam();
			ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier
					.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);
			IIOMetadata metadata = writer.getDefaultImageMetadata(
					typeSpecifier, writeParam);
			if (metadata.isReadOnly()
					|| !metadata.isStandardMetadataFormatSupported()) {
				continue;
			}

			setDPI(metadata);

			final ImageOutputStream stream = ImageIO
					.createImageOutputStream(output);
			try {
				writer.setOutput(stream);
				writer.write(metadata, new IIOImage(gridImage, null, metadata),
						writeParam);
			} finally {
				stream.close();
			}
			break;
		}
	}

	/**
	 * …Ë÷√Õº∆¨dpi
	 * @param metadata
	 * @throws IIOInvalidTreeException
	 */
	private static void setDPI(IIOMetadata metadata)
			throws IIOInvalidTreeException {
		String metadataFormat = "javax_imageio_jpeg_image_1.0";
		IIOMetadataNode root = new IIOMetadataNode(metadataFormat);
		IIOMetadataNode jpegVariety = new IIOMetadataNode("JPEGvariety");
		IIOMetadataNode markerSequence = new IIOMetadataNode("markerSequence");
		
		IIOMetadataNode app0JFIF = new IIOMetadataNode("app0JFIF");
		app0JFIF.setAttribute("majorVersion", "1");
		app0JFIF.setAttribute("minorVersion", "2");
		app0JFIF.setAttribute("thumbWidth", "0");
		app0JFIF.setAttribute("thumbHeight", "0");
		app0JFIF.setAttribute("resUnits", DENSITY_UNITS_PIXELS_PER_INCH);
		app0JFIF.setAttribute("Xdensity", String.valueOf(300));
		app0JFIF.setAttribute("Ydensity", String.valueOf(300));

		root.appendChild(jpegVariety);
		root.appendChild(markerSequence);
		jpegVariety.appendChild(app0JFIF);

		metadata.mergeTree(metadataFormat, root);
	}


}
