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
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Properties;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import com.kanasansoft.ScreenCapture.ScreenCapture;

public class Jyazo {

	final String APP_HOME_NAME = ".jyazo";
	final String SETTING_PROP_FILE_NAME = "setting.properties";
	final String SETTING_SAMPLE_PROP_FILE_NAME = "setting-sample.properties";

	File appHome_ = null;
	SettingData settingData_ = null;

	public static void main(String[] args) {
		new Jyazo();
	}

	Jyazo() {

		String id = getId();
		if(id == null){return;}

		appHome_ = getApplicationHome();
		if(appHome_ == null){return;}
		settingData_ = getSettingData(new File(appHome_,SETTING_PROP_FILE_NAME));
		if(settingData_ == null){return;}
		if(settingData_.postSetIds.size()==0){return;}
		if(settingData_.serverIds.size()==0){return;}

		ArrayList<CaptureDecoration> CaptureDecorations = new ArrayList<CaptureDecoration>();
		for(String postSetId : settingData_.postSetIds){

			PostSet postSet = settingData_.postSets.get(postSetId);

			String message              = postSet.name;

			String textSizeBuf          = postSet.textSize;
			String textColorBuf         = postSet.textColor;
			String selectAreaColorBuf   = postSet.selectAreaColor;
			String unselectAreaColorBuf = postSet.unselectAreaColor;

			if(textSizeBuf.equalsIgnoreCase("")){
				textSizeBuf=settingData_.textSize;}
			if(textColorBuf.equalsIgnoreCase("")){
				textColorBuf=settingData_.textColor;}
			if(selectAreaColorBuf.equalsIgnoreCase("")){
				selectAreaColorBuf=settingData_.selectAreaColor;}
			if(unselectAreaColorBuf.equalsIgnoreCase("")){
				unselectAreaColorBuf=settingData_.unselectAreaColor;}

			int textSize;
			Color textColor;
			Color selectAreaColor;
			Color unselectAreaColor;

			textSize = textSizeBuf.equalsIgnoreCase("")
			? 96 : Integer.parseInt(textSizeBuf);
			textColor = textColorBuf.equalsIgnoreCase("")
			? new Color(0,0,0,32) : getColorFromString(textColorBuf);
			selectAreaColor = selectAreaColorBuf.equalsIgnoreCase("")
			? new Color(255,255,255,0) : getColorFromString(selectAreaColorBuf);
			unselectAreaColor = unselectAreaColorBuf.equalsIgnoreCase("")
			? new Color(0,0,0,32) : getColorFromString(unselectAreaColorBuf);

			CaptureDecorations.add(new CaptureDecoration(message,textSize,textColor,selectAreaColor,unselectAreaColor));

		}

		JyazoScreenCapture jsc = new JyazoScreenCapture();
		jsc.setCaptureDecorations(CaptureDecorations);
		jsc.setSelectMessageIndex(0);
		BufferedImage image = jsc.captureSelective();
		if(image == null){return;}
		String postSetId = settingData_.postSetIds.get(jsc.getSelectMessageIndex());
		PostSet postSet = settingData_.postSets.get(postSetId);

		for(String serverId : postSet.serverIds){

			Server server = settingData_.servers.get(serverId);

			String sendUrl = server.url;
			if(sendUrl.length() == 0){return;}

			Proxy proxy = null;
			String useProxy = server.useProxy;
			if(useProxy.equalsIgnoreCase("yes")){
				String proxyHost = server.proxyHost;
				if(proxyHost.length() == 0){return;}
				String proxyPort = server.proxyPort;
				if(proxyPort.length() == 0){return;}
				InetSocketAddress addr = new InetSocketAddress(proxyHost,Integer.parseInt(proxyPort, 10));
				proxy = new Proxy(Proxy.Type.HTTP, addr);
			}else{
				proxy = Proxy.NO_PROXY;
			}

			String url = postData(id, image, sendUrl, proxy);
			if(url != null){
				openURIByBrawser(url);
				if(postSet.serverIds.get(0).equalsIgnoreCase(serverId)){
					setStringToClipboard(url);
				}
			}

		}

	}

	public Color getColorFromString(String colorString){
		int colorNum = Integer.parseInt(colorString,16);
		int alpha = (colorNum & 0xff000000) >> 030;
		int red   = (colorNum & 0x00ff0000) >> 020;
		int green = (colorNum & 0x0000ff00) >> 010;
		int blue  = (colorNum & 0x000000ff) >> 000;
		return new Color(red,green,blue,alpha);
	}

