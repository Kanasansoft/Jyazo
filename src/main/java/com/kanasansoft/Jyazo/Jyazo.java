package com.kanasansoft.Jyazo;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import com.kanasansoft.ScreenCapture.ScreenCapture;

public class Jyazo {

	final String APP_HOME_NAME = ".jyazo";
	final String POST_SETS_PROP_FILE_NAME = "postsets.properties";

	File appHome_ = null;
	Properties postSetsData_ = null;

	public static void main(String[] args) {
		new Jyazo();
	}

	Jyazo() {

		String id = getId();
		if(id == null){return;}

		appHome_ = getApplicationHome();

		postSetsData_ = getPostSetsData();
		if(postSetsData_ == null){return;}
		String[] postSetIds = postSetsData_.getProperty("post_set_ids", "").split(" ");
		if(postSetIds.length == 0){return;}
		ArrayList<String> postSetNames = new ArrayList<String>();
		for(int i=0;i<postSetIds.length;i++){
			String postSetName = postSetsData_.getProperty("post_set."+postSetIds[i]+".name", "");
			if(postSetName.length() == 0){return;}
			postSetNames.add(postSetName);
		}

		JyazoScreenCapture jsc = new JyazoScreenCapture();
		jsc.setSelectMessages(postSetNames.toArray(new String[]{}));
		jsc.setSelectMessageIndex(0);
		BufferedImage image = jsc.captureSelective();
		if(image == null){return;}
		String postSetId = postSetIds[jsc.getSelectMessageIndex()];

		String[] serverIds = postSetsData_.getProperty("post_set."+postSetId+".serverids", "").split(" ");
		if(serverIds.length == 0){return;}

		for(int i=0;i<serverIds.length;i++){

			String sendUrl = postSetsData_.getProperty("server."+serverIds[i]+".url", "");
			if(sendUrl.length() == 0){return;}

			Proxy proxy = null;
			String useProxy = postSetsData_.getProperty("server."+serverIds[i]+".use_proxy", "no");
			if(useProxy.equalsIgnoreCase("yes")){
				String proxyHost = postSetsData_.getProperty("server."+serverIds[i]+".proxy_host", "");
				if(proxyHost.length() == 0){return;}
				String proxyPort = postSetsData_.getProperty("server."+serverIds[i]+".proxy_port", "");
				if(proxyPort.length() == 0){return;}
				InetSocketAddress addr = new InetSocketAddress(proxyHost,Integer.parseInt(proxyPort, 10));
				proxy = new Proxy(Proxy.Type.HTTP, addr);
			}else{
				proxy = Proxy.NO_PROXY;
			}

			String url = postData(id, image, sendUrl, proxy);
			if(url != null){
				openURIByBrawser(url);
				if(i == 0){
					setStringToClipboard(url);
				}
			}

		}

	}

	private String getId(){
		final String USER_ID = "id";
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		String id = prefs.get(USER_ID , null);
		if(id==null){
			Date now = new Date();
			Formatter fmt = new Formatter();
			String strtime = fmt.format("%1$tY%<tm%<td%<tH%<tM%<tS%<tN", now).toString();
			id = strtime + "_by_Jyazo";
			prefs.put(USER_ID, id);
		}
		return id;
	}

	File getApplicationHome(){
		String homePath = System.getProperty("user.home");
		File appHome = new File(homePath,APP_HOME_NAME);
		if(!appHome.exists()){
			appHome.mkdir();
		}
		return appHome;
	}

