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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Formatter;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;

import com.kanasansoft.ScreenCapture.ScreenCapture;

public class Jyazo {

	public static void main(String[] args) {
		new Jyazo();
	}

	Jyazo() {
		String id = getId();
		if(id == null){return;}
		JyazoScreenCapture jsc = new JyazoScreenCapture();
		jsc.displayText("Capture!");
		BufferedImage image = jsc.captureSelective();
		if(image == null){return;}
		String url = postGyazo(id, image);
		if(url == null){return;}
		setStringToClipboard(url);
		openURIByBrawser(url);
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

	private String postGyazo(String id, BufferedImage image){
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
		String gyazoUrl = "http://gyazo.com/upload.cgi";
		StringBuffer imageUrl = new StringBuffer();
		try{
			URL url = new URL(gyazoUrl);
			URLConnection uc = url.openConnection();
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

		String message = "";

		JyazoScreenCapture(){
		}

		public void displayText(String text){
			if(text == null){
				text = "";
			}
			message = text;
		}

		@Override
		public BufferedImage makeSecectingImage(){
			BufferedImage image = super.makeSecectingImage();
			int width = image.getWidth();
			int height = image.getHeight();
			Graphics g = image.getGraphics();
			g.setFont(new Font("Dialog", Font.BOLD, 144));
			g.setColor(new Color(0,0,0,31));
			FontMetrics fontMetrics = g.getFontMetrics();
			int stringWidth = fontMetrics.stringWidth(message);
			int stringHeight = fontMetrics.getHeight();
			g.drawString(message, (width-stringWidth)/2, (height+stringHeight)/2);
			return image;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			super.frameRedraw();
		}

	}

}
