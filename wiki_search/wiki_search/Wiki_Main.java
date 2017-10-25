package wiki_search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Wiki_Main {

	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Invalid args! Enter xml file path");
			System.exit(1);
		}
		//System.out.println(Globals.xml_file_path);

		while(true) {
			int choice = menu();
			
			switch (choice) {
			case 1:
				Globals.xml_file_path = args[0];
				Globals.populateStopWords();
				generateIndex();
				break;

			case 2:
				handleSearch();
				break;
			case 0:System.exit(0);
			}
		}
	}

	private static void handleSearch() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("\nEnter Search query: ");
			String query = br.readLine();
		}catch(Exception e) {
			e.printStackTrace();
		}

	}

	private static int menu(){
		System.out.println("1. Generate Index\n2. Search\n0. Exit\n");
		int choice = 2;

		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Enter your choice: ");
			choice = Integer.parseInt(br.readLine());
		}catch(Exception e) {
			e.printStackTrace();
		}

		return choice;
	}

	private static void generateIndex() {
		Engine engine = new Engine();
		engine.parseXML();
		engine.mergeIndexes();
		
	}
}
