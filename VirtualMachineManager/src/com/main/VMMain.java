package com.main;

import org.json.simple.JSONObject;

import com.message.Message;

public class VMMain 
{
	static Message messageObject;

	/**
	CmdLine Args:-
	rabbitIP,
	ReceiveQueue name(queue name dedicated for Virtual Machine Manager),
	ModuleName (seems unused),
	Gateway Addr
	
	Rabbit IP required as we need to know on which
	machine Rabbit MQ server is present and from where
	messages will come
	*/

	public static void main(String[] args) 
	{
		messageObject=new Message(args[0],args[1],args[2],args[3]);
		messageObject.recieveMessage();	
	}

	/**
	 * Method to process Request in separate thread
	 * @param message
	 */
	public void processRequest(JSONObject message)
	{
		if(message!=null)
		{
			new Thread(new Runnable() 
			{
				public void run() 
				{
					JSONObject response=parseMessage(message);
					System.out.println(response.toJSONString());
					
					if(response!=null)
					{
						messageObject.sendMessage(response);
					}  
				}
			}).start();
		}
	}

	/**
	 * Method to parse the message and return response message for a request
	 */
	@SuppressWarnings("unchecked")
	public JSONObject parseMessage(JSONObject request)
	{
		System.out.println("Message recieved");
		JSONObject response=null;
		JSONObject message=(JSONObject)request.get("parameters");
		String type=(String)message.get("type");
		VMUtils obj=new VMUtils();
		
		switch(type)
		{
		/*
		 * A VM was found where cpu usage was not full. So can start the service
		 * on the VM. The VM details(IP) was set in the load balancer
		 * after reading the routing.xml file
		 */
		case "vm_details":	response=obj.getVMDetails(message,messageObject);
		break;
		/*
		 * All VMs were overloaded, so a new VM needs to be started
		 */
		case "start_vm":	response=obj.startVM(message, messageObject);
		break;
		case "stop_vm":		response=obj.stopVM(message, messageObject);
		break;
		/*
		 * A VM with less load was found, so a new vm need not be
		 * started
		 */
		case "vm_info":		response=obj.getVMInfo(message,messageObject);
		break;
		}
		response.put("queue", "gateway");
		response.put("request_id", request.get("request_id"));
		return response;
	}

	/*@SuppressWarnings("unchecked")
	public static void main(String[] args) 
	{
		messageObject=new Message(args[0],args[1],args[2],args[3]);
		JSONObject re=new JSONObject();
		re.put("request_id", "123");
		JSONObject par=new JSONObject();
		par.put("type", "start_vm");
		re.put("parameters", par);
		new VMMain().parseMessage(re);
	}*/
}
