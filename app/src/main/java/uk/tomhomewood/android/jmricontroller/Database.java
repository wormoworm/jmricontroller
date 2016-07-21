package uk.tomhomewood.android.jmricontroller;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * A global database class that holds all persistent data for the app
 * @author Tom
 *
 */
public class Database extends SQLiteOpenHelper {
	private final static String TAG =  "Database";
	
//	private Context context;

	//Database Version
	private static final int DATABASE_VERSION = 1;

	//Database Name
	private static final String DATABASE_NAME = "jmriControllerDatabase";
	
	//Table names
	private final String TABLE_TURNOUTS = "turnouts";
	private final String TABLE_LOCOMOTIVES = "locomotives";
	
	//Shared column names
	public static final String KEY_ID = "id";
	public static final String KEY_ADDRESS = "address";
	public static final String KEY_NAME = "name";
	
	//Column names for the locomotive table
	public static final String KEY_NUMBER = "number";
	public static final String KEY_OWNER = "owner";
	public static final String KEY_MANUFACTURER = "manufacturer";
	public static final String KEY_MODEL = "model";
	public static final String KEY_IMAGE_PATH = "imagePath";
	public static final String KEY_COMMENT = "comment";
	public static final String KEY_MAX_SPEED = "maxSpeed";
	public static final String KEY_FUNCTIONS = "functions";
	
	
	//Column name sets for each of the tables
	private final String[] columnsTurnoutsAll = {
			KEY_ADDRESS,
			KEY_NAME};
	private final String[] columnsLocomotivesAll = {
			KEY_ID,
			KEY_ADDRESS,
			KEY_NUMBER,
			KEY_NAME,
			KEY_OWNER,
			KEY_MANUFACTURER,
			KEY_MODEL,
			KEY_IMAGE_PATH,
			KEY_COMMENT,
			KEY_MAX_SPEED,
			KEY_FUNCTIONS
			};
	
	//Creation statements for each of the tables
	private String createTableTurnouts = "CREATE TABLE IF NOT EXISTS "+TABLE_TURNOUTS+" ("
		+KEY_ADDRESS+" TEXT PRIMARY KEY,"
		+KEY_NAME+" TEXT)";

	private String createTableLocomotives = "CREATE TABLE IF NOT EXISTS "+TABLE_LOCOMOTIVES+" ("
			+KEY_ID+" TEXT PRIMARY KEY,"
			+KEY_NUMBER+" TEXT,"
			+KEY_NAME+" TEXT,"
			+KEY_OWNER+" TEXT,"
			+KEY_MANUFACTURER+" TEXT,"
			+KEY_MODEL+" TEXT,"
			+KEY_ADDRESS+" TEXT,"
			+KEY_IMAGE_PATH+" TEXT,"
			+KEY_COMMENT+" TEXT,"
			+KEY_MAX_SPEED+" INTEGER,"
			+KEY_FUNCTIONS+" TEXT)";

