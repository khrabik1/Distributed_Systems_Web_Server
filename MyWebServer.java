/*--------------------------------------------------------

	1. Name / Date: Katheryn Hrabik, February 2019

	2. Java 1.8.0_152

	3. Precise command-line compilation examples / instructions:

	MyWebServer:
	javac MyWebServer.java
	java MyWebServer



	4. Precise examples / instructions to run this program:


	In FireFox browser please use: http://localhost:2540/
	Trailing '/'s seem to be fine when accessing files manually
	via browser

	

	5. List of files needed for running the program.

	Files needed:
	a. MyWebServer.java
	b. (Files and sub directories populated with your script)

	5. Notes:

	Extensive comments added.
	As mentioned on checklist, when using the "back" button
	from the bottom-most directory, it sometimes takes a 
	double click, and there is sometimes a pointer exception 
	that doesn't actually affect functionality.
	----------------------------------------------------------*/

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.Stack;


class WebWorker extends Thread{
	//Initializing the socket object
	Socket sock;
	//Creating new worker object holding the socket
		
	WebWorker (Socket s) 
	{sock = s;}
	//Run method drives the entire worker thread.
	//Each thread runs independently for each separate
	//process/request
	
	public void run() {
		//PrintStream is passed around to handle output
		//to the FireFox browser
		PrintStream out = null;
		//We use BufferedReader to collect requests
		//from the browser
		BufferedReader in = null;
		
		
		try {
			//Setting up the BufferedReader to receive a stream of input from
			//browser
			in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			//Setting up PrintStream to navigate socket and send data out
			//do the browser
			out = new PrintStream(sock.getOutputStream());
			
			//aka until ctrl+c or socket closes
			while(true) {
				//Initialize string to hold everything coming in from 
				//BufferedReader
				String input;
				//Reading in, line by line
				input = in.readLine();
				//Multiple conditions required on my machine to prevent
				//browser hang.
				if(input.length() != 0 && input!=null) {
					//To focus on GET requests 
					if (input.contains("GET")){
						if(input.contains("?person")) {
							//when input from AddNums web form is encountered
							//it skips the directory traversal process etc.
							//and runs a script to create an HTML response
							//page.
							cgiRunner(input,out);
						}
						//All GET requests including favicons are ignored.
						if(!input.contains("favicon") && !input.contains("?person")) {
							//Processing starts with sendName, described
							//below
							System.out.println("Sending Request for: " + input);
							sendName(input, out);
						}
					}
				
				}
				//Last line of header request pops back blank. When this happens,
				//I chose to push the socket closed.
				//I also chose not to print anything at this step because it activates
				//for every single header including a bunch of favicons at each
				//step which was clogging up my log file.
				else if(input.length() == 0 || input == null) {
					//System.out.println("Reached the end of header, closing connection.\n");
					sock.close();
				}
				
			}
			
			
		}
		//Catching exceptions when connection resets.
		//Commented out to keep lines out of log
		catch(IOException ioe) {
			//System.out.println("Connection reset\n");
		}
	}
	
	public static void cgiRunner(String input, PrintStream out) {
		//Script to handle CGI request manually
		System.out.println("Request for AddNums response form initiated");
		//As before, hard coding length due to unreliability
		int len = 10000;
		//File type will be HTML.
		String fileType = "text/html";
		
		//INSANE amount of manual parsing to split on special
		//characters from CGI request line.
		String outData = "";
		String[] mainD = input.split(" ");
		String mainData = mainD[1];
		
		String[] parsed= mainData.split("&");
		String name = parsed[0];
		String numOne = parsed[1];
		String numTwo = parsed[2];
		
		String[] nameParse = name.split("\\?");
		String uName = nameParse[1];
		nameParse = uName.split("=");
		String userName = nameParse[1];
		
		String[] numOneParse = numOne.split("=");
		numOne = numOneParse[1];
		
		String[] numTwoParse = numTwo.split("=");
		numTwo = numTwoParse[1];
		
		//Sending to integer to add values
		int total = Integer.parseInt(numOne) + Integer.parseInt(numTwo);
		//Sending back to string to append to data string
		String totalStr = Integer.toString(total);
		//MIME Header, plus body of HTML to display response
		outData = "HTTP/1.1 200 OK" + "\r\n" + 
				"Content-Length: " + len + "\r\n" +
				"Content-Type: " + fileType + "\r\n\r\n" +
				"<html><body><h1>Katheryn Hrabik's AddNum Results</h1>"+
				"<h2>Hello, " + userName + "!</h2>" +
				"<p>The sum of " + numOne + " and " + numTwo + " is " + totalStr + 
				"</p></body></html>";
		//prints all data back to browser
		out.println(outData);
		
	}
	