	public static void copyFile(String from, String to) throws IOException {
		FileChannel fromChannel = null;
		FileChannel toChannel = null;
		try {
			fromChannel = new FileInputStream(from).getChannel();
			toChannel = new FileOutputStream(to).getChannel();
			fromChannel.transferTo(0, fromChannel.size(), toChannel);
		} finally {
			if(fromChannel!=null){
				fromChannel.close();
			}
			if(toChannel!=null){
				toChannel.close();
			}
		}
	}

	public void copyFileFromResource(String from, String to) throws IOException {
		InputStream is = null;
		FileOutputStream fos = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			is = this.getClass().getResourceAsStream(from);
			fos =new FileOutputStream(to);
			bis = new BufferedInputStream(is);
			bos = new BufferedOutputStream(fos);
			int data = 0;			
			while((data=bis.read())!=-1){
				bos.write(data);
			}
		} finally {
			if(bos!=null){
				try {
					bos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(bis!=null){
				try {
					bis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(fos!=null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(is!=null){
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
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
			try {
				copyFileFromResource(
						"/"+SETTING_PROP_FILE_NAME,
						new File(appHome,SETTING_PROP_FILE_NAME).getPath());
			} catch (IOException e) {
				e.printStackTrace();
				appHome.delete();
				return null;
			}
			try {
				copyFileFromResource(
						"/"+SETTING_SAMPLE_PROP_FILE_NAME,
						new File(appHome,SETTING_SAMPLE_PROP_FILE_NAME).getPath());
			} catch (IOException e) {
				e.printStackTrace();
				appHome.delete();
				return null;
			}
		}
		return appHome;
	}

	SettingData getSettingData(File propFile){

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

		SettingData settingData = new SettingData();

		settingData.textSize = prop.getProperty("post_sets.text_size", "");
		settingData.textColor = prop.getProperty("post_sets.text_color", "");
		settingData.selectAreaColor = prop.getProperty("post_sets.select_area_color", "");
		settingData.unselectAreaColor = prop.getProperty("post_sets.unselect_area_color", "");
		settingData.postSetIds = new ArrayList<String>();
		settingData.serverIds = new ArrayList<String>();
		settingData.postSets = new HashMap<String,PostSet>();
		settingData.servers = new HashMap<String,Server>();

		String psids = prop.getProperty("post_sets.post_set_ids", "").trim();
		if(!psids.equalsIgnoreCase("")){
			settingData.postSetIds.addAll(new ArrayList<String>(Arrays.asList(psids.split(" "))));
		}

		String svids = prop.getProperty("post_sets.server_ids", "").trim();
		if(!svids.equalsIgnoreCase("")){
			settingData.serverIds.addAll(new ArrayList<String>(Arrays.asList(svids.split(" "))));
		}

		for(String postSetId : settingData.postSetIds){

			String pre = "post_sets.post_set."+postSetId+".";
			PostSet postSet = new PostSet();

			postSet.name = prop.getProperty(pre+"name", "");
			postSet.textSize = prop.getProperty(pre+"text_size", "");
			postSet.textColor = prop.getProperty(pre+"text_color", "");
			postSet.selectAreaColor = prop.getProperty(pre+"select_area_color", "");
			postSet.unselectAreaColor = prop.getProperty(pre+"unselect_area_color", "");

			postSet.serverIds = new ArrayList<String>();

			String pssvids = prop.getProperty(pre+"server_ids", "").trim();
			if(!pssvids.equalsIgnoreCase("")){
				postSet.serverIds = new ArrayList<String>(Arrays.asList(pssvids.split(" ")));
			}

			settingData.postSets.put(postSetId, postSet);

		}

		for(String serverId : settingData.serverIds){

			String pre = "post_sets.server."+serverId+".";
			Server server = new Server();

			server.url = prop.getProperty(pre+"url", "");
			server.useProxy = prop.getProperty(pre+"use_proxy", "");
			server.proxyHost = prop.getProperty(pre+"proxy_host", "");
			server.proxyPort = prop.getProperty(pre+"proxy_port", "");

			settingData.servers.put(serverId, server);

		}

		return settingData;

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

		private ArrayList<CaptureDecoration> captureDecorations_ = new ArrayList<CaptureDecoration>();
		private Integer selectMessageIndex_ = 0;
		private final CaptureDecoration DEFAULT_CAPTURE_DECORATION =
			new CaptureDecoration("",96,new Color(0,0,0,0),new Color(0,0,0,0),new Color(0,0,0,0));
		private CaptureDecoration currentCaptureDecoration = DEFAULT_CAPTURE_DECORATION;
/*
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
*/
		ArrayList<Integer> changeMessageKeys_ = new ArrayList<Integer>(
				Arrays.asList(
						new Integer[]{
								KeyEvent.VK_1,
								KeyEvent.VK_2,
								KeyEvent.VK_3,
								KeyEvent.VK_4,
								KeyEvent.VK_5,
								KeyEvent.VK_6,
								KeyEvent.VK_7,
								KeyEvent.VK_8,
								KeyEvent.VK_9,
								KeyEvent.VK_0
						}
				)
		);

		JyazoScreenCapture(){
		}

		public void setSelectMessageIndex(Integer selectMessageIndex) {
			if(selectMessageIndex != null && 0 <= selectMessageIndex && selectMessageIndex < captureDecorations_.size()){
				selectMessageIndex_ = selectMessageIndex;
				currentCaptureDecoration = captureDecorations_.get(selectMessageIndex);
				setSelectionColor(currentCaptureDecoration.selectionColor);
				setUnselectionColor(currentCaptureDecoration.unselectionColor);
			} else {
				selectMessageIndex_ = null;
				currentCaptureDecoration = DEFAULT_CAPTURE_DECORATION;
			}
		}

		public Integer getSelectMessageIndex() {
			return selectMessageIndex_;
		}

		public void setCaptureDecorations(
				ArrayList<CaptureDecoration> captureDecorations) {
			if(captureDecorations == null){
				captureDecorations_ = new ArrayList<CaptureDecoration>();
				setSelectMessageIndex(null);
			}else{
				captureDecorations_ = captureDecorations;
				setSelectMessageIndex(null);
			}
		}

		@Override
		public BufferedImage makeSecectingImage(){

			BufferedImage image = super.makeSecectingImage();

			Integer selectMessageIndex = getSelectMessageIndex();
			if(selectMessageIndex == null){
				return image;
			}

			int width = image.getWidth();
			int height = image.getHeight();

			Graphics g = image.getGraphics();
			g.setFont(new Font("Dialog", Font.BOLD, currentCaptureDecoration.textSize));
			g.setColor(currentCaptureDecoration.textColor);

			String message = currentCaptureDecoration.message;

			FontMetrics fontMetrics = g.getFontMetrics();
			int stringWidth = fontMetrics.stringWidth(message);
			int stringHeight = fontMetrics.getHeight();
			g.drawString(message, (width-stringWidth)/2, (height+stringHeight)/2);

			return image;

		}

		@Override
		public void keyPressed(KeyEvent e) {
			int keyCode = e.getKeyCode();
			Integer num = changeMessageKeys_.indexOf(keyCode);
			if(num != null && 0 <= num && num < captureDecorations_.size()){
				setSelectMessageIndex(num);
			}
			super.keyPressed(e);
			frameRedraw();
		}

	}

	class SettingData{
		public ArrayList<String> postSetIds;
		public ArrayList<String> serverIds;
		public HashMap<String,PostSet> postSets;
		public HashMap<String,Server> servers;
		public String textSize;
		public String textColor;
		public String selectAreaColor;
		public String unselectAreaColor;
	}
	class PostSet{
		public String name;
		public ArrayList<String> serverIds;
		public String textSize;
		public String textColor;
		public String selectAreaColor;
		public String unselectAreaColor;
	}
	class Server{
		public String url;
		public String useProxy;
		public String proxyHost;
		public String proxyPort;
	}
	class CaptureDecoration{
		public String message;
		public int textSize;
		public Color textColor;
		public Color selectionColor;
		public Color unselectionColor;
		CaptureDecoration(){
		}
		CaptureDecoration(String message, int textSize, Color textColor){
			this.message = message;
			this.textSize = textSize;
			this.textColor = textColor;
		}
		CaptureDecoration(String message, int textSize, Color textColor, Color selectionColor, Color unselectionColor){
			this.message = message;
			this.textSize = textSize;
			this.textColor = textColor;
			this.selectionColor = selectionColor;
			this.unselectionColor = unselectionColor;
		}
	}
}
