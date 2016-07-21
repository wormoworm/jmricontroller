package uk.tomhomewood.android.jmricontroller;

public class Turnout {

	public final static int STATE_UNKNOWN = 1;
	public final static int STATE_CLOSED = 2;
	public final static int STATE_THROWN = 4;

	private String name, address;
	private int state;
	
	public Turnout(String address, String name){
		this.name = name;
		this.address = address;
		setState(STATE_UNKNOWN);
	}

	public Turnout(String address, String name, int state){
		this(address, name);
		setState(state);
	}
	
	public void setState(int newState) {
		state = newState;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public int getState() {
		return state;
	}
	
	public boolean equals(Turnout turnout){
		return address.equals(turnout.getAddress());
	}

    @Override
    public String toString() {
        return "Address: "+getAddress()+", name: "+getName()+", state: "+getState();
    }
}