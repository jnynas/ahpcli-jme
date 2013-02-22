/*
 Copyright © 2012 Paul Houghton and Futurice on behalf of the Tantalum Project.
 All rights reserved.

 Tantalum software shall be used to make the world a better place for everyone.

 This software is licensed for use under the Apache 2 open source software license,
 http://www.apache.org/licenses/LICENSE-2.0.html

 You are kindly requested to return your improvements to this library to the
 open source community at http://projects.developer.nokia.com/Tantalum

 The above copyright and license notice notice shall be included in all copies
 or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */
package org.tantalum.j2me;

import java.util.Vector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import org.tantalum.Workable;
import org.tantalum.Worker;
import org.tantalum.storage.FlashDatabaseException;
import org.tantalum.util.L;
import org.tantalum.util.LengthLimitedVector;

/**
 * RMS Utility methods
 *
 * @author ssaa
 */
public class RMSUtils {

    private static final int MAX_RECORD_NAME_LENGTH = 32;
    private static final int MAX_OPEN_RECORD_STORES = 10;
    private static final String RECORD_HASH_PREFIX = "@";
    private static final LengthLimitedVector openRecordStores = new LengthLimitedVector(MAX_OPEN_RECORD_STORES) {
        protected void lengthExceeded(final Object extra) {
            final RecordStore rs = (RecordStore) extra;
            String rsName = "";

            try {
                rsName = rs.getName();
                //#debug
                L.i("Closing LRU record store", rsName + " open=" + openRecordStores.size());
                rs.closeRecordStore();
                //#debug
                L.i("LRU record store closed", rsName + " open=" + openRecordStores.size());
            } catch (Exception ex) {
                //#debug
                L.e("Can not close extra record store", rsName, ex);
            }
        }
    };

    static {
        /**
         * Close all open record stores during shutdown
         *
         */
        Worker.forkShutdownTask(new Workable() {
            public Object exec(final Object in) {
                //#debug
                L.i("Closing record stores during shutdown", "open=" + openRecordStores.size());
                openRecordStores.setMaxLength(0);
                //#debug
                L.i("Closed record stores during shutdown", "open=" + openRecordStores.size());

                return in;
            }
        });
    }

    /**
     * Return of a list of record stores whose name indiates that they are caches
     * 
     * @return 
     */
    public static Vector getCachedRecordStoreNames() {
        final String[] rs = RecordStore.listRecordStores();
        final Vector v = new Vector(rs.length);

        for (int i = 0; i < rs.length; i++) {
            if (rs[i].startsWith(RECORD_HASH_PREFIX)) {
                v.addElement(rs[i]);
            }
        }

        return v;
    }

    private static String getRecordStoreCacheName(final String key) {
        final StringBuffer sb = new StringBuffer(MAX_RECORD_NAME_LENGTH);

        sb.append(RECORD_HASH_PREFIX);
        sb.append(Integer.toString(key.hashCode(), Character.MAX_RADIX));
        final int fullLength = sb.length() + key.length();
        if (fullLength > MAX_RECORD_NAME_LENGTH) {
            sb.append(key.substring(fullLength - MAX_RECORD_NAME_LENGTH));
        } else {
            sb.append(key);
        }

        return sb.toString();
    }

    /**
     * Write to the record store a cached value based on the hashcode of the key
     * to the data
     *
     * @param key
     * @param data
     * @throws RecordStoreFullException 
     */
    public static void cacheWrite(final String key, final byte[] data) throws RecordStoreFullException {
        write(getRecordStoreCacheName(key), data);
    }

    /**
     * Read from the record store a cached value based on the hashcode of the
     * key to the data
     *
     * @param key
     * @return
     * @throws FlashDatabaseException 
     */
    public static byte[] cacheRead(final String key) throws FlashDatabaseException {
        return read(getRecordStoreCacheName(key));
    }

    /**
     * Delete one item from a cache
     * 
     * @param key 
     */
    public static void cacheDelete(final String key) {
        delete(getRecordStoreCacheName(key));
    }