	boolean initialPostSetsData(File propFile){

		Properties prop = new Properties();

		//post set ids
		prop.setProperty("post_set_ids", "public_only localhost_only public_and_localhost");

		//post set
		prop.setProperty("post_set.public_only.name", "Public Only");
		prop.setProperty("post_set.public_only.serverids", "public");

		prop.setProperty("post_set.localhost_only.name", "Localhost Only");
		prop.setProperty("post_set.localhost_only.serverids", "localhost");

		prop.setProperty("post_set.public_and_localhost.name", "Public and Localhost");
		prop.setProperty("post_set.public_and_localhost.serverids", "public localhost");
		
		//post
		prop.setProperty("server.public.url", "http://gyazo.com/upload.cgi");
		prop.setProperty("server.public.use_proxy", "no");
		prop.setProperty("server.public.proxy_host", "192.168.0.100");
		prop.setProperty("server.public.proxy_port", "8080");

		prop.setProperty("server.localhost.url", "http://localhost/cgi-bin/gyazo/upload.cgi");
		prop.setProperty("server.localhost.use_proxy", "no");
		prop.setProperty("server.localhost.proxy_host", "192.168.0.100");
		prop.setProperty("server.localhost.proxy_port", "8080");

		//write
		boolean isSuccess = true;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(propFile));
			prop.store(bos, "Jyazo");
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			isSuccess = false;
		} catch (IOException e) {
			e.printStackTrace();
			isSuccess = false;
		} finally {
			try {
				if(bos != null){
					bos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				isSuccess = false;
			}
		}
		return isSuccess;
	}

	Properties getPostSetsData(){
		File propFile = new File(appHome_,POST_SETS_PROP_FILE_NAME); 
		if(!propFile.exists()){
			try {
				propFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			if(!initialPostSetsData(propFile)){
				return null;
			}
		}
		Properties prop = new Properties();
		BufferedInputStream bis = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(propFile));
			prop.load(bis);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(bis != null){
					bis.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(prop.isEmpty()){
			return null;
		}
		return prop;
	}

	private String postData(String id, BufferedImage image, String sendUrl, Proxy proxy){
		byte[] bytes = null;
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(image, "png", os);
			bytes = os.toByteArray();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		String boundary = "----BOUNDARYBOUNDARY----";
		StringBuffer imageUrl = new StringBuffer();
		try{
			URL url = new URL(sendUrl);
			URLConnection uc = url.openConnection(proxy);
			uc.setDoOutput(true);
			uc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
			DataOutputStream dos = new DataOutputStream(uc.getOutputStream());
			dos.writeBytes("--" + boundary + "\r\n");
			dos.writeBytes("Content-Disposition: form-data; name=\"id\"");
			dos.writeBytes("\r\n");
			dos.writeBytes("\r\n");
			dos.writeBytes(id);
			dos.writeBytes("\r\n");
			dos.writeBytes("--" + boundary + "\r\n");
			dos.writeBytes("Content-Disposition: form-data; name=\"imagedata\"");
			dos.writeBytes("\r\n");
			dos.writeBytes("\r\n");
			dos.write(bytes);
			dos.writeBytes("\r\n");
			dos.writeBytes("\r\n");
			dos.writeBytes("--" + boundary + "--");
			dos.flush();
			dos.close();
			InputStream is = uc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String responseData = null;
			while((responseData = br.readLine()) != null){
				imageUrl.append(responseData);
			}
			is.close();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
		return imageUrl.toString();
	}

	private void setStringToClipboard(String string){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Clipboard clipboard = toolkit.getSystemClipboard();
		StringSelection stringSelection = new StringSelection(string);
		clipboard.setContents(stringSelection, stringSelection);
	}

	private boolean openURIByBrawser(String uri){
        Desktop desktop = Desktop.getDesktop();
        try {
			desktop.browse(new URI(uri));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	class JyazoScreenCapture extends ScreenCapture{

		private String message_ = "";
		private String[] messages_ = {};
		private int selectMessageIndex_ = 0;

		ArrayList<Integer> changeMessageKeys_ = new ArrayList<Integer>(){{
			add(KeyEvent.VK_1);
			add(KeyEvent.VK_2);
			add(KeyEvent.VK_3);
			add(KeyEvent.VK_4);
			add(KeyEvent.VK_5);
			add(KeyEvent.VK_6);
			add(KeyEvent.VK_7);
			add(KeyEvent.VK_8);
			add(KeyEvent.VK_9);
			add(KeyEvent.VK_0);
		}};

		JyazoScreenCapture(){
		}

		public void setSelectMessageIndex(int selectMessageIndex) {
			if(0 <= selectMessageIndex && selectMessageIndex < messages_.length){
				selectMessageIndex_ = selectMessageIndex;
				message_ = messages_[selectMessageIndex_];
			} else {
				selectMessageIndex_ = -1;
				message_ = "";
			}
		}

		public int getSelectMessageIndex() {
			return selectMessageIndex_;
		}

		void setSelectMessages(String[] texts){
			if(texts == null){
				messages_ = new String[0];
			}else{
				messages_ = texts;
			}
		}
/*
		public void displayText(String text){
			if(text == null){
				text = "";
			}
			message_ = text;
		}
*/
		@Override
		public BufferedImage makeSecectingImage(){
			BufferedImage image = super.makeSecectingImage();
			int width = image.getWidth();
			int height = image.getHeight();
			Graphics g = image.getGraphics();
			g.setFont(new Font("Dialog", Font.BOLD, 96));
			g.setColor(new Color(0,0,0,31));
			FontMetrics fontMetrics = g.getFontMetrics();
			int stringWidth = fontMetrics.stringWidth(message_);
			int stringHeight = fontMetrics.getHeight();
			g.drawString(message_, (width-stringWidth)/2, (height+stringHeight)/2);
			return image;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			Integer num = changeMessageKeys_.indexOf(keyCode);
			if(num != null && 0 <= num && num < messages_.length){
				setSelectMessageIndex(num);
			}
			super.keyPressed(e);
			super.frameRedraw();
		}

	}

}