	/**
	 * Constructor for the Database. Upgrading between database versions is handled by the system.
	 * @param context		The context of the activity or service that instantiated the Database.
	 */
	public Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
//		this.context = context;
	}

	/**
	 * Called when the database is created. Handles creation of the various tables.
	 */
	@Override
	public void onCreate(SQLiteDatabase database) {
		//Create the various tables we will use
		database.execSQL(createTableTurnouts);
		database.execSQL(createTableLocomotives);
	}
	
	/**
	 * Called when the database is upgraded. Handles any changes we define.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
		//Changes are handled in a for loop, with each change represented as an interaction
//		String updateOperation;
		for(int i=oldVersion+1; i<=newVersion; i++){
			switch(i){
			default:
				//updateOperation = null;
				break;
			}
/*
			if(updateOperation!=null){
				database.execSQL(updateOperation);
			}
*/
		}
		Log.d(TAG, "Database upgraded to version "+newVersion);
		onCreate(database);
	}

	public boolean addTurnout(Turnout turnout){
		boolean returnCode = false;
		if(turnoutExists(turnout)){
//			Log.e(TAG, "Turnout with address: "+turnout.getAddress()+" already exists");
		}
		else{
			ContentValues row = creatRowFromTurnout(turnout);
			SQLiteDatabase database = getWritableDatabase();
			try{
				database.insertOrThrow(TABLE_TURNOUTS, null, row);
				returnCode = true;
				//Log.d(TAG, "Turnout added: Address = "+turnout.getAddress());
			}
			catch(SQLException e){
//				Log.e(TAG, "Error adding turnout: "+e.toString());
			}
			database.close();
		}
		return returnCode;
	}
	
	public Turnout getTurnoutByAddress(String turnoutAddress){
		Turnout turnout = null;
		String selection = KEY_ADDRESS+" = \""+turnoutAddress+"\"";
		SQLiteDatabase database = this.getReadableDatabase();	//Connect to the database
		Cursor cursor = database.query(TABLE_TURNOUTS, columnsTurnoutsAll, selection, null,  null, null, null, null);
		if (cursor.moveToFirst()){								//True if we got a result from the database
			turnout = getTurnoutFromCursor(cursor);
		}
		database.close();
		return turnout;
	}
	
	public ArrayList<Turnout> getTurnouts() {
		ArrayList<Turnout> turnouts = new ArrayList<Turnout>();
		SQLiteDatabase database = this.getReadableDatabase();	//Connect to the database
		Cursor cursor = database.query(TABLE_TURNOUTS, columnsTurnoutsAll, null, null,  null, null, null, null);
		Turnout temp;
		if (cursor.moveToFirst()){								//True if we got a result from the database
			do {						//Loop through
				temp = getTurnoutFromCursor(cursor);
				if(temp!=null){
					turnouts.add(temp);
				}
			}
			while(cursor.moveToNext());
		}
		database.close();
		return turnouts;
	}

	public boolean turnoutExists(Turnout turnout) {
		return getTurnoutByAddress(turnout.getAddress())!=null;
	}
	
	public void deleteTurnouts(){
		SQLiteDatabase database = getWritableDatabase();
		database.execSQL("DELETE FROM "+TABLE_TURNOUTS);
		database.close();
	}

	private ContentValues creatRowFromTurnout(Turnout turnout) {
		ContentValues row = new ContentValues();
		row.put(KEY_ADDRESS, turnout.getAddress());
		row.put(KEY_NAME, turnout.getName());
		return row;
	}

	private Turnout getTurnoutFromCursor(Cursor cursor) {
		Turnout turnout = null;
		turnout = new Turnout(cursor.getString(0), cursor.getString(1));
		return turnout;
	}
	
	public boolean locomotiveExists(Locomotive locomotive){
		return getTurnoutByAddress(locomotive.getAddress())!=null;
	}

	public boolean addLocomotive(Locomotive locomotive) {
		boolean returnCode = false;
		if(locomotiveExists(locomotive)){
			//Log.e(TAG, "Locomotive with address: "+locomotive.getAddress()+" already exists");
		}
		else{
			ContentValues row = creatRowFromLocomotive(locomotive);
			SQLiteDatabase database = getWritableDatabase();
			try{
				database.insertOrThrow(TABLE_LOCOMOTIVES, null, row);
				returnCode = true;
				//Log.d(TAG, "Locomotive added: Address = "+locomotive.getAddress());
			}
			catch(SQLException e){
//				Log.e(TAG, "Error adding locomotive: "+e.toString());
			}
			database.close();
		}
		return returnCode;
	}
	
	public Locomotive getLocomotiveByAddress(String locomotiveAddress){
		Locomotive locomotive = null;
		String selection = KEY_ADDRESS+" = \""+locomotiveAddress+"\"";
		SQLiteDatabase database = this.getReadableDatabase();	//Connect to the database
		Cursor cursor = database.query(TABLE_LOCOMOTIVES, columnsLocomotivesAll, selection, null,  null, null, null, null);
		if (cursor.moveToFirst()){								//True if we got a result from the database
			locomotive = getLocomotiveFromCursor(cursor);
		}
		database.close();
		return locomotive;
	}
	
	public ArrayList<Locomotive> getLocomotives(){
		ArrayList<Locomotive> locomotives = new ArrayList<Locomotive>();
		SQLiteDatabase database = this.getReadableDatabase();	//Connect to the database
		Cursor cursor = database.query(TABLE_LOCOMOTIVES, columnsLocomotivesAll, null, null,  null, null, null, null);
		Locomotive temp;
		if (cursor.moveToFirst()){								//True if we got a result from the database
			do {						//Loop through
				temp = getLocomotiveFromCursor(cursor);
				if(temp!=null){
					locomotives.add(temp);
				}
			}
			while(cursor.moveToNext());
		}
		database.close();
		return locomotives;
	}
	
	public void deleteLocomotives(){
		SQLiteDatabase database = getWritableDatabase();
		database.execSQL("DELETE FROM "+TABLE_LOCOMOTIVES);
		database.close();
	}
	
	private ContentValues creatRowFromLocomotive(Locomotive locomotive) {
		ContentValues row = new ContentValues();
		row.put(KEY_ID, locomotive.getId());
		row.put(KEY_ADDRESS, locomotive.getAddress());
		row.put(KEY_NUMBER, locomotive.getNumber());
		row.put(KEY_NAME, locomotive.getName());
		row.put(KEY_OWNER, locomotive.getOwner());
		row.put(KEY_MANUFACTURER, locomotive.getManufacturer());
		row.put(KEY_MODEL, locomotive.getModel());
		row.put(KEY_IMAGE_PATH, locomotive.getImagePath());
		row.put(KEY_COMMENT, locomotive.getComment());
		row.put(KEY_MAX_SPEED, locomotive.getMaxSpeed());
		row.put(KEY_FUNCTIONS, locomotive.getFunctionsJsonString());
		return row;
	}

	private Locomotive getLocomotiveFromCursor(Cursor cursor) {
		Locomotive locomotive = null;
		locomotive = new Locomotive(cursor.getString(0), cursor.getString(1));
		locomotive.setNumber(cursor.getString(2));
		locomotive.setName(cursor.getString(3));
		locomotive.setOwner(cursor.getString(4));
		locomotive.setManufacturer(cursor.getString(5));
		locomotive.setModel(cursor.getString(6));
		locomotive.setImagePath(cursor.getString(7));
		locomotive.setComment(cursor.getString(8));
		locomotive.setMaxSpeed(cursor.getInt(9));
		locomotive.setFunctionsJsonString(cursor.getString(10));
		return locomotive;
	}
}