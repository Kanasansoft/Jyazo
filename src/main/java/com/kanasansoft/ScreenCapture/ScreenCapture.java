package com.kanasansoft.ScreenCapture;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

public class ScreenCapture implements KeyListener, MouseListener, MouseMotionListener {

	private AutoRedrawFrame frame_ = null;
	private BufferedImage capturedImage_ = null;
//	private BufferedImage affectedImage_ = null;
	private BufferedImage selectionImage_ = null;
	private BufferedImage unselectionImage_ = null;
//	private Color selectionColor_ = null;
//	private Color unselectionColor_ = null;
	private boolean running = true;
	private boolean selecting_ = false;
	private boolean captured_ = false;
	private Point startPoint_ = null;
	private Point endPoint_ = null;

	public static void main(String[] args) {
//		new ScreenCapture().capture();
	}

	public void setSelectionColor(Color color){
		if(color == null){return;}
		selectionImage_ = getColorFilteredImage(capturedImage_, color);
	}

	public void setUnselectionColor(Color color){
		if(color == null){return;}
		unselectionImage_ = getColorFilteredImage(capturedImage_, color);
	}

	public BufferedImage getColorFilteredImage(BufferedImage originalImage, Color colorFilter){
		if(originalImage == null){return null;}
		if(colorFilter == null){return null;}
		double alpha       = (double)colorFilter.getAlpha();
		double filterRed   = ((double)colorFilter.getRed()  ) * alpha;
		double filterGreen = ((double)colorFilter.getGreen()) * alpha;
		double filterBlue  = ((double)colorFilter.getBlue() ) * alpha;
		int height = originalImage.getHeight();
		int width  = originalImage.getWidth();
		BufferedImage image = new BufferedImage(width, height, originalImage.getType());
		for(int y = 0;y<height;y++){
			for(int x =0;x<width;x++){
				Color originalColor  = new Color(originalImage.getRGB(x,y));
				double originalRed   = (double)originalColor.getRed();
				double originalGreen = (double)originalColor.getGreen();
				double originalBlue  = (double)originalColor.getBlue();
				double newRed   = (originalRed   * (255 - alpha) + filterRed  ) / 255;
				double newGreen = (originalGreen * (255 - alpha) + filterGreen) / 255;
				double newBlue  = (originalBlue  * (255 - alpha) + filterBlue ) / 255;
				Color newColor = new Color((int)newRed,(int)newGreen,(int)newBlue);
				image.setRGB(x, y, newColor.getRGB());
			}
		}
		return image;
	}

	public BufferedImage makeSecectingImage(){
		if(capturedImage_ == null){return null;}
		if(selectionImage_ == null){return null;}
		if(unselectionImage_ == null){return null;}
		int w = capturedImage_.getWidth();
		int h = capturedImage_.getHeight();
		BufferedImage image = new BufferedImage(w, h, capturedImage_.getType());
		Graphics g = image.getGraphics();
		if (selecting_) {
			g.drawImage(unselectionImage_, 0, 0, null);
			g.drawImage(selectionImage_,
					startPoint_.x, startPoint_.y, endPoint_.x, endPoint_.y,
					startPoint_.x, startPoint_.y, endPoint_.x, endPoint_.y,
					null);
		}else{
			g.drawImage(unselectionImage_, 0, 0, null);
		}
		return image;
	}

	public BufferedImage capture() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		return capture(gd);
	}

	public BufferedImage capture(GraphicsDevice gd) {
		DisplayMode mode = gd.getDisplayMode();
		Robot robot = null;
		try {
			robot = new Robot(gd);
		} catch (AWTException e) {
			e.printStackTrace();
			return null;
		}
		Rectangle rectangle = new Rectangle(0,0,mode.getWidth(),mode.getHeight());
		return robot.createScreenCapture(rectangle);
	}

	public BufferedImage captureSelective() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		return captureSelective(gd);
	}

	public BufferedImage captureSelective(GraphicsDevice gd) {

		frame_ = null;
		capturedImage_ = null;
		selectionImage_ = null;
		unselectionImage_ = null;
		running = true;
		selecting_ = false;
		captured_ = false;
		startPoint_ = null;
		endPoint_ = null;
		capturedImage_ = capture(gd);
		setSelectionColor(new Color(255,255,255,0));
		setUnselectionColor(new Color(0,0,0,32));

		frame_ = new AutoRedrawFrame();
		frameRedraw();
		frame_.setUndecorated(true);

		if(gd.isFullScreenSupported()){
			try{
				gd.setFullScreenWindow(frame_);
			}catch(Exception e){
				gd.setFullScreenWindow(null);
				return null;
			}
		}else{
			return null;
		}

		frame_.addKeyListener(this);
		frame_.addMouseListener(this);
		frame_.addMouseMotionListener(this);
		frame_.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

		while(running == true){
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}

		if(!captured_){
			return null;
		}
		int x = Math.min(startPoint_.x,endPoint_.x);
		int y = Math.min(startPoint_.y,endPoint_.y);
		int w = Math.abs(startPoint_.x-endPoint_.x);
		int h = Math.abs(startPoint_.y-endPoint_.y);
		if(w==0||h==0){
			return null;
		}
		BufferedImage image = new BufferedImage(w, h, capturedImage_.getType());
		Graphics g = image.getGraphics();
		g.drawImage(capturedImage_,
				0, 0, w, h,
				x, y, x+w, y+h,
				null);

		return image;

	}

	public void frameRedraw(){
		frame_.setImage(makeSecectingImage());
		frame_.repaint();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		frame_.repaint();
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getKeyCode()==KeyEvent.VK_ESCAPE){
			running = false;
			frame_.dispose();
		}else{
			frameRedraw();
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		frame_.repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1){
			selecting_ = true;
			captured_ = false;
			startPoint_ = new Point(e.getX(),e.getY());
			endPoint_ = new Point(e.getX(),e.getY());
			frameRedraw();
		}else if(selecting_ == true){
			selecting_ = false;
			captured_ = false;
			frameRedraw();
		}else{
			running = false;
			frame_.dispose();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1 && selecting_ == true){
			selecting_ = false;
			captured_  = true;
			endPoint_ = new Point(e.getX(),e.getY());
			running = false;
			frame_.dispose();
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		endPoint_ = new Point(e.getX(),e.getY());
		frameRedraw();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	class AutoRedrawFrame extends Frame {
		private static final long serialVersionUID = 1L;
		private BufferedImage image_ = null;
		public void setImage(BufferedImage bufferedImage) {
			image_ = bufferedImage;
		}
		@Override
		public void paint(Graphics g) {
			if(image_!=null){
				g.drawImage(image_, 0, 0, this);
			}
		}
	}

}
