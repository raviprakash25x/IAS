package com.main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jcraft.jsch.Session;
import com.message.Message;

public class VMUtils 
{
	@SuppressWarnings("unchecked")
	public JSONObject getVMInfo(JSONObject message,Message messageObject)
	{
		JSONObject vmDetails=new JSONObject();
		String IP=(String)((JSONObject)message.get("parameters")).get("ip");
		try
		{
			File inputFile = new File("VMs.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("machine");
			boolean flag=false;
			for(int i=0;i<nList.getLength();i++)
			{
				Node node = nList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					 Element element = (Element) node;
					 NodeList childList=element.getElementsByTagName("vm");
					 for(int j=0;j<childList.getLength();j++)
					 {
						 Node childNode = childList.item(j);
						 if (childNode.getNodeType() == Node.ELEMENT_NODE) 
						 {
							 Element childElement=(Element)childNode;
							 String ip=(String)childElement.getAttribute("ip");
							 if(ip.equalsIgnoreCase(IP))
							 {
								 vmDetails.put("ip", IP);
								 vmDetails.put("username", (String)childElement.getAttribute("username"));
								 vmDetails.put("password", (String)childElement.getAttribute("password"));
								 flag=true;
								 break;
							 }	 
						 }
					 }	 
				}
				if(flag)
					break;
			}
			messageObject.logMessage("INFO", "VM details fetched sucessfully");
			vmDetails.put("status", "1");
		}
		catch (Exception e) 
		{
			vmDetails.put("status", "0");
			e.printStackTrace();
			messageObject.logMessage("ERROR", "Unable to fetch VM details");
		}
		return vmDetails;
	}	
	@SuppressWarnings("unchecked")
	public JSONObject getVMDetails(JSONObject message,Message messageObject)
	{
		JSONObject vmDetails=new JSONObject();
		try
		{
			File inputFile = new File("VMs.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("machine");
			JSONArray machineArray=new JSONArray();
			for(int i=0;i<nList.getLength();i++)
			{
				Node node = nList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					 Element element = (Element) node;
					 JSONObject machine=new JSONObject();
					 machine.put("ip",element.getAttribute("ip"));
					 machine.put("username",element.getAttribute("username"));
					 machine.put("capacity",element.getAttribute("capacity"));
					 JSONArray vmArray=new JSONArray();
					 NodeList childList=element.getElementsByTagName("vm");
					 for(int j=0;j<childList.getLength();j++)
					 {
						 Node childNode = childList.item(j);
						 if (childNode.getNodeType() == Node.ELEMENT_NODE) 
						 {
							 Element childElement=(Element)childNode;
							 JSONObject vm=new JSONObject();
							 vm.put("name", childElement.getAttribute("name"));
							 vm.put("ip", childElement.getAttribute("ip"));
							 vmArray.add(vm);
						 }
					 }
					 machine.put("vm_list", vmArray);
					 machineArray.add(machine);
				}
			}
			vmDetails.put("type", "vm_details");
			vmDetails.put("details", machineArray);
			vmDetails.put("status", "1");
			messageObject.logMessage("INFO", "VM details sent to Gateway");
		}
		catch (Exception e) 
		{
			vmDetails.put("status", "0");
			e.printStackTrace();
			messageObject.logMessage("ERROR", "Error in fetching VM details =>"+e.getLocalizedMessage());
		}
		return vmDetails;
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Start a new VM on one of the free machines.
	 * @param message
	 * @param messageObject
	 * @return details of vM
	 */
	public JSONObject startVM(JSONObject message,Message messageObject)
	{
		JSONObject vmDetails=new JSONObject();
		try
		{
			File inputFile = new File("VMs.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("machine");
			Element machine=null;
			String ip=new String();
			int vmNo=1;
			
			for(int i=0;i<nList.getLength();i++)
			{
				Node node = nList.item(i);
				
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					 Element element = (Element) node;
					 NodeList childList=element.getElementsByTagName("vm");
					 int capacity=Integer.parseInt(element.getAttribute("capacity"));
					 /*
					  * Look at the VMs.xml file. The parent node
					  * contains capacity (The no. of VMs it can host).
					  * If number of child nodes (VMs already running) is
					  * less than the capacity, then can start new VMs.
					  * Hence IP of the machine is retrieved
					  */
					 if(capacity>childList.getLength())
					 {
						ip=element.getAttribute("ip"); 
						//the count of the new VM to be started
						vmNo=childList.getLength()+1;
						machine=element;
					 }	 
				}
			}
			
			JSONObject serverDetails=null;
			
			/*
			 * No machine found which can host new VMs. Hence
			 * a new machine needs to be started.
			 */
			if(ip.isEmpty())
			{
				serverDetails=messageObject.callServiceURL("http://"+Message.getGateWayAddr()+"/Serverless/UserServlet?service_name=server_manager&&type=server_request");
			}	
			//A machine is found hence its IP is appended
			else
			{
				serverDetails=messageObject.callServiceURL("http://"+Message.getGateWayAddr()+"/Serverless/UserServlet?service_name=server_manager&&type=server_request&&ip="+ip);
			}	
			
			String IP=sponVM(serverDetails,vmNo);
			addTag(serverDetails, machine, doc, vmNo,inputFile,IP);
			vmDetails.put("ip", IP);
			//TODO hard coded but needs to be dynamic from the xml file
			vmDetails.put("username", "vagrant");
			vmDetails.put("password", "vagrant");
			vmDetails.put("status", "1");
			messageObject.logMessage("INFO", "New VM started");
		}
		catch (Exception e) 
		{
			vmDetails.put("status", "0");
			e.printStackTrace();
			messageObject.logMessage("ERROR", "Unable to start new VM =>"+e.getLocalizedMessage());
		}
		return vmDetails;
	}
	
	@SuppressWarnings({ "unchecked" })
	public JSONObject stopVM(JSONObject message,Message messageObject)
	{
		JSONObject vmDetails=new JSONObject();
		JSONObject parameters=(JSONObject)message.get("parameters");
		String IP=(String)parameters.get("ip");
		try
		{
			File inputFile = new File("VMs.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("machine");
			JSONObject serverDetails=null;
			boolean flag=false;
			for(int i=0;i<nList.getLength();i++)
			{
				Node node = nList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					 Element element=(Element)node;
					 NodeList childList=element.getElementsByTagName("vm");
					 for(int j=0;j<childList.getLength();j++)
					 {
						 Node childNode = childList.item(i);
						 if (childNode.getNodeType() == Node.ELEMENT_NODE) 
						 {
							 Element childElement=(Element)childNode;
							 String ip=childElement.getAttribute("ip");
							 if(IP.equalsIgnoreCase(ip))
							 {
								 serverDetails=new JSONObject();
								 serverDetails.put("ip", ip);
								 serverDetails.put("username", childElement.getAttribute("username"));
								 serverDetails.put("password", childElement.getAttribute("password"));
								 removeNode(childNode);
								 flag=true;
								 break;
							 }
						 }
					 }
				}
				if(flag)
					break;
				
			}
			Transformer tf = TransformerFactory.newInstance().newTransformer();
			tf.setOutputProperty(OutputKeys.INDENT, "yes");
			tf.setOutputProperty(OutputKeys.METHOD, "xml");
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource domSource = new DOMSource(doc);
			StreamResult sr = new StreamResult(inputFile);
			tf.transform(domSource, sr);
			if(serverDetails==null)
				throw new Exception();
			Connection con=new Connection();
			Session session=con.connect(serverDetails);
			con.runCommand("echo vagrant | sudo -S shutdown now", session);
			con.disconnect(session);
			vmDetails.put("status", "1");
			messageObject.logMessage("INFO", "VM stopped successfully");
		}
		catch (Exception e) 
		{
			vmDetails.put("status", "0");
			e.printStackTrace();
			messageObject.logMessage("ERROR", "Unable to stop VM =>"+e.getLocalizedMessage());
		}
		return vmDetails;
	}
	/**
	 * Starts a new VM on the given server(Server details)
	 * @param serverDetails
	 * @param vmNo
	 * @return
	 * @throws Exception
	 */
	public String sponVM(JSONObject serverDetails,int vmNo) throws Exception
	{
		System.out.println("Starting a new Virtual machine");
		String username=(String)serverDetails.get("server_username");
		String ip=(String)serverDetails.get("server_ip");
		String password=(String)serverDetails.get("server_password");
		//TODO what is SingleMachineSetup.sh file?
		runScript("bash SingleMachineSetup.sh "+ip+" "+username+" "+password);
		String IP=runScript("bash test.sh "+ip+" "+username+" "+password+" mc"+vmNo);
		System.out.println("New virtual machine started");
		return IP.substring(0,IP.length()-1);
	}
	
	/**
	 * Runs the given command script
	 * @param command
	 * @return
	 * @throws Exception
	 */
	public String runScript(String command) throws Exception
	{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    CommandLine commandline = CommandLine.parse(command);
	    DefaultExecutor exec = new DefaultExecutor();
	    PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
	    exec.setStreamHandler(streamHandler);
	    exec.execute(commandline);
	    return outputStream.toString();
	}
	
	public static void removeNode(Node node) 
	{
	    if (node != null) 
	    {
	        while (node.hasChildNodes()) 
	        {
	            removeNode(node.getFirstChild());
	        }
	        Node parent = node.getParentNode();
	        if (parent != null) 
	        {
	            parent.removeChild(node);
	            NodeList childNodes = parent.getChildNodes();
	            if (childNodes.getLength() > 0) 
	            {
	                List<Node> lstTextNodes = new ArrayList<Node>(childNodes.getLength());
	                for (int index = 0; index < childNodes.getLength(); index++) 
	                {
	                    Node childNode = childNodes.item(index);
	                    if (childNode.getNodeType() == Node.TEXT_NODE) 
	                    {
	                        lstTextNodes.add(childNode);
	                    }
	                }
	                for (Node txtNodes : lstTextNodes) 
	                {
	                    removeNode(txtNodes);
	                }
	            }
	        }
	    }
	}
	
	public void addTag(JSONObject serverDetails,Element machine,Document doc,int vmNo,File inputFile,String IP) throws Exception
	{
		Element service=doc.createElement("vm");
		service.setAttribute("ip", IP);
		service.setAttribute("name", "mc"+vmNo);
		service.setAttribute("username", "vagrant");
		service.setAttribute("password", "vagrant");
		if(machine==null)
		{
			Element root = doc.getDocumentElement();
			machine=doc.createElement("machine");
			machine.setAttribute("ip", (String) serverDetails.get("server_ip"));
			machine.setAttribute("username", (String) serverDetails.get("server_username"));
			machine.setAttribute("capacity", "2");
			root.appendChild(machine);
		}	
		machine.appendChild(service);
		Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		tf.setOutputProperty(OutputKeys.METHOD, "xml");
		tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		DOMSource domSource = new DOMSource(doc);
		StreamResult sr = new StreamResult(inputFile);
		tf.transform(domSource, sr);
	}
}
