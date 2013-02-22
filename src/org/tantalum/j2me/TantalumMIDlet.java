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

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.tantalum.PlatformUtils;
import org.tantalum.Worker;

/**
 * This is a convenience class to embody the best practice patterns for starting
 * and stopping an app which uses Tantalum. Using this class is optional, but
 * makes your life simpler. You can either extend it, or implement your own
 * variant.
 *
 * @author phou
 */
public abstract class TantalumMIDlet extends MIDlet {

    /**
     * If you create a MIDlet constructor, you must call super() as the first
     * line of your MIDlet's constructor. Alternatively, you can call
     * Worker.init() yourself with custom parameters for special needs.
     *
     */
    protected TantalumMIDlet() {
        super();

        PlatformUtils.setProgram(this);
    }

    /**
     * Call this to close your MIDlet in an orderly manner, exactly the same way
     * it is closed if the system sends you a destoryApp().
     *
     * Ongoing Work tasks will complete, or if you set unconditional then they
     * will complete within 3 seconds.
     *
     * @param unconditional
     */
    public void shutdown(final boolean unconditional) {
        Worker.shutdown(unconditional);
    }

    /**
     * Do not call this directly. Call exitMidlet(false) instead.
     *
     * This is part of MIDlet lifecycle model and may be called by the platform,
     * for example when a users presses and holds the RED button on their phone
     * to force an application to close.
     *
     * @param unconditional
     */
    protected final void destroyApp(final boolean unconditional) {
        shutdown(unconditional);
    }

    /**
     * Do nothing, not generally used
     *
     */
    protected void pauseApp() {
    }
}