	public static void sendName(String input, PrintStream out) throws IOException {
		//This method is used to isolate file name from GET
		//request through parsing.
		String name;
		String subString = input.substring(5);
		String[] parts = subString.split(" ");
		name = parts[0];
		//This is a helper method I made to give files a simple
		//string classification (no objects, not fancy)
		fileOrDir(name, out);
	}
	
	public static void updatePath(String name, PrintStream out) {
		//Using a stack (initialized in MyWebServer) to recurse and
		//collect file names and extensions
		String temp = "";
		//When the name passed in doesn't match the "last in"
		//i.e. when come upon a new file/directory		
		if (!MyWebServer.dirList.lastElement().equals(name)){
			//push the name to the stack
			MyWebServer.dirList.push(name);
			for(String dir : MyWebServer.dirList) {
				temp += dir;
				temp += "\\";
			}
			//now that we've created a full listing, assign
			//it to represent the directory
				MyWebServer.dir = temp;
			}
		
		else {
			//Else triggers when the name given is different than
			//our current directory name to add all available
			//extensions
			while(MyWebServer.dirList.contains(name))
				MyWebServer.dirList.pop();
				MyWebServer.dirList.push(name);
			for(String dir : MyWebServer.dirList) {
				temp += dir;
				temp += "\\";
			}
			//now that we've created a full listing, assign it to 
			//represent the directory
			MyWebServer.dir = temp;
		}
		
		System.out.println("PATH from updatepath: " + MyWebServer.dir);
		
	}
	
	
	
	public static void fileOrDir(String name, PrintStream out) throws IOException {
		//A simple helper method to create a String representation of the type,
		//just to make life a little easier
		String type = "";
		//All standard file types get passed a String "file" idenifier
		if(name.contains(".html") || name.contains(".htm") || name.contains(".txt")
				|| name.contains(".java") || name.contains(".gif") || name.contains(".class")
				|| name.contains(".jpg") || name.contains(".jpeg")) {
			type = "file";
		}
		//Other option, if not a file, is directory (at least for our purposes)
		else {			
			type = "directory";
			
		}
		//Calling process manager (explained below)
		processMgr(name, type, out);
	}
	
	public static void processMgr(String name, String type, PrintStream out) throws IOException {
		//Updating the path, assuming name is not blank (blank, generally just
		//when initially entering root)
		if(type.equals("directory")) {
			if (!name.equals("")) {
			updatePath(name, out);	
			}
			else {
				//Like mentioned above, when first entering root name
				//is generally blank. in this case, we pop the root address
				//to ensure we're traversing to the right place
				//and not getting lost in higher directories
				while(!MyWebServer.dirList.isEmpty()) {
				MyWebServer.dirList.pop();
				}
				MyWebServer.dirList.push(MyWebServer.root);
				updatePath(name, out);
			}
			System.out.println("directory list: " + MyWebServer.dirList.toString());
			//method to populate directory listing and its MIME header
			//(see method for notes)
			handleAsDir(MyWebServer.dir, name, out);
		}
		else if(type.equals("file")) {
			//Less work needs done when encountering a file.
			//We just pull it from the current directory folder and
			//populate the MIME header based on file type
			handleAsFile(MyWebServer.dir, name, out);
		}
	}
	
	public static void handleAsDir(String dir, String name, PrintStream out) {
		//Creating a new file object using our directory listing from
		//MyWebServer (passed in above)
		File f1 = new File(dir);
		//Populates a listing of all files in the directory
		File[] fileList = f1.listFiles();
		//Printing to console for log
		/*System.out.println("Here's the list of files from Directory Handler: \n");
		for( File f : fileList) {
			System.out.println(f.toString());
		}*/
		//All below lines are to create the MIME header for the directory
		//listing. As specified, populates as an HTML file with hyper links
		//to traverse files and sub directories
		String fileType = "text/html";
		//Getting length dynamically was unreliable, so I chose to hard
		//code a length. I know this is bad practice!
		int len = 1000000;
		
		//MIME Header:
		String outData = 	
				"HTTP/1.1 200 OK" + "\r\n" + 
				"Content-Length: " + len + "\r\n" +
				"Content-Type: " + fileType + "\r\n\r\n";
		//Starting HTML, Creating link to head back to root from 
		//any sub directory.
		outData = outData + "<html><body>\n";
		outData = outData + "<h1>Katheryn Hrabik's WebServer Directory</h1>";
		
		outData = outData + "<a href= \"../\">Parent Directory</a><br>";
		
		for(File f: fileList) {
			//This odd replace method was needed because for some reason
			//it couldn't read the \ character even with the escape
			//unless this was done
			String temp = f.toString().replace("\\", "/");
			//Helper function: explained below
			String address = splitter(temp);
			//Printing out a hyperlink for each file in file list
			System.out.println("address from splitter(temp): " + address);
			outData = outData + "<a href="+ address + ">"+address +"</a><br>\n";
		}
		
		//Closing out HTML document and printing to the browser.	
		outData = outData + "</html></body>";
		
		out.print(outData);
		
		}
	
