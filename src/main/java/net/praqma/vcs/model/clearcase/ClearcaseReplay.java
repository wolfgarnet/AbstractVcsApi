package net.praqma.vcs.model.clearcase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.exceptions.UnableToReplayException;
import net.praqma.util.debug.Logger;
import net.praqma.vcs.clearcase.listeners.ClearcaseReplayListener;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.AbstractReplay;
import net.praqma.vcs.model.ChangeSetElement;
import net.praqma.vcs.model.ChangeSetElement.Status;

public class ClearcaseReplay extends AbstractReplay {
	
	private ClearcaseBranch ccBranch;
	
	private Logger logger = Logger.getLogger();

	public ClearcaseReplay( ClearcaseBranch branch ) {
		super( branch );
		
		this.ccBranch = branch;
	}
	
	public void setBranch( ClearcaseBranch branch ) {
		super.setBranch( branch );
		this.ccBranch = branch;
	}

	@Override
	public void replay( AbstractCommit commit ) throws UnableToReplayException {
		doReplay( new ReplayImpl( commit ) );
	}
	
	public class ReplayImpl extends Replay{
		public ReplayImpl( AbstractCommit commit ) {
			super( commit );
		}

		public boolean setup() {
			try {
				Activity.create( null, ccBranch.getPVob(), true, "CCReplay: " + commit.getKey(), ccBranch.getSnapshotView().GetViewRoot() );
			} catch (UCMException e1) {
				logger.error( "ClearCase Activity could not be created: " + e1.getMessage() );
				return false;
			}
			
			try {
				Version.checkOut( ccBranch.getDevelopmentPath(), ccBranch.getDevelopmentPath() );
			} catch (UCMException e1) {
				logger.error( "ClearCase could not checkout path " + ccBranch.getDevelopmentPath() + ": " + e1.getMessage() );
				return false;
			}
			
			return true;
		}
		
		public boolean replay() {
			List<ChangeSetElement> cs = commit.getChangeSet().asList();
			
			boolean success = true;
			
			for( ChangeSetElement cse : cs ) {
				File file = new File( ccBranch.getDevelopmentPath(), cse.getFile().getPath() );
				logger.debug( "File(" + cse.getStatus() + "): " + file );
				
				Version version = null;
				if( !file.exists() ) {
					try {
						version = Version.create( file, ccBranch.getSnapshotView() );
					} catch (UCMException e1) {
						logger.error( "ClearCasecould not create version: " + e1.getMessage() );
						success = false;						
						continue;
					}
				} else {
					try {
						version = Version.getUnextendedVersion( file, ccBranch.getDevelopmentPath() );
					} catch (UCMException e1) {
						logger.error( "ClearCase could not get version: " + e1.getMessage() );
						success = false;
						continue;
					}
				}
				
				if( cse.getStatus() != Status.DELETED) {
					logger.debug( "Writing" );
						
					PrintStream ps;
					try {
						ps = new PrintStream( new BufferedOutputStream(new FileOutputStream(version.getVersion(), true) ) );
						ps.println( commit.getKey() + " - " + commit.getAuthorDate() );
						ps.close();
					} catch (FileNotFoundException e) {
						success = false;
						logger.error( e );
					}
				} else {
					try {
						version.remove();
					} catch (UCMException e1) {
						logger.error( "ClearCase could not remove version: " + e1.getMessage() );
						success = false;
						continue;
					}
				}
			}
			
			return success;
		}
		
		public boolean cleanup( boolean status ) {
			logger.debug( "Cleaning up Clearcase" );
			
			boolean success = true;
			
			try {
				Version.checkIn( ccBranch.getDevelopmentPath(), ccBranch.getDevelopmentPath() );
			} catch (UCMException e1) {
				logger.error( "ClearCase could not checkin: " + e1.getMessage() );
				success = false;
			}
			
			String baselineName = ClearcaseReplayListener.runSelectBaselineName( commit );
			
			try {
				Baseline.create( baselineName, ccBranch.getComponent(), ccBranch.getDevelopmentPath(), true, true );
			} catch (UCMException e1) {
				logger.error( "ClearCase could not create baseline: " + e1.getMessage() );
				success = false;
			}
			
			return success;
		}
	}
	
}
