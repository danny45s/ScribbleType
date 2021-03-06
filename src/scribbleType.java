import java.awt.Color;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;


public class scribbleType {
	
	private static final double TEMPLATE_BORDER = 0.15;
	private static final int COLOR_THRESHOLD = 100; //higher means responds to lighter colors
	private static final double PERCENTAGE_THRESHOLD = 0.001; //higher means it will need more dark pixels to stop trimming letters
	private static final int KERNING = 5;
	private static final int SPACE_LENGTH = 30;
	
	public static void main(String[] args) throws IOException {
		BufferedImage template = null;
		Scanner scan = new Scanner(System.in);
		System.out.print("Enter file name: ");
		template = ImageIO.read(new File(scan.next()));
		blackAndWhite(template, COLOR_THRESHOLD);
		BufferedImage croppedTemplate = cropImage((rotateImage(template, COLOR_THRESHOLD)), COLOR_THRESHOLD);
		ImageIO.write(rotateImage(template, COLOR_THRESHOLD), "jpg", new File("rotated.jpg"));
		ImageIO.write(croppedTemplate, "jpg", new File("cropped.jpg"));
		
		BufferedImage[][] letters = splitLetters(croppedTemplate);
		
	/*	for (int i = 0; i < letters.length; i++) {
			for (int j = 0; j < letters[i].length; j++) {
				String filename = ("letter" + i + "_" + j + ".jpg");
				ImageIO.write(letters[i][j], "jpg", new File(filename));
			}
		}
	*/	
		System.out.print("Enter width of canvas in pixels: ");
		int canvasWidth = scan.nextInt();
		System.out.print("Enter height of canvas in lines: ");
		int canvasHeight = scan.nextInt();
		scan.close();
	
		BufferedImage scribble = convertTextToImage("input.txt", canvasWidth, 
				canvasHeight, letters);
		ImageIO.write(scribble, "jpg", new File("output.jpg"));
	}
	
	public static BufferedImage[][] splitLetters(BufferedImage template) {
		BufferedImage[][] letters = new BufferedImage[26][6];
		int templateHeight = template.getHeight();
		int templateWidth = template.getWidth();
		BufferedImage singleLetter;
		
		for (int i = 0; i < 14; i++) {
			for (int j = 0; j < 11; j++) {
				singleLetter = template.getSubimage((int) (templateWidth * (i + TEMPLATE_BORDER) / 14),
						(int) (templateHeight * (j + TEMPLATE_BORDER) / 11), 
						(int) (templateWidth * (1 - 2 * TEMPLATE_BORDER) / 14), 
						(int) (templateHeight * (1 - 2 * TEMPLATE_BORDER) / 11));
				int letterIndex = (j*14 + i) / 6;
				int typeIndex = (j*14 + i) % 6;
				letters[letterIndex][typeIndex] = singleLetter;
			}
		}
		
		letters[25][4] = letters[25][2]; //copies "z" and "Z" to avoid null elements
		letters[25][5] = letters[25][2]; //do not use index 2 and 5 to maintain proper
		letters[25][2] = letters[25][1]; //weighting of the letter z	
		return letters;
	}
	
	public static BufferedImage convertTextToImage(String filename,
			int canvasWidth, int canvasHeight, BufferedImage[][] letters) 
					throws IOException {
		int letterHeight = letters[0][0].getHeight();
		int type = letters[0][0].getType();
		BufferedImage scribble = new BufferedImage(canvasWidth, canvasHeight * letterHeight, type);
		scribble.createGraphics().setPaint(Color.WHITE);
		scribble.createGraphics().fillRect(0, 0, canvasWidth, canvasHeight * letterHeight);
		Scanner scan = new Scanner(new File(filename));
		String text = "";
		while (scan.hasNext()) {
			text += scan.next();
			if (scan.hasNext()) {
				text += " ";
			}
		}
		scan.close();
		int prevPositionX = 0;
		int currentPositionY = 0;
		for (int i = 0; i < text.length(); i++) {
			int letterIndex = 0;
			int typeIndex = 0;
			if (Character.isLowerCase(text.charAt(i))) {
				letterIndex = text.charAt(i) - 'a';
				typeIndex = (int) (Math.random() * 3);
			}
			else if (Character.isUpperCase(text.charAt(i))){
				letterIndex = text.charAt(i) - 'A';
				typeIndex = (int) (Math.random() * 3) + 3;
			}
			if (Character.isAlphabetic(text.charAt(i))) {
				BufferedImage trimmedImage = trimWhiteSpace(letters[letterIndex][typeIndex], COLOR_THRESHOLD, PERCENTAGE_THRESHOLD);
				int finalPositionX = prevPositionX + trimmedImage.getWidth();
				if (finalPositionX > canvasWidth) {
					prevPositionX = 0;
					currentPositionY++;
				}
				scribble.createGraphics().drawImage(trimmedImage, null, 
						prevPositionX, currentPositionY * letterHeight);
				prevPositionX += trimmedImage.getWidth() + KERNING;
			}
			else {
				prevPositionX += SPACE_LENGTH;
			}
		}
		return scribble;
	}
	
