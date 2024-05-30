/*
package com.eveningoutpost.dexdrip.services;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MongoWrapper {

	MongoClient mongoClient_;
	String dbUriStr_;
	String dbName_;
	String collection_;
	String index_;
	String machineName_;
	private final static String TAG = "WixelReader";//KS WixelReader.class.getName();

	public MongoWrapper(String dbUriStr, String collection, String index, String machineName) {
		dbUriStr_ = dbUriStr;
		// dbName is the last part of the string starting with /dbname
		dbName_ = dbUriStr.substring(dbUriStr.lastIndexOf('/') + 1);
		collection_ = collection;
		index_ = index;
		machineName_ = machineName;
	}

	// Unfortunately, this also throws other exceptions that are not documetned...
    public DBCollection openMongoDb() throws UnknownHostException {

    	MongoClientURI dbUri = new MongoClientURI(dbUriStr_+"?socketTimeoutMS=180000");
	    mongoClient_ = new MongoClient(dbUri);

	    DB db = mongoClient_.getDB( dbName_ );
	    DBCollection coll = db.getCollection(collection_);
	    coll.createIndex(new BasicDBObject(index_, 1));  // create index on "i", ascending

	    return coll;

    }

     public void closeMongoDb() {
         if(mongoClient_ != null) {
    	 	mongoClient_.close();
         }
     }

     public boolean WriteDebugDataToMongo(String message)
     {
    	 String complete = machineName_ + " " + new Date().toLocaleString() + " " + message;
    	 BasicDBObject doc = new BasicDBObject("DebugMessage", complete);
    	 return WriteToMongo(doc);
     }


     public boolean WriteToMongo(TransmitterRawData trd)
     {
    	 BasicDBObject bdbo = trd.toDbObj(machineName_ + " " + new Date(trd.CaptureDateTime).toLocaleString());
    	 return WriteToMongo(bdbo);
     }

     public boolean WriteToMongo(BasicDBObject bdbo)
     {
     	DBCollection coll;
     	try {
     		coll = openMongoDb();
         	coll.insert(bdbo);

 		} catch (UnknownHostException e) {
 		   Log.e(TAG, "WriteToMongo caught UnknownHostException! ",e);
 			return false;
 		} catch (MongoException e) {
 		   Log.e(TAG, "WriteToMongo caught MongoException! ", e);
 			return false;
 		} catch (Exception e) {
 		   Log.e(TAG, "WriteToMongo caught Exception! ", e);
 			closeMongoDb();
 			return false;
 		}
     	finally {
 			closeMongoDb();
 		}
     	return true;
     }

	public static boolean almostEquals( TransmitterRawData e1, TransmitterRawData e2)//KS from WixelReader.java
	{
		if (e1 == null || e2==null) {
			return false;
		}
		// relative time is in ms
		if ((Math.abs(e1.CaptureDateTime - e2.CaptureDateTime) < 120 * 1000 ) &&
				(e1.TransmissionId == e2.TransmissionId)) {
			return true;
		}
		return false;
	}

	// records will be marked by their timestamp
     public List<TransmitterRawData> ReadFromMongo(int numberOfRecords) {
    	System.out.println( "Starting to read from mongodb");

    	List<TransmitterRawData> trd_list = new LinkedList<TransmitterRawData>();
      	DBCollection coll;
      	TransmitterRawData lastTrd = null;
      	try {
      		coll = openMongoDb();
            BasicDBObject query = new BasicDBObject("RawValue", new BasicDBObject("$exists", true));
            DBCursor cursor = coll.find(query);
            cursor.sort(new BasicDBObject("CaptureDateTime", -1));
            try {
                while(cursor.hasNext() && trd_list.size() < numberOfRecords) {
                    //System.out.println(cursor.next());
                    Log.d(TAG, "Read an object from mongodb");
                    TransmitterRawData trd = new TransmitterRawData((BasicDBObject)cursor.next());
                    // Do our best to fix the relative time...
                    trd.RelativeTime = new Date().getTime() - trd.CaptureDateTime;
                    // since we are reading it from the db, it was uploaded...
                    trd.Uploaded = 1;
                    if(lastTrd == null) {
                    	trd_list.add(0,trd);
                    	lastTrd = trd;
                    	System.out.println( trd.toTableString());
                    } else if(!almostEquals(lastTrd, trd)) {//KS WixelReader.
                    	lastTrd = trd;
                    	trd_list.add(0,trd);
                    	System.out.println( trd.toTableString());
                    }

                }
             } finally {
                cursor.close();
             }

        } catch (UnknownHostException e) {
            Log.e(TAG, "ReadFromMongo: caught UnknownHostException! ", e);
            return null;
        } catch (MongoException e) {
            Log.e(TAG, "ReadFromMongo: caught MongoException! " , e);
            return trd_list;
        } catch (Exception e) {
  		      Log.e(TAG, "ReadFromMongo: caught Exception! " , e);
  		      closeMongoDb();
 			return null;
 		}finally {
  			closeMongoDb();
  		}
      	return trd_list;

     }


}
*/
