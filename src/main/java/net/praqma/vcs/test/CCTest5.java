package net.praqma.vcs.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import net.praqma.clearcase.Cool;
import net.praqma.clearcase.PVob;
import net.praqma.clearcase.Vob;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Project;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCM;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.util.Utilities;
import net.praqma.util.debug.PraqmaLogger;
import net.praqma.util.debug.PraqmaLogger.Logger;
import net.praqma.vcs.AVA;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.clearcase.ClearcaseBranch;
import net.praqma.vcs.model.clearcase.ClearcaseReplay;
import net.praqma.vcs.model.clearcase.ClearcaseVCS;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.model.exceptions.OperationNotImplementedException;
import net.praqma.vcs.model.exceptions.OperationNotSupportedException;
import net.praqma.vcs.model.exceptions.UnableToPerformException;
import net.praqma.vcs.model.exceptions.UnableToReplayException;
import net.praqma.vcs.model.git.GitBranch;

public class CCTest5 {

	static net.praqma.util.debug.Logger logger = net.praqma.util.debug.Logger.getLogger();
	
	public static void main( String[] args ) throws UnableToPerformException, URISyntaxException, OperationNotSupportedException, MalformedURLException, OperationNotImplementedException, UnableToReplayException, UCMException, ElementDoesNotExistException, ElementNotCreatedException {
		
		if( args.length < 4 ) {
			System.err.println( "Too few parameters" );
			System.err.println( "Usage: CCTest5 <vobname> <componentname> <projectname> <viewroot>" );
			System.exit( 1 );
		}
		
		logger.toStdOut( true );
		new AVA();
		
		/* Do the ClearCase thing... */
		UCM.setContext( UCM.ContextType.CLEARTOOL );
		
		/* Setup the logger */
        Logger logger2 = PraqmaLogger.getLogger(false);
        logger2.subscribeAll();
        logger2.setLocalLog( new File( "gittest.log") );
        Cool.setLogger(logger2);
		
		logger.toStdOut( true );
		
		String vname = args[0];
		String cname = args[1];
		String projectName = args[2];
		String streamName = projectName + "_int";
		File path = new File( args[3] );
		
		PVob pvob = ClearcaseVCS.bootstrap();

		ClearcaseVCS cc = new ClearcaseVCS( null, vname, cname, projectName, streamName, 
                Project.POLICY_INTERPROJECT_DELIVER  | 
                Project.POLICY_CHSTREAM_UNRESTRICTED | 
                Project.POLICY_DELIVER_NCO_DEVSTR, pvob );
		
		cc.get(true);
		logger.info( "Clearcase initialized" );
		
		ClearcaseBranch ccbranch = new ClearcaseBranch( cc, cc.getLastCreatedVob(), cc.getIntegrationStream(), cc.getInitialBaseline(), new File( path, projectName + "_1" ), projectName + "_1_view", projectName + "_1_dev" );
		ccbranch.get();
		ccbranch.getCommits(true);
                                                                   
	}

}