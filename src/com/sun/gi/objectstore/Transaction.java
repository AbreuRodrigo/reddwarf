package com.sun.gi.objectstore;

import java.io.*;
import java.sql.Timestamp;

/**
 * <p>Title: Transaction.java</p>
 * <p>Description: A transactonal context through which data stored in the ObejctStore
 * can be manipulated.</p>
 * <p>Copyright: Copyright (c) 2003 Sun Microsystems, Inc</p>
 * <p>Company: Sun Microsystems, Inc</p>
 * @author Jeff Kesselman
 * @version 1.0
 */

public interface Transaction {
	/**
	 * This method is called to create a new entry in the ObjectStore.
	 * @param object The object who's state should be stored
	 * @param name An optional name for the object (null means no-name).
	 * @return The Object ID assigned to this data record.
	 */
    public long create(Serializable object, String name);
    
    /**
     * This method creates a new entry in the ObjectStore and forces its ID to
     * the long passed in objectID.  Its really a special purpose routine only used
     * for a couple of things internal to the Darkstar stack.
     * @param objectID The object ID to assign to this data record
     * @param object The object who's state will be stored in this data record
     * @param name The obejct's name (or null)
     * @return true if successful, false if it fails.
     */
    public boolean create(long objectID, Serializable object, String name);
    
    /**
     * This method removes an entry from the Object Store
     * @param objectID The ID of the data record to remove
     */
    public void destroy(long objectID);
    
    /** 
     * <p>This method fetches a local copy of an object stored in the object store. It 
     * does not lock the object ion the store.</p>
     * <p>If the object has previously been "peeked" by this transaction, the a referene to the previously 
     * created local copy is returned.  If the object has previously been "getted" by this
     * transaction then a reference to that write-locked copy is returned.</p>
     * <p>Any object that has only been "peeked' and not "getted" will be thrown away 
     * and all state changes lost at the end of the transaction.</p>   
     * @param objectID  The ID of the object to return.
     * @return A reference to a local copy of the object referenced by objectID
     */
    public Serializable peek(long objectID);
    
    /** 
     * <p>This method takes a write-lock on the object referenced by objectID and returns
     * a copy of the object to work with.</p>
     * <p>If the obejct has been perviously "getted" then this call will return a refernce 
     * to the same object.  If the object has been "peeked' but not "getted", or if this
     * is the first get operation on the obejct udne this transaction, a new copy will be returned,</p>
     * <p>When the transaction commits, all changes to the state of "getted" objects is
     * captured and written back to the ObjectStore.</p<>   
     * @param objectID  The ID of the object to return.
     * @return A reference to a copy of the object referenced by objectID
     */
    public Serializable lock(long objectID) throws DeadlockException;
    
    /**
     * This method returns the object ID for an object that has previously been created with a name.
     * @param name The name of the object
     * @return The named object's object ID.
     */
    public long lookup(String name); // proxies to Ostore
    
    /**
     * This method is called to abort this transaction.  All changes to "getted"
     * objects get discarded and their locks released.
     *
     */
    public void abort();
    
    /** 
     * This method is called to commit this transaction.  All changes to "getted"
     * objects are written abck to the ObjectStore and their locks are released.
     *
     */
    public void commit();
    
    /**
     * Returns the AppID that this transaction is using to access objects in the
     * ObjectStore.
     * @return The AppID
     */
    public long getCurrentAppID();


    // DJE
    public void start(long appID, Timestamp tstamp, long tiebreaker,
	    ClassLoader cl);
    public void tstampInterrupt();
   
}
