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
package org.tantalum;

/**
 * An implementation of Android's AsyncTask pattern for cross platform application
 * on J2ME and Android. On Android it is intended as a drop-in
 * replacement for the platform library class. The less commonly used features
 * of the Android implementation are not supported. Unlike the Android version,
 * you can join() or jointUI() a Tantalum AsyncTask to trigger out-of-sequence
 * execution or wait a specified max time interval for the AsyncTask to complete.
 * This design pattern is from Java 7's fork-join framework, but instead of
 * using for server scalability we use it here for convenience in improving
 * client side user experience.
 */
public abstract class AsyncTask extends UITask {
    /*
     * Control if objects passed to executeOnExecutor() are thread safe to
     * allow parallel handling on the UI and a worker thread.
     * 
     * Set this "false" to duplacate exactly the Android AsyncTask threading. This
     * will slow the worker thread execution fork until after onPreExecute()
     * has completed on the UI thread. In most cases, this is not needed and the
     * object can be queued to both the worker and UI threads simultaneously for
     * better performance.
     */

    private static volatile boolean agressiveThreading = true;

    /**
     * If tasks are called with execute(Params) then they will all execute in
     * guaranteed sequence on one Worker thread. It is generally better to use
     * executeOnExecutor(Params) instead unless
     */
    public static final int ASYNC_TASK_WORKER_INDEX = Worker.nextSerialWorkerIndex();
    /*
     * Note that an AsyncTask is stateful, including parameters. You can therefore
     * not fork() an AsyncTask instance more than once at a time. It is recommended
     * you clone a task if you want to queue it multiple times- this would also affect
     * cancelletion logic ("which queued instance do you want to cancel?").
     */
    private volatile Object params = ""; // For default toString debug helper

    /**
     * Run for complete Android compatability, or if your AsyncTask is (!) not
     * thread safe.
     */
    public static void disableAgressiveThreading() {
        AsyncTask.agressiveThreading = false;
    }
    
    /**
     * For compatability with Android, run one Runnable task on a single
     * background thread. Execution is guaranteed to be in the same order
     * objects are queued.
     *
     * NOTE: This Android use of "Runnable" is not consistent with the Tantalum
     * standard the "Workable.exec()" is performed on a background Worker thread
     * and "Runnable.run()" is performed on the EDT.
     *
     * @param runnable
     */
    public static void execute(final Runnable runnable) {
        Worker.forkSerial(new Workable() {
            public Object exec(final Object in) {
                runnable.run();
                
                return in;
            }
        }, ASYNC_TASK_WORKER_INDEX);
    }

    /**
     * Run the async task on a single background thread. Note that this may be
     * slower than executeOnExecutor() but is preferred if you require execution
     * in the order in which execute() is called, or if execute() is not
     * thread-safe such that multiple execute() calls can not run in parallel.
     *
     * @param params
     * @return
     */
    public final AsyncTask execute(final Object params) {
        this.params = params;
        synchronized (this) {
            if (status <= EXEC_FINISHED) {
                throw new IllegalStateException("AsyncTask can not be started, wait for previous exec to complete: status=" + getStatusString());
            }
            setStatus(EXEC_PENDING);
        }
        PlatformUtils.runOnUiThread(new Runnable() {
            public void run() {
                onPreExecute();
                Worker.forkSerial(AsyncTask.this, ASYNC_TASK_WORKER_INDEX);
            }
        });

        return this;
    }

    /**
     * Run the async task on a background thread pool, using the next available
     * thread. Execution order is not guaranteed, but this may return results
     * faster than execute() due to multi-core processors and concurrence
     * benefits during blocking IO calls.
     *
     * This method must be invoked on the UI thread
     *
     * @param params
     * @return
     */
    public final AsyncTask executeOnExecutor(final Object params) {
        synchronized (this) {
            if (status <= EXEC_FINISHED) {
                throw new IllegalStateException("AsyncTask can not be started, wait for previous exec to complete: status=" + getStatusString());
            }
            setStatus(EXEC_PENDING);
        }

        final boolean agressive = AsyncTask.agressiveThreading;
        this.params = params;

        PlatformUtils.runOnUiThread(
                new Runnable() {
                    public void run() {
                        onPreExecute();
                        if (!agressive) {
                            /*
                             * This is the sequence Android uses, but it is slow
                             * so by default agressive = true and the task is
                             * queued to both worker and UI thread simultaneously
                             */
                            AsyncTask.this.fork();
                        }
                    }
                });
        if (agressive) {
            AsyncTask.this.fork();
        }

        return this;
    }

    /**
     * Override if needed
     *
     */
    protected void onPreExecute() {
    }

    /**
     * Call this from any thread to initiate a call toe onProgressUpdate() on
     * the UI thread
     *
     * @param progress
     */
    protected void publishProgress(final Object progress) {
        PlatformUtils.runOnUiThread(new Runnable() {
            public void run() {
                onProgressUpdate(progress);
            }
        });
    }

    /**
     * This is executed on the UI thread
     *
     * Override if needed and use the volatile variable "progress"
     * 
     * @param progress 
     */
    protected void onProgressUpdate(final Object progress) {
    }

    /**
     * This is executed on the UI thread
     *
     * Override if needed
     * 
     * @param result 
     */
    protected void onPostExecute(Object result) {
    }

    /**
     * Debug helper, override for more specific debug info if needed
     *
     * @return
     */
    public String toString() {
        return this.getClass().getName() + ", AsyncTask params: " + params.toString();
    }
}
