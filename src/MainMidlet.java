import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.io.file.*;

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.tantalum.Task;
import org.tantalum.j2me.TantalumMIDlet;
import org.tantalum.net.HttpGetter;
import org.tantalum.net.HttpPoster;
import org.tantalum.net.json.JSONGetter;
import org.tantalum.net.json.JSONModel;
import org.tantalum.util.L;

import org.apache.commons.codec.binary.Base64;
import org.dx.seppo.StreamWriter;


public class MainMidlet extends TantalumMIDlet  implements CommandListener {

	private DisplayManager manager;
	private Displayable screen1;
	private Displayable screen2;
	private Displayable screen3;
	private Command	 back;
	private Command	 next;	

	private static final int CHUNK_SIZE = 1024;
	private static final String url = "http://adhocpush.herokuapp.com/messages/testchannel";
	
	/**
	 * Creates several screens and navigates between them.
	 */
	public MainMidlet() {
		this.manager = new DisplayManager(Display.getDisplay(this));
		this.back = new Command("Back", Command.BACK, 1);
		this.next = new Command("Next", Command.OK, 1);
		
		this.screen1 = getSreen1();
		this.screen1.setCommandListener(this);
		this.screen1.addCommand(this.back);
		this.screen1.addCommand(this.next);
		
		this.screen2 = getSreen2();
		this.screen2.setCommandListener(this);
		this.screen2.addCommand(this.back);
		this.screen2.addCommand(this.next);
		
		this.screen3 = getSreen3();
		this.screen3.setCommandListener(this);
		this.screen3.addCommand(this.back);
		this.screen3.addCommand(this.next);			
		
					
		}
	private void waitForRequest() {	
		String key = "http://adhocpush.herokuapp.com/messages/testchannel";
		//String key = "http://httpbin.org/delay/10";
		
		final JSONModel jsonModel = new JSONModel();
		JSONGetter getter = new JSONGetter(key, jsonModel);
		getter.setRetriesRemaining(0);
	
		getter.chain(new Task() {			
			protected Object doInBackground(Object in) {				
				processCommand(jsonModel);
				return null;				
			}});
		getter.fork();		
		}
	
	 /**
     * Encodes an array of bytes
     * @param imgBytes Array of bytes to encode
     * @return String representing the Base64 format of
     * the array parameter
     */
        
    protected void sendFile( String fname, OutputStream os ) throws IOException{ 
    	FileConnection fileConn = (FileConnection)Connector.open(fname, Connector.READ);    	
    	InputStream is = fileConn.openInputStream();
    	long length = fileConn.fileSize();
       
        //byte[] bytes = new byte[0];
        //String encodedStr = "";
        int read = 0;
        int written = 0;
        Base64 encoder = new Base64();
        
        while (read < length) {
        	 byte[] data = new byte[CHUNK_SIZE];
             int readAmount = is.read(data, 0, CHUNK_SIZE);
             byte[] out = encoder.encode(data);
             os.write(out);
             int writeAmount = out.length;
             read += readAmount;
             written += writeAmount;
        }
        L.i("lenght", "" + length);
        L.i("read", "" + read);
        L.i("written", "" + written);
        
        is.close();
        fileConn.close();	    
    }
        

    private void uploadFile(final String fname, String url) {
    	
    	HttpPoster httpPoster = new HttpPoster(url);
    	httpPoster.setWriter(new StreamWriter(){

			public void writeReady(OutputStream os) {
				try {
					sendFile(fname, os);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}    		
    	});
    	
    	httpPoster.setRequestProperty("Content-type", "multipart/mixed");
    	
    	httpPoster.chain(new Task(){
    		protected Object doInBackground(Object in) {
    			L.i("", "sent");
    			return null;
    			}
    		}); 
    	httpPoster.fork();
    }
    
	private void downloadFile(String url, final String destinationPath) {		
		HttpGetter httpGetter = new HttpGetter(url);
		httpGetter.chain(new Task(){		
			protected Object doInBackground(Object in) {				
				FileConnection fc;				
				String path = System.getProperty("fileconn.dir.photos");
				String fname = path + "werner.jpg";
				L.i("downloadFile", fname);
				
				try {
					fc = (FileConnection)Connector.open(fname, Connector.READ_WRITE);
					if(!fc.exists()) {
		                  fc.create();
		              }
					OutputStream out = fc.openOutputStream();
					out.write((byte[])in);
					out.close();
					fc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
				waitForRequest();
				return null;	
			}});
		httpGetter.fork();
	}
	
	private Displayable getSreen1() {
		return new TextBox("Text [Screen 1]", "", 100, TextField.ANY);
	}

	private Displayable getSreen2() {
		return new List("List [Screen 2]", List.IMPLICIT);
	}

	private Displayable getSreen3() {
		return new Form("Form [Screen 3]");
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#startApp()
	 */
	protected void startApp() throws MIDletStateChangeException {
		this.manager.next(this.screen1);
		//waitForRequest();		
	}
	
	protected void processCommand(JSONModel jsonModel){
		String type = "";
		try {
			type = jsonModel.getString("_type");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		L.i("type", type);
		if (type.equals("launch_url")) {
			try {
				JSONObject obj = jsonModel.jsonObject;
				
				JSONObject argsObj = obj.getJSONObject("args");
				String url = argsObj.getString("url");
				launchUrl(url);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			waitForRequest();
		}
		else if (type.equals("download_files")) {
			try {
				JSONObject obj = jsonModel.jsonObject;
				
				JSONObject argsObj = obj.getJSONObject("args");
				JSONArray filesArr = argsObj.getJSONArray("files");
				
				for (int i = 0; i < filesArr.length(); ++i) {
					JSONObject fileObj = filesArr.getJSONObject(i);
					String url = fileObj.getString("url");
					String targetDir = fileObj.getString("targetdir");
					downloadFile(url, targetDir);
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else if (type.equals("upload_files")) {
			try {
				JSONObject obj = jsonModel.jsonObject;
				JSONObject cookieObj = obj.getJSONObject("request_cookie");
				JSONObject argsObj = obj.getJSONObject("args");
				JSONArray filesArr = argsObj.getJSONArray("files");
				
				
				for (int i = 0; i < filesArr.length(); ++i) {
					String fname = filesArr.getString(i);
					uploadFile(fname, url);
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void launchUrl(String url) {
		try {
			L.i("", url);
			platformRequest(url);
		} catch (ConnectionNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command command, Displayable displayable) {
		if (command == this.next) {
			L.i("", "command");
			String path = System.getProperty("fileconn.dir.photos");
			String fname = path + "werner.jpg";
			
			
			uploadFile(fname, url);
		}
		
		if (command == this.back) {
			this.manager.back();
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#destroyApp(boolean)
	 */
	//protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {}

	/* (non-Javadoc)
	 * @see javax.microedition.midlet.MIDlet#pauseApp()
	 */
	protected void pauseApp() {}

}
