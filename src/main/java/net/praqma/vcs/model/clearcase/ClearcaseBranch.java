package net.praqma.vcs.model.clearcase;

import java.io.File;

import net.praqma.clearcase.PVob;
import net.praqma.clearcase.Vob;
import net.praqma.clearcase.ucm.UCMException;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Component;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.UCMEntity;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.clearcase.ucm.view.UCMView;
import net.praqma.clearcase.ucm.view.SnapshotView.COMP;
import net.praqma.util.debug.Logger;
import net.praqma.vcs.model.AbstractBranch;
import net.praqma.vcs.model.AbstractCommit;
import net.praqma.vcs.model.exceptions.ElementDoesNotExistException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException;
import net.praqma.vcs.model.exceptions.ElementNotCreatedException.FailureType;

public class ClearcaseBranch extends AbstractBranch{

	private File viewroot;
	private File developmentPath;
	private Baseline baseline;
	private String viewtag;
	private Stream parent;
	
	private Vob vob;
	private ClearcaseVCS ccVCS;
	
	private Stream devStream;
	private SnapshotView snapshot;
	
	private Component component;
	
	private static Logger logger = Logger.getLogger();
	
	public ClearcaseBranch( ClearcaseVCS ccVCS, Vob vob, Stream parent, Baseline baseline, File viewroot, String viewtag, String name ) throws ElementNotCreatedException {
		this.viewroot = viewroot;
		this.viewtag = viewtag;
		this.baseline = baseline;
		this.name = name;
		this.parent = parent;
		
		this.vob = vob;
		this.ccVCS = ccVCS;
		
		try {
			this.component = baseline.getComponent();
		} catch (UCMException e) {
			logger.error( "Could not create Clearcase branch: " + e.getMessage() );
			throw new ElementNotCreatedException( "Could not create Clearcase branch: " + e.getMessage(), FailureType.DEPENDENCY );
		}
		
		this.developmentPath = new File( viewroot, vob + "/" + this.component.getShortname() );
		
		File view = new File( viewroot, vob.toString() );
		this.localRepositoryPath = view;
	}

	/**
	 * Create and initializes a Clearcase branch(stream) and a corresponding view(not updated).
	 * @param vob {@link Vob}
	 * @param pvob {@link PVob}
	 * @param parent The parent {@link Stream} 
	 * @param baseline The {@link Baseline} the branch is initialized from
	 * @param viewroot The view root as a {@link File}
	 * @param viewtag The view tag of the {@link SnapshotView}
	 * @param name The name of the {@link Stream} given as a basename.
	 * @return
	 * @throws ElementNotCreatedException
	 */
	public static ClearcaseBranch create( ClearcaseVCS ccVCS, Vob vob, Stream parent, Baseline baseline, File viewroot, String viewtag, String name ) throws ElementNotCreatedException {
		ClearcaseBranch branch = new ClearcaseBranch( ccVCS, vob, parent, baseline, viewroot, viewtag, name );
		branch.initialize();
		//branch.get();
		return branch;
	}
	
	
	
	@Override
	public boolean initialize() {
		return initialize(false);
	}
	
	public boolean initialize( boolean get ) {
		logger.info( "Creating Clearcase branch/stream " + name );
		return doInitialize( new InitializeImpl( get ) );
	}
	
	private class InitializeImpl extends Initialize {
		public InitializeImpl( boolean get ) {
			super( get );
		}

		public boolean initialize() {

			try {
				logger.info( "Creating development stream"  );
				devStream = Stream.create( parent, name + "@" + ccVCS.getPVob(), false, baseline );
			} catch (UCMException e) {
				if( get ) {
					try {
						devStream = UCMEntity.getStream( name, ccVCS.getPVob(), false );
						logger.info( "Stream already exists" );
					} catch (UCMException e1) {
						logger.error( "Could not find stream: " + e.getMessage() );
						return false;
					}
				} else {
					logger.error("Error while creating Development Stream: " + e.getMessage());
					return false;
				}
			}
			
			try {
				logger.info( "Creating development view" );
				viewroot.mkdirs();
				snapshot = SnapshotView.Create( devStream, viewroot, viewtag );
			} catch (UCMException e) {
				if( get ) {
					try {
						snapshot = UCMView.GetSnapshotView(viewroot);
						logger.info( "View already exists" );
					} catch (Exception e1) {
						logger.error( "Could not find view: " + e.getMessage() );
						/* try to generate new */
						try {
							snapshot = SnapshotView.Create( devStream, viewroot, viewtag + System.currentTimeMillis() );
						} catch (UCMException e2) {
							logger.error( "Could not generate new view: " + e2.getMessage() );
							return false;
						}
						
					}
				} else {
					logger.error("Error while creating Snapshot View: " + e.getMessage());
					return false;
				}
			}
			
			return true;
		}
		
		public boolean cleanup( boolean status ) {
			return true;
		}
	}
	
	public boolean get() throws ElementDoesNotExistException {
		//ClearcaseBranch branch = new ClearcaseBranch( vob, pvob, parent, baseline, viewroot, viewtag, name );
		
		boolean exists = true;
		try{
			UCMEntity.getStream( name, ccVCS.getPVob(), false );
		} catch( UCMException e ) {
			logger.debug( "Stream did not exist" );
			exists = false;
		}
		
		if( !UCMView.ViewExists( viewtag ) ) {
			logger.debug( "View did not exist" );
			exists = false;
		/* The view exists, but is it the correct path? */
		} else {
			if( !viewroot.exists() ) {
				exists = false;
			}
		}
		
		if( !exists ) {
			logger.debug( "Must initialize" );
			return initialize(true);
		} else {
			logger.debug( "DONT initialize" );
			try {
				snapshot = UCMView.GetSnapshotView(viewroot);
			} catch (UCMException e) {
				logger.error( "Could not get view: " + e.getMessage() );
				throw new ElementDoesNotExistException( "Could not get clearcase view" );
			}
		}
		
		return true;
	}
	
	
	public void checkout() {
		doCheckout( new CheckoutImpl(null) );
	}
	
	@Override
	public void checkout( AbstractCommit commit ) {
		doCheckout( new CheckoutImpl(commit) );
	}
	
	public class CheckoutImpl extends Checkout {
		public CheckoutImpl( AbstractCommit commit ) {
			super( commit );
		}

		public boolean setup() {
			if( snapshot == null || devStream == null ) {
				try {
					get();
				} catch (ElementDoesNotExistException e) {
		        	logger.error("Error while getting stream: " + e.getMessage());
		        	return false;
				}
			}
			
			return true;
		}
		
		public boolean checkout() {
			try {
				snapshot.Update( true, true, true, false, COMP.MODIFIABLE, null );
			} catch (UCMException e) {
	        	logger.error("Error while updating view: " + e.getMessage());
	        	return false;
			}
			
			return true;
		}
		
		public boolean cleanup( boolean status ) {
			/* TODO something useful */
			return status;
		}
	}
	
	public SnapshotView getSnapshotView() {
		return snapshot;
	}
	
	public PVob getPVob() {
		return ccVCS.getPVob();
	}
	
	public Vob getVob() {
		return vob;
	}
	
	public Baseline getBaseline() {
		return baseline;
	}
	
	public File getDevelopmentPath() {
		return this.developmentPath;
	}
	
	public Component getComponent() {
		return this.component;
	}

}
