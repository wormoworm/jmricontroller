package uk.tomhomewood.android.jmricontroller;

import java.io.File;
import java.io.StringReader;
import java.net.Inet4Address;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import uk.tomhomewood.http.Http;
import uk.tomhomewood.http.HttpEvents;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class RosterDownloader extends AsyncTask<Void, Void, Boolean> implements HttpEvents{
	private final String TAG = "RosterDownloader";
	
	public static final String KEY_LOCOMOTIVE = "locomotive";
	
	private final int REQUEST_DOWNLOAD_ROSTER = 1;
	private final int REQUEST_DOWNLOAD_LOCOMOTIVE_IMAGE = 2;
	
	private RosterDownloadListener downloadListener;
	
	private Context context;
	private Http http;
	private Database database;
	private String addressString;
	private String locoImagesDirectoryPath;
	
	public RosterDownloader(Context context, Inet4Address ipAddress, int portNumber, RosterDownloadListener rosterDownloadListener){
		this.context = context;
		http = new Http(context, this);
		http.setDebuggingEnabled(false);
		database = new Database(context);
		addressString = "http://"+ipAddress.toString().substring(1)+":"+portNumber;
		downloadListener = rosterDownloadListener;
		locoImagesDirectoryPath = FileUtils.getSubDirectory(context, FileUtils.DIRECTORY_LOCOMOTIVE_IMAGES).getAbsolutePath();
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		http.executeGetRequest(REQUEST_DOWNLOAD_ROSTER, addressString+"/prefs/roster.xml", 5, 1, false, null);
		return null;
	}

	private void processRosterResponse(String rosterString) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputSource source = new InputSource();
			source.setCharacterStream(new StringReader(rosterString));
			Document document = builder.parse(source);
			Element rootElement = document.getDocumentElement(); 
			NodeList items = rootElement.getElementsByTagName("locomotive");
			int nItems = items.getLength();
			int nLocomotivesAdded = 0;
			for (int i=0; i<nItems; i++){	       	
				if(processRosterElement(items.item(i))){
					nLocomotivesAdded++;
				}
			}
			downloadListener.rosterDownloadComplete(nLocomotivesAdded);
			Log.d(TAG, nLocomotivesAdded+" locomotives added.");
		}
		catch (Exception e) {
			Log.e(TAG, "Error downloading roster: "+e.toString());
			e.printStackTrace();
		}
	}

	private boolean processRosterElement(Node node) {
		boolean returnCode = false;
		NamedNodeMap map = node.getAttributes();
		String nodeName, nodeValue;
		JSONObject functionsJson = null;
		String id = null, number = null, name = null, owner = null, manufacturer = null, model = null, dccAddress = null, imagePath = null, remoteImageName = null, comment = null;
		Integer maxSpeed = null;
		int nItems = map.getLength();
		for (int i=0; i<nItems; i++) {
			nodeName = map.item(i).getNodeName();
			nodeValue = map.item(i).getNodeValue();
			if(nodeName.equals("id")){
				id = nodeValue;
			}
			else if(nodeName.equals("roadNumber")){
				number = nodeValue;
			}
			else if(nodeName.equals("roadName")){
				name = nodeValue;
			}
			else if(nodeName.equals("dccAddress")){
				dccAddress = "L"+nodeValue;
			}
			else if(nodeName.equals("owner")){
				owner = nodeValue;
			}
			else if(nodeName.equals("mfg")){
				manufacturer = nodeValue;
			}
			else if(nodeName.equals("model")){
				model = nodeValue;
			}
			else if(nodeName.equals("imageFilePath")){
				if(nodeValue!=null && !nodeValue.isEmpty()){
					remoteImageName = nodeValue;
					File localImageFile = new File(locoImagesDirectoryPath, remoteImageName);
					imagePath = localImageFile.getAbsolutePath();
				}
			}
			else if(nodeName.equals("maxSpeed")){
				try{
					maxSpeed = Integer.parseInt(nodeValue);
				}
				catch(NumberFormatException e){}
			}
			else if(nodeName.equals("comment")){
				comment = nodeValue;
			}
		}
		NodeList children = node.getChildNodes();
		int nChildren = children.getLength();
		Node child;
		for(int i=0; i<nChildren; i++){
			child = children.item(i);
			if(child.getNodeName().equals("dcclocoaddress")){
				NamedNodeMap attributes = child.getAttributes();
				int nAttributes = attributes.getLength();
				String addressNumber = null, addressType = null;
				String attributeName, attributeValue;
				for(int j=0; j<nAttributes; j++){
					attributeName = map.item(i).getNodeName();
					attributeValue = map.item(i).getNodeValue();
					if(attributeName.equals("number")){
						addressNumber = attributeValue;
					}
					else if(attributeName.equals("longaddress")){
						addressType = attributeValue.equals("yes")? "L" : "S";
					}
				}
				if(addressNumber!=null && addressType!=null){
					dccAddress = addressType+addressNumber;
				}
			}
			else if(child.getNodeName().equals("functionlabels")){
				functionsJson = getFunctionsJson(child);
			}
		}
		
		if(id!=null && number!=null && dccAddress!=null){
			Locomotive locomotive = new Locomotive(id, dccAddress);
			if(owner!=null){
				locomotive.setOwner(owner);
			}
			if(number!=null){
				locomotive.setNumber(number);
			}
			if(name!=null){
				locomotive.setName(name);
			}
			if(manufacturer!=null){
				locomotive.setManufacturer(manufacturer);
			}
			if(model!=null){
				locomotive.setModel(model);
			}
			if(imagePath!=null){
				locomotive.setImagePath(imagePath);
			}
			if(maxSpeed!=null){
				locomotive.setMaxSpeed(maxSpeed);
			}
			if(comment!=null){
				locomotive.setComment(comment);
			}
			if(functionsJson!=null){
				locomotive.setFunctionsJsonString(functionsJson.toString());
			}
			if(database.addLocomotive(locomotive)){
				if(locomotive.hasImagePath()){
					downloadLocomotiveImage(locomotive, remoteImageName, imagePath);
				}
				returnCode = true;
			}
		}
		return returnCode;
	}

	private JSONObject getFunctionsJson(Node child) {
		JSONObject functionsJson = new JSONObject();
		NodeList nodes = child.getChildNodes();
		
		NamedNodeMap attributes;
		String attributeName, attributeValue;
		int nAttributes;
		Integer functionNumber = null;
		String functionName = null;
		boolean functionLockable = false;
		
		int nNodes = nodes.getLength();
		for(int i=0; i<nNodes; i++){
				if(nodes.item(i).getNodeType()==Node.ELEMENT_NODE){
				attributes = nodes.item(i).getAttributes();
				if(attributes!=null){
					nAttributes = attributes.getLength();
					for(int j=0; j<nAttributes; j++){
						attributeName = attributes.item(j).getNodeName();
						attributeValue = attributes.item(j).getNodeValue();
						if(attributeName.equals("num")){
							try{
								functionNumber = Integer.parseInt(attributeValue);
							}
							catch(NumberFormatException e){
							}
						}
						else if(attributeName.equals("lockable")){
							functionLockable = attributeValue.equals("true");
						}
					}
				}
				functionName = nodes.item(i).getTextContent();
			}
				if(functionNumber!=null){
					try{
						JSONObject functionJson = new JSONObject();
						functionJson.put(Function.KEY_NUMBER, functionNumber);
						functionJson.put(Function.KEY_NAME, functionName);
						functionJson.put(Function.KEY_LOCKABLE, functionLockable);
						functionsJson.put(functionNumber+"", functionJson);
					}
					catch(JSONException e){
						Log.d(TAG, "Error creating function JSON object: "+e.toString());
					}
			}
		}
		return functionsJson;
	}

	private void downloadLocomotiveImage(Locomotive locomotive, String remoteImageName, String localImagePath) {
		if(!FileUtils.checkFileExists(localImagePath, false)){		//Only download the image if it does not already exist
			Bundle extras = new Bundle();
			extras.putSerializable(KEY_LOCOMOTIVE, locomotive);
			String imageUrl = addressString+"/prefs/resources/"+remoteImageName;
			http.downloadFile(REQUEST_DOWNLOAD_LOCOMOTIVE_IMAGE, imageUrl, locoImagesDirectoryPath, remoteImageName, 3, 1, extras);
		}
	}

	@Override
	public void fileUploadProgress(Integer requestCode, long bytesUploaded, long fileSize, Bundle extras) {
	}

	@Override
	public void httpRequestComplete(int requestCode, String responseText, Bundle extras) {
		switch(requestCode){
		case REQUEST_DOWNLOAD_ROSTER:
			processRosterResponse(responseText);
			break;
		case REQUEST_DOWNLOAD_LOCOMOTIVE_IMAGE:
			Locomotive locomotive = (Locomotive) extras.getSerializable(KEY_LOCOMOTIVE);
			Log.d(TAG, "Locomotive image downloaded: "+locomotive.getId());
			break;
		default:
			break;
		}
	}

	@Override
	public void httpError(int requestCode, int errorCode, Bundle extras) {
		Log.d(TAG, "ERROR: "+errorCode);
		switch(requestCode){
		case REQUEST_DOWNLOAD_ROSTER:
			downloadListener.rosterDownloadFailed();
			break;
		}
	}
	
	public interface RosterDownloadListener{
		
		public void rosterDownloadComplete(int nLocomotives);
		
		public void rosterDownloadFailed();
	}
}
