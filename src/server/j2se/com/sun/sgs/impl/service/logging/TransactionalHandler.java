/*
 * Copyright 2007 Sun Microsystems, Inc.
 *
 * This file is part of Project Darkstar Server.
 *
 * Project Darkstar Server is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation and
 * distributed hereunder to you.
 *
 * Project Darkstar Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sun.sgs.impl.service.logging;

import com.sun.sgs.service.NonDurableTransactionParticipant;
import com.sun.sgs.service.Transaction;
import com.sun.sgs.service.TransactionProxy;

import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;


/**
 * A {@code Handler} implementation that given a backing {@code
 * Handler}, provides transactional semantics for the {@code
 * #publish(LogRecord)} method.  This class is used by the {@link
 * LoggingServiceImpl} to wrap existing handlers as specificed by the
 * application.
 *
 * @see com.sun.sgs.service.LoggingService
 */
public class TransactionalHandler extends Handler
    implements NonDurableTransactionParticipant {

    /**
     * A mapping from transaction to the list of records waiting to be
     * published on transaction commit.
     */
    private final Map<Transaction,List<LogRecord>> bufferedRecords;
    
    /**
     * The proxy used to join transactions upon the first {@link
     * #publish(LogRecord)} call for that transaction.
     */
    private final TransactionProxy proxy;

    /**
     * The backing handler for this instance.  All modification calls
     * are passed through to this handler.
     */
    private final Handler handler;

    /**
     * Constructs a new {@code TransactionalHandler} with the provided
     * {@code proxy} for joining transactions a the backing handler
     * for performing the actual logging.
     *
     * @param proxy the proxy used to join transactions
     * @param backingHandler the handler used to perform the actual
     *        logging at commit time
     */
    TransactionalHandler(TransactionProxy proxy, Handler backingHandler) {
	this.proxy = proxy;
	this.handler = backingHandler;
	bufferedRecords = new HashMap<Transaction,List<LogRecord>>();
    }

    /**
     * Removes any buffered records for this transaction and performs
     * no logging.
     *
     * @param txn the failed transaction that in the past performed
     *        some logging operation
     */
    public void abort(Transaction txn) {
	bufferedRecords.remove(txn);	
    }
    
    /**
     * {@inheritDoc}
     */
    public void close() {
	handler.close();
    }

    /**
     * Removes any buffered records for this transaction and logs all
     * buffered records using the backing {@code Handler}.
     *
     * @param txn the successful transaction that in the past
     *        performed some logging operation
     */
    public void commit(Transaction txn) { 
	List<LogRecord> records = bufferedRecords.remove(txn);
	for (LogRecord r : records) {
	    handler.publish(r);
	}
    }

    /**
     * {@inheritDoc}
     */
    public void flush() {
	handler.flush();
    }

    /**
     * Returns the {@code Handler} used to publish all buffered {@code
     * LogRecord}s when a transaction commmits.
     *
     * @return the backing handler used for actual record publication
     */
    public Handler getBackingHandler() {
	return handler;
    }

    /**
     * {@inheritDoc}
     */
    public String getEncoding() {
	return handler.getEncoding();
    }

    /**
     * {@inheritDoc}
     */
    public ErrorManager getErrorManager() {
	return handler.getErrorManager();
    }

    /**
     * {@inheritDoc}
     */
    public Filter getFilter() {
	return handler.getFilter();
    }

    /**
     * {@inheritDoc}
     */
    public Formatter getFormatter() {
	return handler.getFormatter();
    }

    /**
     * {@inheritDoc}
     */
    public Level getLevel() {
	return handler.getLevel();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLoggable(LogRecord record) {
	return handler.isLoggable(record);
    }

    /**
     * {@inheritDoc}
     *
     * This participant always returns {@code false}.
     *
     * @return {@code false}
     */
    public boolean prepare(Transaction txn) {
	return false;
    }

    /**
     * Removes any buffered records for this transaction and logs all
     * buffered records using the backing {@code Handler}.
     *
     * @param txn the successful transaction that in the past
     *        performed some logging operation
     */
    public void prepareAndCommit(Transaction txn) {
	commit(txn);
    }
   
    /**
     * Joins the current transaction and buffers the provided record
     * for later publication until transaction commit time.
     *
     * @param record the record to be buffer and later publication
     *        upon transaction success.
     */
    public void publish(LogRecord record) {
	Transaction txn = proxy.getCurrentTransaction();
	List<LogRecord> records = bufferedRecords.get(txn);
	if (records == null) {
	    txn.join(this);
	    records = new LinkedList<LogRecord>();
	    bufferedRecords.put(txn, records);
	}
	records.add(record);
    }

    /**
     * {@inheritDoc}
     */
    public void reportError(String msg, Exception ex, int code) {
	// we can't call report error on the handler directly because
	// it has protected access, so we emulate the code in
	// Hander.java directly here, including the catch block
	try {
	    handler.getErrorManager().error(msg,ex,code);
	} catch (Exception ex2) {
	    System.err.println("TransactionalHandler.reportError caught:");
	    ex2.printStackTrace();
	}
    }

    /**
     * {@inheritDoc}
     */
    public void setEncoding(String encoding) 
	throws java.io.UnsupportedEncodingException {
	handler.setEncoding(encoding);
    }

    /**
     * {@inheritDoc}
     */
    public void setErrorManager(ErrorManager em) {
	handler.setErrorManager(em);
    }

    /**
     * {@inheritDoc}
     */
    public void setFilter(Filter newFilter) {
	handler.setFilter(newFilter);
    }

    /**
     * {@inheritDoc}
     */
    public void setFormatter(Formatter newFormatter) {
	handler.setFormatter(newFormatter);
    }

     /**
     * {@inheritDoc}
     */
   public void setLevel(Level newLevel) {
	handler.setLevel(newLevel);
    }

}