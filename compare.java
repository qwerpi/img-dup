import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;

public class compare {
	
	static double mindiff = Double.POSITIVE_INFINITY;
	static int strategy = Image.SCALE_FAST;
	
	static HashMap<String, int[][]> data;
	static HashMap<String, Integer> newImages = new HashMap<String, Integer>();
	static boolean hasNewData = false;

	public static void main(String[] args) throws Exception {
		String path = ".";
		if (args.length > 0)
			path = args[0];
		if (args.length > 1 && args[1].equalsIgnoreCase("slow"))
			strategy = Image.SCALE_SMOOTH;
		
		ObjectInputStream ois = null;
		try {
			if (strategy == Image.SCALE_FAST) {
				ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream("fast.dat")));
			} else {
				ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream("smooth.dat")));
			}
		} catch (FileNotFoundException ex) {
			System.err.println("Database has not yet been created");
		}
		
		if (ois != null) {
			data = (HashMap<String, int[][]>) ois.readObject();
		}
		
		if (data == null) {
			data = new HashMap<String, int[][]>();
		}
		
		long time = System.currentTimeMillis();
		Set<String> exts = new HashSet<String>(Arrays.asList(new String[]{"jpg","jpeg","png","gif","tiff","bmp"}));
		File[] dir = new File(path).listFiles();
		ArrayList<String> imageNames = new ArrayList<String>();
		ArrayList<ImgDiff> diffs = new ArrayList<ImgDiff>();
		int num = 0;
		int longest = 0;
		for (File f : dir) {
			String ext = f.toString().substring(f.toString().lastIndexOf('.') + 1);
			if (exts.contains(ext)) {
				imageNames.add(f.toString());
				if (f.toString().length() > longest)
					longest = f.toString().length();
			}
		}
		if (longest > 30)
			longest = 30;
		System.out.println(imageNames);

		int[][][] images = new int[imageNames.size()][256][256];
		int digits = (int)Math.ceil(Math.log10(imageNames.size()));
		System.out.println("Reading image data...");
		for (int i = 0; i < images.length; i++){
			images[i] = getScaledGrayscaleImage(imageNames.get(i));
		}
		
		System.out.println("Comparing images...");
		for (int i = 0; i < images.length - 1; i++) {
			//System.out.println(imageNames.get(i));
			for (int j = i + 1; j < images.length; j++) {
				//System.out.println("\t" + imageNames.get(j));
				double diff = 0;
				for (int r = 0; r < 256; r++)
					for (int c = 0; c < 256; c++)
						diff += Math.abs(images[i][r][c] - images[j][r][c]);
				diff /= 256 * 256;
				diffs.add(new ImgDiff(imageNames.get(i), imageNames.get(j), diff));
				if (diff < mindiff)
					mindiff = diff;
				if (diff == 0 && newImages.containsKey(imageNames.get(i)))
					newImages.put(imageNames.get(i), 0);
				if (diff == 0 && newImages.containsKey(imageNames.get(j)))
					newImages.put(imageNames.get(j), 0);
			}
		}
		Collections.sort(diffs);
		time = System.currentTimeMillis() - time;
		for (ImgDiff imgd : diffs) {
			System.out.println(imgd.formatString(longest));
		}
		System.out.println("Comparisons completed in " + time + " ms");
		boolean marked = false;
		Iterator it = newImages.keySet().iterator();
		while (it.hasNext()) {
			if (newImages.get(it.next()) == 1) {
				marked = true;
				break;
			}
		}
		if (!marked && !newImages.isEmpty()) {
			Scanner kb = new Scanner(System.in);
			System.out.print("There were no new images detected. Save anyway? (y/n) ");
			String response = kb.nextLine();
			if (response.substring(0,1).equalsIgnoreCase("n")) {
				hasNewData = false;
				System.out.println("Not saving new data");
			}
		}
		if (hasNewData) {
			System.out.println("Writing new data to " + (strategy == Image.SCALE_FAST ? "fast.dat" : "smooth.dat"));
			ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(strategy == Image.SCALE_FAST ? "fast.dat" : "smooth.dat")));
			oos.writeObject(data);
		}
	}
	
	public static int[][] getScaledGrayscaleImage(String filename) throws Exception {
		if (data.containsKey(filename))
			return data.get(filename);
		hasNewData = true;
		newImages.put(filename, 1);
		System.out.println("Reading " + filename + " for the first time...");
		int[][] res = new int[256][256];
		Image tkImg = ImageIO.read(new File(filename)).getScaledInstance(256, 256, strategy);
		BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		img.getGraphics().drawImage(tkImg, 0, 0, null);
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++) {
				int c = img.getRGB(i,j);
				int r = (c >> 16) & 0xFF;
				int g = (c >> 8) & 0xFF;
				int b = c & 0xFF;
				res[i][j] = (r + b + g) / 3;
			}
		}
		data.put(filename, res);
		return res;
	}
	
	static class ImgDiff implements Comparable {
		String image1, image2;
		double diff;
		
		public ImgDiff(String image1, String image2, double diff) {
			this.image1 = image1;
			this.image2 = image2;
			this.diff = diff;
		}
		
		public int compareTo(Object other) {
			return (int)Math.signum(((ImgDiff)other).diff - diff);
		}
		
		public String formatString(int length) {
			if (image1.length() > length)
				image1 = image1.substring(image1.length() - length);
			if (image2.length() > length)
				image2 = image2.substring(image2.length() - length);
			return String.format("%-" + length + "s\t%-" + length + "s\t%f", image1, image2, diff);
		}
		
		public String toString() {
			return formatString(Math.max(image1.length(), image2.length()));
		}
	}

}