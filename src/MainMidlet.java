import javax.microedition.io.ConnectionNotFoundException;
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

import org.json.me.JSONArray;
import org.json.me.JSONException;
import org.json.me.JSONObject;
import org.tantalum.Task;
import org.tantalum.j2me.TantalumMIDlet;
import org.tantalum.net.json.JSONGetter;
import org.tantalum.net.json.JSONModel;
import org.tantalum.util.L;

public class MainMidlet extends TantalumMIDlet  implements CommandListener {

	private DisplayManager manager;
	private Displayable screen1;
	private Displayable screen2;
	private Displayable screen3;
	private Command	 back;
	private Command	 next;	
	private String url;
	
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
		System.out.println("start");
		
		waitForRequest();
					
		}
	private void waitForRequest() {	
		String key = "http://adhocpush.herokuapp.com/messages/testchannel";
		final JSONModel jsonModel = new JSONModel();
		JSONGetter getter = new JSONGetter(key, jsonModel);
		
		getter.chain(new Task() {			
			protected Object doInBackground(Object in) {				
				processCommand(jsonModel);
				waitForRequest();
				return null;				
			}});
		getter.fork();		
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
		}
		else if (type.equals("find_files")) {
			
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
			if (displayable == this.screen1) {
				this.manager.next(this.screen2);
			} else
			if (displayable == this.screen2) {
				this.manager.next(this.screen3);
			}
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
