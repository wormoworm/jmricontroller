package uk.tomhomewood.android.jmricontroller;

import java.io.File;
import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

public class Locomotive implements Serializable{
//	private final String TAG = "Locomotive";
	
	private static final long serialVersionUID = 1L;

	private String id, number, name, owner, manufacturer, model, address, imagePath, comment;
	private int maxSpeed;

	private String functionsJsonString;

	public Locomotive(String id, String dccAddress) {
		this.id = id;
		this.address = dccAddress;
		maxSpeed = 100;
	}

	public String getId() {
		return id;
	}

	public String getAddress() {
		return address;
	}

	public void setNumber(String number){
		this.number = number;
	}

	public String getNumber() {
		if(number!=null && !number.isEmpty()){
			return number;
		}
		else{
			return id;
		}
	}

	public String getName(){
		return name;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(String manufacturer) {
		this.manufacturer = manufacturer;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getImagePath() {
		return imagePath;
	}
	
	public File getImageFile(){
		if(imagePath!=null){
			File imageFile = new File(imagePath);
			if(imageFile.exists() && imageFile.isFile()){
				return imageFile;
			}
		}
		return null;
	}

	public boolean hasImage(){
		return getImageFile()!=null;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	public boolean hasImagePath(){
		return getImagePath()!=null;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public void setMaxSpeed(Integer maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public boolean equals(Locomotive locomotive){
		return address.equals(locomotive.getAddress());
	}
	
	public void setFunctionsJsonString(String functionsJsonString){
		this.functionsJsonString = functionsJsonString;
	}

	public JSONObject getFunctionJson(int functionNumber){
		JSONObject functionJson = null;
		JSONObject functionsJson = getFunctionsJson();
		if(functionsJson!=null){
			functionJson = functionsJson.optJSONObject(""+functionNumber);
		}
		return functionJson;
	}

	public JSONObject getFunctionsJson(){
		JSONObject functionsJson = null;
		if(functionsJsonString!=null){
			try{
				functionsJson = new JSONObject(functionsJsonString);
			}
			catch(JSONException e){}
		}
		return functionsJson;
	}

	public String getFunctionsJsonString(){
		return functionsJsonString;
	}
}
