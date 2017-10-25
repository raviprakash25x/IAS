package wiki_search;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class Engine {
	int parseXML() {
		try {
			File xmlFile = new File(Globals.xml_file_path);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			UserHandler userHandler = new UserHandler();
			saxParser.parse(xmlFile, userHandler);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	public void mergeIndexes() {
		File files[] = new File(Globals.outFilePath).listFiles();
		//Arrays.sort(files);
		int fileCount = files.length; 

		while(fileCount > 1) {
			int outCount = 1;

			for(int i=1; (i+1)<=fileCount; i+=2) {
				//File file1 = new File(files[i-1]);
				//File file2 = new File(files[i]);
				mergeFiles(files[i-1], files[i], outCount);
				outCount++;
			}
			files = new File(Globals.outFilePath).listFiles();
			//Arrays.sort(files);
		fileCount = files.length;
		}
	}

	/**
	 * merges the indexes of two files
	 * @param file1
	 * @param file2
	 */
	private static void mergeFiles(File file1, File file2, int outCount) {
		File outFile = new File(Globals.outFilePath+"temp");

		try {
			PrintWriter writer = new PrintWriter(outFile, "UTF-8");
			BufferedReader br1 = new BufferedReader(new FileReader(file1));
			BufferedReader br2 = new BufferedReader(new FileReader(file2));
			String line1 = br1.readLine();
			String line2 = br2.readLine();

			while(line1 != null || line2 != null) {
				
				if(line1 != null && line2 != null) {
					//TODO can be optimised to split only newly updated line
					String lineSplit1[] = line1.split(":");
					String lineSplit2[] = line2.split(":");
					String key1 = lineSplit1[0];
					String key2 = lineSplit2[0];
					int comp = key1.compareTo(key2);
					
					if(comp == 0) {
						//merge
						//String merged = mergeFrequencies(key1, lineSplit1[1], lineSplit2[1]);
						String merged = key1 + ":" + lineSplit1[1] + lineSplit2[1];
						writer.println(merged);
						line1 = br1.readLine();
						line2 = br2.readLine();
					}
					else if(comp < 0) {
						writer.println(line1);
						line1 = br1.readLine();
					}
					else {
						writer.println(line2);
						line2 = br2.readLine();
					}
					
				}
				else if(line1 == null) {
					writer.println(line2);
					line2 = br2.readLine();
				}
				else {
					writer.println(line1);
					line1 = br1.readLine();
				}
			}

			br1.close();
			br2.close();
			writer.close();
			file1.delete();
			file2.delete();
			//File newFileName = new File(Globals.outFilePath+outCount);
			outFile.renameTo(file1);
			//TODO delete old files and rename temp, close writer
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
