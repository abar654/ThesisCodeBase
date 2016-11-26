import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Pretrainer extends JFrame{
	
	private static final String imageFolderPath = "C:\\Users\\Andrew\\Desktop\\NAPS_L";
	private static final String chosenImagesFile = "\\imagesAndrew.csv";
	private static final String usedImagesFile = "viewTimes.txt";
	private static final int numImagesPerSet = 5;
	 
	private ArrayList<String> normFiles;
	private ArrayList<String> lahvFiles;
	private ArrayList<String> lalvFiles;
	private ArrayList<String> mahvFiles;
	private ArrayList<String> malvFiles;
	private ArrayList<String> hahvFiles;
	private ArrayList<String> halvFiles;
	private ArrayList<String> imagesUsedThisSession;
	
	private JLabel imageLabel;
	private int numImagesShown;
	private JButton nextButton;
	private Timer timer;
	
	public Pretrainer() {

		try {
			
			//List all the usable images by their rating
			normFiles = new ArrayList<String>();
			lahvFiles = new ArrayList<String>();
			lalvFiles = new ArrayList<String>();
			mahvFiles = new ArrayList<String>();
			malvFiles = new ArrayList<String>();
			hahvFiles = new ArrayList<String>();
			halvFiles = new ArrayList<String>();
			imagesUsedThisSession = new ArrayList<String>();
			
			numImagesShown = 0;
			
			//Open the file containing the already used images
			File usedImages = new File(usedImagesFile);
			if(!usedImages.exists()) {
				FileWriter fileOut = new FileWriter(usedImagesFile, true);
				fileOut.close();
			}
			Scanner usedFilesInput = new Scanner(usedImages);
			
			//Make a set of all the already used filenames
			HashSet<String> usedImageSet = new HashSet<String>();
			
			while(usedFilesInput.hasNextLine()) {
				String record = usedFilesInput.nextLine();
				String[] fields = record.split(",");
				
				usedImageSet.add(fields[0]);
			}
			
			usedFilesInput.close();
			
			//Open the file containing the chosen image list
			File file = new File(imageFolderPath + chosenImagesFile);
			Scanner input = new Scanner(file);
			
			//Throw away the header line
			String dummy = input.nextLine();
			
			while(input.hasNextLine()) {
				//Read the image file names into the appropriate arraylist
				String nextLine = input.nextLine();
				String[] fields = nextLine.split(",");
				
				String filename = fields[0];
				double valence = Double.parseDouble(fields[1]);
				double arousal = Double.parseDouble(fields[2]);
				
				//Check this image hasn't been used unless it is a normalising image
				if(!usedImageSet.contains(filename) || (arousal <= 4.54 && valence > 6.54 && valence <= 7.2)) {
				
					//First split on arousal, then on valence
					if(arousal <= 4.54) {
						//Low Arousal
						if(valence <= 6.54) {
							//Low Valence
							lalvFiles.add(filename);
						} else if(valence <= 7.2) {
							//Neutral Valence
							normFiles.add(filename);
						} else {
							//High Valence
							lahvFiles.add(filename);
						}		
					} else if(arousal <= 5.23) {
						//Med Arousal
						if(valence <= 5.87) {
							//Low Valence
							malvFiles.add(filename);
						} else {
							//High Valence
							mahvFiles.add(filename);
						}
					} else {
						//High Arousal
						if(valence <= 4.3) {
							//Low Valence
							halvFiles.add(filename);
						} else {
							//High Valence
							hahvFiles.add(filename);
						}
						
					}
				}
			}
			
			imageLabel  = new JLabel("", new ImageIcon(), JLabel.CENTER);
			
			initialiseUI();
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void main(String[] args) {
			
		//Create the Pretrainer
		JFrame pretrainer = new Pretrainer();
		pretrainer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pretrainer.pack();
		pretrainer.setVisible(true);

	}
	
	/*
	 * Function taken from:
	 * http://stackoverflow.com/questions/6714045/how-to-resize-jlabel-imageicon
	 */
	
	private Image getScaledImage(Image srcImg, int w, int h){
	    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2 = resizedImg.createGraphics();

	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    g2.drawImage(srcImg, 0, 0, w, h, null);
	    g2.dispose();

	    return resizedImg;
	}

	private void initialiseUI() {
		
		//Create an ImageIcon
		setNewImage();
		
		//Create a Button
		nextButton = new JButton("Next");
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				
				setNewImage();
				
				nextButton.setText((numImagesShown) + ": Next");
				
				//Make the button unclickable for 6 seconds
				nextButton.setEnabled(false);
				timer = new Timer();
				timer.schedule(new ButtonTask(), 6000);
				
			}
		});
		
		//Make the button unclickable for 6 seconds
		nextButton.setEnabled(false);
		timer = new Timer();
		timer.schedule(new ButtonTask(), 6000);
		
		//Add both to the main panel
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
	
		mainPanel.add(imageLabel, BorderLayout.CENTER);
		mainPanel.add(nextButton, BorderLayout.SOUTH);
	
		add(mainPanel);
		
	}

	private void setNewImage() {
		
		try {
			
			List<String> currentFileList;
			//Determine which set of images we need to be using
			if(numImagesShown < numImagesPerSet) {
				//Normalisation
				currentFileList = normFiles;
			} else if(numImagesShown < numImagesPerSet*2) {
				//Low arousal high valence
				currentFileList = lahvFiles;
			} else if(numImagesShown < numImagesPerSet*3) {
				//Low arousal low valence
				currentFileList = lalvFiles;
			} else if(numImagesShown < numImagesPerSet*4) {
				//Normalisation
				currentFileList = normFiles;
			} else if(numImagesShown < numImagesPerSet*5) {
				//Med arousal high valence
				currentFileList = mahvFiles;
			} else if(numImagesShown < numImagesPerSet*6) {
				//Med arousal low valence
				currentFileList = malvFiles;
			} else if(numImagesShown < numImagesPerSet*7) {
				//Normalisation
				currentFileList = normFiles;
			} else if(numImagesShown < numImagesPerSet*8) {
				//High arousal high valence
				currentFileList = hahvFiles;
			} else if(numImagesShown < numImagesPerSet*9) {
				//High arousal low valence
				currentFileList = halvFiles;
			} else {
				//Finished the session
				outputFilename("finished");
				setVisible(false);
				dispose();
				return;
				//Maybe launch the rating component
			}
			
			//Choose an image at random
			if(currentFileList.size() < 1) {
				
				System.out.println("Out of images to show");
				
				outputFilename("finished");
				
				setVisible(false);
				dispose();
				return;
			}
			
			Random rand = new Random();
			int imageIndex = rand.nextInt(currentFileList.size());
			String path = imageFolderPath + "\\" + currentFileList.get(imageIndex) + ".jpg";

			//Open the image
			File file = new File(path);
			java.net.URL imgURL;
			imgURL = file.toURI().toURL();
	
			ImageIcon imageIcon = new ImageIcon(imgURL, "The image");
			Image bigImage = imageIcon.getImage();
			
			//Size the image correctly depending on orientation
			int h = 0;
			int w = 0;
			
			char orientation = currentFileList.get(imageIndex).charAt(currentFileList.get(imageIndex).length()-1);

			if(orientation == 'h') {
				h = 600;
				w = 800;
			} else if(orientation == 'v') {
				h = 800;
				w = 600;
			} else {
				System.err.println("Error in filename regarding orientation");
			}
			
			Image smallerImage = getScaledImage(bigImage, w, h);
			imageIcon = new ImageIcon(smallerImage);
			
			//Set the image
			imageLabel.setIcon(imageIcon);
			
			//Record the start time of this image i.e. current time
			outputFilename(currentFileList.get(imageIndex));
			
			//Remove the image from the viewing list
			currentFileList.remove(imageIndex);
			
			numImagesShown++;			
			
		} catch (Exception e){
			System.err.println("Error setting image");
		}
		
	}

	private void outputFilename(String filename) {

		try {
			
			FileWriter fileOut = new FileWriter(usedImagesFile, true);
			BufferedWriter writer = new BufferedWriter(fileOut);
			
			Date date = new Date();
			
			writer.write(filename + "," + date.getTime() + "\n");
			
			writer.close();
			fileOut.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	class ButtonTask extends TimerTask {
		public void run() {
			nextButton.setEnabled(true);
			timer.cancel();
		}
	}

}
