package com.load;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.message.Message;

public class ServiceRequest 
{
	/**
	 * Method for processing a request for a service according to load
	 * @param message
	 * @return
	 */
	@SuppressWarnings("unchecked")
	/**
	 * The message(recieved from RMQ) in jsonObject is
	 * processed
	 * @param message
	 * @param messageObject
	 * @return
	 */
	public JSONObject processRequest(JSONObject message,Message messageObject)
	{
		JSONObject response = new JSONObject();
		/* This service name is also used to decide which queue
		 * to send the response
		 */
		String servicename=(String)message.get("service_name");
		String x=(String)message.get("request_id");

		if(!servicename.equalsIgnoreCase("logging"))
			messageObject.logMessage("INFO", "Request came for "+servicename+" with request id "+x);
		
		try
		{
			/*load the routing.xml file
			 * This file contains :-
			 * service | IP | queueName
			 */
			File inputFile = new File("routing.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("server");
			boolean flag=false;

			for(int i=0;i<nList.getLength();i++)
			{
				Node node = nList.item(i);
				
				if (node.getNodeType() == Node.ELEMENT_NODE) 
				{
					Element element = (Element) node;
					//status : if the system is up and running
					String status=element.getAttribute("status");
					String system=(String)element.getAttribute("system");
					double cpu=Double.parseDouble(element.getAttribute("cpu"));
					/*
					 * if the system is up and it is a system service.
					 * System service:- LoadBalancer, Service MAnager, VMManager, etc
					 * (if the request is not a system service, then only flow 
					 * will go to elseif block as system service has to run
					 * irrespective of the load. Replication of system service
					 * is not handled as of now.
					 */
					if(system.equalsIgnoreCase("yes") && status.equalsIgnoreCase("up"))
					{
						NodeList childList=element.getElementsByTagName("service");
						
						for(int j=0;j<childList.getLength();j++)
						{
							Element cE = (Element)childList.item(j);
							String name=cE.getAttribute("name");
							
							if(servicename.equalsIgnoreCase(name))
							{
								flag=true;
							}		 
						}	 
					}
					/*
					 * If the system is running and load is <= 75%
					 * If the request is a service. Eg:- login, getWeather, etc
					 */
					else if(status.equalsIgnoreCase("up") && cpu<=75.0)
					{
						NodeList childList=element.getElementsByTagName("service");
						
						for(int j=0;j<childList.getLength();j++)
						{
							Element cE = (Element)childList.item(j);
							String name=cE.getAttribute("name");
							
							if(servicename.equalsIgnoreCase(name))
							{
								flag=true;
							}		 
						}	 
					}
				}
			}
			/*
			 * flag = true means a system is found where the
			 * required service is running
			 */
			if(!flag)
			{
				String IP=new MonitorTable().getFreeServer();
				//TODO find out where this ip is put to use
				if(IP!=null)
				{
					JSONObject j=new JSONObject();
					j.put("ip", IP);
					response.put("server", j);
				}	
				response.put("queue","service_manager");
				response.put("type","create_service");
				if(!servicename.equalsIgnoreCase("logging"))
					messageObject.logMessage("INFO", "Request forwarded to service manager for "+servicename+" with request id "+x);
			}
			//found a system where service is running
			else
			{
				response.put("queue",servicename);
				response.put("type","service_request");
				
				if(!servicename.equalsIgnoreCase("logging"))
					messageObject.logMessage("INFO", "Request forwarded to "+servicename+" with request id "+x);
			}
			response.put("parameters",message.get("parameters"));
			response.put("service_name",servicename);
			response.put("request_id", x);
		}
		catch (Exception e) 
		{
			if(!servicename.equalsIgnoreCase("logging"))
				messageObject.logMessage("ERROR", "Unable to forward service request =>"+e.getLocalizedMessage());
		}
		return response;
	}
}
