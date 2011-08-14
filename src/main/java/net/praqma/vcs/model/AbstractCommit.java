package net.praqma.vcs.model;

import java.util.Date;

import net.praqma.util.debug.Logger;
import net.praqma.vcs.model.exceptions.OperationNotImplementedException;

public abstract class AbstractCommit {
	
	protected String key;
	protected String parentKey;
	protected String title;
	
	protected String author;
	protected Date authorDate;
	
	protected String committer;
	protected Date committerDate;
	
	protected int number = -1;
	
	private Logger logger = Logger.getLogger();
	
	//protected List<ChangeSetElement> changeSet = new ArrayList<ChangeSetElement>();
	protected ChangeSet changeSet = new ChangeSet();
	
	protected AbstractBranch branch;
	
	public AbstractCommit( String key, AbstractBranch branch ) {
		this.key = key;
		this.branch = branch;
	}
	
	public AbstractCommit( String key, AbstractBranch branch, int number ) {
		this.key = key;
		this.branch = branch;
		this.number = number;
	}
	
	public void load() throws OperationNotImplementedException {
		throw new OperationNotImplementedException( "load" );
	}
	
	protected void doLoad( Load load ) {
		boolean status = load.preLoad();
		
		/* Only perform if pre step went good */
		if( status ) {
			status = load.perform();
		}
		
		load.postLoad( status );
	}
	
	public abstract class Load {
		public boolean preLoad() {
			return true;
		}
		
		public boolean perform() {
			return true;
		}
		
		public boolean postLoad( boolean status ) {
			return true;
		}
	}
	
	public AbstractBranch getBranch() {
		return branch;
	}
	
	public ChangeSet getChangeSet() {
		return this.changeSet;
	}
	
	public String getKey() {
		return key;
	}
	
	public Date getAuthorDate() {
		return authorDate;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setNumber( int i ) {
		this.number = i;
	}
	
	public int getNumber() {
		return this.number;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();

		if( title != null ) {
			sb.append( " -- " + title + " --\n" );
			sb.append( " " + authorDate + "\n" );
		}
		
		sb.append( "Key: " + key + "\n" );
		
		for(ChangeSetElement cs : changeSet.asList()) {
			sb.append( " * " + cs.getFile() + "\n" );
		}
		
		return sb.toString();
	}	
}