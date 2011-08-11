package net.praqma.vcs.test;

import net.praqma.util.debug.Logger;
import net.praqma.util.debug.Logger.LogLevel;
import net.praqma.vcs.OpenVCS;
import net.praqma.vcs.model.extensions.PullListener;
import net.praqma.vcs.model.extensions.PullListenerImpl;

public class ExtentionTest {

	static Logger logger = Logger.getLogger();
	
	public static void main( String[] args ) {
		new OpenVCS();
		OpenVCS.getInstance().registerExtension( "Test", new PullListenerImpl() );
		logger.setMinLogLevel( LogLevel.INFO );
		logger.log( "hej", LogLevel.DEBUG );
		logger.log( "hej" );
		PullListener.runPrePullListener();
	}
	
}