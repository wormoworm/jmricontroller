package uk.tomhomewood.android.jmricontroller;

public class Function{

	public static final String KEY_NUMBER = "number";
	public static final String KEY_NAME = "name";
	public static final String KEY_LOCKABLE = "lockable";

	private int number;
	private String name;
	private boolean lockable, isTurnedOn;

	public Function(int number, String name, boolean lockable){
		this.number = number;
		this.name = name;
		this.lockable = lockable;
		isTurnedOn = false;
	}

	public void setName(String name){
		this.name = name;
	}

	public void setLockable(boolean lockable){
		this.lockable = lockable;
	}

	public int getNumber() {
		return number;
	}

	public String getName() {
		return name;
	}

	public boolean isLockable() {
		return lockable;
	}
	
	public void turnOn(){
		isTurnedOn = true;
	}
	
	public void turnOff(){
		isTurnedOn = false;
	}

	public boolean isTurnedOn() {
		return isTurnedOn;
	}
}