import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Scanner;

public class PrePreprocessor {
	
	public static void main(String[] args) {
		
		try {
			
			//Open the viewTimes file
			File file = new File("viewTimes.txt");
			Scanner input = new Scanner(file);
			
			ArrayList<String> viewTimes = new ArrayList<String>();
			ArrayList<String> imageNames = new ArrayList<String>();
			
			//Read in the times at which each image was viewed, and the image name
			while(input.hasNextLine()) {
				
				String record = input.nextLine();
				
				//Format of record
				//imageName,dateShownMillis
				
				String[] fields = record.split(",");
				
				viewTimes.add(fields[1]);
				imageNames.add(fields[0]);
				
			}				

			input.close();
			
			//Output the times to a simple CSV for the matlab processor
			FileWriter fileOut = new FileWriter("viewTimesOnly.csv");
			BufferedWriter writer = new BufferedWriter(fileOut);
			
			for(String time: viewTimes) {
				writer.write(time + "\n");
			}
			
			writer.close();
			fileOut.close();
			
			//Now calculate the ratings for each image based on the imagesAndrew.csv
			//Read in imagesAndrew and create a HashMap with imageName->rating
			//Note: rating will be 1 to 5 mapped from the valence score
			HashMap<String, Integer> imageRatings = new HashMap<String, Integer>();
			
			//Open the file containing the chosen image list
			file = new File("imagesAndrew.csv");
			input = new Scanner(file);
			
			//Throw away the header line
			String dummy = input.nextLine();
			
			while(input.hasNextLine()) {
				
				String nextLine = input.nextLine();
				String[] fields = nextLine.split(",");
				
				String filename = fields[0];
				double valence = Double.parseDouble(fields[1]);

				//Now calculate the rating from the valence
				int rating = 0;
				
				if(valence < 4.2) {
					rating = 1;
				} else if(valence < 5.2) {
					rating = 2;
				} else if(valence < 6.2) {
					rating = 3;
				} else if(valence < 7.2) {
					rating = 4;
				} else {
					rating = 5;
				}
				
				//Store the rating in the hashmap
				imageRatings.put(filename, rating);		

			}
			
			//For each imageName in imageNames output its rating on a newline in pretrainerRatings.csv
			fileOut = new FileWriter("pretrainerRatings.csv");
			writer = new BufferedWriter(fileOut);
			
			for(String image: imageNames) {
				if(imageRatings.containsKey(image)) {
					writer.write(imageRatings.get(image) + "\n");
				}
			}
			
			writer.close();
			fileOut.close();
			
		} catch(FileNotFoundException e) {
			
			System.out.println("Error reading message input file");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