	public static String splitter(String filePath) {
		//Simple helper function. Strictly speaking, should not
		//have been necessary, but '.split' and '.indexOf' were
		//not behaving reliably.
		String thing = filePath;
		String[] parts = thing.split("/");
		//Print statement for log
		/*System.out.println("Printing out each parsed item from path: ");
		for(String s : parts) {
			System.out.println(s);
		}*/
		//str gives us the end of the file path
		//this is really just so it doesn't print the entire file
		//path to the browser window
		String str = parts[parts.length-1];
		System.out.println("this should be the file name: " + str);
		
		return str;
	}

	
	public static void handleAsFile(String dir, String name, PrintStream out) throws IOException {
		//Print statement for log
		System.out.println("Handling " + name + " as file");
		//This logic was borrowed and modified from the 150 line
		//web server.
		//It creates a file type String identifier to be used
		//in the MIME header (created below)
		String fileType = "";
		if(name.contains(".html") || name.contains(".htm")) {
			fileType = "text/html";
		}
		else if(name.contains(".txt") || name.contains(".java")) {
			fileType = "text/plain";
		}
		else if(name.contains(".gif")) {
			fileType = "image/gif";
		}
		else if(name.contains(".class")) {
			fileType = "application/octet-stream";
		}
		else if(name.contains(".jpg") || name.contains(".jpeg") || name.contains(".JPG")) {
			fileType = "image/jpeg";
		}
		else {
			fileType = "text/plain";
		}
		//Again, length was behaving unreliably so I chose to hard-code a
		//large number. Bad practice, I'm sorry!
		int len = 1000000;
		String line;
		//Creating the new reader to read in the lines of the file
		//we'll be sending to the browser.
		BufferedReader reader = new BufferedReader(new FileReader(MyWebServer.dir + "\\" + name));
		//PrintWriter prntout = new PrintWriter(new OutputStreamWriter(out), true);
		//Creation of the MIME header
		String outData = 	
				"HTTP/1.1 200 OK" + "\r\n" + "Content-Length: " + len + "\r\n" +
				"Content-Type: " + fileType + "\r\n\r\n";
		
		
		//Read in each line of the file and send it on to be printed
		//to browser
		while((line = reader.readLine()) != null) {
			outData = outData + line + 	"\n";
			
	}
		

		
		//Once all is concatenated, send to browser.
		out.println(outData);
		
	}
	}



	public class MyWebServer {
		//Initializing stack, see above for functionality
		static Stack<String> dirList = new Stack<>();
		//Starting from root for dir object
		static String dir = "";
		//To keep track of the root directory (and pass it as String)
		//See notes above on traversal using stack
		static String root = Paths.get("").toAbsolutePath().toString();
		
		public static void main(String a[]) throws IOException{
			//Start process of traversal by pushing the root to the stack
			dirList.push(root);
			int q_len = 6;
			//Port as specified in instructions
			int port = 2540;
			//Creating the actual sock object to act on
			Socket sock;
			ServerSocket servsock = new ServerSocket(port, q_len);
			
			System.out.println("Katie Hrabik's MyWebServer starting up");
			System.out.println("See checklist for notes, and please use"
							+" http://localhost:2540/ in browser");
			
			//Initializing both to root: see stack for further
			//Notes
			dir = Paths.get("").toAbsolutePath().toString();
			root = Paths.get("").toAbsolutePath().toString();
			
			while(true) {
				//Listens for connection
				sock = servsock.accept();
				//Spawns a worker thread for each request
				new WebWorker(sock).start();
			}
		}

	}
	