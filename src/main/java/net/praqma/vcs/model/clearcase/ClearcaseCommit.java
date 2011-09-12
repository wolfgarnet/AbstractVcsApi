package net.praqma.vcs.model.clearcase;

import java.io.File;
import java.util.List;

import net.praqma.clearcase.changeset.ChangeSet2;
import net.praqma.clearcase.changeset.ChangeSetElement2;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.util.debug.Logger;
import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.ChangeSetElement;
import net.praqma.vcs.model.ChangeSetElement.Status;

public class ClearcaseCommit extends AbstractCommit {
	
	private Logger logger = Logger.getLogger();
	private Baseline baseline;
	private ClearcaseBranch ccbranch;

	public ClearcaseCommit( Baseline baseline, ClearcaseBranch branch, int number ) {
		super( baseline.getFullyQualifiedName(), branch, number );
		
		this.ccbranch = branch;
		this.baseline = baseline;
	}
	
	public void instantiate( Baseline baseline, AbstractBranch branch, int number ) {
		
	}

	public void load() {
		LoadImpl load = new LoadImpl();
		doLoad( load );
	}
	
	public class LoadImpl extends Load {
		
		public LoadImpl() {
			super();
		}

		public boolean perform() {
			logger.debug( "CC: perform load" );

			try {
				ClearcaseCommit.this.parentKey = null;
				ClearcaseCommit.this.author = baseline.getUser();
				ClearcaseCommit.this.committer = baseline.getUser();
				ClearcaseCommit.this.authorDate = baseline.getDate();
				ClearcaseCommit.this.committerDate = baseline.getDate();
	
				ClearcaseCommit.this.title = ( baseline.getComment() != null ? baseline.getComment() : baseline.getFullyQualifiedName() );
				//List<Version> versions = baseline.beforeBaselineDifferences( null, ccbranch.getSnapshotView() );
				ChangeSet2 changeset = ChangeSet2.getChangeSet( baseline, null, ccbranch.getSnapshotView().getViewRoot() );
				
				logger.debug( "Changeset for " + ClearcaseCommit.this.baseline.getShortname() );
				
				/*
				for( ChangeSetElement2 e : changeset.getElementsAsList() ) {
					logger.debug( " " + e.getFile() + " " + e.getStatus() + ( e.getOldFile() != null ? " (Moved)" : "" ) );
				}
				*/
				
				List<ChangeSetElement2> elements = changeset.getElementsAsList();
				
				int length = ccbranch.getSnapshotView().getViewRoot().getAbsoluteFile().toString().length();
				
				for( ChangeSetElement2 element : elements ) {
					
					/* Plain change */
					if( element.getStatus().equals( net.praqma.clearcase.ucm.entities.Version.Status.CHANGED ) ) {
						ChangeSetElement cse = new ChangeSetElement( new File( element.getFile().getAbsoluteFile().toString().substring( length ) ), Status.CHANGED );
						if( element.getOldFile() != null ) {
							cse.setRenameFromFile( element.getOldFile() );
							cse.setStatus( Status.RENAMED );
						}
						logger.debug(element.getFile() + " " + cse.getStatus() );
						ClearcaseCommit.this.changeSet.put( element.getFile().toString(), cse );
						continue;
					}
					
					/* Added */
					if( element.getStatus().equals( net.praqma.clearcase.ucm.entities.Version.Status.ADDED ) ) {
						ChangeSetElement cse = new ChangeSetElement( new File( element.getFile().getAbsoluteFile().toString().substring( length ) ), Status.CREATED );
						if( element.getOldFile() != null ) {
							cse.setRenameFromFile( element.getOldFile() );
							cse.setStatus( Status.RENAMED );
						}
						logger.debug(element.getFile() + " " + cse.getStatus() );
						ClearcaseCommit.this.changeSet.put( element.getFile().toString(), cse );
						continue;
					}
					
					/* Deleted */
					if( element.getStatus().equals( net.praqma.clearcase.ucm.entities.Version.Status.DELETED ) ) {
						ChangeSetElement cse = new ChangeSetElement( new File( element.getFile().getAbsoluteFile().toString().substring( length ) ), Status.DELETED );
						logger.debug(element.getFile() + " " + cse.getStatus() );
						ClearcaseCommit.this.changeSet.put( element.getFile().toString(), cse );
						continue;
					}
				}
					
			} catch( UCMException e ) {
				logger.warning( "Could not get differences: " + e.getMessage() );
			}
			
			return true;
		}
	}
	
	public Baseline getBaseline() {
		return baseline;
	}

}
