package mpo.dayon.common.concurrent;

import mpo.dayon.common.error.FatalErrorHandler;
import mpo.dayon.common.log.Log;

import javax.net.ssl.SSLException;
import java.net.SocketException;

public abstract class RunnableEx implements Runnable {
	protected RunnableEx() {
	}

	@Override
    public final void run() {
		try {
			doRun();
		} catch (SocketException | SSLException ex) {
			Log.error(ex.getMessage());
		} catch (Exception ex) {
			FatalErrorHandler.bye("The [" + Thread.currentThread().getName() + "] thread is dead!", ex);
		}
	}

	protected abstract void doRun() throws Exception;
}
