package net.praqma.vcs.util.configuration.implementation;

import java.io.File;

import net.praqma.clearcase.PVob;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.util.debug.Logger;
import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractReplay;
import net.praqma.vcs.model.clearcase.ClearcaseBranch;
import net.praqma.vcs.model.clearcase.ClearcaseReplay;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.model.exceptions.UnsupportedBranchException;
import net.praqma.vcs.util.configuration.AbstractConfiguration;
import net.praqma.vcs.util.configuration.exception.ConfigurationException;

public class ClearCaseConfiguration extends AbstractConfiguration {

	private static final long serialVersionUID = -7104484491917047522L;
	private static Logger logger = Logger.getLogger();
	
	/**/
	
	private String viewtagIn;
	private String viewtagOut;
	private String streamNameIn;
	private String streamNameOut;
	private String pathNameOut;
	private String pvobName;
	private String foundationBaselineName;
	private String parentStreamName;
	
	/* Generated */
	
	transient private PVob pvob;
	transient private Stream parentStream;
	transient private Baseline foundationBaseline;
	transient private File inputPath;
	transient private File outputPath;
	
	public ClearCaseConfiguration( String pathName, String viewtag, String pvobName, String foundationBaselineName, String parentStreamName, String streamName ) throws ConfigurationException {
		super( pathName );

		this.pvobName = pvobName;
		this.viewtagIn = viewtag;
		this.foundationBaselineName = foundationBaselineName;
		this.streamNameIn = streamName;
		this.parentStreamName = parentStreamName;
	}
	
	public ClearCaseConfiguration( String pathNameIn, String viewtagIn, String streamNameIn, String pathNameOut, String viewtagOut, String streamNameOut, String pvobName, String foundationBaselineName, String parentStreamName ) throws ConfigurationException {
		super( pathNameIn );
		
		this.pathNameOut = pathNameOut;
		this.viewtagIn = viewtagIn;
		this.viewtagOut = viewtagOut;		
		this.streamNameOut = streamNameOut;
		this.streamNameIn = streamNameIn;

		this.pvobName = pvobName;
		
		this.foundationBaselineName = foundationBaselineName;
		
		this.parentStreamName = parentStreamName;
	}
	
	public ClearCaseConfiguration( File path, String viewtag, PVob pvob, Baseline baseline, Stream parentStream, Stream stream ) throws ConfigurationException {
		super( path );

		this.pvob = pvob;

		this.viewtagIn = viewtag;
		this.foundationBaseline = baseline;
		this.streamNameIn = stream.getShortname();
		this.parentStream = parentStream;
	}
	
	@Override
	public void generate() throws ConfigurationException {
		super.generate();
		
		logger.debug( "Creating pvob " + pvobName );
		
		/* Generate the pvob */
		this.pvob = PVob.get( pvobName );
		if( this.pvob == null ) {
			throw new ConfigurationException( "PVob " + pvobName + " does not exist" );
		}
		
		logger.debug( "Setting foundation baseline " + foundationBaselineName );
		
		/* Foundation baseline */
		setFoundationBaseline( foundationBaselineName );
		
		logger.debug( "Setting parent stream " + parentStreamName );
		
		/* Parent stream */
		setParentStream( parentStreamName );
	}

	public void setParentStream( String stream ) throws ConfigurationException {
		if( stream != null && stream.length() > 0 ) {
			try {
				parentStream = UCMEntity.getStream( stream, pvob, false );
			} catch (UCMException e) {
				throw new ConfigurationException( "Could not get parent stream: " + e.getMessage() );
			}
		} else {
			/* No parent */
			parentStream = null;
		}
	}
	
	public void setFoundationBaseline( String baseline ) throws ConfigurationException {
		try {
			foundationBaseline = UCMEntity.getBaseline( baseline, pvob, false );
		} catch (UCMException e) {
			/* Not that important */
		}
	}

	public Stream getParentStream() {
		return parentStream;
	}

	public void setStreamName( String streamName ) {
		this.streamNameIn = streamName;
	}

	public String getStreamName() {
		return streamNameIn;
	}

	public Baseline getFoundationBaseline() {
		return foundationBaseline;
	}

	public PVob getPVob() {
		return pvob;
	}

	public String getViewtag() {
		return viewtagIn;
	}
	
	public void setInputPath( File path ) {
		this.inputPath = path;
	}
	
	public void setOutputPath( File path ) {
		this.outputPath = path;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append( "Parent: " + super.toString() );
		
		sb.append( "Parent stream: " + parentStream + "\n" );
		sb.append( "PVob: " + pvob + "\n" );
		
		sb.append( "INPUT:\n" );
		sb.append( "View : " + pathName + "\n" );
		sb.append( "View tag: " + viewtagIn + "\n" );
		sb.append( "Stream name: " + streamNameIn + "\n" );
		
		sb.append( "OUTPUT:\n" );
		sb.append( "View : " + pathNameOut + "\n" );
		sb.append( "View tag: " + viewtagOut + "\n" );
		sb.append( "Stream name: " + streamNameOut + "\n" );
		
		
		return sb.toString();
	}

	@Override
	public AbstractBranch getBranch() throws ElementNotCreatedException, ElementDoesNotExistException {
		if( branch == null ) {
			ClearcaseBranch branch = null;
			if( streamNameOut == null || viewtagOut == null || pathNameOut == null ) {
				branch = new ClearcaseBranch( pvob, parentStream, foundationBaseline, path, viewtagIn, streamNameIn );
			} else {
				branch = new ClearcaseBranch( pvob, parentStream, null, foundationBaseline, path, new File( pathNameOut ), viewtagIn, viewtagOut, streamNameIn, streamNameOut );
			}
			/* Set input path */
			if( inputPath != null ) {
				branch.setInputPath( inputPath );
			}
			
			/* Set output path */
			if( outputPath != null ) {
				branch.setOutputPath( outputPath );
			}
			
			branch.get(true);
			this.branch = branch;
		}
		return branch;
	}

	@Override
	public AbstractReplay getReplay() throws UnsupportedBranchException, ElementNotCreatedException, ElementDoesNotExistException {
		return new ClearcaseReplay( getBranch() );
	}



}
