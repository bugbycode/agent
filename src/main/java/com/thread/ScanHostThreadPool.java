package com.thread;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.jing.cloud.module.HostInfo;
import com.jing.cloud.module.Message;
import com.jing.cloud.module.MessageCode;
import com.jing.cloud.module.ScanHostResult;
import com.util.OSUtil;
import com.util.RandomUtil;
import com.util.StringUtil;

import io.netty.channel.Channel;

public class ScanHostThreadPool extends ThreadGroup{

	private final Logger logger = LogManager.getLogger(ScanHostThreadPool.class);
	
	private String cmd_path = "nmap";
	
	private ScanHostResult hr;
	
	private String token;
	
	private String address;
	
	private LinkedList<String> queue;
	
	private int total;
	
	private LinkedList<Message> msgQueue;
	
	private boolean isRun = false;
	
	private boolean isClosed = true;
	
	private Channel channel;
	
	public ScanHostThreadPool(String cmdPath,Channel channel) {
		super(RandomUtil.GetGuid32());
		this.isClosed = false;
		this.isRun = true;
		this.channel = channel;
		this.queue = new LinkedList<String>();
		this.msgQueue = new LinkedList<Message>();
		if(StringUtil.isNotBlank(cmdPath)) {
			this.cmd_path = cmdPath;
		}
		new MessageThread().start();
		for(int i = 0;i < 50;i++) {
			new WorkThread().start();
		}
	}
	
	public synchronized void addMessage(Message message) {
		msgQueue.addFirst(message);
		this.notifyAll();
	}
	
	private synchronized Message getMessage() throws InterruptedException {
		while(!isRun || msgQueue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("Thread pool closed.");
			}
		}
		return msgQueue.removeLast();
	}
	
	private synchronized String getHost() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("Thread pool closed.");
			}
		}
		return queue.removeFirst();
	}
	
	private synchronized void startScan() {
		String[] hostArr = {};
		if(address.indexOf(',') == -1) {
			hostArr = new String[] {address};
		}else {
			hostArr = address.split(",");
		}
		for(String ip : hostArr) {
			ip = ip.trim();
			if(ip.endsWith("*")) {
				int index = ip.lastIndexOf('.');
				String start = ip.substring(0, index);
				queue.add(start + ".1/24");
			}else {
				queue.add(ip);
			}
		}
		this.total = queue.size();
		this.notifyAll();
	}
	
	private synchronized void send() {
		Message message = new Message(this.token, MessageCode.SCAN_OS_RESULT, this.hr);
		if(channel != null) {
			channel.writeAndFlush(message);
		}
		this.isRun = true;
		this.notifyAll();
	}
	
	public synchronized void close() {
		this.isClosed = true;
		this.notifyAll();
	}
	
	private class MessageThread extends Thread {

		@Override
		public void run() {
			while(!isClosed) {
				try {
					hr = new ScanHostResult();
					Message message = getMessage();
					token = null;
					if(message.getType() == MessageCode.SCAN_OS) {
						isRun = false;
						address = message.getData().toString();
						token = message.getToken();
						startScan();
					}
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
	
	private class WorkThread extends Thread{

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			InputStream in = null;
			Process p = null;
			while(!isClosed) {
				try {
					String ip = getHost();
					String [] command = new String[3];
					if("linux".equals(OSUtil.getOSName().trim().toLowerCase())) {
						command[0] = "/bin/bash";
						command[1] = "-c";
					}else {
						command[0] = "cmd";
						command[1] = "/c";
					}
					command[2] = cmd_path + " -p T:22,23,443,80,1521,1433,3306,3389 -oX - -O --osscan-guess " + ip;
					
					logger.info("Run command " + command[2]);
					
					p = Runtime.getRuntime().exec(command);
					
					in = p.getInputStream();
					
					SAXReader reader = new SAXReader();
					Document doc = reader.read(in);
					Element root = doc.getRootElement();
					List<Element> list = root.elements("host");
					Iterator<Element> it = list.iterator();
					while(it.hasNext()) {
						HostInfo host = new HostInfo();
						Element hostElement = it.next();
						Element osElement = hostElement.element("os");
						List<Element> osList = osElement.elements("osmatch");
						List<Element> addressElement = hostElement.elements("address");
						for(Element address : addressElement) {
							if("ipv4".equals(address.attributeValue("addrtype"))) {
								String addr = address.attributeValue("addr");
								host.setIp(addr);
							}
						}
						if(osList.isEmpty()) {
							host.setOs("unknown");
						}else {
							StringBuffer osBuf = new StringBuffer();
							int osSize = osList.size();
							int index = 0;
							while(index < osSize) {
								if(index > 0) {
									osBuf.append(',');
								}
								Element osmatch = osList.get(index++);
								osBuf.append(osmatch.attributeValue("name"));
							}
							host.setOs(osBuf.toString());
						}
						hr.push(host);
						logger.info(host);
					}
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				} catch (IOException e) {
					logger.error(e.getMessage());
				} catch (DocumentException e) {
					logger.error(e.getMessage());
				} finally {
					total--;
					if(total == 0) {
						send();
					}
					try {
						if(in != null) {
							in.close();
						}
						
						if(p != null) {
							p.destroy();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		ScanHostThreadPool stp = new ScanHostThreadPool(null, null);
		stp.addMessage(new Message(RandomUtil.GetGuid32(), MessageCode.SCAN_OS, "192.168.1.*"));
	}
}
