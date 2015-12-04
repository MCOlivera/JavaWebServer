/*
 * HTTPServer.java
 * Author: S.Prasanna
 * @version 1.00 
*/

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class HTTPServer extends Thread {
	
	static final String HTML_START = 
	"<html>" +
	"<title>HTTP Server</title>" +
	"<body>";

	static final String HTML_END = 
	"</body>" +
	"</html>";

	Socket connectedClient = null;	
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	

	public HTTPServer(Socket client) {
		connectedClient = client;
	}			

	public void run() {
		boolean hasParameters = false;
		String currentLine = null, postBoundary = null, contentength = null, filename = null, contentLength = null, responseString = "";
		
		try {
			System.out.println("The Client " + connectedClient.getInetAddress() + ":" + connectedClient.getPort() + " is connected");

			inFromClient = new BufferedReader(new InputStreamReader (connectedClient.getInputStream()));                  
			outToClient = new DataOutputStream(connectedClient.getOutputStream());

			currentLine = inFromClient.readLine();
			String headerLine = currentLine;            	
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();
			String values = "<table>";

			if (httpMethod.equals("GET")) {    
				System.out.println("GET request");    	
				if (httpQueryString.equals("/")) {
					filename = "index.html";		  
				} else {
					String x[] = httpQueryString.split(Pattern.quote("?"));
					filename = x[0].substring(1);
					if (x.length > 1){
						hasParameters = true;
						String str[] = x[1].split("&");
						for (int i=0; i<str.length; i++){
							String s[] = str[i].split("=");
							values += "<tr>";
							for (int j=0; j<2; j++){
								values += "<td>" + s[j] + "</td>";
							}
							values += "</tr>";
						}
						values += "</table>";
					}
				}

				File file = new File(filename);

				try{
					Scanner scan = new Scanner(file);
						//read all lines of the text file
					while (scan.hasNext()){
						responseString += scan.nextLine();
					}

					scan.close();

					if (hasParameters)
						responseString += HTTPServer.HTML_START + values + HTTPServer.HTML_END;

					sendResponse(200, responseString, getExtension(filename));
				} catch (FileNotFoundException e){
					String error = e.toString();
					if (error.indexOf("Permission denied") != -1){
						System.out.println("Forbidden.");
						sendResponse(403, HTTPServer.HTML_START + "<b>Error 403: Forbidden</b>" + HTTPServer.HTML_END, "html");
					} else if (error.indexOf("No such file or directory") != -1){
						System.out.println("File not found.");
						sendResponse(404, HTTPServer.HTML_START + "<b>Error 404: File Not Found</b>" + HTTPServer.HTML_END, "html");
					}
				}
				
			} else {
				System.out.println("POST request");
				do {
					currentLine = inFromClient.readLine();

					if (currentLine.indexOf("Content-Type: multipart/form-data") != -1) {
						String boundary = currentLine.split("boundary=")[1];

						while(inFromClient.readLine().indexOf(boundary) == -1);

						String prevLine = inFromClient.readLine();
						currentLine = inFromClient.readLine();			  

						responseString = "<table>";

						while (true) {
							if (currentLine.equals("--" + boundary + "--")) {
								responseString += "<td>";
								responseString += prevLine.substring(prevLine.indexOf("name=")+1);
								responseString += "</td></tr>";
								break;
							}
							else if (currentLine.equals("--" + boundary)) {
								responseString += "<td>";
								responseString += prevLine.substring(prevLine.indexOf("name=")+1);
								responseString += "</td></tr>";
							}
							else if (!prevLine.isEmpty() && prevLine.indexOf(boundary) == -1){
								responseString += "<tr><td>";
								responseString += prevLine.substring(prevLine.indexOf("name=")+6, prevLine.length()-1);
								responseString += "</td>";
							}

							prevLine = currentLine;			  		
							currentLine = inFromClient.readLine();
						}

						responseString += "</table>";

						sendResponse(200, HTTPServer.HTML_START + responseString+ HTTPServer.HTML_END, "html");
					}			
				} while (inFromClient.ready());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	public String getExtension(String filename){
		String extension = "";

		int i = filename.lastIndexOf('.');
		if (i > 0) {
			extension = filename.substring(i+1);
		}

		return(extension);
	}

	public void sendResponse(int statusCode, String responseString, String type) throws Exception {
		
		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer" + "\r\n";
		String contentLengthLine = null;
		String fileName = null;		
		String contentTypeLine = "Content-Type: text/" + type + "\r\n";
		FileInputStream fin = null;
		
		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK\r\n";
		else if (statusCode == 403)
			statusLine = "HTTP/1.1 403 Forbidden\r\n";	
		else if (statusCode == 404)
			statusLine = "HTTP/1.1 404 Not Found\r\n";	

		responseString = responseString;
		contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";			

		outToClient.writeBytes(statusLine);
		outToClient.writeBytes(serverdetails);
		outToClient.writeBytes(contentTypeLine);
		outToClient.writeBytes(contentLengthLine);
		outToClient.writeBytes("Connection: close\r\n");
		outToClient.writeBytes("\r\n");		
		
		outToClient.writeBytes(responseString);
		
		outToClient.close();
	}
	
	public String readFile(File file){
		String line = "";

		try{
			Scanner scan = new Scanner(file);

			//read all lines of the text file
			while (scan.hasNext()){
				line += scan.nextLine();
			}

			scan.close();

		} catch (FileNotFoundException e){
			System.out.println("File not found.");
		}

		return(line);
	}

	public static void main (String args[]) throws Exception {
		
		if (args.length == 1){
			ServerSocket Server = new ServerSocket (Integer.parseInt(args[0]), 10, InetAddress.getByName("127.0.0.1"));         
			System.out.println("HTTP Server Waiting for client on port " + args[0]);
			System.out.println("Please use Firefox as the web browser for GET method.");
			System.out.println("Please use Postman for POST method.");

			while(true) {	                	   	      	
				Socket connected = Server.accept();
				(new HTTPServer(connected)).start();
			}
		} else {
			System.out.println("Usage: java HTTPServer <port>");
		}
	}
}