	//threshold of average r/g/b value, weightThreshold percentage of a "scan"
	//that is colored in (non-blank)
	public static BufferedImage trimWhiteSpace(BufferedImage letter, int threshold, double weightThreshold) throws IOException {
		int left = 0;
		int right = letter.getWidth();
		int[] colorWeights = new int[right];
		for (int i = 0; i < letter.getWidth(); i++) {
			for (int j = 0; j < letter.getHeight(); j++) {
				Color color = new Color(letter.getRGB(i, j));
				if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
					colorWeights[i]++; //if the color is past the threshold add to weight
				}
			}
		}
		for (int i = 0; i < letter.getWidth() - 1 && 
				((double)colorWeights[i] / letter.getHeight()) < weightThreshold; i++) {
			left++;
		}
		for (int i = letter.getWidth() - 1; right > left + 1 && 
				((double) colorWeights[i] / letter.getHeight()) < weightThreshold; i--) {
			right--;
		}
		//ImageIO.write(letter.getSubimage(left, 0, right - left, letter.getHeight()), "jpg", new File("letter" + right + "_" + left + ".jpg"));
		return letter.getSubimage(left, 0, right - left, letter.getHeight());
	}
	
	public static BufferedImage rotateImage(BufferedImage template, int threshold) {
		Point leftCorner = new Point(template.getWidth() - 1, 0);
		Point rightCorner = new Point(0, 0);
		for (int j = 0; j < template.getHeight(); j++) {
			for (int i = 0; i < template.getWidth(); i++) {
				Color color = new Color(template.getRGB(i, j));
				if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
					if (i > rightCorner.x) {
						rightCorner.setLocation(i, j);
					}
					if (i < leftCorner.x) {
						leftCorner.setLocation(i, j);
					}
					break; 
				}
			}
		}
		Point transformVector = new Point(rightCorner.x - leftCorner.x, 
				leftCorner.y - rightCorner.y);
		AffineTransform transform = new AffineTransform();
		transform.rotate(transformVector.x, transformVector.y, template.getWidth() / 2, template.getHeight() / 2);
		BufferedImage rotatedTemplate = new BufferedImage(template.getWidth(), template.getHeight(), template.getType());
		rotatedTemplate.createGraphics().setPaint(Color.WHITE);
		rotatedTemplate.createGraphics().fillRect(0, 0, template.getWidth(), template.getHeight());
		rotatedTemplate.createGraphics().drawImage(template, transform, null);
		return rotatedTemplate;
	}
	
	public static BufferedImage cropImage(BufferedImage template, int threshold) {
		int topEdge = 0;
		int bottomEdge = template.getHeight();
		int leftEdge = 0;
		int rightEdge = template.getWidth();
		for (int i = 0; i < template.getWidth(); i++) {
			Color color = new Color(template.getRGB(i, template.getHeight()/2));
			if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
				leftEdge = i;
				break;
			}
		}
		for (int i = template.getWidth() - 1; i >= 0; i--) {
			Color color = new Color(template.getRGB(i, template.getHeight()/2));
			if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
				rightEdge = i;
				break;
			}
		}
		for (int j = 0; j < template.getHeight(); j++) {
			Color color = new Color(template.getRGB(template.getWidth()/2, j));
			if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
				topEdge = j;
				break;
			}
		}
		for (int j = template.getHeight() - 1; j >= 0; j--) {
			Color color = new Color(template.getRGB(template.getWidth()/2, j));
			if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
				bottomEdge = j;
				break;
			}
		}
		return template.getSubimage(leftEdge, topEdge, rightEdge - leftEdge, bottomEdge - topEdge);
	}
	
	public static void blackAndWhite(BufferedImage image, int threshold) {
		for (int i = 0; i < image.getWidth(); i++) {
			for (int j = 0; j < image.getHeight(); j++) {
				Color color = new Color(image.getRGB(i, j));
				if ((color.getRed() + color.getGreen() + color.getBlue()) / 3 < threshold) {
					image.setRGB(i, j, Color.BLACK.getRGB());
				}
				else {
					image.setRGB(i, j, Color.white.getRGB());
				}
			}
		}
	}
	
}