    /**
     * Writes the byte array to the record store. Deletes the previous data.
     *
     * @param recordStoreName
     * @param data
     * @throws RecordStoreFullException 
     */
    public static void write(String recordStoreName, final byte[] data) throws RecordStoreFullException {
        RecordStore rs = null;

        try {
            //delete old value
            //#debug
            L.i("Add to RMS", recordStoreName + " (" + data.length + " bytes)");
            recordStoreName = truncateRecordStoreName(recordStoreName);
            rs = getRecordStore(recordStoreName, true);

            if (rs.getNumRecords() == 0) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
            //#debug
            L.i("Added to RMS", recordStoreName + " (" + data.length + " bytes)");
        } catch (Exception e) {
            //#debug
            L.e("RMS write problem", recordStoreName, e);
            if (e instanceof RecordStoreFullException) {
                throw (RecordStoreFullException) e;
            }
        }
    }

    /**
     * Get a RecordStore. This method supports a global pool of open record
     * stores and thereby avoids repeated opening and closing of record stores
     * which are used several times.
     *
     * @param recordStoreName
     * @param createIfNecessary
     * @return null if the record store does not exist
     * @throws RecordStoreException
     */
    public static RecordStore getRecordStore(final String recordStoreName, final boolean createIfNecessary) throws RecordStoreException {
        RecordStore rs = null;
        boolean success = false;

        try {
            rs = RecordStore.openRecordStore(recordStoreName, createIfNecessary);
            openRecordStores.addElement(rs);
            success = true;
        } catch (RecordStoreNotFoundException e) {
            success = !createIfNecessary;
            rs = null;
        } finally {
            if (!success) {
                //#debug
                L.i("Can not open record store", "Deleting " + recordStoreName);
                delete(recordStoreName);
            }
        }

        return rs;
    }

    /**
     * Delete one item
     * 
     * @param recordStoreName 
     */
    public static void delete(String recordStoreName) {
        try {
            final RecordStore[] recordStores;
            recordStoreName = truncateRecordStoreName(recordStoreName);

            synchronized (openRecordStores) {
                recordStores = new RecordStore[openRecordStores.size()];
                openRecordStores.copyInto(recordStores);
            }

            /**
             * Close existing references to the record store
             * 
             * NOTE: This does not absolutely guarantee that there is no other
             * thread accessing this record store at this exact moment. If that
             * happens, you will be prevented from deleting the record store and
             * "RMS delete problem" message will show up in your debug. For this
             * reason, your application's logic may need to take into account
             * that a delete might not be completed.
             * 
             * TODO Without adding complexity, a file locking mechanism or other
             * solution may be added in future. Another solution might be to read
             * and remember the RMS contents on startup, use that as an in-memory
             * index... Still expensive. -paul
             */
            for (int i = 0; i < recordStores.length; i++) {
                try {
                    if (recordStores[i].getName().equals(recordStoreName)) {
                        openRecordStores.markAsExtra(recordStores[i]);
                    }
                } catch (RecordStoreNotOpenException ex) {
                    L.e("Mark as extra close of record store failed", recordStoreName, ex);
                }
            }
            RecordStore.deleteRecordStore(recordStoreName);
        } catch (RecordStoreNotFoundException ex) {
            //#debug
            L.i("RMS not found (normal result)", recordStoreName);
        } catch (RecordStoreException ex) {
            //#debug
            L.e("RMS delete problem", recordStoreName, ex);
        }
    }

    /**
     * Shorten the name to fit within the 32 character limit imposed by RMS.
     * 
     * @param recordStoreName
     * @return 
     */
    public static String truncateRecordStoreName(String recordStoreName) {
        if (recordStoreName.length() > MAX_RECORD_NAME_LENGTH) {
            recordStoreName = recordStoreName.substring(0, MAX_RECORD_NAME_LENGTH);
        }

        return recordStoreName;
    }

    /**
     * Reads the data from the given record store.
     *
     * @param recordStoreName
     * @return
     * @throws FlashDatabaseException 
     */
    public static byte[] read(String recordStoreName) throws FlashDatabaseException {
        RecordStore rs = null;
        byte[] data = null;

        try {
            recordStoreName = truncateRecordStoreName(recordStoreName);
            //#debug
            L.i("Read from RMS", recordStoreName);
            rs = getRecordStore(recordStoreName, false);
            if (rs != null && rs.getNumRecords() > 0) {
                data = rs.getRecord(1);
                //#debug
                L.i("End read from RMS", recordStoreName + " (" + data.length + " bytes)");
            } else {
                //#debug
                L.i("End read from RMS", recordStoreName + " (NOTHING TO READ)");
            }
        } catch (Exception e) {
            //#debug
            L.e("Can not read RMS", recordStoreName, e);
            throw new FlashDatabaseException("Can not read record from RMS: " + recordStoreName + " : " + e);
        }

        return data;
    }
}
